package com.mnnode.app.server

import android.util.Log
import com.mnnode.app.model.ChatCapability
import com.mnnode.app.model.ModelManager
import com.mnnode.app.model.DEFAULT_MODEL_ID
import com.mnnode.app.model.HARD_MAX_OUTPUT_TOKENS
import com.mnnode.app.model.ModelChatRequest
import com.mnnode.app.model.ModelChatResult
import com.mnnode.app.model.ModelToolCall
import com.mnnode.app.model.ToolTemplateContract
import com.mnnode.app.config.RuntimeDefaults
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
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import java.util.UUID

class ChatController(
    private val chatCapability: ChatCapability,
    private val sessionStore: SessionStore,
) {
    private val modelLoading = AtomicBoolean(false)
    @Volatile private var historyLimit = RuntimeDefaults.Sessions.MODEL_HISTORY_LIMIT
    @Volatile var lastError: String? = null

    private val requestQueue = ChatRequestQueue()
    private val backgroundExecutor = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "mnnode-chat-bg").apply { isDaemon = true }
    }

    val isLoading: Boolean get() = modelLoading.get()
    fun isModelLoaded(modelId: String) = chatCapability.isLoaded(modelId)
    fun resetLoadedModel() = chatCapability.resetLoadedModel()

    // ---- Token clamping ----

    fun clampTokens(modelIdRaw: String, requested: Int, serverCap: Int = HARD_MAX_OUTPUT_TOKENS): Int =
        chatCapability.clampTokens(modelIdRaw, requested, serverCap)

    fun effectiveMaxTokens(modelIdRaw: String, serverCap: Int = HARD_MAX_OUTPUT_TOKENS): Int =
        chatCapability.maxTokens(modelIdRaw, serverCap)

    fun contextWindowTokens(modelIdRaw: String): Int =
        chatCapability.contextWindowTokens(modelIdRaw)

    fun configureHistoryLimit(limit: Int) {
        historyLimit = limit.coerceIn(1, RuntimeDefaults.Sessions.MODEL_HISTORY_MAX_LIMIT)
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
                chatCapability.preload(normalized).also { if (!it.ok) lastError = it.message }
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
        return chatCapability.complete(request)
    }

    // ---- Chat execution (streaming) ----

    private fun executeChatStream(request: ModelChatRequest, onChunk: (text: String, done: Boolean) -> Unit): ModelChatResult {
        return chatCapability.stream(request, onChunk)
    }

    // ---- Request assembly ----

    fun boundRequest(request: ModelChatRequest, serverModelId: String, serverMaxOutputTokens: Int): ModelChatRequest {
        val normalized = ModelManager.normalizeId(request.modelId).ifBlank { serverModelId }
        return request.copy(
            modelId = normalized,
            maxTokens = clampTokens(normalized, request.maxTokens ?: serverMaxOutputTokens, serverMaxOutputTokens),
        )
    }

    fun sessionRequest(request: ModelChatRequest): ModelChatRequest {
        val explicitSession = request.sessionId.isNotBlank()
        val sessionId = sessionStore.normalizeModelSessionId(request.sessionId)
        val history = if (explicitSession && request.messages.size <= 1) {
            sessionStore.modelHistory(sessionId, historyLimit)
        } else emptyList()
        val contextMessages = trimContextMessages(
            messages = history + request.messages,
            contextBudget = contextWindowTokens(request.modelId),
            outputBudget = request.maxTokens ?: HARD_MAX_OUTPUT_TOKENS,
        )
        val canReuseNativeSessionCache = explicitSession &&
            request.messages.size <= 1 &&
            request.messages.lastOrNull()?.role == "user"
        return request.copy(
            sessionId = sessionId, persistSession = explicitSession,
            useSessionCache = canReuseNativeSessionCache,
            messages = contextMessages,
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
        requestQueue.cancel(job.id, reason) { chatCapability.cancel() }

    fun requestStatus(requestId: String): JSONObject = requestQueue.statusOf(requestId)
    fun queueSnapshot(): JSONObject = requestQueue.snapshot()

    fun cancelCurrent() = chatCapability.cancel()

    fun resetSessionCache() = chatCapability.resetSessionCache()

    fun releaseModel() {
        cancelCurrent()
        resetLoadedModel()
        chatCapability.releaseModel()
    }

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

    fun openAiStreamContent(requestId: String, request: ModelChatRequest, turnRequest: ModelChatRequest, includeUsage: Boolean) =
        streamContent(requestId, request, turnRequest, StreamFormat.OPENAI, includeUsage)

    fun ollamaStreamContent(requestId: String, request: ModelChatRequest, turnRequest: ModelChatRequest) =
        streamContent(requestId, request, turnRequest, StreamFormat.OLLAMA)

    private fun streamContent(
        requestId: String,
        request: ModelChatRequest,
        turnRequest: ModelChatRequest,
        format: StreamFormat,
        includeUsage: Boolean = false,
    ) =
        object : OutgoingContent.WriteChannelContent() {
            override val contentType = if (format == StreamFormat.OPENAI) EventStreamContentType else NdjsonContentType
            override suspend fun writeTo(channel: ByteWriteChannel) {
                writeChatStream(requestId, request, turnRequest, channel, format, includeUsage)
            }
        }

    private suspend fun writeChatStream(
        requestId: String, request: ModelChatRequest, turnRequest: ModelChatRequest,
        channel: ByteWriteChannel, format: StreamFormat, includeUsage: Boolean,
    ) {
        val queue = LinkedBlockingQueue<StreamEvent>()
        val meta = StreamMeta("chatcmpl_mnnode_${System.currentTimeMillis()}", System.currentTimeMillis() / 1000, request.modelId)
        var bufferToolCandidate = format == StreamFormat.OPENAI && request.tools != null
        val bufferedText = StringBuilder()
        channel.writeEvent(format.start(meta))
        val job = executeStreamAsync(request) { text, done ->
            if (text.isNotEmpty()) queue.put(StreamEvent.Chunk(text))
            if (done) queue.put(StreamEvent.Done)
        }

        var done = false
        var aborted = false
        var abortReason: String? = null
        var lastActivityAt = System.currentTimeMillis()
        try {
            while (!done) {
                when (val event = withContext(Dispatchers.IO) { queue.poll(RuntimeDefaults.Queue.STREAM_HEARTBEAT_MS, TimeUnit.MILLISECONDS) }) {
                    is StreamEvent.Chunk -> {
                        lastActivityAt = System.currentTimeMillis()
                        if (bufferToolCandidate) {
                            bufferedText.append(event.text)
                            if (!looksLikeToolPrefix(bufferedText.toString())) {
                                channel.writeEvent(format.chunk(meta, bufferedText.toString()))
                                bufferedText.clear()
                                bufferToolCandidate = false
                            }
                        } else {
                            channel.writeEvent(format.chunk(meta, event.text))
                        }
                    }
                    is StreamEvent.Done -> { lastActivityAt = System.currentTimeMillis(); done = true }
                    is StreamEvent.Error -> { channel.writeEvent(format.error(request.modelId, event.message)); done = true }
                    null -> {
                        if (System.currentTimeMillis() - lastActivityAt > RuntimeDefaults.Queue.CHAT_TIMEOUT_MS) {
                            lastError = "Chat stream idle timed out after ${RuntimeDefaults.Queue.CHAT_TIMEOUT_MS}ms"
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
                val normalizedResult = if (result != null && result.ok && bufferToolCandidate) {
                    val text = bufferedText.toString().ifBlank { result.text }
                    val toolCalls = ToolTemplateContract.parse(text)
                    if (toolCalls.isNotEmpty()) {
                        val parsed = result.copy(text = "", toolCalls = toolCalls)
                        channel.writeEvent(format.toolCalls(meta, toolCalls))
                        parsed
                    } else {
                        if (text.isNotEmpty()) channel.writeEvent(format.chunk(meta, text))
                        result.copy(text = text)
                    }
                } else result
                if (normalizedResult != null && normalizedResult.ok) saveModelTurn(turnRequest, normalizedResult)
                channel.writeEvent(format.done(meta, normalizedResult, includeUsage))
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

        fun toolCalls(meta: StreamMeta, calls: List<ModelToolCall>) = when (this) {
            OPENAI -> "data: ${toolCallsChunkJson(meta, calls)}\n\n"
            OLLAMA -> ollamaLine(meta.modelId, "")
        }

        fun heartbeat(meta: StreamMeta) = when (this) {
            OPENAI -> ": keep-alive\n\n"
            OLLAMA -> ollamaLine(meta.modelId, "")
        }

        fun error(modelId: String, msg: String) = when (this) {
            OPENAI -> "data: ${ModelApiMapper.error("chat_failed", msg)}\n\n"
            OLLAMA -> "${JSONObject().put("model", modelId).put("created_at", java.time.Instant.now().toString()).put("error", msg).put("done", true)}\n"
        }

        fun done(meta: StreamMeta, result: ModelChatResult?, includeUsage: Boolean) = when (this) {
            OPENAI -> buildString {
                val reason = if (result?.toolCalls?.isNotEmpty() == true) "tool_calls" else "stop"
                append("data: ${chunkJson(meta, null, reason)}\n\n")
                if (includeUsage && result != null) append("data: ${usageChunkJson(meta, result)}\n\n")
                append("data: [DONE]\n\n")
            }
            OLLAMA -> ollamaDoneLine(meta.modelId, result)
        }
    }

    private sealed class StreamEvent {
        data class Chunk(val text: String) : StreamEvent()
        data object Done : StreamEvent()
        data class Error(val message: String) : StreamEvent()
    }

    companion object {
        private const val TAG = "MNNodeChat"
        val EventStreamContentType = ContentType.Text.EventStream.withParameter("charset", "utf-8")
        val NdjsonContentType = ContentType.parse("application/x-ndjson").withParameter("charset", "utf-8")
    }
}

private fun trimContextMessages(
    messages: List<ModelChatRequestMessageAlias>,
    contextBudget: Int,
    outputBudget: Int,
): List<ModelChatRequestMessageAlias> {
    if (messages.isEmpty()) return emptyList()
    val inputBudget = (contextBudget - outputBudget.coerceAtLeast(0) - RuntimeDefaults.Tokens.CONTEXT_SAFETY_MARGIN)
        .coerceAtLeast(RuntimeDefaults.Tokens.INPUT_BUDGET_MIN)
    val system = messages.filter { it.role.equals("system", ignoreCase = true) }
    val nonSystem = messages.filterNot { it.role.equals("system", ignoreCase = true) }
    val selected = ArrayDeque<ModelChatRequestMessageAlias>()
    var used = system.sumOf { estimateMessageTokens(it) }

    for (message in nonSystem.asReversed()) {
        val cost = estimateMessageTokens(message)
        if (selected.isNotEmpty() && used + cost > inputBudget) break
        selected.addFirst(message)
        used += cost
    }

    return (system.takeLast(RuntimeDefaults.Sessions.MAX_SYSTEM_MESSAGES) + selected).ifEmpty { messages.takeLast(1) }
}

private typealias ModelChatRequestMessageAlias = com.mnnode.app.model.ModelChatMessage

private fun estimateMessageTokens(message: ModelChatRequestMessageAlias): Int {
    val textTokens = (message.text().length + 3) / 4
    val imageTokens = message.parts.count { it is com.mnnode.app.model.ModelChatPart.Image } * RuntimeDefaults.Tokens.IMAGE_ESTIMATE
    return RuntimeDefaults.Tokens.MESSAGE_OVERHEAD + textTokens + imageTokens
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

private fun looksLikeToolPrefix(raw: String): Boolean {
    val text = raw.trimStart()
    if (text.isBlank()) return true
    if (text.startsWith("<")) {
        return "<tool_call>".startsWith(text.take("<tool_call>".length)) ||
            text.startsWith("<tool_call>") ||
            text.startsWith("<think>") ||
            text.startsWith("</think>")
    }
    if (text.startsWith("```")) return true
    if (text.startsWith("{") || text.startsWith("[")) return true
    return false
}

private fun toolCallsChunkJson(meta: StreamMeta, calls: List<ModelToolCall>): JSONObject {
    val delta = JSONObject()
        .put("tool_calls", JSONArray(calls.mapIndexed { index, call ->
            ModelApiMapper.openAiToolCallJson(call).put("index", index)
        }))
    return JSONObject()
        .put("id", meta.id).put("object", "chat.completion.chunk")
        .put("created", meta.created).put("model", meta.modelId)
        .put("choices", JSONArray().put(JSONObject()
            .put("index", 0)
            .put("delta", delta)
            .put("finish_reason", JSONObject.NULL)))
}

private fun ollamaLine(modelId: String, content: String): String {
    return """{"model":"$modelId","created_at":"${java.time.Instant.now()}","message":{"role":"assistant","content":${JSONObject.quote(content)}},"done":false}
"""
}

private fun usageChunkJson(meta: StreamMeta, result: ModelChatResult): JSONObject {
    return JSONObject()
        .put("id", meta.id)
        .put("object", "chat.completion.chunk")
        .put("created", meta.created)
        .put("model", meta.modelId)
        .put("choices", JSONArray())
        .put("usage", ModelApiMapper.openAiUsage(result))
        .put("mnnode", ModelApiMapper.runtimeMetrics(result))
}

private fun ollamaDoneLine(modelId: String, result: ModelChatResult?): String {
    val json = JSONObject()
        .put("model", modelId)
        .put("created_at", java.time.Instant.now().toString())
        .put("done", true)
    if (result != null) {
        json.put("total_duration", result.elapsedMs * 1_000_000L)
            .put("load_duration", 0L)
            .put("prompt_eval_count", result.promptTokens)
            .put("prompt_eval_duration", 0L)
            .put("eval_count", result.generatedTokens)
            .put("eval_duration", result.elapsedMs * 1_000_000L)
            .put("mnnode", ModelApiMapper.runtimeMetrics(result))
    }
    return "$json\n"
}

private class ChatRequestQueue(
    private val maxQueuedRequests: Int = RuntimeDefaults.Queue.MAX_QUEUED_REQUESTS,
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
