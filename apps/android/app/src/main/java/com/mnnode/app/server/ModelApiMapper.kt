package com.mnnode.app.server

import com.mnnode.app.model.ModelChatMessage
import com.mnnode.app.model.ModelChatPart
import com.mnnode.app.model.ModelChatRequest
import com.mnnode.app.model.ModelChatResult
import com.mnnode.app.model.DEFAULT_MODEL_ID
import com.mnnode.app.model.DEFAULT_OUTPUT_TOKENS
import org.json.JSONArray
import org.json.JSONObject

object ModelApiMapper {
    fun parseOpenAiChat(raw: String): ModelChatRequest {
        val json = JSONObject(raw)
        return ModelChatRequest(
            modelId = json.optString("model", DEFAULT_MODEL_ID),
            messages = parseOpenAiMessages(json.optJSONArray("messages") ?: JSONArray()),
            maxTokens = json.optInt("max_tokens", json.optInt("maxTokens", DEFAULT_OUTPUT_TOKENS)),
            stream = json.optBoolean("stream", false),
            source = "openai.chat",
            sessionId = parseSessionId(json),
        )
    }

    fun parseOllamaChat(raw: String): ModelChatRequest {
        val json = JSONObject(raw)
        val messages = parseOllamaMessages(json.optJSONArray("messages") ?: JSONArray())
        return ModelChatRequest(
            modelId = json.optString("model", DEFAULT_MODEL_ID),
            messages = messages,
            maxTokens = json.optJSONObject("options")?.optInt("num_predict", DEFAULT_OUTPUT_TOKENS)
                ?: DEFAULT_OUTPUT_TOKENS,
            stream = json.optBoolean("stream", true),
            source = "ollama.chat",
            sessionId = parseSessionId(json),
        )
    }

    fun parseSceneChat(json: JSONObject): ModelChatRequest {
        return ModelChatRequest(
            modelId = json.optString("model", json.optString("modelId", DEFAULT_MODEL_ID)),
            messages = parseSceneMessages(json),
            maxTokens = json.optInt("max_tokens", json.optInt("maxTokens", DEFAULT_OUTPUT_TOKENS)),
            stream = false,
            source = "scene.model-chat",
            sessionId = parseSessionId(json),
        )
    }

    fun openAiResponse(result: ModelChatResult): JSONObject {
        return JSONObject()
            .put("id", "chatcmpl_mnnode_${System.currentTimeMillis()}")
            .put("object", "chat.completion")
            .put("created", System.currentTimeMillis() / 1000)
            .put("model", result.modelId)
            .put(
                "choices",
                JSONArray().put(
                    JSONObject()
                        .put("index", 0)
                        .put("message", JSONObject()
                            .put("role", "assistant")
                            .put("content", result.text))
                        .put("finish_reason", if (result.ok) "stop" else "error")
                )
            )
            .put("usage", JSONObject()
                .put("prompt_tokens", result.promptTokens)
                .put("completion_tokens", result.generatedTokens)
                .put("total_tokens", result.promptTokens + result.generatedTokens))
    }

    fun ollamaResponse(result: ModelChatResult): JSONObject {
        return JSONObject()
            .put("model", result.modelId)
            .put("created_at", java.time.Instant.now().toString())
            .put("message", JSONObject()
                .put("role", "assistant")
                .put("content", result.text))
            .put("done", true)
            .put("total_duration", result.elapsedMs * 1_000_000L)
    }

    fun error(code: String, message: String): JSONObject {
        return JSONObject()
            .put(
                "error",
                JSONObject()
                    .put("message", message)
                    .put("type", "invalid_request_error")
                    .put("code", code)
            )
    }

    private fun parseOpenAiMessages(array: JSONArray): List<ModelChatMessage> {
        return List(array.length()) { index -> array.optJSONObject(index) }
            .filterNotNull()
            .map { message ->
                ModelChatMessage(
                    role = message.optString("role", "user"),
                    parts = parseOpenAiContent(message.opt("content")),
                )
            }
    }

    private fun parseSceneMessages(json: JSONObject): List<ModelChatMessage> {
        val messages = json.optJSONArray("messages")
        if (messages != null && messages.length() > 0) return parseOpenAiMessages(messages)

        val parts = buildList {
            json.optString("prompt").takeIf { it.isNotBlank() }?.let { add(ModelChatPart.Text(it)) }
            json.optString("image").takeIf { it.isNotBlank() }?.let { image ->
                ModelChatPart.decodeImagePart(image)?.let { add(it) }
            }
        }
        return listOf(ModelChatMessage("user", parts))
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

    private fun parseOllamaMessages(array: JSONArray): List<ModelChatMessage> {
        return List(array.length()) { index -> array.optJSONObject(index) }
            .filterNotNull()
            .map { message ->
                val parts = mutableListOf<ModelChatPart>()
                val text = message.optString("content")
                if (text.isNotBlank()) parts += ModelChatPart.Text(text)
                val images = message.optJSONArray("images") ?: JSONArray()
                for (imageIndex in 0 until images.length()) {
                    ModelChatPart.decodeImagePart(images.optString(imageIndex))?.let { parts += it }
                }
                ModelChatMessage(message.optString("role", "user"), parts)
            }
    }

    private fun parseSessionId(json: JSONObject): String {
        return json.optString("sessionId")
            .ifBlank { json.optString("session_id") }
            .ifBlank { json.optJSONObject("metadata")?.optString("sessionId").orEmpty() }
            .ifBlank { json.optJSONObject("metadata")?.optString("session_id").orEmpty() }
            .trim()
    }
}
