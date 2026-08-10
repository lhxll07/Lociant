package io.lociant.android.runtime

import android.util.Log
import io.lociant.core.tools.ToolExposure
import io.lociant.android.server.LociantServer
import io.lociant.android.server.ModelApiMapper
import io.lociant.core.model.ModelChatResult
import io.lociant.core.model.ModelToolCall
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.security.SecureRandom
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Exposes the Kotlin device-layer tools (accessibility, sensors, window,
 * camera, local inference) to the Rust server over a localhost TCP JSON
 * request/response protocol:
 *
 *   {"token": "…", "method": "tools.list"}
 *   {"token": "…", "method": "tools.call", "name": "…", "arguments": {…}}
 *
 * Rust is the policy owner (exposure + remote_allowed); this side only
 * executes. The random token is passed to Rust through the spawn environment
 * so other local processes cannot call phone tools.
 */
class DeviceAdapterServer(
    private val server: LociantServer,
    private val token: String,
    private val port: Int = DEVICE_PORT,
) {
    private val executor = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "lociant-device-adapter").apply { isDaemon = true }
    }
    private val running = AtomicBoolean(false)
    private var socket: ServerSocket? = null

    fun start() {
        if (!running.compareAndSet(false, true)) return
        executor.execute {
            try {
                ServerSocket().apply {
                    reuseAddress = true
                    bind(InetSocketAddress("127.0.0.1", port))
                }.also { socket = it }.use { serverSocket ->
                    while (running.get()) {
                        val client = try {
                            serverSocket.accept()
                        } catch (_: Exception) {
                            break
                        }
                        handle(client)
                    }
                }
            } catch (error: Throwable) {
                Log.w(TAG, "device adapter server failed", error)
            }
        }
    }

    fun stop() {
        running.set(false)
        runCatching { socket?.close() }
        executor.shutdownNow()
    }

    private fun handle(client: java.net.Socket) {
        // Chat generation can take minutes; one daemon thread per connection
        // keeps tools.list responsive while a model is running.
        Thread {
            try {
                // Long read timeout for chat streams; short requests finish early.
                client.soTimeout = 600_000
                val request = JSONObject(
                    BufferedReader(InputStreamReader(client.getInputStream())).readLine() ?: return@Thread
                )
                when {
                    request.optString("token") != token ->
                        respond(client, JSONObject().put("ok", false).put("error", "unauthorized"))
                    request.optString("method") == "tools.list" ->
                        respond(client, JSONObject().put("ok", true)
                            .put("tools", server.toolRegistry().definitions(ToolExposure.Action)))
                    request.optString("method") == "tools.call" ->
                        respond(client, server.toolRegistry().call(
                            name = request.optString("name"),
                            args = request.optJSONObject("arguments") ?: JSONObject(),
                            exposure = ToolExposure.Action,
                        ))
                    request.optString("method") == "models.list" ->
                        respond(client, JSONObject().put("ok", true)
                            .put("models", JSONArray(server.modelManager().listModelsJson(refresh = true))))
                    request.optString("method") == "chat.invoke" ->
                        handleChat(client, request)
                    else -> respond(client, JSONObject().put("ok", false).put("error", "unknown method"))
                }
            } catch (error: Throwable) {
                Log.w(TAG, "device adapter request failed", error)
            } finally {
                runCatching { client.close() }
            }
        }.apply {
            isDaemon = true
            start()
        }
    }

    private fun handleChat(client: java.net.Socket, request: JSONObject) {
        val out = client.getOutputStream()
        fun emit(payload: JSONObject) {
            out.write((payload.toString() + "\n").toByteArray())
            out.flush()
        }
        val body = request.optJSONObject("request")
        if (body == null) {
            emit(JSONObject().put("type", "done").put("ok", false).put("message", "missing request"))
            return
        }
        val chatRequest = ModelApiMapper.parseOpenAiChat(body.toString())
        emit(JSONObject().put("type", "start"))
        val result: ModelChatResult = server.chatController().streamOneTurn(
            request = chatRequest,
            onChunk = { text, _ ->
                if (text.isNotEmpty()) emit(JSONObject().put("type", "chunk").put("text", text))
            },
            onReasoning = { reasoning, _ ->
                if (reasoning.isNotEmpty()) {
                    emit(JSONObject().put("type", "reasoning").put("text", reasoning))
                }
            },
            onToolCall = { call: ModelToolCall ->
                emit(JSONObject()
                    .put("type", "tool_call")
                    .put("id", call.id)
                    .put("name", call.name)
                    .put("arguments", call.arguments))
            },
        )
        if (result.ok) {
            val calls = JSONArray(result.toolCalls.map { call ->
                JSONObject().put("id", call.id).put("name", call.name).put("arguments", call.arguments)
            })
            emit(JSONObject()
                .put("type", "done")
                .put("ok", true)
                .put("text", result.text)
                .put("reasoning", result.reasoning)
                .put("toolCalls", calls)
                .put("usage", JSONObject()
                    .put("prompt_tokens", result.promptTokens)
                    .put("completion_tokens", result.generatedTokens)
                    .put("total_tokens", result.totalTokens)))
        } else {
            emit(JSONObject().put("type", "done").put("ok", false).put("message", result.message))
        }
    }

    private fun respond(client: java.net.Socket, payload: JSONObject) {
        client.getOutputStream().use { out ->
            out.write((payload.toString() + "\n").toByteArray())
            out.flush()
        }
    }

    companion object {
        private const val TAG = "LociantDeviceAdapter"
        const val DEVICE_PORT = 11436
        const val TOKEN_ENV = "LOCIANT_DEVICE_TOKEN"
        const val PORT_ENV = "LOCIANT_DEVICE_PORT"

        fun newToken(): String {
            val bytes = ByteArray(32)
            SecureRandom().nextBytes(bytes)
            return bytes.joinToString("") { "%02x".format(it) }
        }
    }
}
