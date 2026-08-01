package io.lociant.android.server

import android.util.Log
import io.lociant.runtime.model.ChatCapability
import io.lociant.runtime.model.ModelManager
import io.lociant.core.model.DEFAULT_MODEL_ID
import io.lociant.core.model.HARD_MAX_OUTPUT_TOKENS
import io.lociant.core.model.ModelChatRequest
import io.lociant.core.model.ModelChatResult
import io.lociant.core.model.ModelToolCall
import io.lociant.core.model.ToolTemplateContract
import io.lociant.core.config.RuntimeDefaults
import io.lociant.data.session.SessionStore
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
        Thread(runnable, "lociant-chat-bg").apply { isDaemon = true }
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
        val explicitSession = request.sessionId.isNotEmpty()
        val sessionId = if (explicitSession) sessionStore.requireExistingSession(request.sessionId) else ""
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

    fun submitAsync(request: ModelChatRequest, turnRequest: ModelChatRequest = request): String =
        requestQueue.submit(request.modelId, request.source) {
            executeChat(request).also { result -> saveModelTurn(turnRequest, result) }
        }

    fun submitSync(request: ModelChatRequest, timeoutMs: Long): ModelChatResult =
        requestQueue.submitSync(request.modelId, request.source, timeoutMs, chatCapability::cancel) { executeChat(request) }

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
        streamContent(requestId, request, turnRequest, includeUsage)

    private fun streamContent(
        requestId: String,
        request: ModelChatRequest,
        turnRequest: ModelChatRequest,
        includeUsage: Boolean = false,
    ) =
        object : OutgoingContent.WriteChannelContent() {
            override val contentType = EventStreamContentType
            override suspend fun writeTo(channel: ByteWriteChannel) {
                writeChatStream(requestId, request, turnRequest, channel, includeUsage)
            }
        }

    private suspend fun writeChatStream(
        requestId: String, request: ModelChatRequest, turnRequest: ModelChatRequest,
        channel: ByteWriteChannel, includeUsage: Boolean,
    ) {
        val queue = LinkedBlockingQueue<StreamEvent>()
        val meta = StreamMeta("chatcmpl_lociant_${System.currentTimeMillis()}", System.currentTimeMillis() / 1000, request.modelId)
        var bufferToolCandidate = request.tools != null
        val bufferedText = StringBuilder()
        channel.writeEvent(streamStart(meta))
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
                                channel.writeEvent(streamChunk(meta, bufferedText.toString()))
                                bufferedText.clear()
                                bufferToolCandidate = false
                            }
                        } else {
                            channel.writeEvent(streamChunk(meta, event.text))
                        }
                    }
                    is StreamEvent.Done -> { lastActivityAt = System.currentTimeMillis(); done = true }
                    is StreamEvent.Error -> { channel.writeEvent(streamError(event.message)); done = true }
                    null -> {
                        if (System.currentTimeMillis() - lastActivityAt > RuntimeDefaults.Queue.CHAT_TIMEOUT_MS) {
                            lastError = "Chat stream idle timed out after ${RuntimeDefaults.Queue.CHAT_TIMEOUT_MS}ms"
                            abortReason = lastError
                            aborted = true
                            cancelStream(job, abortReason!!)
                            runCatching { channel.writeEvent(streamError(abortReason!!)) }
                            done = true
                        } else if (job.future.isDone) {
                            done = true
                        } else {
                            channel.writeEvent(streamHeartbeat())
                        }
                    }
                }
            }

            if (!aborted) {
                val result = runCatching {
                    withContext(Dispatchers.IO) { job.future.get(2, TimeUnit.SECONDS) }
                }.getOrNull()
                if (result != null && !result.ok) channel.writeEvent(streamError(result.message))
                val normalizedResult = if (result != null && result.ok && bufferToolCandidate) {
                    val text = bufferedText.toString().ifBlank { result.text }
                    val toolCalls = ToolTemplateContract.parse(text)
                    if (toolCalls.isNotEmpty()) {
                        val parsed = result.copy(text = "", toolCalls = toolCalls)
                        channel.writeEvent(streamToolCalls(meta, toolCalls))
                        parsed
                    } else {
                        if (text.isNotEmpty()) channel.writeEvent(streamChunk(meta, text))
                        result.copy(text = text)
                    }
                } else result
                if (normalizedResult != null && normalizedResult.ok) saveModelTurn(turnRequest, normalizedResult)
                channel.writeEvent(streamDone(meta, normalizedResult, includeUsage))
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

    private fun streamStart(meta: StreamMeta) = "data: ${chunkJson(meta, "role", "assistant")}\n\n"
    private fun streamChunk(meta: StreamMeta, text: String) = "data: ${chunkJson(meta, "content", text)}\n\n"
    private fun streamToolCalls(meta: StreamMeta, calls: List<ModelToolCall>) = "data: ${toolCallsChunkJson(meta, calls)}\n\n"
    private fun streamHeartbeat() = ": keep-alive\n\n"
    private fun streamError(message: String) = "data: ${ModelApiMapper.error("chat_failed", message)}\n\n"
    private fun streamDone(meta: StreamMeta, result: ModelChatResult?, includeUsage: Boolean) = buildString {
        val reason = if (result?.toolCalls?.isNotEmpty() == true) "tool_calls" else "stop"
        append("data: ${chunkJson(meta, null, reason)}\n\n")
        if (includeUsage && result != null) append("data: ${usageChunkJson(meta, result)}\n\n")
        append("data: [DONE]\n\n")
    }

    private sealed class StreamEvent {
        data class Chunk(val text: String) : StreamEvent()
        data object Done : StreamEvent()
        data class Error(val message: String) : StreamEvent()
    }

    companion object {
        private const val TAG = "LociantChat"
        val EventStreamContentType = ContentType.Text.EventStream.withParameter("charset", "utf-8")
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

private typealias ModelChatRequestMessageAlias = io.lociant.core.model.ModelChatMessage

private fun estimateMessageTokens(message: ModelChatRequestMessageAlias): Int {
    val textTokens = (message.text().length + 3) / 4
    val imageTokens = message.parts.count { it is io.lociant.core.model.ModelChatPart.Image } * RuntimeDefaults.Tokens.IMAGE_ESTIMATE
    return RuntimeDefaults.Tokens.MESSAGE_OVERHEAD + textTokens + imageTokens
}

private data class StreamMeta(val id: String, val created: Long, val modelId: String)
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
            text.startsWith("<tool_call>")
    }
    if (text.startsWith("```")) return text.length < 16 || text.contains("tool", ignoreCase = true)
    if (text.startsWith("{") || text.startsWith("[")) {
        if (text.length < 32) return true
        return text.contains("\"tool_calls\"") ||
            text.contains("\"function\"") ||
            text.contains("\"name\"")
    }
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

private fun usageChunkJson(meta: StreamMeta, result: ModelChatResult): JSONObject {
    return JSONObject()
        .put("id", meta.id)
        .put("object", "chat.completion.chunk")
        .put("created", meta.created)
        .put("model", meta.modelId)
        .put("choices", JSONArray())
        .put("usage", ModelApiMapper.openAiUsage(result))
        .put("lociant", ModelApiMapper.runtimeMetrics(result))
}
