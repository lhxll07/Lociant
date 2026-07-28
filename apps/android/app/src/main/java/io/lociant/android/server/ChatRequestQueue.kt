package io.lociant.android.server

import io.lociant.core.config.RuntimeDefaults
import io.lociant.core.model.ModelChatResult
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

data class StreamJob(val id: String, val future: CompletableFuture<ModelChatResult>)

/**
 * Thread-safe async inference queue with a single worker thread,
 * prioritised execution, job cancellation, and status reporting.
 */
class ChatRequestQueue(
    private val maxQueuedRequests: Int = RuntimeDefaults.Queue.MAX_QUEUED_REQUESTS,
) {
    private val queue = LinkedBlockingQueue<Job>()
    private val tasks = ConcurrentHashMap<String, Task>()
    private val runningJobId = AtomicReference<String?>(null)
    @Volatile private var closed = false

    private val worker = Thread(::runLoop, "lociant-inference").apply {
        isDaemon = true
        start()
    }

    fun submit(modelId: String, source: String, work: () -> ModelChatResult): String =
        newJob(modelId, source, JobKind.CHAT, CompletableFuture(), work).also { enqueue(it) }.id

    fun submitSync(
        modelId: String,
        source: String,
        timeoutMs: Long,
        work: () -> ModelChatResult,
    ): ModelChatResult {
        val future = CompletableFuture<ModelChatResult>()
        val job = newJob(modelId, source, JobKind.CHAT, future, work)
        if (!enqueue(job)) return job.task.result ?: rejectedResult(modelId)
        return runCatching { future.get(timeoutMs, TimeUnit.MILLISECONDS) }.getOrElse { error ->
            ModelChatResult(ok = false, modelId = modelId, message = error.message ?: "chat timed out")
        }
    }

    fun submitStream(
        modelId: String,
        source: String,
        onChunk: (String, Boolean) -> Unit,
        work: () -> ModelChatResult,
    ): StreamJob {
        val future = CompletableFuture<ModelChatResult>()
        val job = newJob(modelId, source, JobKind.STREAM, future, work, onChunk)
        if (!enqueue(job)) onChunk("", true)
        return StreamJob(job.id, future)
    }

    fun submitControl(modelId: String, source: String, work: () -> ModelChatResult): String =
        newJob(modelId, source, JobKind.CONTROL, CompletableFuture(), work).also { enqueue(it) }.id

    fun statusOf(requestId: String): JSONObject {
        return tasks[requestId]?.toJson(positionOf(requestId), runningJobId.get() == requestId)
            ?: JSONObject().put("error", "request not found").put("id", requestId)
    }

    fun snapshot(): JSONObject {
        val pendingIds = queue.map { it.id }
        val requests = JSONArray()
        tasks.values.sortedByDescending { it.createdAt }.forEach { task ->
            val position = pendingIds.indexOf(task.id).takeIf { it >= 0 }?.plus(1) ?: 0
            requests.put(task.toJson(position, runningJobId.get() == task.id))
        }
        val runningId = runningJobId.get()
        return JSONObject()
            .put("running", runningId ?: JSONObject.NULL)
            .put("pending", pendingIds.size)
            .put("maxQueuedRequests", maxQueuedRequests)
            .put("requests", requests)
    }

    fun shutdown() {
        closed = true
        worker.interrupt()
        queue.clear()
        tasks.clear()
    }

    fun cancel(requestId: String, reason: String, cancelRunning: () -> Unit): Boolean {
        val task = tasks[requestId] ?: return false
        if (task.status.get().terminal) return false
        val running = runningJobId.get() == requestId
        if (!running) {
            val queuedJob = queue.firstOrNull { it.id == requestId }
            if (queuedJob == null || !queue.remove(queuedJob)) return false
            val result = ModelChatResult(ok = false, modelId = task.modelId, message = reason)
            task.result = result
            task.completedAt = System.currentTimeMillis()
            task.status.set(TaskStatus.CANCELLED)
            queuedJob.future.complete(result)
            return true
        }
        if (task.status.compareAndSet(TaskStatus.RUNNING, TaskStatus.CANCELLING)) {
            cancelRunning()
        }
        return true
    }

    private fun enqueue(job: Job): Boolean {
        cleanup()
        if (closed || queue.size >= maxQueuedRequests) {
            val result = rejectedResult(job.modelId)
            job.task.result = result
            job.task.completedAt = System.currentTimeMillis()
            job.task.status.set(TaskStatus.REJECTED)
            tasks[job.id] = job.task
            job.future.complete(result)
            return false
        }
        tasks[job.id] = job.task
        queue.offer(job)
        return true
    }

    private fun cleanup(olderThanMs: Long = RuntimeDefaults.Queue.TASK_RETENTION_MS) {
        val cutoff = System.currentTimeMillis() - olderThanMs
        tasks.values.removeIf { task ->
            task.status.get().terminal && (task.completedAt ?: task.createdAt) < cutoff
        }
    }

    private fun runLoop() {
        while (!closed) {
            val job = try {
                queue.take()
            } catch (_: InterruptedException) {
                if (closed) break else continue
            }
            runJob(job)
        }
    }

    private fun runJob(job: Job) {
        runningJobId.set(job.id)
        job.task.startedAt = System.currentTimeMillis()
        job.task.status.set(TaskStatus.RUNNING)
        val result = runCatching { job.work() }.getOrElse { error ->
            ModelChatResult(ok = false, modelId = job.modelId, message = error.message ?: "chat failed")
        }
        job.task.result = result
        job.task.completedAt = System.currentTimeMillis()
        job.task.status.set(when {
            job.task.status.get() == TaskStatus.CANCELLING -> TaskStatus.CANCELLED
            result.ok -> TaskStatus.COMPLETED
            else -> TaskStatus.FAILED
        })
        if (job.kind == JobKind.STREAM && !result.ok) runCatching { job.onChunk?.invoke("", true) }
        job.future.complete(result)
        runningJobId.compareAndSet(job.id, null)
    }

    private fun newJob(
        modelId: String,
        source: String,
        kind: JobKind,
        future: CompletableFuture<ModelChatResult>,
        work: () -> ModelChatResult,
        onChunk: ((String, Boolean) -> Unit)? = null,
    ): Job {
        val id = UUID.randomUUID().toString().take(8)
        val task = Task(id, modelId, source, kind, System.currentTimeMillis(), AtomicReference(TaskStatus.QUEUED))
        return Job(id, modelId, kind, future, task, work, onChunk)
    }

    private fun positionOf(requestId: String): Int {
        return queue.map { it.id }.indexOf(requestId).takeIf { it >= 0 }?.plus(1) ?: 0
    }

    private fun rejectedResult(modelId: String): ModelChatResult {
        return ModelChatResult(ok = false, modelId = modelId, message = "chat queue is full")
    }

    private data class Job(
        val id: String,
        val modelId: String,
        val kind: JobKind,
        val future: CompletableFuture<ModelChatResult>,
        val task: Task,
        val work: () -> ModelChatResult,
        val onChunk: ((String, Boolean) -> Unit)? = null,
    )

    private class Task(
        val id: String,
        val modelId: String,
        val source: String,
        val kind: JobKind,
        val createdAt: Long,
        val status: AtomicReference<TaskStatus>,
        var startedAt: Long? = null,
        var completedAt: Long? = null,
        var result: ModelChatResult? = null,
    ) {
        fun toJson(position: Int, running: Boolean): JSONObject {
            return JSONObject()
                .put("id", id)
                .put("modelId", modelId)
                .put("source", source)
                .put("kind", kind.name.lowercase())
                .put("status", status.get().name.lowercase())
                .put("position", if (running) 0 else position)
                .put("createdAt", createdAt)
                .also { json -> startedAt?.let { json.put("startedAt", it) } }
                .also { json -> completedAt?.let { json.put("completedAt", it) } }
                .also { json -> result?.let { json.put("result", it.toJson()) } }
        }
    }

    private enum class JobKind { CHAT, STREAM, CONTROL }

    private enum class TaskStatus(val terminal: Boolean) {
        QUEUED(false),
        RUNNING(false),
        COMPLETED(true),
        FAILED(true),
        REJECTED(true),
        CANCELLING(false),
        CANCELLED(true),
    }
}
