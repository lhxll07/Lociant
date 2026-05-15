package com.mnnode.app.model

import android.util.Base64

data class ModelChatRequest(
    val modelId: String,
    val messages: List<ModelChatMessage>,
    val maxTokens: Int = DEFAULT_OUTPUT_TOKENS,
    val stream: Boolean = false,
    val source: String = "internal",
    val sessionId: String = "",
    val persistSession: Boolean = false,
    val useSessionCache: Boolean = false,
)

data class ModelChatMessage(
    val role: String,
    val parts: List<ModelChatPart>,
) {
    fun text(): String {
        return parts.filterIsInstance<ModelChatPart.Text>().joinToString("\n") { it.text }.trim()
    }
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
            val bytes = runCatching { Base64.decode(base64, Base64.DEFAULT) }.getOrNull() ?: return null
            return Image(mimeType, bytes)
        }
    }
}

data class ModelChatResult(
    val ok: Boolean,
    val modelId: String,
    val text: String = "",
    val message: String = "",
    val elapsedMs: Long = 0,
    val promptTokens: Int = 0,
    val generatedTokens: Int = 0,
) {
    fun toJson(): org.json.JSONObject = org.json.JSONObject()
        .put("ok", ok).put("modelId", modelId).put("runtime", "mnn")
        .put("message", message).put("elapsedMs", elapsedMs).put("text", text)
}

data class NativeChatResult(
    val ok: Boolean,
    val text: String = "",
    val message: String = "",
    val modelInstalled: Boolean = true,
    val promptTokens: Int = 0,
    val generatedTokens: Int = 0,
)

const val DEFAULT_MODEL_ID = "qwen3.5-2b-mnn"
const val DEFAULT_OUTPUT_TOKENS = 512
const val MIN_OUTPUT_TOKENS = 8
const val HARD_MAX_OUTPUT_TOKENS = 4096
