package io.lociant.android.server

import io.lociant.core.model.DEFAULT_MODEL_ID
import io.lociant.core.model.ModelChatMessage
import io.lociant.core.model.ModelChatPart
import io.lociant.core.model.ModelChatRequest
import io.lociant.core.model.ModelChatResult
import io.lociant.core.model.ModelToolCall
import io.lociant.core.model.ModelToolChoice
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/**
 * OpenAI request parsing for the Rust IPC chat bridge. The response mapping
 * moved to the Rust backend; this is the only remaining surface.
 */
object ModelApiMapper {
    fun parseOpenAiChat(raw: String): ModelChatRequest {
        val json = JSONObject(raw)
        return ModelChatRequest(
            modelId = json.optString("model", DEFAULT_MODEL_ID),
            messages = parseOpenAiMessages(json.optJSONArray("messages") ?: JSONArray()),
            maxTokens = optionalInt(json, "max_tokens"),
            stream = json.optBoolean("stream", false),
            source = "openai.chat",
            sessionId = parseSessionId(json),
            tools = json.optJSONArray("tools"),
            toolChoice = parseToolChoice(json.opt("tool_choice")),
            executeTools = json.optBoolean("execute_tools", false),
        )
    }

    private fun parseOpenAiMessages(array: JSONArray): List<ModelChatMessage> {
        return List(array.length()) { index -> array.optJSONObject(index) }
            .filterNotNull()
            .map { message ->
                ModelChatMessage(
                    role = message.optString("role", "user"),
                    parts = parseOpenAiContent(message.opt("content")),
                    name = message.optString("name"),
                    toolCallId = message.optString("tool_call_id"),
                    toolCalls = parseToolCalls(message.optJSONArray("tool_calls")),
                    reasoning = message.optString("reasoning_content"),
                )
            }
    }

    private fun parseOpenAiContent(content: Any?): List<ModelChatPart> {
        return when (content) {
            is String -> listOf(ModelChatPart.Text(content))
            is JSONArray -> List(content.length()) { index -> content.optJSONObject(index) }
                .filterNotNull()
                .mapNotNull { item ->
                    when (item.optString("type")) {
                        "text" -> ModelChatPart.Text(item.optString("text"))
                        "image_url" -> {
                            val url = item.optJSONObject("image_url")?.optString("url").orEmpty()
                            ModelChatPart.decodeImagePart(url)
                        }
                        else -> null
                    }
                }
            else -> emptyList()
        }
    }

    private fun parseSessionId(json: JSONObject): String {
        // `sessionId` is the sole extension key. Invalid explicit values must
        // reach SessionIds unchanged instead of falling through to aliases.
        if (json.has("sessionId") && !json.isNull("sessionId")) return json.getString("sessionId")
        return ""
    }

    private fun optionalInt(json: JSONObject, key: String): Int? {
        if (!json.has(key) || json.isNull(key)) return null
        return json.optInt(key).takeIf { it > 0 }
    }

    private fun parseToolChoice(value: Any?): ModelToolChoice {
        return when (value) {
            null, JSONObject.NULL -> ModelToolChoice.Auto
            is String -> when (value.lowercase()) {
                "none" -> ModelToolChoice.None
                "required" -> ModelToolChoice.Required
                else -> ModelToolChoice.Auto
            }
            is JSONObject -> {
                val fn = value.optJSONObject("function")
                val name = fn?.optString("name").orEmpty().ifBlank { value.optString("name") }
                if (name.isBlank()) ModelToolChoice.Auto else ModelToolChoice.Function(
                    name = name,
                    arguments = fn?.optString("arguments", "{}") ?: value.optString("arguments", "{}"),
                )
            }
            else -> ModelToolChoice.Auto
        }
    }

    private fun parseToolCalls(array: JSONArray?): List<ModelToolCall> {
        if (array == null) return emptyList()
        return List(array.length()) { index -> array.optJSONObject(index) }
            .filterNotNull()
            .mapNotNull { item ->
                val function = item.optJSONObject("function") ?: return@mapNotNull null
                ModelToolCall(
                    id = item.optString("id").ifBlank { "call_${UUID.randomUUID().toString().take(8)}" },
                    name = function.optString("name"),
                    arguments = function.optString("arguments", "{}"),
                )
            }
            .filter { it.name.isNotBlank() }
    }

    /** Usage/telemetry formatting for the live `llm` tool result. */
    fun openAiUsage(result: ModelChatResult): JSONObject {
        return JSONObject()
            .put("prompt_tokens", result.promptTokens)
            .put("completion_tokens", result.generatedTokens)
            .put("total_tokens", result.totalTokens)
            .put("prompt_tokens_details", JSONObject()
                .put("cached_tokens", result.cachedTokens))
    }

    fun runtimeMetrics(result: ModelChatResult): JSONObject {
        return JSONObject()
            .put("elapsed_ms", result.elapsedMs)
            .put("first_token_ms", result.firstTokenMs)
            .put("wall_tokens_per_second", result.wallTokensPerSecond)
            .put("tokens_per_second", result.tokensPerSecond)
            .put("prefill_us", result.prefillUs)
            .put("decode_us", result.decodeUs)
            .put("decode_tokens_per_second", result.tokensPerSecond)
            .put("cache", JSONObject()
                .put("enabled", result.cacheEnabled)
                .put("hit", result.cacheHit))
    }
}
