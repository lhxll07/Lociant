package io.lociant.mcp

import io.lociant.core.api.ApiContract
import io.lociant.core.tools.ToolCallOrigin
import io.lociant.core.tools.ToolExposure
import io.lociant.core.tools.ToolRegistry
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.header
import io.ktor.server.response.respond
import io.ktor.server.sse.ServerSSESession
import io.modelcontextprotocol.kotlin.sdk.server.Server
import io.modelcontextprotocol.kotlin.sdk.server.ServerOptions
import io.modelcontextprotocol.kotlin.sdk.server.StreamableHttpServerTransport
import io.modelcontextprotocol.kotlin.sdk.types.CallToolRequest
import io.modelcontextprotocol.kotlin.sdk.types.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.types.ContentBlock
import io.modelcontextprotocol.kotlin.sdk.types.ImageContent
import io.modelcontextprotocol.kotlin.sdk.types.Implementation
import io.modelcontextprotocol.kotlin.sdk.types.ServerCapabilities
import io.modelcontextprotocol.kotlin.sdk.types.TextContent
import io.modelcontextprotocol.kotlin.sdk.types.ToolAnnotations
import io.modelcontextprotocol.kotlin.sdk.types.ToolSchema
import kotlinx.serialization.json.*
import org.json.JSONArray
import org.json.JSONObject
import java.util.Base64
import java.util.concurrent.ConcurrentHashMap

/**
 * MCP Streamable HTTP endpoint backed by the official MCP Kotlin SDK
 * (io.modelcontextprotocol:kotlin-sdk), the same SDK RikkaHub uses on the
 * client side. JSON-RPC framing, protocol-version negotiation, session
 * management and SSE/JSON response selection are handled by the SDK instead
 * of hand-rolled code.
 *
 * Tools are sourced from [ToolRegistry] with the configured [ToolExposure].
 * One [Server] + [StreamableHttpServerTransport] pair is created per MCP
 * session; the transport manages `Mcp-Session-Id` lifecycle automatically.
 */
class McpController(
    private val toolRegistry: ToolRegistry,
    private val exposure: () -> ToolExposure,
) {
    private val transports = ConcurrentHashMap<String, StreamableHttpServerTransport>()

    suspend fun handlePost(call: ApplicationCall) {
        val transport = getOrCreateTransport(call) ?: return
        transport.handleRequest(null, call)
    }

    suspend fun handleSse(session: ServerSSESession, call: ApplicationCall) {
        val transport = findTransport(call) ?: return
        transport.handleRequest(session, call)
    }

    suspend fun handleDelete(call: ApplicationCall) {
        val transport = findTransport(call) ?: return
        transport.handleRequest(null, call)
    }

    private suspend fun findTransport(call: ApplicationCall): StreamableHttpServerTransport? {
        val sessionId = call.request.header(McpSessionIdHeader)
        if (sessionId.isNullOrEmpty()) {
            call.respond(HttpStatusCode.BadRequest, "Bad Request: No valid session ID provided")
            return null
        }
        val transport = transports[sessionId]
        if (transport == null) {
            call.respond(HttpStatusCode.NotFound, "Session not found")
        }
        return transport
    }

    private suspend fun getOrCreateTransport(call: ApplicationCall): StreamableHttpServerTransport? {
        val sessionId = call.request.header(McpSessionIdHeader)
        if (sessionId != null) {
            val transport = transports[sessionId]
            if (transport == null) {
                call.respond(HttpStatusCode.NotFound, "Session not found")
            }
            return transport
        }

        val transport = StreamableHttpServerTransport(
            StreamableHttpServerTransport.Configuration(enableJsonResponse = true),
        )
        transport.setOnSessionInitialized { id -> transports[id] = transport }
        transport.setOnSessionClosed { id -> transports.remove(id) }

        val server = buildServer()
        server.onClose { transport.sessionId?.let { transports.remove(it) } }
        server.createSession(transport)
        return transport
    }

    private fun buildServer(): Server {
        val server = Server(
            serverInfo = Implementation(name = "lociant", version = ApiContract.VERSION),
            options = ServerOptions(
                capabilities = ServerCapabilities(
                    tools = ServerCapabilities.Tools(listChanged = false),
                ),
            ),
            instructions = "Use Lociant tools for Android-native sensing, screen context, local phone models, camera frames, and explicit phone UI actions.",
        )

        val definitions = toolRegistry.definitions(exposure())
        for (index in 0 until definitions.length()) {
            val item = definitions.optJSONObject(index) ?: continue
            val function = item.optJSONObject("function") ?: continue
            val name = function.optString("name")
            if (name.isBlank()) continue
            val policy = item.optJSONObject("x_policy") ?: JSONObject()

            server.addTool(
                name = name,
                description = function.optString("description"),
                inputSchema = toToolSchema(function.optJSONObject("parameters")),
                toolAnnotations = ToolAnnotations(
                    title = null,
                    readOnlyHint = !policy.optBoolean("sideEffect", false),
                    destructiveHint = policy.optBoolean("destructive", false),
                    idempotentHint = null,
                    openWorldHint = policy.optBoolean("openWorld", false),
                ),
            ) { request -> callTool(name, request) }
        }
        return server
    }

    private fun callTool(name: String, request: CallToolRequest): CallToolResult {
        val args = request.arguments?.let { kotlinToOrg(it) as? JSONObject } ?: JSONObject()
        val response = toolRegistry.call(name, args, exposure(), ToolCallOrigin.Remote)
        val result = response.optJSONObject("result") ?: response
        val isError = !response.optBoolean("ok", false) || result.optBoolean("ok", true) == false

        val content = mutableListOf<ContentBlock>()
        result.keys().forEach { key ->
            val value = result.opt(key)
            if (value is String) {
                parseDataUrl(value)?.let { image ->
                    content.add(ImageContent(data = image.data, mimeType = image.mimeType))
                }
            }
        }
        content.add(TextContent(jsonText(compactForText(result))))

        return CallToolResult(
            content = content,
            isError = isError,
            structuredContent = stripLargeMedia(result) as? JsonObject,
        )
    }

    // ---- Schema / value conversion (org.json <-> kotlinx.serialization.json) ----

    private fun toToolSchema(params: JSONObject?): ToolSchema {
        if (params == null) return ToolSchema()
        return ToolSchema(
            properties = params.optJSONObject("properties")?.let { orgToKotlin(it) as? JsonObject }
                ?: JsonObject(emptyMap()),
            required = params.optJSONArray("required")?.let { array ->
                (0 until array.length()).map { array.getString(it) }
            } ?: emptyList(),
            defs = params.optJSONObject("\$defs")?.let { orgToKotlin(it) as? JsonObject }
                ?: JsonObject(emptyMap()),
        )
    }

    private fun orgToKotlin(value: Any?): JsonElement = when (value) {
        is JSONObject -> buildJsonObject {
            value.keys().forEach { key -> put(key, orgToKotlin(value.opt(key))) }
        }
        is JSONArray -> buildJsonArray {
            for (index in 0 until value.length()) add(orgToKotlin(value.opt(index)))
        }
        is Boolean -> JsonPrimitive(value)
        is Int -> JsonPrimitive(value)
        is Long -> JsonPrimitive(value)
        is Double -> JsonPrimitive(value)
        is String -> JsonPrimitive(value)
        JSONObject.NULL, null -> JsonNull
        else -> JsonPrimitive(value.toString())
    }

    private fun kotlinToOrg(value: JsonElement): Any? = when (value) {
        is JsonObject -> JSONObject().also { out -> value.forEach { (key, item) -> out.put(key, kotlinToOrg(item)) } }
        is JsonArray -> JSONArray().also { out -> value.forEach { out.put(kotlinToOrg(it)) } }
        is JsonPrimitive -> when {
            value.isString -> value.content
            value.booleanOrNull != null -> value.booleanOrNull
            value.longOrNull != null -> value.longOrNull
            value.doubleOrNull != null -> value.doubleOrNull
            else -> value.content
        }
        is JsonNull, null -> JSONObject.NULL
    }

    // ---- Response shaping (kept from the original implementation) ----

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
        is JSONObject -> buildJsonObject {
            value.keys().forEach { key ->
                val item = value.opt(key)
                val image = if (item is String) parseDataUrl(item) else null
                if (image != null) {
                    put("${key}MimeType", image.mimeType)
                    put("${key}Base64Bytes", image.data.length)
                } else {
                    put(key, orgToKotlin(stripLargeMedia(item)))
                }
            }
        }
        is JSONArray -> buildJsonArray {
            for (index in 0 until value.length()) add(orgToKotlin(stripLargeMedia(value.opt(index))))
        }
        else -> orgToKotlin(value)
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

    private data class DataUrl(val mimeType: String, val data: String)

    private companion object {
        private const val McpSessionIdHeader = "mcp-session-id"
    }
}
