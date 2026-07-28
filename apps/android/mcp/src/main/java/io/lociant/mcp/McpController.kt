package io.lociant.mcp

import io.lociant.core.api.ApiContract
import io.lociant.core.tools.ToolCallOrigin
import io.lociant.core.tools.ToolExposure
import io.lociant.core.tools.ToolRegistry
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.receiveText
import io.ktor.server.response.header
import io.ktor.server.response.respondText
import org.json.JSONArray
import org.json.JSONObject
import java.util.Base64

class McpController(
    private val toolRegistry: ToolRegistry,
    private val exposure: () -> ToolExposure,
) {
    suspend fun post(call: ApplicationCall) {
        val raw = call.receiveText()
        val message = runCatching { JSONObject(raw.ifBlank { "{}" }) }.getOrElse {
            call.respondText(error(null, -32700, "Parse error").toString(), JsonContentType, HttpStatusCode.BadRequest)
            return
        }
        val id = message.opt("id")
        if (id == null || id == JSONObject.NULL) {
            call.response.header("MCP-Protocol-Version", ProtocolVersion)
            call.respondText("", JsonContentType, HttpStatusCode.Accepted)
            return
        }
        val response = handle(message)
        call.response.header("MCP-Protocol-Version", ProtocolVersion)
        call.respondText(response.toString(), JsonContentType)
    }

    private fun handle(message: JSONObject): JSONObject {
        val id = message.opt("id")
        return runCatching {
            when (message.optString("method")) {
                "initialize" -> result(id, initializeResult(message.optJSONObject("params")))
                "ping" -> result(id, JSONObject())
                "tools/list" -> result(id, JSONObject().put("tools", mcpTools()))
                "tools/call" -> result(id, callTool(message.optJSONObject("params") ?: JSONObject()))
                "resources/list" -> result(id, JSONObject().put("resources", JSONArray()))
                "prompts/list" -> result(id, JSONObject().put("prompts", JSONArray()))
                else -> error(id, -32601, "Unsupported MCP method: ${message.optString("method")}")
            }
        }.getOrElse { failure ->
            error(id, -32000, failure.message ?: "MCP request failed")
        }
    }

    private fun initializeResult(params: JSONObject?): JSONObject = JSONObject()
        .put("protocolVersion", params?.optString("protocolVersion")?.takeIf { it.isNotBlank() } ?: ProtocolVersion)
        .put("capabilities", JSONObject().put("tools", JSONObject().put("listChanged", false)))
        .put("serverInfo", JSONObject().put("name", "lociant").put("version", ApiContract.VERSION))
        .put("instructions", "Use Lociant tools for Android-native sensing, screen context, local phone models, camera frames, and explicit phone UI actions.")

    private fun mcpTools(): JSONArray {
        val output = JSONArray()
        val tools = toolRegistry.definitions(exposure())
        for (index in 0 until tools.length()) {
            val item = tools.optJSONObject(index) ?: continue
            val function = item.optJSONObject("function") ?: continue
            val name = function.optString("name")
            if (name.isBlank()) continue
            val policy = item.optJSONObject("x_policy") ?: JSONObject()
            output.put(JSONObject()
                .put("name", name)
                .put("description", function.optString("description"))
                .put("inputSchema", function.optJSONObject("parameters") ?: JSONObject().put("type", "object"))
                .put("annotations", JSONObject()
                    .put("readOnlyHint", !policy.optBoolean("sideEffect", false))
                    .put("destructiveHint", policy.optBoolean("destructive", false))
                    .put("openWorldHint", policy.optBoolean("openWorld", false))))
        }
        return output
    }

    private fun callTool(params: JSONObject): JSONObject {
        val name = params.optString("name")
        if (name.isBlank()) return JSONObject()
            .put("content", JSONArray().put(textContent("tools/call requires params.name")))
            .put("isError", true)
        val args = params.optJSONObject("arguments") ?: JSONObject()
        val response = toolRegistry.call(name, args, exposure(), ToolCallOrigin.Remote)
        val result = response.optJSONObject("result") ?: response
        val isError = !response.optBoolean("ok", false) || result.optBoolean("ok", true) == false
        val content = JSONArray()

        result.keys().forEach { key ->
            val value = result.opt(key)
            if (value is String) {
                parseDataUrl(value)?.let { image ->
                    content.put(JSONObject()
                        .put("type", "image")
                        .put("data", image.data)
                        .put("mimeType", image.mimeType))
                }
            }
        }
        content.put(textContent(jsonText(compactForText(result))))

        return JSONObject()
            .put("content", content)
            .put("structuredContent", stripLargeMedia(result))
            .put("isError", isError)
    }

    private fun compactForText(value: Any?): Any? = when (value) {
        is JSONObject -> JSONObject().also { out ->
            value.keys().forEach { key ->
                val item = value.opt(key)
                val image = if (item is String) parseDataUrl(item) else null
                out.put(key, if (image != null) "<${image.mimeType} data url, ${image.data.length} base64 chars>" else compactForText(item))
            }
        }
        is JSONArray -> JSONArray().also { out ->
            for (index in 0 until value.length()) out.put(compactForText(value.opt(index)))
        }
        else -> value
    }

    private fun stripLargeMedia(value: Any?): Any? = when (value) {
        is JSONObject -> JSONObject().also { out ->
            value.keys().forEach { key ->
                val item = value.opt(key)
                val image = if (item is String) parseDataUrl(item) else null
                if (image != null) {
                    out.put("${key}MimeType", image.mimeType)
                    out.put("${key}Base64Bytes", image.data.length)
                } else {
                    out.put(key, stripLargeMedia(item))
                }
            }
        }
        is JSONArray -> JSONArray().also { out ->
            for (index in 0 until value.length()) out.put(stripLargeMedia(value.opt(index)))
        }
        else -> value
    }

    private fun jsonText(value: Any?): String = when (value) {
        is JSONObject -> value.toString(2)
        is JSONArray -> value.toString(2)
        JSONObject.NULL, null -> "null"
        else -> value.toString()
    }

    private fun parseDataUrl(value: String): DataUrl? {
        if (!value.startsWith("data:")) return null
        val comma = value.indexOf(',')
        if (comma <= 5) return null
        val prefix = value.substring(5, comma)
        if (!prefix.contains(";base64")) return null
        val mimeType = prefix.substringBefore(';').ifBlank { "application/octet-stream" }
        val data = value.substring(comma + 1)
        return runCatching {
            Base64.getDecoder().decode(data)
            DataUrl(mimeType, data)
        }.getOrNull()
    }

    private fun textContent(text: String): JSONObject = JSONObject()
        .put("type", "text")
        .put("text", text)

    private fun result(id: Any?, payload: JSONObject): JSONObject = JSONObject()
        .put("jsonrpc", "2.0")
        .put("id", id)
        .put("result", payload)

    private fun error(id: Any?, code: Int, message: String): JSONObject = JSONObject()
        .put("jsonrpc", "2.0")
        .put("id", id ?: JSONObject.NULL)
        .put("error", JSONObject().put("code", code).put("message", message))

    private data class DataUrl(val mimeType: String, val data: String)

    private companion object {
        private const val ProtocolVersion = "2025-06-18"
        private val JsonContentType = ContentType.Application.Json.withParameter("charset", "utf-8")
    }
}
