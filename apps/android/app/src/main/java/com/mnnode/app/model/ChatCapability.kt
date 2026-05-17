package com.mnnode.app.model

import android.util.Log
import java.io.File

class ChatCapability(
    private val modelManager: ModelManager,
    private val mnnRuntime: MnnRuntime,
) {
    fun isLoaded(modelId: String): Boolean = loadedModelId == normalize(modelId)

    fun resetLoadedModel() {
        loadedModelId = null
    }

    fun configureCpuThreads(value: Int): Boolean {
        val changed = mnnRuntime.configureCpuThreads(value)
        if (changed) resetLoadedModel()
        return changed
    }

    fun preload(modelIdRaw: String): ModelChatResult {
        val modelId = normalize(modelIdRaw)
        if (isLoaded(modelId)) return ModelChatResult(ok = true, modelId = modelId, message = "already loaded")
        val dir = resolveModel(modelId)
            ?: return ModelChatResult(ok = false, modelId = modelId, message = "MNN chat model not installed")
        val result = mnnRuntime.preload(dir)
        if (result.ok) loadedModelId = modelId
        return ModelChatResult(ok = result.ok, modelId = modelId, message = result.message)
    }

    fun maxTokens(modelIdRaw: String, serverCap: Int = HARD_MAX_OUTPUT_TOKENS): Int {
        return minOf(serverCap, HARD_MAX_OUTPUT_TOKENS).coerceAtLeast(MIN_OUTPUT_TOKENS)
    }

    fun contextWindowTokens(modelIdRaw: String): Int {
        return modelManager.contextWindowTokens(normalize(modelIdRaw))
            ?.coerceIn(MIN_CONTEXT_TOKENS, HARD_MAX_CONTEXT_TOKENS)
            ?: DEFAULT_CONTEXT_TOKENS
    }

    fun clampTokens(modelIdRaw: String, requested: Int, serverCap: Int = HARD_MAX_OUTPUT_TOKENS): Int {
        return requested.coerceIn(MIN_OUTPUT_TOKENS, maxTokens(modelIdRaw, serverCap))
    }

    fun complete(request: ModelChatRequest): ModelChatResult {
        return run(request) { modelDir, modelId, maxTokens, images ->
            if (images.isEmpty()) {
                mnnRuntime.chatText(
                    modelDir,
                    nativeMessages(request.messages),
                    request.tools?.toString().orEmpty(),
                    maxTokens,
                    request.sessionId,
                    request.useSessionCache,
                )
            } else {
                mnnRuntime.chatImage(
                    modelDir,
                    images.first().bytes,
                    renderMultimodalPrompt(request.messages),
                    maxTokens,
                )
            }.toModelResult(modelId)
        }
    }

    fun stream(request: ModelChatRequest, onChunk: (text: String, done: Boolean) -> Unit): ModelChatResult {
        val responseText = StringBuilder()
        return run(request) { modelDir, modelId, maxTokens, images ->
            val result = if (images.isEmpty()) {
                mnnRuntime.chatTextStream(
                    modelDir,
                    nativeMessages(request.messages),
                    request.tools?.toString().orEmpty(),
                    maxTokens,
                    request.sessionId,
                    request.useSessionCache,
                ) { text, done ->
                    if (text.isNotEmpty()) responseText.append(text)
                    onChunk(text, done)
                }
            } else {
                mnnRuntime.chatImageStream(
                    modelDir,
                    images.first().bytes,
                    renderMultimodalPrompt(request.messages),
                    maxTokens,
                ) { text, done ->
                    if (text.isNotEmpty()) responseText.append(text)
                    onChunk(text, done)
                }
            }
            result.toModelResult(modelId, text = responseText.toString())
        }
    }

    fun cancel() {
        mnnRuntime.cancel()
    }

    private fun run(
        request: ModelChatRequest,
        block: (File, String, Int, List<ModelChatPart.Image>) -> ModelChatResult,
    ): ModelChatResult {
        val started = System.currentTimeMillis()
        val modelId = normalize(request.modelId)
        val images = request.messages.flatMap { it.parts.filterIsInstance<ModelChatPart.Image>() }
        Log.i(TAG, "chat source=${request.source} modelId=$modelId messages=${request.messages.size} images=${images.size}")

        val dir = resolveModel(modelId)
            ?: return ModelChatResult(ok = false, modelId = modelId, message = "MNN chat model not installed", elapsedMs = elapsed(started))
        if (images.size > 1) {
            return ModelChatResult(
                ok = false,
                modelId = modelId,
                message = "Only one image per request is supported in this version",
                elapsedMs = elapsed(started),
            )
        }

        return block(dir, modelId, clampTokens(modelId, request.maxTokens ?: DEFAULT_OUTPUT_TOKENS), images)
            .also { if (it.ok) loadedModelId = modelId }
            .withParsedToolCalls(request)
            .copy(elapsedMs = elapsed(started))
            .also { Log.i(TAG, "chat end modelId=$modelId ok=${it.ok} elapsed=${it.elapsedMs} textLen=${it.text.length}") }
    }

    private fun resolveModel(modelIdRaw: String): File? {
        val modelId = normalize(modelIdRaw)
        val status = modelManager.resolve(modelId)
        val dir = modelManager.resolveDir(modelId)
        if (dir != null && status.spec.runtime == "mnn") return dir
        Log.w(TAG, "model unavailable modelId=$modelId runtime=${status.spec.runtime}")
        return null
    }

    private fun NativeChatResult.toModelResult(
        modelId: String,
        text: String = this.text,
    ): ModelChatResult {
        return ModelChatResult(
            ok = ok,
            modelId = modelId,
            text = text,
            message = message.ifBlank { if (ok) "chat finished" else "chat failed" },
            promptTokens = promptTokens,
            generatedTokens = generatedTokens,
            cachedTokens = cachedTokens,
            cacheEnabled = cacheEnabled,
            cacheHit = cacheHit,
            firstTokenMs = firstTokenMs,
            prefillUs = prefillUs,
            decodeUs = decodeUs,
        )
    }

    private fun ModelChatResult.withParsedToolCalls(request: ModelChatRequest): ModelChatResult {
        if (!ok || request.tools == null || toolCalls.isNotEmpty()) return this
        val parsed = ToolTemplateContract.parse(text)
        return if (parsed.isEmpty()) this else copy(text = "", toolCalls = parsed)
    }

    private fun nativeMessages(messages: List<ModelChatMessage>): List<NativeChatMessage> =
        messages.mapNotNull { message ->
            val json = message.toMnnJsonMessage()
            if (json != null) NativeChatMessage("json", json.toString())
            else message.text().takeIf { it.isNotBlank() }?.let { NativeChatMessage(message.role, it) }
        }

    private fun ModelChatMessage.toMnnJsonMessage(): org.json.JSONObject? {
        val hasExtra = toolCalls.isNotEmpty() || toolCallId.isNotBlank() || name.isNotBlank()
        if (!hasExtra) return null
        val json = org.json.JSONObject()
            .put("role", role)
            .put("content", text())
        if (name.isNotBlank()) json.put("name", name)
        if (toolCallId.isNotBlank()) json.put("tool_call_id", toolCallId)
        if (toolCalls.isNotEmpty()) {
            json.put("tool_calls", org.json.JSONArray(toolCalls.map { call ->
                org.json.JSONObject()
                    .put("id", call.id)
                    .put("type", "function")
                    .put("function", org.json.JSONObject()
                        .put("name", call.name)
                        .put("arguments", call.arguments.ifBlank { "{}" }))
            }))
        }
        return json
    }

    private fun renderMultimodalPrompt(messages: List<ModelChatMessage>): String {
        var imageInserted = false
        return messages.joinToString("\n\n") { message ->
            val text = message.text()
            val hasImage = message.parts.any { it is ModelChatPart.Image }
            val content = buildString {
                if (hasImage && !imageInserted) {
                    append("<img>image_0</img>")
                    imageInserted = true
                    if (text.isNotBlank()) append("\n")
                }
                append(text)
            }.trim()
            "${message.role}: $content"
        }.trim()
    }

    private fun normalize(value: String) = ModelManager.normalizeId(value).ifBlank { DEFAULT_MODEL_ID }

    private fun elapsed(started: Long) = System.currentTimeMillis() - started

    @Volatile private var loadedModelId: String? = null

    companion object {
        private const val TAG = "MNNodeChatCapability"
        private const val MIN_CONTEXT_TOKENS = 1024
        private const val DEFAULT_CONTEXT_TOKENS = 4096
        private const val HARD_MAX_CONTEXT_TOKENS = 32768
    }
}
