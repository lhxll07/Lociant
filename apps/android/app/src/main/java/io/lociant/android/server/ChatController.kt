package io.lociant.android.server

import android.util.Log
import io.lociant.core.config.RuntimeDefaults
import io.lociant.core.model.AutomaticSessionCache
import io.lociant.core.model.DEFAULT_MODEL_ID
import io.lociant.core.model.HARD_MAX_OUTPUT_TOKENS
import io.lociant.core.model.ModelChatRequest
import io.lociant.core.model.ModelChatResult
import io.lociant.core.model.ModelToolCall
import io.lociant.core.model.ToolTemplateContract
import io.lociant.runtime.model.ChatCapability
import io.lociant.runtime.model.ModelManager
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Runs model turns for the device layer. The HTTP/agent-loop surface moved to
 * the Rust backend; the live paths here are the Rust IPC chat bridge
 * ([streamOneTurn]) and the `llm` tool ([boundRequest] / [sessionRequest] /
 * [submitSync] / session persistence).
 */
class ChatController(
    private val chatCapability: ChatCapability,
) {
    private val modelLoading = AtomicBoolean(false)
    @Volatile private var historyLimit = RuntimeDefaults.Sessions.MODEL_HISTORY_LIMIT
    @Volatile var lastError: String? = null

    private val requestQueue = ChatRequestQueue()
    private val automaticSessionCache = AutomaticSessionCache()

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

    // ---- Chat execution ----

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

    /**
     * Runs one model turn for the Rust IPC chat bridge: streams chunks with
     * the same local tool-template filtering the agent loop used (template
     * text is consumed here, parsed tool calls arrive through [onToolCall]).
     */
    fun streamOneTurn(
        request: ModelChatRequest,
        onChunk: (text: String, done: Boolean) -> Unit,
        onReasoning: ((text: String, done: Boolean) -> Unit)? = null,
        onToolCall: ((ModelToolCall) -> Unit)? = null,
    ): ModelChatResult {
        val toolTextBuffer = StringBuilder()
        return executeChatStream(
            request,
            { text, done ->
                if (text.isEmpty()) return@executeChatStream
                toolTextBuffer.append(text)
                val candidate = toolTextBuffer.toString()
                val parsed = ToolTemplateContract.parse(candidate)
                if (parsed.isNotEmpty()) {
                    toolTextBuffer.setLength(0)
                } else if (
                    !looksLikeToolPrefix(candidate) ||
                    candidate.length > TOOL_BUFFER_FLUSH_LIMIT
                ) {
                    onChunk(candidate, done)
                    toolTextBuffer.setLength(0)
                }
            },
            onReasoning,
            onToolCall,
        )
    }

    // ---- Request assembly (llm tool) ----

    fun boundRequest(request: ModelChatRequest, serverModelId: String, serverMaxOutputTokens: Int): ModelChatRequest {
        val normalized = ModelManager.normalizeId(request.modelId).ifBlank { serverModelId }
        val maxTokens = clampTokens(normalized, request.maxTokens ?: serverMaxOutputTokens, serverMaxOutputTokens)
        return request.copy(modelId = normalized, maxTokens = maxTokens)
    }

    fun sessionRequest(request: ModelChatRequest): ModelChatRequest {
        val contextMessages = trimContextMessages(
            messages = request.messages,
            contextBudget = contextWindowTokens(request.modelId),
            outputBudget = request.maxTokens ?: HARD_MAX_OUTPUT_TOKENS,
        )
        return request.copy(messages = contextMessages)
    }

    fun submitSync(request: ModelChatRequest, timeoutMs: Long): ModelChatResult =
        requestQueue.submitSync(request.modelId, request.source, timeoutMs, chatCapability::cancel) {
            executeChat(request)
        }

    companion object {
        private const val TAG = "LociantChat"
        private const val TOOL_BUFFER_FLUSH_LIMIT = 4096
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
