package com.mnnode.app.server

import android.content.Context
import com.mnnode.app.model.ModelManager
import org.json.JSONArray
import org.json.JSONObject

class RuntimeTools(
    private val context: Context,
    private val runtimeState: () -> JSONObject,
) : ToolProvider {
    override fun tools(): List<ToolDefinition> = listOf(
        ToolDefinition(
            name = "get_device_info",
            description = "Return Android package and local runtime resource information.",
            parameters = objectSchema(),
        ) { deviceInfo() },
        ToolDefinition(
            name = "get_runtime_status",
            description = "Return MNNode API runtime status.",
            parameters = objectSchema(),
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
        ToolDefinition(
            name = "list_models",
            description = "List installed and built-in models visible to MNNode.",
            parameters = objectSchema(),
        ) { JSONObject().put("models", JSONArray(modelManager.listModelsJson())) },
        ToolDefinition(
            name = "preload_model",
            description = "Queue model preload for the selected chat model.",
            parameters = objectSchema(JSONObject().put("model", JSONObject().put("type", "string"))),
            policy = ToolPolicy(sideEffect = true),
        ) { args ->
            val model = ModelManager.normalizeId(args.optString("model"))
            preloadModel(model)
            JSONObject().put("queued", true).put("model", model)
        },
        ToolDefinition(
            name = "cancel_chat",
            description = "Cancel the currently running chat inference request.",
            parameters = objectSchema(),
            policy = ToolPolicy(sideEffect = true),
        ) {
            cancelChat()
            JSONObject().put("cancelled", true)
        },
    )
}

internal fun objectSchema(properties: JSONObject = JSONObject()): JSONObject =
    JSONObject().put("type", "object").put("properties", properties)
