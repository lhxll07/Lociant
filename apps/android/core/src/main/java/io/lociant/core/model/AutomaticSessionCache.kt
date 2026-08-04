package io.lociant.core.model

/**
 * Tracks one implicit, in-memory conversation for clients that resend their
 * complete OpenAI message list on every request.
 *
 * The native runtime owns the actual KV cache. This class only decides whether
 * a request is safe to send as a prompt-cache continuation and keeps the
 * matching full history until the model has completed successfully. MNN needs
 * the full prompt to compare its cached prefix, so this class never truncates
 * the messages passed to the native runtime.
 */
class AutomaticSessionCache {
    private var epoch = 0L
    private var nextGeneration = 0L
    private var inFlight = 0
    private var snapshot: Snapshot? = null
    private val pending = mutableMapOf<String, Prepared>()

    @Synchronized
    fun prepare(request: ModelChatRequest, runtimeConfiguration: String): ModelChatRequest {
        if (!isEligible(request)) {
            invalidate()
            return request.copy(sessionId = "", useSessionCache = false)
        }

        val key = CacheKey(
            modelId = request.modelId,
            maxTokens = request.maxTokens,
            runtimeConfiguration = runtimeConfiguration,
            tools = request.tools?.toString().orEmpty(),
        )
        val previous = snapshot
        val canAppend = inFlight == 0 && previous != null && previous.key == key &&
            isStrictPrefix(previous.messages, request.messages)
        val generation = if (canAppend) previous.generation else newGeneration()
        val sessionId = "__automatic_cache_$generation"
        val prepared = Prepared(
            epoch = epoch,
            generation = generation,
            key = key,
            fullMessages = request.messages,
        )
        pending[sessionId] = prepared
        inFlight++
        return request.copy(
            sessionId = sessionId,
            useSessionCache = true,
            persistSession = false,
        )
    }

    /** Commits only complete model turns; failures and cancellations invalidate reuse. */
    @Synchronized
    fun commit(request: ModelChatRequest, result: ModelChatResult) {
        val sessionId = request.sessionId
        if (!sessionId.startsWith(AUTOMATIC_SESSION_PREFIX)) return
        val prepared = pending.remove(sessionId) ?: return
        inFlight = (inFlight - 1).coerceAtLeast(0)
        if (!result.ok || result.cancelled || result.toolCalls.isNotEmpty() || result.text.isBlank()) return
        if (prepared.epoch != epoch) return

        snapshot = Snapshot(
            generation = prepared.generation,
            key = prepared.key,
            messages = prepared.fullMessages + ModelChatMessage(
                role = "assistant",
                parts = listOf(ModelChatPart.Text(result.text)),
            ),
        )
    }

    /** Invalidates pending and committed state after a session/runtime change. */
    @Synchronized
    fun invalidate() {
        epoch++
        snapshot = null
        pending.clear()
        inFlight = 0
    }

    private fun newGeneration(): Long {
        nextGeneration++
        return nextGeneration
    }

    private fun isEligible(request: ModelChatRequest): Boolean {
        if (request.messages.isEmpty() || request.executeTools) return false
        if (request.toolChoice !is ModelToolChoice.Auto && request.toolChoice !is ModelToolChoice.None) return false
        if (request.messages.last().role.lowercase() != "user") return false
        return request.messages.all { message ->
            message.role.lowercase() in TEXT_ROLES &&
                message.name.isBlank() &&
                message.toolCallId.isBlank() &&
                message.toolCalls.isEmpty() &&
                message.parts.isNotEmpty() &&
                message.parts.all { it is ModelChatPart.Text }
        } && request.messages.last().text().isNotBlank()
    }

    private fun isStrictPrefix(prefix: List<ModelChatMessage>, value: List<ModelChatMessage>): Boolean {
        if (value.size != prefix.size + 1) return false
        val samePromptPrefix = prefix.indices.all { index ->
            comparableMessage(prefix[index]) == comparableMessage(value[index])
        }
        return samePromptPrefix &&
            value.last().role.lowercase() == "user"
    }

    /**
     * Reasoning is returned by OpenAI-compatible clients as assistant metadata,
     * but the local MNN prompt only contains role and text. Ignore that
     * metadata when matching a client-resubmitted history; otherwise a client
     * that preserves reasoning_content would disable the native KV cache on
     * every follow-up turn.
     */
    private fun comparableMessage(message: ModelChatMessage): ModelChatMessage =
        if (message.reasoning.isBlank()) message else message.copy(reasoning = "")

    private data class CacheKey(
        val modelId: String,
        val maxTokens: Int?,
        val runtimeConfiguration: String,
        /** Tool schemas are part of MNN's prompt configuration. */
        val tools: String,
    )

    private data class Snapshot(
        val generation: Long,
        val key: CacheKey,
        val messages: List<ModelChatMessage>,
    )

    private data class Prepared(
        val epoch: Long,
        val generation: Long,
        val key: CacheKey,
        val fullMessages: List<ModelChatMessage>,
    )

    companion object {
        const val AUTOMATIC_SESSION_PREFIX = "__automatic_cache_"
        private val TEXT_ROLES = setOf("system", "user", "assistant")
    }
}
