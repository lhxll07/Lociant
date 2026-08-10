package io.lociant.android.server

import io.lociant.core.config.RuntimeDefaults
import io.lociant.core.model.ModelChatResult
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import java.util.concurrent.atomic.AtomicReference
import java.util.UUID

/**
 * Single-worker inference queue. Only the synchronous entry point is used by
 * the live device layer (`ChatController.submitSync`); the async/status
 * surface moved to the Rust backend's agent loop.
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

    fun submitSync(
        modelId: String,
        source: String,
        timeoutMs: Long,
        cancelRunning: () -> Unit = {},
        work: () -> ModelChatResult,
    ): ModelChatResult {
        val future = CompletableFuture<ModelChatResult>()
        val job = newJob(modelId, source, future, work)
        if (!enqueue(job)) return job.task.result ?: rejectedResult(modelId)
        return runCatching { future.get(timeoutMs, TimeUnit.MILLISECONDS) }.getOrElse { error ->
            if (error is TimeoutException) {
                cancelRunning()
            }
            ModelChatResult(ok = false, modelId = modelId, message = error.message ?: "chat timed out")
        }
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
            job.task.status.get() == TaskStatus.CANCELLED -> TaskStatus.CANCELLED
            result.ok -> TaskStatus.COMPLETED
            else -> TaskStatus.FAILED
        })
        job.future.complete(result)
        runningJobId.compareAndSet(job.id, null)
    }

    private fun newJob(
        modelId: String,
        source: String,
        future: CompletableFuture<ModelChatResult>,
        work: () -> ModelChatResult,
    ): Job {
        val id = UUID.randomUUID().toString().take(8)
        val task = Task(id, modelId, System.currentTimeMillis(), AtomicReference(TaskStatus.QUEUED))
        return Job(id, modelId, future, task, work)
    }

    private fun rejectedResult(modelId: String): ModelChatResult {
        return ModelChatResult(ok = false, modelId = modelId, message = "chat queue is full")
    }

    private data class Job(
        val id: String,
        val modelId: String,
        val future: CompletableFuture<ModelChatResult>,
        val task: Task,
        val work: () -> ModelChatResult,
    )

    private class Task(
        val id: String,
        val modelId: String,
        val createdAt: Long,
        val status: AtomicReference<TaskStatus>,
        var startedAt: Long? = null,
        var completedAt: Long? = null,
        var result: ModelChatResult? = null,
    )

    private enum class TaskStatus(val terminal: Boolean) {
        QUEUED(false),
        RUNNING(false),
        CANCELLED(true),
        REJECTED(true),
        COMPLETED(true),
        FAILED(true),
    }
}
