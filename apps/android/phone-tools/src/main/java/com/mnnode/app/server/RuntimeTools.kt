package com.mnnode.app.server

import com.mnnode.app.config.RuntimeDefaults
import com.mnnode.app.model.ModelManager
import com.mnnode.app.util.jsonOk
import org.json.JSONArray
import org.json.JSONObject

class RuntimeTools(
    private val runtimeState: () -> JSONObject,
) : ToolProvider {
    override fun tools(): List<ToolDefinition> = listOf(
        tool(
            name = "runtime_status",
            description = "Return Lociant API runtime status.",
        ) { runtimeState() },
    )
}

class ModelTools(
    private val modelManager: ModelManager,
) : ToolProvider {
    override fun tools(): List<ToolDefinition> = listOf(
        tool(
            name = "model_list",
            description = "List installed and built-in models visible to Lociant.",
        ) { JSONObject().put("models", JSONArray(modelManager.listModelsJson())) },
    )
}

class LlmTools(
    private val modelManager: ModelManager,
    private val runtimeState: () -> JSONObject,
    private val chat: (JSONObject) -> JSONObject,
) : ToolProvider {
    override fun tools(): List<ToolDefinition> = listOf(
        tool(
            name = "llm_status",
            description = "Return phone-local LLM readiness, selected model, context, and available chat models.",
        ) { llmStatus() },
        tool(
            name = "llm_chat",
            description = "Ask the phone-local LLM to answer a prompt through Lociant's shared inference queue. Use this when a desktop agent wants the Android phone to act as a local reasoning node.",
            properties = JSONObject()
                .put("prompt", stringParam("User prompt. Use either prompt or messages."))
                .put("messages", arrayParam("Optional OpenAI-style text or image messages.", objectParam()))
                .put("image", stringParam("Optional image data URL or base64 image bytes."))
                .put("images", arrayParam("Optional image data URLs or base64 image bytes. Only one image is currently used.", stringParam()))
                .put("useCameraFrame", boolParam("Use the latest camera frame from the active vision runtime as the image input."))
                .put("useScreenFrame", boolParam("Capture the current Android screen and use it as the image input. Requires Accessibility screenshot support and a VLM model."))
                .put("screenshotMaxWidth", intParam("Maximum screen capture width when useScreenFrame is true. Default 720."))
                .put("screenshotQuality", intParam("JPEG quality for screen capture when useScreenFrame is true. Default 82."))
                .put("system", stringParam("Optional system instruction used with prompt."))
                .put("model", stringParam("Optional model id. Defaults to Lociant runtime setting."))
                .put("modelId", stringParam("Alias for model."))
                .put("maxTokens", intParam("Optional output token limit."))
                .put("sessionId", stringParam("Optional Lociant model session id for persistent context."))
                .put("timeoutMs", intParam("Optional sync timeout, capped by the runtime queue timeout.")),
        ) { args -> chat(args) },
    )

    private fun llmStatus(): JSONObject {
        val state = runtimeState()
        return jsonOk("running" to state.optBoolean("running", false), "modelId" to state.optString("modelId"), "modelLoaded" to state.optBoolean("modelLoaded", false), "modelLoading" to state.optBoolean("modelLoading", false), "contextWindowTokens" to (state.opt("contextWindowTokens") ?: JSONObject.NULL), "effectiveMaxOutputTokens" to (state.opt("effectiveMaxOutputTokens") ?: JSONObject.NULL), "maxOutputTokens" to (state.opt("maxOutputTokens") ?: JSONObject.NULL), "lastError" to (state.opt("lastError") ?: JSONObject.NULL), "queueTimeoutMs" to RuntimeDefaults.Queue.CHAT_TIMEOUT_MS, "readyModels" to readyChatModels())
    }

    private fun readyChatModels(): JSONArray {
        val all = JSONArray(modelManager.listModelsJson())
        val ready = JSONArray()
        for (index in 0 until all.length()) {
            val model = all.optJSONObject(index) ?: continue
            if (!model.optBoolean("ready", false)) continue
            if (!isChatModel(model)) continue
            ready.put(JSONObject()
                .put("id", model.optString("id"))
                .put("name", model.optString("name"))
                .put("runtime", model.optString("runtime"))
                .put("type", model.optString("type"))
                .put("source", model.optString("source")))
        }
        return ready
    }

    private fun isChatModel(model: JSONObject): Boolean {
        val runtime = model.optString("runtime")
        val type = model.optString("type")
        return runtime == "mnn" || type == "vlm" || type == "chat" || type == "llm"
    }
}
