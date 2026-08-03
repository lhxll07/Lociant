package io.lociant.runtime.model

import android.util.Base64
import io.lociant.core.model.ModelChatMessage
import io.lociant.core.model.ModelChatPart
import io.lociant.core.model.ModelChatRequest
import io.lociant.core.model.ModelChatResult
import io.lociant.core.model.ModelToolCall
import io.lociant.core.model.ModelToolChoice
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Minimal OpenAI-compatible chat client used as a cloud model backend.
 *
 * Lociant stays a local-first node; this client is an opt-in *model provider*
 * symmetric to the local MNN backend. It speaks the same chat-completions
 * protocol Lociant already exposes, so tool calls, streaming and sessions work
 * through the existing pipeline.
 */
class CloudChatClient(
    private val baseUrl: String,
    private val apiKey: String,
    val model: String,
) {
    @Volatile private var activeConnection: java.net.HttpURLConnection? = null

    /**
     * Interrupts the in-flight cloud request. Called on timeout/cancel so a
     * stalled upstream does not block the single inference worker forever.
     */
    fun cancel() {
        activeConnection?.disconnect()
    }

    fun complete(request: ModelChatRequest): ModelChatResult {
        val started = System.currentTimeMillis()
        val connection = open()
        activeConnection = connection
        try {
            writeRequest(connection, requestBody(request, stream = false))
            val status = connection.responseCode
            val text = readBody(if (status in 200..299) connection.inputStream else connection.errorStream)
            if (status !in 200..299) {
                return ModelChatResult(
                    ok = false,
                    modelId = model,
                    message = "Cloud API HTTP $status: ${text.take(400).ifBlank { "request failed" }}",
                    retryable = isTransientStatus(status),
                    elapsedMs = System.currentTimeMillis() - started,
                )
            }
            return parseComplete(JSONObject(text), started)
        } catch (error: Throwable) {
            return ModelChatResult(
                ok = false,
                modelId = model,
                message = error.message ?: "cloud request failed",
                retryable = isTransientFailure(error),
                elapsedMs = System.currentTimeMillis() - started,
            )
        } finally {
            activeConnection = null
            connection.disconnect()
        }
    }

    fun stream(
        request: ModelChatRequest,
        onChunk: (text: String, done: Boolean) -> Unit,
        onReasoning: ((text: String, done: Boolean) -> Unit)? = null,
        onToolCall: ((ModelToolCall) -> Unit)? = null,
    ): ModelChatResult {
        val started = System.currentTimeMillis()
        val connection = open()
        activeConnection = connection
        val responseText = StringBuilder()
        val reasoningText = StringBuilder()
        val toolCallAccum = LinkedHashMap<Int, Triple<String, String, String>>()
        try {
            writeRequest(connection, requestBody(request, stream = true))
            val status = connection.responseCode
            if (status !in 200..299) {
                val errorText = readBody(connection.errorStream)
                onChunk("", true)
                return ModelChatResult(
                    ok = false,
                    modelId = model,
                    message = "Cloud API HTTP $status: ${errorText.take(400).ifBlank { "request failed" }}",
                    retryable = isTransientStatus(status),
                    elapsedMs = System.currentTimeMillis() - started,
                )
            }
            val reader = connection.inputStream.bufferedReader(Charsets.UTF_8)
            while (true) {
                val line = reader.readLine() ?: break
                val trimmed = line.trim()
                if (!trimmed.startsWith("data:")) continue
                val data = trimmed.removePrefix("data:").trim()
                if (data == "[DONE]") break
                runCatching {
                    val chunk = JSONObject(data)
                    val choice = chunk.optJSONArray("choices")?.optJSONObject(0) ?: return@runCatching
                    val delta = choice.optJSONObject("delta") ?: return@runCatching
                    val content = stringOrEmpty(delta, "content")
                    if (content.isNotEmpty()) {
                        responseText.append(content)
                        onChunk(content, false)
                    }
                    val reasoning = stringOrEmpty(delta, "reasoning_content")
                    if (reasoning.isNotEmpty()) {
                        reasoningText.append(reasoning)
                        onReasoning?.invoke(reasoning, false)
                    }
                    delta.optJSONArray("tool_calls")?.let { calls ->
                        for (index in 0 until calls.length()) {
                            val item = calls.optJSONObject(index) ?: continue
                            val i = item.optInt("index", 0)
                            val id = item.optString("id", "")
                            val fn = item.optJSONObject("function") ?: continue
                            val name = fn.optString("name", "")
                            val args = fn.optString("arguments", "")
                            val (oldId, oldName, oldArgs) = toolCallAccum[i] ?: Triple("", "", "")
                            toolCallAccum[i] = Triple(
                                if (oldId.isNotBlank()) oldId else id,
                                oldName + name,
                                oldArgs + args,
                            )
                        }
                    }
                }
            }
            onChunk("", true)
            onReasoning?.invoke("", true)
            // Sort by the provider's tool-call index so parallel calls are
            // reported in the same order the model emitted them, even when the
            // upstream streams their fragments out of order.
            val toolCalls = toolCallAccum.toSortedMap().values.map { (id, name, args) ->
                ModelToolCall(id = id.ifBlank { "call_${System.currentTimeMillis()}" }, name = name, arguments = args.ifBlank { "{}" })
            }
            toolCalls.forEach { onToolCall?.invoke(it) }
            val usage = lastUsage
            return ModelChatResult(
                ok = true,
                modelId = model,
                text = responseText.toString(),
                reasoning = reasoningText.toString(),
                toolCalls = toolCalls,
                elapsedMs = System.currentTimeMillis() - started,
                promptTokens = usage?.optInt("prompt_tokens", 0) ?: 0,
                generatedTokens = usage?.optInt("completion_tokens", 0) ?: 0,
            )
        } catch (error: Throwable) {
            onChunk("", true)
            return ModelChatResult(
                ok = false,
                modelId = model,
                message = error.message ?: "cloud stream failed",
                retryable = isTransientFailure(error),
                elapsedMs = System.currentTimeMillis() - started,
            )
        } finally {
            activeConnection = null
            connection.disconnect()
        }
    }

    // ---- request / response mapping ----

    private var lastUsage: JSONObject? = null

    private fun requestBody(request: ModelChatRequest, stream: Boolean): JSONObject {
        val messages = JSONArray()
        request.messages.forEach { messages.put(openAiMessage(it)) }
        val body = JSONObject()
            .put("model", model)
            .put("messages", messages)
            .put("stream", stream)
        request.maxTokens?.let { body.put("max_tokens", it) }
        request.tools?.takeIf { it.length() > 0 }?.let { body.put("tools", sanitizeToolsForCloud(it)) }
        toolChoice(request.toolChoice)?.let { body.put("tool_choice", it) }
        return body
    }

    /**
     * Drops Lociant-specific tool annotations (x_execution / x_policy /
     * x_lociant_level) before forwarding to the cloud API. Strict
     * OpenAI-compatible providers reject unknown fields in tool definitions.
     */
    private fun sanitizeToolsForCloud(raw: JSONArray): JSONArray {
        val out = JSONArray()
        for (index in 0 until raw.length()) {
            val tool = raw.optJSONObject(index) ?: continue
            val clean = JSONObject()
            tool.keys().forEach { key -> if (!key.startsWith("x_")) clean.put(key, tool.opt(key)) }
            out.put(clean)
        }
        return out
    }

    private fun openAiMessage(message: ModelChatMessage): JSONObject {
        val text = message.parts.filterIsInstance<ModelChatPart.Text>().joinToString("\n") { it.text }
        val images = message.parts.filterIsInstance<ModelChatPart.Image>()
        val msg = JSONObject().put("role", message.role)
        if (images.isEmpty()) {
            msg.put("content", text)
        } else {
            val content = JSONArray()
            if (text.isNotBlank()) content.put(JSONObject().put("type", "text").put("text", text))
            images.forEach { image ->
                val encoded = Base64.encodeToString(image.bytes, Base64.NO_WRAP)
                content.put(JSONObject()
                    .put("type", "image_url")
                    .put("image_url", JSONObject().put("url", "data:${image.mimeType};base64,$encoded")))
            }
            msg.put("content", content)
        }
        if (message.toolCallId.isNotBlank()) msg.put("tool_call_id", message.toolCallId)
        // Thinking-mode providers require the assistant's reasoning_content to be
        // passed back verbatim on the next request.
        if (message.reasoning.isNotBlank()) msg.put("reasoning_content", message.reasoning)
        if (message.toolCalls.isNotEmpty()) {
            val calls = JSONArray()
            message.toolCalls.forEach { call ->
                calls.put(JSONObject()
                    .put("id", call.id)
                    .put("type", "function")
                    .put("function", JSONObject().put("name", call.name).put("arguments", call.arguments)))
            }
            msg.put("tool_calls", calls)
        }
        return msg
    }

    private fun toolChoice(choice: ModelToolChoice): Any? = when (choice) {
        ModelToolChoice.None -> "none"
        ModelToolChoice.Required -> "required"
        ModelToolChoice.Auto -> "auto"
        is ModelToolChoice.Function -> JSONObject()
            .put("type", "function")
            .put("function", JSONObject().put("name", choice.name))
    }

    private fun parseComplete(json: JSONObject, started: Long): ModelChatResult {
        lastUsage = json.optJSONObject("usage")
        val choice = json.optJSONArray("choices")?.optJSONObject(0) ?: JSONObject()
        val message = choice.optJSONObject("message") ?: JSONObject()
        val text = stringOrEmpty(message, "content")
        val reasoning = stringOrEmpty(message, "reasoning_content")
        val toolCalls = parseToolCalls(message.optJSONArray("tool_calls"))
        val usage = lastUsage
        return ModelChatResult(
            ok = true,
            modelId = model,
            text = text,
            reasoning = reasoning,
            toolCalls = toolCalls,
            elapsedMs = System.currentTimeMillis() - started,
            promptTokens = usage?.optInt("prompt_tokens", 0) ?: 0,
            generatedTokens = usage?.optInt("completion_tokens", 0) ?: 0,
        )
    }

    private fun parseToolCalls(calls: JSONArray?): List<ModelToolCall> = buildList {
        if (calls == null) return@buildList
        for (index in 0 until calls.length()) {
            val call = calls.optJSONObject(index) ?: continue
            val fn = call.optJSONObject("function") ?: continue
            add(ModelToolCall(
                id = stringOrEmpty(call, "id").ifBlank { "call_${System.currentTimeMillis()}_$index" },
                name = stringOrEmpty(fn, "name"),
                arguments = stringOrEmpty(fn, "arguments").ifBlank { "{}" },
            ))
        }
    }

    private fun isTransientStatus(status: Int): Boolean =
        status == 408 || status == 429 || status >= 500

    private fun isTransientFailure(error: Throwable): Boolean = when (error) {
        is java.io.IOException, is java.net.SocketTimeoutException, is java.net.UnknownHostException -> true
        else -> false
    }

    private fun stringOrEmpty(json: JSONObject, key: String): String {
        val raw = json.opt(key)
        return if (raw is String) raw else ""
    }

    private fun open(): HttpURLConnection {
        val endpoint = baseUrl.trimEnd('/') + "/chat/completions"
        return URL(endpoint).openConnection() as HttpURLConnection
    }

    private fun writeRequest(connection: HttpURLConnection, body: JSONObject) {
        connection.requestMethod = "POST"
        connection.connectTimeout = CONNECT_TIMEOUT_MS
        connection.readTimeout = if (body.optBoolean("stream", false)) STREAM_READ_TIMEOUT_MS else READ_TIMEOUT_MS
        connection.setRequestProperty("Content-Type", "application/json")
        connection.setRequestProperty("Accept", "application/json")
        if (apiKey.isNotBlank()) connection.setRequestProperty("Authorization", "Bearer $apiKey")
        connection.doOutput = true
        connection.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
    }

    private fun readBody(stream: java.io.InputStream?): String {
        if (stream == null) return ""
        return stream.bufferedReader(Charsets.UTF_8).use { it.readText() }
    }

    companion object {
        private const val CONNECT_TIMEOUT_MS = 15_000
        private const val READ_TIMEOUT_MS = 120_000
        // Idle stall guard: if the upstream sends no bytes for this long the
        // stream fails instead of blocking the worker forever. Heartbeat
        // comment lines reset the socket read timeout, so a healthy stream is
        // never cut off.
        private const val STREAM_READ_TIMEOUT_MS = 180_000
    }
}
