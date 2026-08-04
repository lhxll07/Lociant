package io.lociant.android.server

import android.util.Log
import io.lociant.runtime.model.ChatCapability
import io.lociant.runtime.model.ModelManager
import io.lociant.core.model.DEFAULT_MODEL_ID
import io.lociant.core.model.HARD_MAX_OUTPUT_TOKENS
import io.lociant.core.model.ModelChatRequest
import io.lociant.core.model.ModelChatResult
import io.lociant.core.model.ModelToolCall
import io.lociant.core.model.ToolLoopGuard
import io.lociant.core.model.ToolTemplateContract
import io.lociant.core.model.AutomaticSessionCache
import io.lociant.core.config.RuntimeDefaults
import io.lociant.data.session.SessionStore
import io.ktor.http.ContentType
import io.ktor.http.content.OutgoingContent
import io.ktor.utils.io.ByteWriteChannel
import io.ktor.utils.io.writeStringUtf8
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
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
    @Volatile private var cloudHistoryLimit = RuntimeDefaults.Cloud.HISTORY_LIMIT_DEFAULT
    @Volatile var lastError: String? = null

    private val requestQueue = ChatRequestQueue()
    private val automaticSessionCache = AutomaticSessionCache()
    private val backgroundExecutor = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "lociant-chat-bg").apply { isDaemon = true }
    }

    val isLoading: Boolean get() = modelLoading.get()
    fun isModelLoaded(modelId: String) = chatCapability.isLoaded(modelId)
    fun resetLoadedModel() {
        automaticSessionCache.invalidate()
        chatCapability.resetLoadedModel()
    }

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

    fun configureCloudHistoryLimit(limit: Int) {
        cloudHistoryLimit = limit.coerceIn(1, RuntimeDefaults.Cloud.HISTORY_LIMIT_MAX)
    }

    private fun historyLimitFor(modelIdRaw: String): Int =
        if (chatCapability.isCloudModel(modelIdRaw)) cloudHistoryLimit else historyLimit

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
        val result = runCatching { chatCapability.complete(request) }.getOrElse { error ->
            ModelChatResult(
                ok = false,
                modelId = request.modelId,
                message = error.message ?: "chat failed",
            )
        }
        return result.also {
            automaticSessionCache.commit(request, it)
        }
    }

    // ---- Chat execution (streaming) ----

    private fun executeChatStream(
        request: ModelChatRequest,
        onChunk: (text: String, done: Boolean) -> Unit,
        onReasoning: ((text: String, done: Boolean) -> Unit)? = null,
        onToolCall: ((ModelToolCall) -> Unit)? = null,
    ): ModelChatResult {
        val result = runCatching {
            chatCapability.stream(request, onChunk, onReasoning, onToolCall)
        }.getOrElse { error ->
            ModelChatResult(
                ok = false,
                modelId = request.modelId,
                message = error.message ?: "chat failed",
            )
        }
        return result.also {
            automaticSessionCache.commit(request, it)
        }
    }

    // ---- Request assembly ----

    fun boundRequest(request: ModelChatRequest, serverModelId: String, serverMaxOutputTokens: Int): ModelChatRequest {
        val normalized = ModelManager.normalizeId(request.modelId).ifBlank { serverModelId }
        val maxTokens = if (chatCapability.isCloudModel(normalized)) {
            // Cloud models keep their own output policy: never clamp to the local
            // server cap. An explicit cloud cap still applies when configured.
            val cloudCap = chatCapability.cloudOutputTokenLimit()
            val requested = request.maxTokens
            when {
                cloudCap == null -> requested
                requested == null -> cloudCap
                else -> minOf(requested, cloudCap)
            }
        } else {
            clampTokens(normalized, request.maxTokens ?: serverMaxOutputTokens, serverMaxOutputTokens)
        }
        return request.copy(modelId = normalized, maxTokens = maxTokens)
    }

    fun sessionRequest(request: ModelChatRequest): ModelChatRequest {
        val explicitSession = request.sessionId.isNotEmpty()
        if (explicitSession) automaticSessionCache.invalidate()
        val sessionId = if (explicitSession) sessionStore.requireExistingSession(request.sessionId) else ""
        val history = if (explicitSession && request.messages.size <= 1) {
            sessionStore.modelHistory(sessionId, historyLimitFor(request.modelId))
        } else emptyList()
        val contextMessages = trimContextMessages(
            messages = history + request.messages,
            contextBudget = contextWindowTokens(request.modelId),
            outputBudget = request.maxTokens ?: HARD_MAX_OUTPUT_TOKENS,
        )
        val canReuseNativeSessionCache = explicitSession &&
            request.messages.size <= 1 &&
            request.messages.lastOrNull()?.role == "user"
        val contextRequest = request.copy(
            sessionId = sessionId, persistSession = explicitSession,
            useSessionCache = canReuseNativeSessionCache,
            messages = contextMessages,
        )
        if (explicitSession || chatCapability.isCloudModel(request.modelId)) {
            if (!explicitSession) automaticSessionCache.invalidate()
            return contextRequest
        }
        return automaticSessionCache.prepare(
            contextRequest,
            chatCapability.automaticCacheConfigurationKey(),
        )
    }

    fun saveModelTurn(request: ModelChatRequest, result: ModelChatResult) {
        if (!request.persistSession) return
        runCatching {
            sessionStore.appendModelTurn(sessionId = request.sessionId, modelId = result.modelId,
                resultText = result.text, ok = result.ok, reasoning = result.reasoning)
        }.onFailure { e -> Log.w(TAG, "save model turn failed sessionId=${request.sessionId}", e) }
    }

    /**
     * Persists the user's request messages as soon as a session chat starts.
     * Long tool runs switch apps / push this app to the background; if the OS
     * kills the process mid-flow the conversation would otherwise be lost.
     */
    fun persistUserTurn(request: ModelChatRequest) {
        if (!request.persistSession) return
        runCatching {
            sessionStore.appendUserMessages(sessionId = request.sessionId, modelId = request.modelId, messages = request.messages)
        }.onFailure { e -> Log.w(TAG, "persist user turn failed sessionId=${request.sessionId}", e) }
    }

    // ---- Async queue ----

    fun submitAsync(request: ModelChatRequest, turnRequest: ModelChatRequest = request): String {
        return requestQueue.submit(request.modelId, request.source) {
            executeChat(request).also { result -> saveModelTurn(turnRequest, result) }
        }
    }

    fun submitSync(request: ModelChatRequest, timeoutMs: Long): ModelChatResult =
        requestQueue.submitSync(request.modelId, request.source, timeoutMs, chatCapability::cancel) { executeChat(request) }

    private fun executeStreamAsync(
        request: ModelChatRequest,
        onChunk: (String, Boolean) -> Unit,
        onReasoning: ((String, Boolean) -> Unit)? = null,
        onToolCall: ((ModelToolCall) -> Unit)? = null,
    ): StreamJob =
        requestQueue.submitStream(request.modelId, request.source, onChunk) { executeChatStream(request, onChunk, onReasoning, onToolCall) }

    private fun cancelStream(job: StreamJob, reason: String): Boolean =
        requestQueue.cancel(job.id, reason) { chatCapability.cancel() }

    fun requestStatus(requestId: String): JSONObject = requestQueue.statusOf(requestId)
    fun queueSnapshot(): JSONObject = requestQueue.snapshot()

    fun cancelCurrent() = chatCapability.cancel()

    fun resetSessionCache() {
        automaticSessionCache.invalidate()
        chatCapability.resetSessionCache()
    }

    /** Keeps the internal automatic-cache generation out of the public API. */
    fun visibleSessionId(request: ModelChatRequest): String =
        request.sessionId.takeIf { request.persistSession }.orEmpty()

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

    fun openAiStreamResult(
        requestId: String,
        request: ModelChatRequest,
        result: ModelChatResult,
        includeUsage: Boolean = false,
    ) = object : OutgoingContent.WriteChannelContent() {
        override val contentType = EventStreamContentType
        override suspend fun writeTo(channel: ByteWriteChannel) {
            val meta = StreamMeta("chatcmpl_lociant_${System.currentTimeMillis()}", System.currentTimeMillis() / 1000, request.modelId)
            channel.writeEvent(streamStart(meta))
            if (result.reasoning.isNotBlank()) channel.writeEvent(streamReasoning(meta, result.reasoning))
            result.text.chunked(16).forEach { channel.writeEvent(streamChunk(meta, it)) }
            channel.writeEvent(streamDone(meta, result, includeUsage))
        }
    }

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
        val job = executeStreamAsync(request, { text, done ->
            if (text.isNotEmpty()) queue.put(StreamEvent.Chunk(text))
            if (done) queue.put(StreamEvent.Done)
        }, { reasoning, done ->
            if (reasoning.isNotEmpty()) queue.put(StreamEvent.Reasoning(reasoning))
            if (done) queue.put(StreamEvent.Done)
        })

        var done = false
        var aborted = false
        var abortReason: String? = null
        var lastActivityAt = System.currentTimeMillis()
        var lastHeartbeatAt = System.currentTimeMillis()
        try {
            while (!done) {
                when (val event = withContext(Dispatchers.IO) { queue.poll(ROUND_POLL_MS, TimeUnit.MILLISECONDS) }) {
                    is StreamEvent.Chunk -> {
                        lastActivityAt = System.currentTimeMillis()
                        if (bufferToolCandidate) {
                            bufferedText.append(event.text)
                            val candidate = bufferedText.toString()
                            // A tool call is small; if the buffered text grows past the
                            // threshold it is a long plain response (for example JSON with
                            // a "name" key) that would otherwise stream silently until the
                            // model finishes.
                            if (!looksLikeToolPrefix(candidate) || candidate.length > TOOL_BUFFER_FLUSH_LIMIT) {
                                channel.writeEvent(streamChunk(meta, candidate))
                                bufferedText.clear()
                                bufferToolCandidate = false
                            }
                        } else {
                            channel.writeEvent(streamChunk(meta, event.text))
                        }
                    }
                    is StreamEvent.Reasoning -> {
                        lastActivityAt = System.currentTimeMillis()
                        channel.writeEvent(streamReasoning(meta, event.text))
                    }
                    is StreamEvent.ToolCall -> {
                        lastActivityAt = System.currentTimeMillis()
                        channel.writeEvent(streamToolCalls(meta, listOf(event.call)))
                    }
                    is StreamEvent.Phase -> { lastActivityAt = System.currentTimeMillis() }
                    is StreamEvent.Done -> { lastActivityAt = System.currentTimeMillis(); done = true }
                    is StreamEvent.Error -> { channel.writeEvent(streamError(event.message)); done = true }
                    null -> {
                        if (System.currentTimeMillis() - lastActivityAt > RuntimeDefaults.Queue.CHAT_TIMEOUT_MS) {
                            lastError = "Chat stream idle timed out after ${RuntimeDefaults.Queue.CHAT_TIMEOUT_MS}ms"
                            abortReason = lastError
                            aborted = true
                            val reason = abortReason ?: "stream timed out"
                            cancelStream(job, reason)
                            runCatching { channel.writeEvent(streamError(reason)) }
                            done = true
                        } else if (job.future.isDone) {
                            done = true
                        } else if (System.currentTimeMillis() - lastHeartbeatAt >= RuntimeDefaults.Queue.STREAM_HEARTBEAT_MS) {
                            channel.writeEvent(streamPing(meta, System.currentTimeMillis() - lastActivityAt))
                            lastHeartbeatAt = System.currentTimeMillis()
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


    /**
     * Streams a multi-round tool-execution loop live: each model turn relays its
     * reasoning / content / tool calls as SSE events, tools execute between turns,
     * and the stream ends with a single [DONE].
     */
    fun openAiAgentStreamContent(
        requestId: String,
        request: ModelChatRequest,
        turnRequest: ModelChatRequest,
        includeUsage: Boolean = false,
        maxRounds: Int = RuntimeDefaults.Agent.MAX_ROUNDS,
        executeTool: (ModelToolCall) -> JSONObject = { ModelChatToolFallback(it) },
    ) = object : OutgoingContent.WriteChannelContent() {
        override val contentType = EventStreamContentType
        override suspend fun writeTo(channel: ByteWriteChannel) {
            val meta = StreamMeta("chatcmpl_lociant_${System.currentTimeMillis()}", System.currentTimeMillis() / 1000, request.modelId)
            val queue = LinkedBlockingQueue<StreamEvent>()
            channel.writeEvent(streamStart(meta))

            var current = request
            var finalResult: ModelChatResult? = null
            var rounds = 0
            var job: StreamJob? = null
            var generation = 0L
            var done = false
            var aborted = false
            var abortReason: String? = null
            var lastActivityAt = System.currentTimeMillis()
            var lastHeartbeatAt = lastActivityAt
            var retries = 0
            var executedToolCalls = 0
            val loopGuard = ToolLoopGuard()

            suspend fun startRound(target: ModelChatRequest) {
                // A completed round can have queued callbacks just before its
                // future completes. Clear them before starting the next model
                // request so an old chunk cannot leak into the new turn.
                queue.clear()
                val roundGeneration = ++generation
                channel.writePhase(meta, "round", round = rounds)
                val toolTextBuffer = StringBuilder()
                val parseTextTools = !chatCapability.isCloudModel(target.modelId)
                job = executeStreamAsync(target, { text, _ ->
                    if (roundGeneration != generation || text.isEmpty()) return@executeStreamAsync
                    if (!parseTextTools) {
                        queue.put(StreamEvent.Chunk(text))
                    } else {
                        // Local models may emit a tool template as plain text.
                        // Cloud models use standard tool_calls and never enter
                        // this parser path.
                        toolTextBuffer.append(text)
                        val candidate = toolTextBuffer.toString()
                        val parsed = ToolTemplateContract.parse(candidate)
                        if (parsed.isNotEmpty()) {
                            toolTextBuffer.setLength(0)
                        } else if (!looksLikeToolPrefix(candidate) || candidate.length > TOOL_BUFFER_FLUSH_LIMIT) {
                            queue.put(StreamEvent.Chunk(candidate))
                            toolTextBuffer.setLength(0)
                        }
                    }
                }, { reasoning, _ ->
                    if (roundGeneration == generation && reasoning.isNotEmpty()) {
                        queue.put(StreamEvent.Reasoning(reasoning))
                    }
                }, { call ->
                    if (roundGeneration == generation) queue.put(StreamEvent.ToolCall(call))
                })
            }

            try {
                startRound(current)
                while (!done) {
                when (val event = withContext(Dispatchers.IO) { queue.poll(ROUND_POLL_MS, TimeUnit.MILLISECONDS) }) {
                    is StreamEvent.Chunk -> {
                        lastActivityAt = System.currentTimeMillis()
                        channel.writeEvent(streamChunk(meta, event.text))
                    }
                    is StreamEvent.Reasoning -> {
                        lastActivityAt = System.currentTimeMillis()
                        channel.writeEvent(streamReasoning(meta, event.text))
                    }
                    is StreamEvent.ToolCall -> {
                        lastActivityAt = System.currentTimeMillis()
                        channel.writeEvent(streamToolCalls(meta, listOf(event.call)))
                    }
                    is StreamEvent.Phase -> {
                        lastActivityAt = System.currentTimeMillis()
                        channel.writeEvent(streamPhase(meta, event))
                    }
                    is StreamEvent.Error -> {
                        channel.writeEvent(streamError(event.message))
                        done = true
                    }
                    is StreamEvent.Done -> done = true
                    null -> {
                        val currentJob = job ?: run { done = true; null }
                        if (currentJob == null) continue
                        if (currentJob.future.isDone) {
                            lastActivityAt = System.currentTimeMillis()
                            val result = runCatching { currentJob.future.get() }.getOrElse {
                                ModelChatResult(
                                    ok = false,
                                    modelId = request.modelId,
                                    message = it.message ?: "agent round failed",
                                )
                            }
                            finalResult = result
                            val toolCalls = result.toolCalls
                            when {
                                !result.ok -> {
                                    // Only transient upstream failures (timeout / 5xx /
                                    // rate limit) deserve a retry. Permanent errors such
                                    // as 400 invalid_request fail fast with their message.
                                    if (result.retryable && retries < RuntimeDefaults.Agent.MAX_TRANSIENT_RETRIES) {
                                        retries++
                                        lastError = result.message.ifBlank { "agent round failed" }
                                        Log.w(TAG, "agent round retry reason=${lastError}")
                                        channel.writePhase(meta, "retry", message = lastError!!)
                                        startRound(current)
                                    } else {
                                        lastError = result.message.ifBlank { "agent round failed" }
                                        recordAgentRoundError(lastError!!)
                                        runCatching { channel.writeEvent(streamError(lastError!!)) }
                                        done = true
                                    }
                                }
                                result.text.isBlank() && result.reasoning.isBlank() && toolCalls.isEmpty() -> {
                                    // Thinking-mode models sometimes end a turn with an
                                    // empty completion; do not silently end the session.
                                    if (retries < RuntimeDefaults.Agent.MAX_TRANSIENT_RETRIES) {
                                        retries++
                                        lastError = "model returned an empty response"
                                        Log.w(TAG, "agent round empty retry")
                                        channel.writePhase(meta, "retry", message = lastError!!)
                                        startRound(current)
                                    } else {
                                        lastError = "model returned an empty response"
                                        recordAgentRoundError(lastError!!)
                                        runCatching { channel.writeEvent(streamError(lastError!!)) }
                                        done = true
                                    }
                                }
                                toolCalls.isEmpty() || !current.executeTools -> done = true
                                rounds >= maxRounds -> {
                                    // Long multi-step UI tasks need many rounds; ending
                                    // silently here used to look like the session hung.
                                    lastError = "reached the tool-round limit ($maxRounds); the task may be incomplete"
                                    finalResult = result.copy(ok = false, toolCalls = emptyList(), message = lastError!!)
                                    recordAgentRoundError(lastError!!)
                                    runCatching { channel.writeEvent(streamError(lastError!!)) }
                                    done = true
                                }
                                else -> {
                                    val repeated = toolCalls.firstOrNull { loopGuard.observe(it) }
                                    when {
                                        repeated != null -> {
                                            lastError = "stopped after repeated tool call: ${repeated.name}"
                                            finalResult = result.copy(ok = false, toolCalls = emptyList(), message = lastError!!)
                                            recordAgentRoundError(lastError!!)
                                            runCatching { channel.writeEvent(streamError(lastError!!)) }
                                            done = true
                                        }
                                        executedToolCalls + toolCalls.size > RuntimeDefaults.Agent.MAX_TOOL_CALLS -> {
                                            lastError = "reached the tool-call limit (${RuntimeDefaults.Agent.MAX_TOOL_CALLS}); the task may be incomplete"
                                            finalResult = result.copy(ok = false, toolCalls = emptyList(), message = lastError!!)
                                            recordAgentRoundError(lastError!!)
                                            runCatching { channel.writeEvent(streamError(lastError!!)) }
                                            done = true
                                        }
                                        else -> {
                                            retries = 0
                                            val messages = current.messages.toMutableList()
                                            // Keep one assistant tool-call message for the
                                            // whole model turn, followed by its results.
                                            messages.add(ModelApiMapper.toolAssistantMessage(toolCalls, result.reasoning))
                                            toolCalls.forEach { call ->
                                                channel.writePhase(meta, "tool_running", tool = call.name, round = rounds)
                                                messages.add(ModelApiMapper.toolResultMessage(call, runAgentTool(call, executeTool)))
                                            }
                                            executedToolCalls += toolCalls.size
                                            channel.writePhase(meta, "tool_done", round = rounds)
                                            current = current.copy(messages = trimContextMessages(
                                                messages = messages,
                                                contextBudget = agentContextBudget(current.modelId, current.maxTokens),
                                                outputBudget = current.maxTokens ?: HARD_MAX_OUTPUT_TOKENS,
                                            ))
                                            rounds++
                                            startRound(current)
                                        }
                                    }
                                }
                            }
                        } else if (System.currentTimeMillis() - lastActivityAt > RuntimeDefaults.Queue.CHAT_TIMEOUT_MS) {
                            lastError = "Chat agent stream idle timed out"
                            runCatching { channel.writeEvent(streamError(lastError!!)) }
                            cancelStream(currentJob, lastError!!)
                            done = true
                        } else if (System.currentTimeMillis() - lastHeartbeatAt >= RuntimeDefaults.Queue.STREAM_HEARTBEAT_MS) {
                            channel.writeEvent(streamPing(meta, System.currentTimeMillis() - lastActivityAt))
                            lastHeartbeatAt = System.currentTimeMillis()
                        }
                    }
                }
                }

                if (finalResult != null) saveModelTurn(turnRequest, finalResult)
                channel.writeEvent(streamDone(meta, finalResult, includeUsage))
            } catch (error: CancellationException) {
                aborted = true
                abortReason = "client cancelled agent stream"
                throw error
            } catch (error: Throwable) {
                aborted = true
                abortReason = "agent stream disconnected: ${error.message ?: error::class.java.simpleName}"
                Log.i(TAG, "agent stream ended requestId=$requestId reason=$abortReason")
            } finally {
                if (aborted) job?.let { cancelStream(it, abortReason ?: "agent stream aborted") }
            }
        }
    }

    /**
     * Writes a live phase event straight to the client. Phase events bypass the
     * event queue so the UI can show tool progress even while a tool is running.
     */
    private suspend fun ByteWriteChannel.writePhase(
        meta: StreamMeta,
        phase: String,
        tool: String = "",
        round: Int = 0,
        message: String = "",
    ) {
        writeEvent(streamPhase(meta, StreamEvent.Phase(phase, tool, round, message)))
    }

    private fun agentContextBudget(modelId: String, maxTokens: Int?): Int {
        val outputBudget = maxTokens ?: HARD_MAX_OUTPUT_TOKENS
        return if (chatCapability.isCloudModel(modelId)) {
            // inputBudget = contextBudget - outputBudget - margin ≈ CLOUD_AGENT_CONTEXT_BUDGET
            CLOUD_AGENT_CONTEXT_BUDGET + outputBudget + RuntimeDefaults.Tokens.CONTEXT_SAFETY_MARGIN
        } else {
            contextWindowTokens(modelId)
        }
    }

    fun trimAgentContext(request: ModelChatRequest): ModelChatRequest = request.copy(
        messages = trimContextMessages(
            messages = request.messages,
            contextBudget = agentContextBudget(request.modelId, request.maxTokens),
            outputBudget = request.maxTokens ?: HARD_MAX_OUTPUT_TOKENS,
        )
    )

    /** Runs one tool with a hard deadline so a hanging phone-side tool cannot freeze the SSE stream. */
    private suspend fun runAgentTool(
        call: ModelToolCall,
        executeTool: (ModelToolCall) -> JSONObject,
    ): JSONObject = try {
        withTimeout(RuntimeDefaults.Agent.TOOL_TIMEOUT_MS) {
            withContext(Dispatchers.Default) { executeTool(call) }
        }
    } catch (error: TimeoutCancellationException) {
        JSONObject().put("ok", false).put("tool_call_id", call.id)
            .put("error", JSONObject()
                .put("code", "tool_timeout")
                .put("message", "tool ${call.name} timed out after ${RuntimeDefaults.Agent.TOOL_TIMEOUT_MS / 1000}s"))
    } catch (error: CancellationException) {
        throw error
    } catch (error: Throwable) {
        JSONObject().put("ok", false).put("tool_call_id", call.id)
            .put("error", JSONObject()
                .put("code", "tool_failed")
                .put("message", error.message ?: "tool failed"))
    }

    private fun recordAgentRoundError(message: String) {
        runCatching {
            sessionStore.recordRuntimeEvent(
                type = "runtime.agent_round_error",
                level = "error",
                payload = JSONObject().put("message", message),
            )
        }
    }

    private fun ModelChatToolFallback(call: ModelToolCall): JSONObject = JSONObject()
        .put("ok", false)
        .put("tool_call_id", call.id)
        .put("error", JSONObject().put("code", "tool_failed").put("message", "tool execution unavailable"))

    private fun streamStart(meta: StreamMeta) = "data: ${chunkJson(meta, "role", "assistant")}\n\n"
    private fun streamChunk(meta: StreamMeta, text: String) = "data: ${chunkJson(meta, "content", text)}\n\n"
    private fun streamReasoning(meta: StreamMeta, text: String) = "data: ${reasoningChunkJson(meta, text)}\n\n"
    private fun streamToolCalls(meta: StreamMeta, calls: List<ModelToolCall>) = "data: ${toolCallsChunkJson(meta, calls)}\n\n"
    private fun streamPing(meta: StreamMeta, idleMs: Long): String {
        val lociant = JSONObject().put("type", "ping").put("idleMs", idleMs)
        return "data: ${phaseJson(meta, lociant)}\n\n"
    }

    private fun streamPhase(meta: StreamMeta, event: StreamEvent.Phase): String {
        val lociant = JSONObject().put("type", "phase").put("phase", event.phase)
            .apply {
                if (event.tool.isNotBlank()) put("tool", event.tool)
                if (event.round > 0) put("round", event.round)
                if (event.message.isNotBlank()) put("message", event.message)
            }
        return "data: ${phaseJson(meta, lociant)}\n\n"
    }

    private fun phaseJson(meta: StreamMeta, lociant: JSONObject): JSONObject = JSONObject()
        .put("id", meta.id).put("object", "chat.completion.chunk")
        .put("created", meta.created).put("model", meta.modelId)
        .put("choices", JSONArray())
        .put("lociant", lociant)

    private fun streamError(message: String) = "data: ${ModelApiMapper.error("chat_failed", message)}\n\n"
    private fun streamDone(meta: StreamMeta, result: ModelChatResult?, includeUsage: Boolean) = buildString {
        val reason = if (result?.toolCalls?.isNotEmpty() == true) "tool_calls" else "stop"
        append("data: ${chunkJson(meta, null, reason)}\n\n")
        if (includeUsage && result != null) append("data: ${usageChunkJson(meta, result)}\n\n")
        append("data: [DONE]\n\n")
    }

    private sealed class StreamEvent {
        data class Chunk(val text: String) : StreamEvent()
        data class Reasoning(val text: String) : StreamEvent()
        data class ToolCall(val call: ModelToolCall) : StreamEvent()
        data class Phase(
            val phase: String,
            val tool: String = "",
            val round: Int = 0,
            val message: String = "",
        ) : StreamEvent()
        data object Done : StreamEvent()
        data class Error(val message: String) : StreamEvent()
    }

    companion object {
        private const val TAG = "LociantChat"
        private const val TOOL_BUFFER_FLUSH_LIMIT = 4096
        // Short poll so round boundaries (model finished, tool result ready) are
        // noticed immediately instead of waiting out a long heartbeat window.
        private const val ROUND_POLL_MS = 250L
        // A phone-side tool (accessibility, screenshots, app switching) must
        // finish within this window or the round fails with tool_timeout.
        // The value lives in RuntimeDefaults.Agent so streaming and non-streaming
        // execution share the same budget.
        // Cloud agent loops must not re-send unbounded history every round.
        // Cap the input budget so each DeepSeek request stays small and fast
        // instead of growing until the upstream connection stalls.
        private const val CLOUD_AGENT_CONTEXT_BUDGET = 40_000
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
        .takeLast(RuntimeDefaults.Sessions.MAX_SYSTEM_MESSAGES)
    val nonSystem = messages.filterNot { it.role.equals("system", ignoreCase = true) }
    val latestUserIndex = nonSystem.indexOfLast { it.role.equals("user", ignoreCase = true) }
        .takeIf { it >= 0 }
        ?: nonSystem.indexOfLast { !it.role.equals("tool", ignoreCase = true) }
    val selectedIndices = ArrayDeque<Int>()
    var used = system.sumOf { estimateMessageTokens(it) }

    for (index in nonSystem.indices.reversed()) {
        val message = nonSystem[index]
        val cost = estimateMessageTokens(message)
        if (selectedIndices.isNotEmpty() && used + cost > inputBudget) break
        selectedIndices.addFirst(index)
        used += cost
    }
    // The latest user request is mandatory even when a large tool result used
    // most of the budget. Without it the model can only see its own last action
    // and will often repeat that action forever.
    if (latestUserIndex >= 0 && !selectedIndices.contains(latestUserIndex)) {
        selectedIndices.addLast(latestUserIndex)
    }

    val selected = selectedIndices.toList().distinct().sorted().map { nonSystem[it] }
    val trimmed = normalizeToolMessagePairs(system + selected)
    return trimmed.ifEmpty { listOfNotNull(nonSystem.getOrNull(latestUserIndex)) }
}

/** Keeps tool-call messages valid for strict OpenAI-compatible providers. */
private fun normalizeToolMessagePairs(
    messages: List<ModelChatRequestMessageAlias>,
): List<ModelChatRequestMessageAlias> {
    val normalized = mutableListOf<ModelChatRequestMessageAlias>()
    var index = 0
    while (index < messages.size) {
        val message = messages[index]
        when {
            message.role.equals("tool", ignoreCase = true) -> index++
            message.role.equals("assistant", ignoreCase = true) && message.toolCalls.isNotEmpty() -> {
                val results = mutableListOf<ModelChatRequestMessageAlias>()
                var next = index + 1
                while (next < messages.size && messages[next].role.equals("tool", ignoreCase = true)) {
                    results += messages[next]
                    next++
                }
                val resultIds = results.map { it.toolCallId }.toSet()
                if (message.toolCalls.all { it.id in resultIds }) {
                    normalized += message
                    normalized += results
                }
                index = next
            }
            else -> {
                normalized += message
                index++
            }
        }
    }
    return normalized
}

private typealias ModelChatRequestMessageAlias = io.lociant.core.model.ModelChatMessage

private fun estimateMessageTokens(message: ModelChatRequestMessageAlias): Int {
    val textTokens = (message.text().length + 3) / 4
    val imageTokens = message.parts.count { it is io.lociant.core.model.ModelChatPart.Image } * RuntimeDefaults.Tokens.IMAGE_ESTIMATE
    return RuntimeDefaults.Tokens.MESSAGE_OVERHEAD + textTokens + imageTokens
}

private data class StreamMeta(val id: String, val created: Long, val modelId: String)
private fun reasoningChunkJson(meta: StreamMeta, text: String): JSONObject {
    val delta = JSONObject().put("reasoning_content", text)
    return JSONObject()
        .put("id", meta.id).put("object", "chat.completion.chunk")
        .put("created", meta.created).put("model", meta.modelId)
        .put("choices", JSONArray().put(JSONObject()
            .put("index", 0).put("delta", delta)
            .put("finish_reason", JSONObject.NULL)))
}

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
