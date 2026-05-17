package com.mnnode.app.server

import org.json.JSONArray
import org.json.JSONObject

class ToolRegistry(
    providers: List<ToolProvider>,
) {
    private val tools = linkedMapOf<String, ToolDefinition>()

    init {
        providers.flatMap { it.tools() }.forEach { tool ->
            tools[tool.name] = tool
        }
    }

    fun manifest(): JSONObject = JSONObject()
        .put("object", "list")
        .put("data", definitions())

    fun definitions(): JSONArray = JSONArray(tools.values.map { it.toJson() })

    fun definition(name: String): JSONObject? = tools[name]?.toJson()

    fun has(name: String): Boolean = tools.containsKey(name)

    fun call(name: String, args: JSONObject = JSONObject()): JSONObject {
        val tool = tools[name] ?: return error("tool_not_found", "Unknown tool: $name")
        if (!tool.policy.local) return error("tool_not_local", "Tool is not executable inside Lociant: $name").put("tool", name)
        return runCatching {
            val result = tool.handler(args)
            val ok = result.optBoolean("ok", true)
            JSONObject()
                .put("ok", ok)
                .put("tool", name)
                .put("result", result)
                .put("policy", tool.policy.toJson())
                .apply {
                    if (!ok) put("error", toolErrorFrom(result))
                }
        }.getOrElse { error ->
            error("tool_failed", error.message ?: "tool failed").put("tool", name)
        }
    }

    private fun error(code: String, message: String): JSONObject = JSONObject()
        .put("ok", false)
        .put("error", JSONObject().put("code", code).put("message", message))

    private fun toolErrorFrom(result: JSONObject): JSONObject {
        val nested = result.optJSONObject("error")
        return JSONObject()
            .put("code", nested?.optString("code")?.takeIf { it.isNotBlank() } ?: result.optString("code", "tool_failed"))
            .put("message", nested?.optString("message")?.takeIf { it.isNotBlank() } ?: result.optString("message", "tool failed"))
    }
}

interface ToolProvider {
    fun tools(): List<ToolDefinition>
}

data class ToolPolicy(
    val local: Boolean = true,
    val remoteAllowed: Boolean = true,
    val requiresActivity: Boolean = false,
    val sideEffect: Boolean = false,
) {
    fun toJson(): JSONObject = JSONObject()
        .put("local", local)
        .put("remoteAllowed", remoteAllowed)
        .put("requiresActivity", requiresActivity)
        .put("sideEffect", sideEffect)

    fun executionLabel(): String = if (local) "local" else "remote"
}

class ToolDefinition(
    val name: String,
    val description: String,
    val parameters: JSONObject,
    val policy: ToolPolicy = ToolPolicy(),
    val handler: (JSONObject) -> JSONObject,
) {
    fun toJson(): JSONObject = JSONObject()
        .put("type", "function")
        .put("function", JSONObject()
            .put("name", name)
            .put("description", description)
            .put("parameters", parameters))
        .put("x_execution", policy.executionLabel())
        .put("x_policy", policy.toJson())
}
