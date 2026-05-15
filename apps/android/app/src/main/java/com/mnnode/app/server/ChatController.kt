package com.mnnode.app.server

import android.util.Log
import com.mnnode.app.model.MnnRuntime
import com.mnnode.app.model.ModelManager
import com.mnnode.app.model.DEFAULT_MODEL_ID
import com.mnnode.app.model.HARD_MAX_OUTPUT_TOKENS
import com.mnnode.app.model.MIN_OUTPUT_TOKENS
import com.mnnode.app.model.ModelChatMessage
import com.mnnode.app.model.ModelChatPart
import com.mnnode.app.model.ModelChatRequest
import com.mnnode.app.model.ModelChatResult
import com.mnnode.app.session.SessionStore
import io.ktor.http.ContentType
import io.ktor.http.content.OutgoingContent
import io.ktor.utils.io.ByteWriteChannel
import io.ktor.utils.io.writeStringUtf8
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import java.util.UUID

class ChatController(
    private val modelManager: ModelManager,
    private val mnnRuntime: MnnRuntime,
    private val sessionStore: SessionStore,
) {
    private val modelLoading = AtomicBoolean(false)
    private val loadedModelId = AtomicReference<String?>(null)
    @Volatile var lastError: String? = null

    private val requestQueue = ChatRequestQueue()
    private val backgroundExecutor = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "mnnode-chat-bg").apply { isDaemon = true }
    }

    val isLoading: Boolean get() = modelLoading.get()
    fun isModelLoaded(modelId: String) = loadedModelId.get() == ModelManager.normalizeId(modelId)
    fun resetLoadedModel() = loadedModelId.set(null)

    // ---- Model resolution ----

    private fun resolveModel(modelIdRaw: String): File? {
        val id = ModelManager.normalizeId(modelIdRaw).ifBlank { DEFAULT_MODEL_ID }
        val status = modelManager.resolve(id)
        val dir = modelManager.resolveDir(id)
        if (dir != null && status.spec.runtime == "mnn") return dir
        Log.w(TAG, "model unavailable modelId=$id runtime=${status.spec.runtime}")
        return null
    }

    private fun installHint(modelIdRaw: String) = modelManager.installHint(
        ModelManager.normalizeId(modelIdRaw).ifBlank { DEFAULT_MODEL_ID })

    // ---- Token clamping ----

    fun clampTokens(modelIdRaw: String, requested: Int, serverCap: Int = HARD_MAX_OUTPUT_TOKENS): Int {
        val modelCap = modelManager.maxNewTokens(ModelManager.normalizeId(modelIdRaw).ifBlank { DEFAULT_MODEL_ID })
        val effective = minOf(serverCap, HARD_MAX_OUTPUT_TOKENS, modelCap ?: HARD_MAX_OUTPUT_TOKENS)
            .coerceAtLeast(MIN_OUTPUT_TOKENS)
        return requested.coerceIn(MIN_OUTPUT_TOKENS, effective)
    }

    fun effectiveMaxTokens(modelIdRaw: String, serverCap: Int = HARD_MAX_OUTPUT_TOKENS): Int {
        val modelCap = modelManager.maxNewTokens(ModelManager.normalizeId(modelIdRaw).ifBlank { DEFAULT_MODEL_ID })
        return minOf(serverCap, HARD_MAX_OUTPUT_TOKENS, modelCap ?: HARD_MAX_OUTPUT_TOKENS)
            .coerceAtLeast(MIN_OUTPUT_TOKENS)
    }

    // ---- Preload ----

    fun preload(modelId: String) {
        requestQueue.submitControl(ModelManager.normalizeId(modelId).ifBlank { DEFAULT_MODEL_ID }, "preload") {
            val normalized = ModelManager.normalizeId(modelId).ifBlank { DEFAULT_MODEL_ID }
            if (isModelLoaded(modelId)) {
                return@submitControl ModelChatResult(ok = true, modelId = normalized, message = "already loaded")
            }
            modelLoading.set(true)
            val result = runCatching {
                val dir = resolveModel(modelId) ?: return@submitControl ModelChatResult(
                    ok = false,
                    modelId = normalized,
                    message = "MNN chat model not installed",
                )
                val raw = mnnRuntime.preload(dir)
                if (raw.ok) loadedModelId.set(normalized)
                else lastError = raw.message
                raw
            }.onFailure { e ->
                lastError = e.message ?: "model preload failed"
                Log.w(TAG, "preload failed modelId=$modelId", e)
            }.getOrNull()
            modelLoading.set(false)
            ModelChatResult(
                ok = result?.ok == true,
                modelId = normalized,
                message = result?.message ?: lastError ?: "model preload failed",
            )
        }
    }

    // ---- Chat execution (non-streaming) ----

    private fun executeChat(request: ModelChatRequest): ModelChatResult {
        val started = System.currentTimeMillis()
        val modelId = ModelManager.normalizeId(request.modelId).ifBlank { DEFAULT_MODEL_ID }
        val imageCount = request.messages.sumOf { m -> m.parts.count { it is ModelChatPart.Image } }
        Log.i(TAG, "chat start source=${request.source} modelId=$modelId messages=${request.messages.size} images=$imageCount maxTokens=${request.maxTokens}")

        val dir = resolveModel(modelId)
            ?: return ModelChatResult(ok = false, modelId = modelId,
                message = "MNN chat model not installed",
                elapsedMs = System.currentTimeMillis() - started)

        val images = request.messages.flatMap { m -> m.parts.filterIsInstance<ModelChatPart.Image>() }
        if (images.size > 1) {
            Log.w(TAG, "chat rejected multiple images modelId=$modelId images=${images.size}")
            return ModelChatResult(ok = false, modelId = modelId,
                message = "Only one image per request is supported in this version",
                elapsedMs = System.currentTimeMillis() - started)
        }

        val maxTokens = clampTokens(modelId, request.maxTokens)
        val raw = if (images.isEmpty()) {
            mnnRuntime.chatText(dir, normalizeMessages(request.messages), maxTokens,
                request.sessionId, request.useSessionCache)
        } else {
            mnnRuntime.chatImage(dir, images.first().bytes,
                renderMultimodalPrompt(request.messages), maxTokens)
        }

        return ModelChatResult(
            ok = raw.ok, modelId = modelId, text = raw.text,
            message = raw.message.ifBlank { "chat finished" },
            elapsedMs = System.currentTimeMillis() - started,
            promptTokens = raw.promptTokens, generatedTokens = raw.generatedTokens,
        ).also {
            Log.i(TAG, "chat end modelId=$modelId ok=${it.ok} elapsed=${it.elapsedMs} textLen=${raw.text.length}")
        }
    }

    // ---- Chat execution (streaming) ----

    private fun executeChatStream(request: ModelChatRequest, onChunk: (text: String, done: Boolean) -> Unit): ModelChatResult {
        val started = System.currentTimeMillis()
        val modelId = ModelManager.normalizeId(request.modelId).ifBlank { DEFAULT_MODEL_ID }
        val imageCount = request.messages.sumOf { m -> m.parts.count { it is ModelChatPart.Image } }
        Log.i(TAG, "chatStream start source=${request.source} modelId=$modelId messages=${request.messages.size} images=$imageCount maxTokens=${request.maxTokens}")

        val dir = resolveModel(modelId)
            ?: return ModelChatResult(ok = false, modelId = modelId,
                message = "MNN chat model not installed",
                elapsedMs = System.currentTimeMillis() - started)

        val images = request.messages.flatMap { m -> m.parts.filterIsInstance<ModelChatPart.Image>() }
        if (images.size > 1) {
            return ModelChatResult(ok = false, modelId = modelId,
                message = "Only one image per request is supported in this version",
                elapsedMs = System.currentTimeMillis() - started)
        }

        val maxTokens = clampTokens(modelId, request.maxTokens)
        val responseText = StringBuilder()
        val raw = if (images.isEmpty()) {
            mnnRuntime.chatTextStream(dir, normalizeMessages(request.messages), maxTokens,
                request.sessionId, request.useSessionCache,
                onChunk = { text, done ->
                    if (text.isNotEmpty()) responseText.append(text)
                    onChunk(text, done)
                })
        } else {
            mnnRuntime.chatImageStream(dir, images.first().bytes,
                renderMultimodalPrompt(request.messages), maxTokens,
                onChunk = { text, done ->
                    if (text.isNotEmpty()) responseText.append(text)
                    onChunk(text, done)
                })
        }

        return ModelChatResult(
            ok = raw.ok, modelId = modelId,
            text = responseText.toString(),
            message = raw.message.ifBlank { "chat stream finished" },
            elapsedMs = System.currentTimeMillis() - started,
            promptTokens = raw.promptTokens, generatedTokens = raw.generatedTokens,
        ).also {
            Log.i(TAG, "chatStream end modelId=$modelId ok=${it.ok} elapsed=${it.elapsedMs} textLen=${it.text.length}")
        }
    }

    // ---- Request assembly ----

    fun boundRequest(request: ModelChatRequest, serverModelId: String, serverMaxOutputTokens: Int): ModelChatRequest {
        val normalized = ModelManager.normalizeId(request.modelId).ifBlank { serverModelId }
        if (normalized != loadedModelId.get()) loadedModelId.set(null)
        return request.copy(
            modelId = normalized,
            maxTokens = clampTokens(normalized, request.maxTokens, serverMaxOutputTokens),
        )
    }

    fun sessionRequest(request: ModelChatRequest): ModelChatRequest {
        val explicitSession = request.sessionId.isNotBlank()
        val sessionId = sessionStore.normalizeModelSessionId(request.sessionId)
        val history = if (explicitSession && request.messages.size <= 1) sessionStore.modelHistory(sessionId) else emptyList()
        return request.copy(
            sessionId = sessionId, persistSession = explicitSession,
            useSessionCache = explicitSession && request.messages.size == 1 && request.messages.firstOrNull()?.role == "user",
            messages = (history + request.messages).takeLast(MAX_HISTORY_MESSAGES),
        )
    }

    fun saveModelTurn(request: ModelChatRequest, result: ModelChatResult) {
        if (!request.persistSession) return
        runCatching {
            sessionStore.appendModelTurn(sessionId = request.sessionId, modelId = result.modelId,
                requestMessages = request.messages, resultText = result.text, ok = result.ok)
        }.onFailure { e -> Log.w(TAG, "save model turn failed sessionId=${request.sessionId}", e) }
    }

    // ---- Async queue ----

    fun submitAsync(request: ModelChatRequest): String =
        requestQueue.submit(request.modelId, request.source) { executeChat(request) }

    fun submitSync(request: ModelChatRequest, timeoutMs: Long): ModelChatResult =
        requestQueue.submitSync(request.modelId, request.source, timeoutMs) { executeChat(request) }

    private fun executeStreamAsync(request: ModelChatRequest, onChunk: (String, Boolean) -> Unit): StreamJob =
        requestQueue.submitStream(request.modelId, request.source, onChunk) { executeChatStream(request, onChunk) }

    private fun cancelStream(job: StreamJob, reason: String): Boolean =
        requestQueue.cancel(job.id, reason) { mnnRuntime.cancel() }

    fun requestStatus(requestId: String): JSONObject = requestQueue.statusOf(requestId)
    fun queueSnapshot(): JSONObject = requestQueue.snapshot()

    fun cancelCurrent() = mnnRuntime.cancel()

    fun shutdown() {
        requestQueue.shutdown(); backgroundExecutor.shutdownNow()
    }

    fun recordRequestAsync(method: String, endpoint: String, status: Int, elapsedMs: Long, modelId: String) {
        backgroundExecutor.execute {
            runCatching { sessionStore.recordApiRequest(method, endpoint, status, elapsedMs, modelId) }
                .onFailure { e -> Log.w(TAG, "record request failed method=$method endpoint=$endpoint", e) }
        }
    }

    // ---- Streaming HTTP ----

    fun openAiStreamContent(requestId: String, request: ModelChatRequest, turnRequest: ModelChatRequest) =
        streamContent(requestId, request, turnRequest, StreamFormat.OPENAI)

    fun ollamaStreamContent(requestId: String, request: ModelChatRequest, turnRequest: ModelChatRequest) =
        streamContent(requestId, request, turnRequest, StreamFormat.OLLAMA)

    private fun streamContent(requestId: String, request: ModelChatRequest, turnRequest: ModelChatRequest, format: StreamFormat) =
        object : OutgoingContent.WriteChannelContent() {
            override val contentType = if (format == StreamFormat.OPENAI) EventStreamContentType else NdjsonContentType
            override suspend fun writeTo(channel: ByteWriteChannel) { writeChatStream(requestId, request, turnRequest, channel, format) }
        }

    private suspend fun writeChatStream(
        requestId: String, request: ModelChatRequest, turnRequest: ModelChatRequest,
        channel: ByteWriteChannel, format: StreamFormat,
    ) {
        val queue = LinkedBlockingQueue<StreamEvent>()
        val meta = StreamMeta("chatcmpl_mnnode_${System.currentTimeMillis()}", System.currentTimeMillis() / 1000, request.modelId)
        channel.writeEvent(format.start(meta))
        val job = executeStreamAsync(request) { text, done ->
            if (text.isNotEmpty()) queue.put(StreamEvent.Chunk(text))
            if (done) queue.put(StreamEvent.Done)
        }

        var done = false
        var aborted = false
        var abortReason: String? = null
        val streamStartedAt = System.currentTimeMillis()
        try {
            while (!done) {
                when (val event = withContext(Dispatchers.IO) { queue.poll(STREAM_HEARTBEAT_MS, TimeUnit.MILLISECONDS) }) {
                    is StreamEvent.Chunk -> channel.writeEvent(format.chunk(meta, event.text))
                    is StreamEvent.Done -> done = true
                    is StreamEvent.Error -> { channel.writeEvent(format.error(request.modelId, event.message)); done = true }
                    null -> {
                        if (System.currentTimeMillis() - streamStartedAt > CHAT_TIMEOUT_MS) {
                            lastError = "Chat stream timed out after ${CHAT_TIMEOUT_MS}ms"
                            abortReason = lastError
                            aborted = true
                            cancelStream(job, abortReason!!)
                            runCatching { channel.writeEvent(format.error(request.modelId, abortReason!!)) }
                            done = true
                        } else if (job.future.isDone) {
                            done = true
                        } else {
                            channel.writeEvent(format.heartbeat(meta))
                        }
                    }
                }
            }

            if (!aborted) {
                val result = runCatching {
                    withContext(Dispatchers.IO) { job.future.get(2, TimeUnit.SECONDS) }
                }.getOrNull()
                if (result != null && !result.ok) channel.writeEvent(format.error(result.modelId, result.message))
                if (result != null && result.ok) saveModelTurn(turnRequest, result)
                channel.writeEvent(format.done(meta))
            }
        } catch (e: CancellationException) {
            aborted = true
            abortReason = "client cancelled stream"
            throw e
        } catch (e: Throwable) {
            aborted = true
            abortReason = "client disconnected: ${e.message ?: e::class.java.simpleName}"
        } finally {
            if (aborted) {
                val reason = abortReason ?: "stream aborted"
                if (cancelStream(job, reason)) {
                    Log.i(TAG, "stream cancelled requestId=$requestId jobId=${job.id} reason=$reason")
                }
            }
        }
    }

    private suspend fun ByteWriteChannel.writeEvent(text: String) { writeStringUtf8(text); flush() }

    // ---- Helpers ----

    private fun normalizeMessages(messages: List<ModelChatMessage>): List<Pair<String, String>> =
        messages.map { it.role to it.text() }.filter { it.second.isNotBlank() }

    private fun renderMultimodalPrompt(messages: List<ModelChatMessage>): String {
        var imageInserted = false
        return messages.joinToString("\n\n") { message ->
            val text = message.text()
            val hasImage = message.parts.any { it is ModelChatPart.Image }
            val content = buildString {
                if (hasImage && !imageInserted) { append("<img>image_0</img>"); imageInserted = true; if (text.isNotBlank()) append("\n") }
                append(text)
            }.trim()
            "${message.role}: $content"
        }.trim()
    }

    // ---- Types ----

    private enum class StreamFormat {
        OPENAI,
        OLLAMA;

        fun start(meta: StreamMeta) = when (this) {
            OPENAI -> "data: ${chunkJson(meta, "role", "assistant")}\n\n"
            OLLAMA -> ollamaLine(meta.modelId, "")
        }

        fun chunk(meta: StreamMeta, text: String) = when (this) {
            OPENAI -> "data: ${chunkJson(meta, "content", text)}\n\n"
            OLLAMA -> ollamaLine(meta.modelId, text)
        }

        fun heartbeat(meta: StreamMeta) = when (this) {
            OPENAI -> ": keep-alive\n\n"
            OLLAMA -> ollamaLine(meta.modelId, "")
        }

        fun error(modelId: String, msg: String) = when (this) {
            OPENAI -> "data: ${ModelApiMapper.error("chat_failed", msg)}\n\n"
            OLLAMA -> "${JSONObject().put("model", modelId).put("created_at", java.time.Instant.now().toString()).put("error", msg).put("done", true)}\n"
        }

        fun done(meta: StreamMeta) = when (this) {
            OPENAI -> "data: ${chunkJson(meta, null, "stop")}\n\ndata: [DONE]\n\n"
            OLLAMA -> """{"model":"${meta.modelId}","created_at":"${java.time.Instant.now()}","done":true}
"""
        }
    }

    private sealed class StreamEvent {
        data class Chunk(val text: String) : StreamEvent()
        data object Done : StreamEvent()
        data class Error(val message: String) : StreamEvent()
    }

    companion object {
        private const val TAG = "MNNodeChat"
        const val CHAT_TIMEOUT_MS = 300_000L
        private const val STREAM_HEARTBEAT_MS = 10_000L
        const val MAX_HISTORY_MESSAGES = 16
        val EventStreamContentType = ContentType.Text.EventStream.withParameter("charset", "utf-8")
        val NdjsonContentType = ContentType.parse("application/x-ndjson").withParameter("charset", "utf-8")
    }
}

private data class StreamMeta(val id: String, val created: Long, val modelId: String)
private data class StreamJob(val id: String, val future: CompletableFuture<ModelChatResult>)

private fun chunkJson(meta: StreamMeta, key: String?, value: String): JSONObject {
    val delta = JSONObject()
    if (key != null) delta.put(key, value)
    return JSONObject()
        .put("id", meta.id).put("object", "chat.completion.chunk")
        .put("created", meta.created).put("model", meta.modelId)
        .put("choices", JSONArray().put(JSONObject()
            .put("index", 0).put("delta", delta)
            .put("finish_reason", if (key == null) value else JSONObject.NULL)))
}

private fun ollamaLine(modelId: String, content: String): String {
    return """{"model":"$modelId","created_at":"${java.time.Instant.now()}","message":{"role":"assistant","content":${JSONObject.quote(content)}},"done":false}
"""
}

private class ChatRequestQueue(
    private val maxQueuedRequests: Int = DEFAULT_MAX_QUEUED_REQUESTS,
) {
    private val queue = LinkedBlockingQueue<Job>()
    private val tasks = ConcurrentHashMap<String, Task>()
    private val runningJobId = AtomicReference<String?>(null)
    @Volatile private var closed = false

    private val worker = Thread(::runLoop, "mnnode-inference").apply {
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

    private fun cleanup(olderThanMs: Long = 300_000) {
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

    companion object {
        private const val DEFAULT_MAX_QUEUED_REQUESTS = 16
    }
}
