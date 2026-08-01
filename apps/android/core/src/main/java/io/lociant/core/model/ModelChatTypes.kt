package io.lociant.core.model

import android.util.Base64
import io.lociant.core.config.RuntimeDefaults

data class ModelChatRequest(
    val modelId: String,
    val messages: List<ModelChatMessage>,
    val maxTokens: Int? = null,
    val stream: Boolean = false,
    val source: String = "internal",
    val sessionId: String = "",
    val persistSession: Boolean = false,
    val useSessionCache: Boolean = false,
    val tools: org.json.JSONArray? = null,
    val toolChoice: ModelToolChoice = ModelToolChoice.Auto,
    val executeTools: Boolean = false,
)

data class ModelChatMessage(
    val role: String,
    val parts: List<ModelChatPart>,
    val name: String = "",
    val toolCallId: String = "",
    val toolCalls: List<ModelToolCall> = emptyList(),
) {
    fun text(): String {
        return parts.filterIsInstance<ModelChatPart.Text>().joinToString("\n") { it.text }.trim()
    }
}

data class ModelToolCall(
    val id: String,
    val name: String,
    val arguments: String = "{}",
)

sealed class ModelToolChoice {
    data object Auto : ModelToolChoice()
    data object None : ModelToolChoice()
    data object Required : ModelToolChoice()
    data class Function(val name: String, val arguments: String = "{}") : ModelToolChoice()
}

sealed class ModelChatPart {
    data class Text(val text: String) : ModelChatPart()
    data class Image(val mimeType: String, val bytes: ByteArray) : ModelChatPart()

    companion object {
        fun decodeImagePart(raw: String): Image? {
            if (raw.isBlank()) return null
            val mimeType = raw.substringAfter("data:", "").substringBefore(";base64", "image/jpeg")
                .ifBlank { "image/jpeg" }
            val base64 = raw.substringAfter("base64,", raw)
            // Check the encoded size before Base64.decode can allocate the full byte array.
            if (base64.length > MAX_BASE64_IMAGE_CHARS) return null
            val bytes = runCatching { Base64.decode(base64, Base64.DEFAULT) }.getOrNull() ?: return null
            if (bytes.size > MAX_IMAGE_BYTES) return null
            return Image(mimeType, bytes)
        }

        private const val MAX_IMAGE_BYTES = 16 * 1024 * 1024
        private const val MAX_BASE64_IMAGE_CHARS = (MAX_IMAGE_BYTES * 4 / 3) + 4
    }
}

data class ModelChatResult(
    val ok: Boolean,
    val modelId: String,
    val text: String = "",
    val message: String = "",
    val toolCalls: List<ModelToolCall> = emptyList(),
    val elapsedMs: Long = 0,
    val promptTokens: Int = 0,
    val generatedTokens: Int = 0,
    val cachedTokens: Int = 0,
    val cacheEnabled: Boolean = false,
    val cacheHit: Boolean = false,
    val firstTokenMs: Long = 0,
    val prefillUs: Long = 0,
    val decodeUs: Long = 0,
) {
    val totalTokens: Int get() = promptTokens + generatedTokens
    val wallTokensPerSecond: Double get() =
        if (elapsedMs > 0 && generatedTokens > 0) generatedTokens * 1000.0 / elapsedMs else 0.0
    val tokensPerSecond: Double get() =
        if (decodeUs > 0 && generatedTokens > 0) generatedTokens * 1_000_000.0 / decodeUs else wallTokensPerSecond

    fun toJson(): org.json.JSONObject = org.json.JSONObject()
        .put("ok", ok).put("modelId", modelId).put("runtime", "mnn")
        .put("message", message).put("elapsedMs", elapsedMs).put("text", text)
        .put("usage", org.json.JSONObject()
            .put("promptTokens", promptTokens)
            .put("generatedTokens", generatedTokens)
            .put("cachedTokens", cachedTokens)
            .put("totalTokens", totalTokens)
            .put("tokensPerSecond", tokensPerSecond)
            .put("wallTokensPerSecond", wallTokensPerSecond)
            .put("firstTokenMs", firstTokenMs)
            .put("prefillUs", prefillUs)
            .put("decodeUs", decodeUs))
}

data class NativeChatResult(
    val ok: Boolean,
    val text: String = "",
    val message: String = "",
    val modelInstalled: Boolean = true,
    val promptTokens: Int = 0,
    val generatedTokens: Int = 0,
    val cachedTokens: Int = 0,
    val cacheEnabled: Boolean = false,
    val cacheHit: Boolean = false,
    val firstTokenMs: Long = 0,
    val prefillUs: Long = 0,
    val decodeUs: Long = 0,
)

data class NativeChatMessage(
    val role: String,
    val content: String,
)

const val DEFAULT_MODEL_ID = RuntimeDefaults.MODEL_ID
const val DEFAULT_OUTPUT_TOKENS = RuntimeDefaults.Tokens.OUTPUT_DEFAULT
const val MIN_OUTPUT_TOKENS = RuntimeDefaults.Tokens.OUTPUT_MIN
const val HARD_MAX_OUTPUT_TOKENS = RuntimeDefaults.Tokens.OUTPUT_MAX
