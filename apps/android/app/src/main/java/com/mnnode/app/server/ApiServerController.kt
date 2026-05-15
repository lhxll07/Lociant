package com.mnnode.app.server

import android.content.Context
import android.util.Log
import com.mnnode.app.model.ModelManager
import com.mnnode.app.model.ModelChatRequest
import com.mnnode.app.model.MnnRuntime
import com.mnnode.app.session.SessionStore
import com.mnnode.app.storage.LocalStore
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.server.engine.EmbeddedServer
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.request.httpMethod
import io.ktor.server.request.header
import io.ktor.server.request.path
import io.ktor.server.request.receiveText
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import org.json.JSONArray
import org.json.JSONObject
import java.net.Inet4Address
import java.net.NetworkInterface
import java.util.concurrent.Executors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ApiServerController(
    private val context: Context,
    private val modelManager: ModelManager,
    private val mnnRuntime: MnnRuntime,
    private val localStore: LocalStore,
    private val sessionStore: SessionStore,
) {
    @Volatile private var server: EmbeddedServer<*, *>? = null
    @Volatile private var starting = false
    private var port = DEFAULT_PORT
    private var modelId = DEFAULT_MODEL_ID
    private var maxOutputTokens = DEFAULT_MAX_OUTPUT_TOKENS
    private var cpuThreads = MnnRuntime.DEFAULT_CPU_THREADS
    private var autoStart = false
    private var currentSessionId = DEFAULT_SESSION_ID
    private var serverEpoch = 0
    private var lastError: String? = null
    private val serverExecutor = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "mnnode-api-server").apply { isDaemon = true }
    }

    private val chatController = ChatController(modelManager, mnnRuntime, sessionStore)

    fun chatController() = chatController

    init { loadSettings() }

    // ---- Public API ----

    @Synchronized
    fun command(command: String, payload: JSONObject = JSONObject()): JSONObject {
        when (command) {
            "start" -> start(payload)
            "stop" -> stop()
            "settings" -> {
                updateSettings(payload)
                if (server != null) tryPreload()
            }
            "session.create" -> selectSession(sessionStore.createModelSession(modelId))
            "session.select" -> selectSession(payload.optString("sessionId", payload.optString("id")))
            "session.delete" -> deleteSession(payload.optString("sessionId", payload.optString("id")))
            "status" -> loadSettings()
        }
        return state()
    }

    @Synchronized fun startForService(payload: JSONObject = JSONObject()) { start(payload) }
    @Synchronized fun stopForService() { stop() }
    fun close() { stop() }

    fun state(): JSONObject = buildStateJson("api.server.state")
    fun serviceState(): JSONObject = buildStateJson(null)
    fun runtimeSummary(): JSONObject = buildStateJson(null, includeHistory = false)

    // ---- Lifecycle ----

    private fun start(payload: JSONObject) {
        if (server != null || starting) return
        starting = true
        val epoch = ++serverEpoch
        updateSettings(payload)
        lastError = null
        serverExecutor.execute { startServer(epoch) }
    }

    @Synchronized
    private fun startServer(epoch: Int) {
        if (epoch != serverEpoch || !starting) return
        runCatching {
            server = embeddedServer(Netty, host = "0.0.0.0", port = port) {
                routing {
                    get("/health") { call.respondText(healthJson().toString(), JsonContentType) }
                    get("/v1/models") {
                        val response = modelsJson()
                        chatController.recordRequestAsync(call.request.httpMethod.value, call.request.path(), 200, 0, modelId)
                        call.respondText(response.toString(), JsonContentType)
                    }
                    post("/v1/chat/completions") { handleChat(call, ChatProtocol.OPENAI) }
                    post("/api/chat") { handleChat(call, ChatProtocol.OLLAMA) }
                    get("/v1/chat/status/{requestId}") { handleAsyncStatus(call) }
                    get("/v1/chat/queue") { handleQueueSnapshot(call) }
                    get("/") { call.respondText(healthJson().toString(), JsonContentType) }
                    get("/{...}") {
                        chatController.recordRequestAsync(call.request.httpMethod.value, call.request.path(), 404, 0, modelId)
                        call.respondText(errorJson("not_found", "Endpoint not found").toString(), JsonContentType, HttpStatusCode.NotFound)
                    }
                }
            }.start(wait = false)
            if (epoch != serverEpoch) {
                server?.stop(700, 1800); server = null
            } else {
                Log.i(TAG, "server started port=$port modelId=$modelId")
                tryPreload()
            }
        }.onFailure { error ->
            server = null; lastError = error.message ?: "server start failed"
            Log.e(TAG, "server start failed port=$port", error)
        }.also { starting = false }
    }

    private fun stop() {
        serverEpoch += 1
        starting = false
        chatController.lastError = null
        runCatching { server?.stop(700, 1800) }
            .onFailure { error -> lastError = error.message ?: "server stop failed" }
        server = null
    }

    private fun tryPreload() {
        val epoch = serverEpoch
        if (!chatController.isModelLoaded(modelId)) {
            chatController.preload(modelId)
        }
    }

    // ---- Route handlers ----

    private suspend fun handleChat(call: ApplicationCall, protocol: ChatProtocol) {
        val started = System.currentTimeMillis()
        val requestId = "${protocol.idPrefix}_$started"
        val endpoint = call.request.path()
        Log.i(TAG, "request start id=$requestId path=$endpoint")

        val raw = call.receiveText()
        val response = try {
            val parsed = parseChat(protocol, raw).withHeaderSession(call.request.headerSessionId())
            val currentRequest = chatController.boundRequest(parsed, modelId, maxOutputTokens)
            val request = chatController.sessionRequest(currentRequest)
            val turnRequest = currentRequest.copy(sessionId = request.sessionId, modelId = request.modelId, persistSession = request.persistSession)
            if (JSONObject(raw).optBoolean("async", false)) {
                val asyncId = chatController.submitAsync(request)
                Log.i(TAG, "request async id=$requestId asyncId=$asyncId")
                call.respondText(JSONObject().put("id", asyncId).put("status", "queued").toString(), JsonContentType, HttpStatusCode.Accepted)
                return
            }
            if (request.stream) {
                call.respond(streamContent(protocol, requestId, request, turnRequest))
                chatController.recordRequestAsync(call.request.httpMethod.value, endpoint, 200, System.currentTimeMillis() - started, modelId)
                Log.i(TAG, "request stream end id=$requestId elapsed=${System.currentTimeMillis() - started}")
                return
            }
            val result = withContext(Dispatchers.IO) {
                chatController.submitSync(request, ChatController.CHAT_TIMEOUT_MS)
            }
            chatController.saveModelTurn(turnRequest, result)
            val status = if (result.ok) HttpStatusCode.OK else HttpStatusCode.BadRequest
            status to responseJson(protocol, result, request.sessionId)
        } catch (error: Throwable) {
            HttpStatusCode.BadRequest to ModelApiMapper.error("invalid_request", error.message ?: "invalid request")
        }
        chatController.recordRequestAsync(call.request.httpMethod.value, endpoint, response.first.value, System.currentTimeMillis() - started, modelId)
        Log.i(TAG, "request end id=$requestId status=${response.first.value} elapsed=${System.currentTimeMillis() - started}")
        call.respondText(response.second.toString(), JsonContentType, response.first)
    }

    private fun parseChat(protocol: ChatProtocol, raw: String) = when (protocol) {
        ChatProtocol.OPENAI -> ModelApiMapper.parseOpenAiChat(raw)
        ChatProtocol.OLLAMA -> ModelApiMapper.parseOllamaChat(raw)
    }

    private fun streamContent(protocol: ChatProtocol, requestId: String, request: ModelChatRequest, turnRequest: ModelChatRequest) = when (protocol) {
        ChatProtocol.OPENAI -> chatController.openAiStreamContent(requestId, request, turnRequest)
        ChatProtocol.OLLAMA -> chatController.ollamaStreamContent(requestId, request, turnRequest)
    }

    private fun responseJson(protocol: ChatProtocol, result: com.mnnode.app.model.ModelChatResult, sessionId: String): JSONObject {
        if (!result.ok) return ModelApiMapper.error("chat_failed", result.message)
        return when (protocol) {
            ChatProtocol.OPENAI -> ModelApiMapper.openAiResponse(result).put("sessionId", sessionId)
            ChatProtocol.OLLAMA -> ModelApiMapper.ollamaResponse(result).put("session_id", sessionId)
        }
    }

    private suspend fun handleAsyncStatus(call: ApplicationCall) {
        val requestId = call.parameters["requestId"] ?: ""
        val status = chatController.requestStatus(requestId)
        call.respondText(status.toString(), JsonContentType)
    }

    private suspend fun handleQueueSnapshot(call: ApplicationCall) {
        val snapshot = chatController.queueSnapshot()
        call.respondText(snapshot.toString(), JsonContentType)
    }

    // ---- State reporting ----

    private fun buildStateJson(type: String?, includeHistory: Boolean = true): JSONObject {
        val running = server != null
        val lanUrl = "http://${lanAddress()}:$port"
        val json = JSONObject()
        if (type != null) json.put("type", type)
        json.put("running", running)
            .put("starting", starting)
            .put("host", "0.0.0.0")
            .put("port", port)
            .put("url", "http://0.0.0.0:$port")
            .put("lanUrl", lanUrl)
            .put("authEnabled", false)
            .put("modelId", modelId)
            .put("modelLoading", chatController.isLoading)
            .put("modelLoaded", chatController.isModelLoaded(modelId))
            .put("activeRequest", JSONObject.NULL)
            .put("defaultOutputTokens", DEFAULT_MAX_OUTPUT_TOKENS)
            .put("maxOutputTokens", maxOutputTokens)
            .put("hardMaxOutputTokens", HARD_MAX_OUTPUT_TOKENS)
            .put("cpuThreads", cpuThreads)
            .put("maxCpuThreads", maxCpuThreads())
            .put("modelMaxOutputTokens", chatController.effectiveMaxTokens(modelId, maxOutputTokens))
            .put("effectiveMaxOutputTokens", chatController.effectiveMaxTokens(modelId, maxOutputTokens))
            .put("autoStart", autoStart)
            .put("currentSessionId", currentSessionId)
            .put("lastError", chatController.lastError ?: lastError ?: JSONObject.NULL)
            .put("message", message())
            .put("packageName", context.packageName)
        if (includeHistory) {
            json.put("sessions", sessionStore.recentModelSessions())
                .put("requestCount", sessionStore.apiRequestCount())
                .put("recentRequests", sessionStore.recentApiRequests())
        }
        return json
    }

    private fun message(): String = when {
        starting -> "Ktor server is starting."
        chatController.isLoading -> "Model server is running. Loading the selected model before first inference."
        server != null -> "Ktor server is running with OpenAI and Ollama-style chat endpoints."
        else -> lastError ?: "Server stopped. Start it to expose model chat endpoints on the local network."
    }

    private fun healthJson(): JSONObject = buildStateJson(null, includeHistory = false).apply {
        put("name", "MNNode Model Server")
        put("version", "0.1.0")
        put("endpoints", JSONArray(listOf("/health", "/v1/models", "/v1/chat/completions", "/api/chat")))
    }

    private fun modelsJson(): JSONObject {
        val source = JSONArray(modelManager.listModelsJson())
        val models = JSONArray()
        for (index in 0 until source.length()) {
            val model = source.optJSONObject(index) ?: continue
            if (!model.optBoolean("ready", false)) continue
            if (!isApiModel(model)) continue
            models.put(JSONObject()
                .put("id", model.optString("id"))
                .put("object", "model").put("created", 0)
                .put("owned_by", model.optString("runtime", "mnnode")))
        }
        return JSONObject().put("object", "list").put("data", models)
    }

    private fun isApiModel(model: JSONObject): Boolean {
        val runtime = model.optString("runtime")
        val type = model.optString("type")
        return runtime == "mnn" || type == "vlm" || type == "chat" || type == "llm"
    }

    private fun errorJson(code: String, message: String) = JSONObject()
        .put("error", JSONObject().put("message", message).put("type", "invalid_request_error").put("code", code))

    // ---- Settings ----

    private fun loadSettings() { applySettings(localStore.getObject(SETTINGS_NAMESPACE, SETTINGS_KEY)) }

    private fun updateSettings(payload: JSONObject) { applySettings(settingsFrom(payload)); saveSettings() }

    private fun saveSettings() {
        localStore.set(SETTINGS_NAMESPACE, SETTINGS_KEY, JSONObject()
            .put("port", port).put("modelId", modelId)
            .put("maxOutputTokens", maxOutputTokens)
            .put("cpuThreads", cpuThreads)
            .put("autoStart", autoStart).put("currentSessionId", currentSessionId))
    }

    private fun settingsFrom(payload: JSONObject): JSONObject {
        val saved = localStore.getObject(SETTINGS_NAMESPACE, SETTINGS_KEY)
        val merged = JSONObject(saved.toString())
        payload.keys().forEach { key -> merged.put(key, payload.opt(key)) }
        return merged
    }

    private fun applySettings(settings: JSONObject) {
        port = settings.optInt("port", DEFAULT_PORT).coerceIn(1024, 65535)
        modelId = ModelManager.normalizeId(settings.optString("modelId", DEFAULT_MODEL_ID)).ifBlank { DEFAULT_MODEL_ID }
        maxOutputTokens = settings.optInt("maxOutputTokens", DEFAULT_MAX_OUTPUT_TOKENS).coerceIn(MIN_OUTPUT_TOKENS, HARD_MAX_OUTPUT_TOKENS)
        val nextCpuThreads = settings.optInt("cpuThreads", MnnRuntime.DEFAULT_CPU_THREADS).coerceIn(MnnRuntime.MIN_CPU_THREADS, maxCpuThreads())
        if (cpuThreads != nextCpuThreads) {
            cpuThreads = nextCpuThreads
            if (mnnRuntime.configureCpuThreads(cpuThreads)) chatController.resetLoadedModel()
        }
        autoStart = settings.optBoolean("autoStart", false)
        currentSessionId = sessionStore.normalizeModelSessionId(settings.optString("currentSessionId", DEFAULT_SESSION_ID))
    }

    private fun maxCpuThreads() = Runtime.getRuntime().availableProcessors()
        .coerceAtLeast(MnnRuntime.MIN_CPU_THREADS)
        .coerceAtMost(MnnRuntime.MAX_CPU_THREADS)

    // ---- Sessions ----

    private fun selectSession(rawSessionId: String) {
        currentSessionId = sessionStore.ensureModelSession(rawSessionId, modelId)
        saveSettings()
    }

    private fun deleteSession(rawSessionId: String) {
        val deletedId = sessionStore.normalizeModelSessionId(rawSessionId)
        if (sessionStore.deleteModelSession(deletedId) && deletedId == currentSessionId) {
            currentSessionId = DEFAULT_SESSION_ID; saveSettings()
        }
    }

    // ---- Extensions ----

    private fun ModelChatRequest.withHeaderSession(headerSessionId: String) =
        if (sessionId.isBlank() && headerSessionId.isNotBlank()) copy(sessionId = headerSessionId) else this

    private fun io.ktor.server.request.ApplicationRequest.headerSessionId() =
        header("X-MNNode-Session-Id") ?: header("X-Session-Id") ?: ""

    // ---- Network ----

    private fun lanAddress(): String = runCatching {
        NetworkInterface.getNetworkInterfaces().asSequence()
            .flatMap { it.inetAddresses.asSequence() }
            .filterIsInstance<Inet4Address>()
            .firstOrNull { !it.isLoopbackAddress && it.hostAddress?.startsWith("169.254.") != true }
            ?.hostAddress
    }.getOrNull() ?: "127.0.0.1"

    companion object {
        private const val TAG = "MNNodeApi"
        private const val DEFAULT_PORT = 11434
        private const val DEFAULT_MODEL_ID = "qwen3.5-2b-mnn"
        private const val DEFAULT_SESSION_ID = "model-server/chat/default"
        private const val DEFAULT_MAX_OUTPUT_TOKENS = com.mnnode.app.model.DEFAULT_OUTPUT_TOKENS
        private const val MIN_OUTPUT_TOKENS = com.mnnode.app.model.MIN_OUTPUT_TOKENS
        private const val HARD_MAX_OUTPUT_TOKENS = com.mnnode.app.model.HARD_MAX_OUTPUT_TOKENS
        private const val SETTINGS_NAMESPACE = "scene/model-server/settings"
        private const val SETTINGS_KEY = "server"
        private val JsonContentType = ContentType.Application.Json.withParameter("charset", "utf-8")
    }

    private enum class ChatProtocol(val idPrefix: String) {
        OPENAI("openai"),
        OLLAMA("ollama"),
    }
}
