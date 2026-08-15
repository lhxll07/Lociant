package io.lociant.tools

import io.lociant.core.tools.*
import io.lociant.runtime.model.ModelManager
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
