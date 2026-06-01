package com.mnnode.app.server

import android.content.Context
import com.mnnode.app.config.RuntimeDefaults
import com.mnnode.app.model.ModelManager
import org.json.JSONArray
import org.json.JSONObject

class RuntimeTools(
    private val context: Context,
    private val runtimeState: () -> JSONObject,
) : ToolProvider {
    override fun tools(): List<ToolDefinition> = listOf(
        tool(
            name = "runtime_resources",
            description = "Return Android package and local runtime resource information.",
        ) { deviceInfo() },
        tool(
            name = "runtime_status",
            description = "Return Lociant API runtime status.",
        ) { runtimeState() },
    )

    private fun deviceInfo(): JSONObject {
        val runtime = Runtime.getRuntime()
        return JSONObject()
            .put("packageName", context.packageName)
            .put("availableProcessors", runtime.availableProcessors())
            .put("maxMemory", runtime.maxMemory())
            .put("totalMemory", runtime.totalMemory())
            .put("freeMemory", runtime.freeMemory())
    }
}

class ModelTools(
    private val modelManager: ModelManager,
    private val preloadModel: (String) -> Unit,
    private val cancelChat: () -> Unit,
) : ToolProvider {
    override fun tools(): List<ToolDefinition> = listOf(
        tool(
            name = "model_list",
            description = "List installed and built-in models visible to Lociant.",
        ) { JSONObject().put("models", JSONArray(modelManager.listModelsJson())) },
        tool(
            name = "model_preload",
            description = "Queue model preload for the selected chat model.",
            properties = JSONObject().put("model", stringParam()),
            policy = ToolPolicy(sideEffect = true),
        ) { args ->
            val model = ModelManager.normalizeId(args.optString("model"))
            preloadModel(model)
            JSONObject().put("queued", true).put("model", model)
        },
        tool(
            name = "inference_cancel",
            description = "Cancel the currently running chat inference request.",
            policy = ToolPolicy(sideEffect = true),
        ) {
            cancelChat()
            JSONObject().put("cancelled", true)
        },
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
        return JSONObject()
            .put("ok", true)
            .put("running", state.optBoolean("running", false))
            .put("modelId", state.optString("modelId"))
            .put("modelLoaded", state.optBoolean("modelLoaded", false))
            .put("modelLoading", state.optBoolean("modelLoading", false))
            .put("contextWindowTokens", state.opt("contextWindowTokens") ?: JSONObject.NULL)
            .put("effectiveMaxOutputTokens", state.opt("effectiveMaxOutputTokens") ?: JSONObject.NULL)
            .put("maxOutputTokens", state.opt("maxOutputTokens") ?: JSONObject.NULL)
            .put("lastError", state.opt("lastError") ?: JSONObject.NULL)
            .put("queueTimeoutMs", RuntimeDefaults.Queue.CHAT_TIMEOUT_MS)
            .put("readyModels", readyChatModels())
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
