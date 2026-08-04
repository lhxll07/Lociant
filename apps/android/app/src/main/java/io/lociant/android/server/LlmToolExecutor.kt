package io.lociant.android.server

import io.lociant.core.config.RuntimeDefaults
import io.lociant.core.model.ModelChatMessage
import io.lociant.core.model.ModelChatPart
import io.lociant.core.model.ModelChatRequest
import io.lociant.runtime.model.ModelManager
import io.lociant.tools.LociantAccessibilityService
import io.lociant.tools.runtime.VisionRuntime
import org.json.JSONArray
import org.json.JSONObject

/** Executes the `llm_chat` tool without coupling tool input parsing to HTTP routing. */
class LlmToolExecutor(
    private val modelManager: ModelManager,
    private val chatController: ChatController,
    private val defaultModelId: () -> String,
    private val defaultMaxOutputTokens: () -> Int,
) {
    fun execute(args: JSONObject): JSONObject {
        val model = args.optString("model").ifBlank { args.optString("modelId") }
        val maxTokens = optionalPositiveInt(args, "maxTokens")
        val sessionId = if (args.has("sessionId") && !args.isNull("sessionId")) args.getString("sessionId") else ""
        val currentRequest = chatController.boundRequest(
            ModelChatRequest(
                modelId = model,
                messages = messages(args),
                maxTokens = maxTokens,
                stream = false,
                source = "mcp.llm_chat",
                sessionId = sessionId,
            ),
            defaultModelId(),
            defaultMaxOutputTokens(),
        )
        if (hasImages(currentRequest) && modelManager.resolve(currentRequest.modelId).spec.type != "vlm") {
            return JSONObject()
                .put("ok", false)
                .put("modelId", currentRequest.modelId)
                .put("text", "")
                .put("message", "Image input requires a VLM model.")
                .put("error", JSONObject()
                    .put("code", "vlm_required")
                    .put("message", "Select or install a VLM model before calling llm_chat with images."))
        }
        val request = chatController.sessionRequest(currentRequest)
        val turnRequest = currentRequest.copy(
            sessionId = request.sessionId,
            modelId = request.modelId,
            persistSession = request.persistSession,
        )
        chatController.persistUserTurn(turnRequest)
        val timeoutMs = optionalPositiveInt(args, "timeoutMs")
            ?.toLong()
            ?.coerceIn(1_000L, RuntimeDefaults.Queue.CHAT_TIMEOUT_MS)
            ?: RuntimeDefaults.Queue.CHAT_TIMEOUT_MS
        val result = chatController.submitSync(request, timeoutMs)
        chatController.saveModelTurn(turnRequest, result)
        return JSONObject()
            .put("ok", result.ok)
            .put("modelId", result.modelId)
            .put("sessionId", chatController.visibleSessionId(request))
            .put("text", result.text)
            .put("message", result.message)
            .put("input", inputSummary(currentRequest))
            .put("usage", ModelApiMapper.openAiUsage(result))
            .put("metrics", ModelApiMapper.runtimeMetrics(result))
            .apply {
                if (!result.ok) put("error", JSONObject()
                    .put("code", "chat_failed")
                    .put("message", result.message.ifBlank { "local LLM chat failed" }))
            }
    }

    private fun messages(args: JSONObject): List<ModelChatMessage> {
        val topLevelImages = images(args)
        val parsed = parseMessages(args.optJSONArray("messages"))
        if (parsed.isNotEmpty()) {
            return if (topLevelImages.isEmpty()) parsed else appendImages(parsed, topLevelImages)
        }
        val prompt = args.optString("prompt").trim()
        require(prompt.isNotBlank() || topLevelImages.isNotEmpty()) { "prompt, messages, or image is required" }
        val system = args.optString("system").trim()
        return buildList {
            if (system.isNotBlank()) add(ModelChatMessage("system", listOf(ModelChatPart.Text(system))))
            add(ModelChatMessage("user", buildList {
                if (prompt.isNotBlank()) add(ModelChatPart.Text(prompt))
                addAll(topLevelImages)
            }))
        }
    }

    private fun parseMessages(messages: JSONArray?): List<ModelChatMessage> {
        if (messages == null || messages.length() == 0) return emptyList()
        return buildList {
            for (index in 0 until messages.length()) {
                val message = messages.optJSONObject(index) ?: continue
                val parts = contentParts(message.opt("content"))
                if (parts.isNotEmpty()) add(ModelChatMessage(message.optString("role", "user"), parts))
            }
        }
    }

    private fun contentParts(content: Any?): List<ModelChatPart> = when (content) {
        is String -> listOf(ModelChatPart.Text(content))
        is JSONArray -> buildList {
            val text = buildString {
                for (index in 0 until content.length()) {
                    val item = content.optJSONObject(index) ?: continue
                    if (item.optString("type") == "text") {
                        if (isNotEmpty()) append('\n')
                        append(item.optString("text"))
                    }
                }
            }
            if (text.isNotBlank()) add(ModelChatPart.Text(text))
            addAll(openAiImages(content))
        }
        else -> emptyList()
    }

    private fun openAiImages(content: JSONArray): List<ModelChatPart.Image> = buildList {
        for (index in 0 until content.length()) {
            val item = content.optJSONObject(index) ?: continue
            if (item.optString("type") != "image_url") continue
            val url = item.optJSONObject("image_url")?.optString("url") ?: item.optString("image_url")
            ModelChatPart.decodeImagePart(url)?.let(::add)
        }
    }

    private fun images(args: JSONObject): List<ModelChatPart.Image> = buildList {
        if (args.optBoolean("useCameraFrame", false)) {
            val bytes = VisionRuntime.previewBytes()
                ?: throw IllegalArgumentException("useCameraFrame requires an active vision runtime with a preview frame")
            add(ModelChatPart.Image("image/jpeg", bytes))
        }
        if (args.optBoolean("useScreenFrame", false) || args.optBoolean("useScreenshot", false)) add(captureScreen(args))
        args.optString("image").takeIf(String::isNotBlank)?.let { raw ->
            add(ModelChatPart.decodeImagePart(raw)
                ?: throw IllegalArgumentException("image must be base64 or a data:image URL"))
        }
        args.optJSONArray("images")?.let { array ->
            for (index in 0 until array.length()) {
                val raw = array.optString(index)
                if (raw.isNotBlank()) add(ModelChatPart.decodeImagePart(raw)
                    ?: throw IllegalArgumentException("images[$index] must be base64 or a data:image URL"))
            }
        }
    }

    private fun captureScreen(args: JSONObject): ModelChatPart.Image {
        val service = LociantAccessibilityService.instance
            ?: throw IllegalArgumentException("Screen capture requires the Lociant accessibility service")
        val snapshot = service.takeScreenShot(
            maxWidth = args.optInt("screenshotMaxWidth", 720).coerceIn(320, 1440),
            quality = args.optInt("screenshotQuality", 82).coerceIn(45, 95),
        )
        require(snapshot.optBoolean("ok", false)) { snapshot.optString("message", "Android screen capture failed") }
        return ModelChatPart.decodeImagePart(snapshot.optString("image"))
            ?: throw IllegalArgumentException("Android screen capture returned an invalid image")
    }

    private fun appendImages(
        messages: List<ModelChatMessage>,
        images: List<ModelChatPart.Image>,
    ): List<ModelChatMessage> {
        val target = messages.indexOfLast { it.role.equals("user", ignoreCase = true) }
            .takeIf { it >= 0 } ?: messages.lastIndex
        return messages.mapIndexed { index, message ->
            if (index == target) message.copy(parts = message.parts + images) else message
        }
    }

    private fun hasImages(request: ModelChatRequest): Boolean =
        request.messages.any { message -> message.parts.any { it is ModelChatPart.Image } }

    private fun inputSummary(request: ModelChatRequest): JSONObject {
        val images = request.messages.flatMap { it.parts.filterIsInstance<ModelChatPart.Image>() }
        return JSONObject()
            .put("messageCount", request.messages.size)
            .put("imageCount", images.size)
            .put("imageBytes", images.sumOf { it.bytes.size })
            .put("imageMimeType", images.firstOrNull()?.mimeType ?: JSONObject.NULL)
    }

    private fun optionalPositiveInt(json: JSONObject, key: String): Int? =
        if (!json.has(key) || json.isNull(key)) null else json.optInt(key).takeIf { it > 0 }
}
