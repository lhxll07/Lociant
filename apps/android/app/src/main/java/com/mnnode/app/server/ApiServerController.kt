package com.mnnode.app.server

import android.content.Context
import android.util.Log
import com.mnnode.app.model.ChatCapability
import com.mnnode.app.model.ModelManager
import com.mnnode.app.model.ModelChatRequest
import com.mnnode.app.model.ModelToolCall
import com.mnnode.app.model.ModelToolChoice
import com.mnnode.app.model.ModelMarket
import com.mnnode.app.model.MnnRuntime
import com.mnnode.app.runtime.TriggerEngine
import com.mnnode.app.runtime.DeviceInteraction
import com.mnnode.app.config.RuntimeDefaults
import com.mnnode.app.runtime.VisionRuntime
import com.mnnode.app.scene.SceneManager
import com.mnnode.app.session.SessionStore
import com.mnnode.app.storage.LocalStore
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.server.response.header
import io.ktor.server.engine.EmbeddedServer
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.request.httpMethod
import io.ktor.server.request.header
import io.ktor.server.request.path
import io.ktor.server.request.receiveText
import io.ktor.server.response.respond
import io.ktor.server.response.respondBytes
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.options
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import org.json.JSONArray
import org.json.JSONObject
import java.net.Inet4Address
import java.net.NetworkInterface
import java.io.File
import java.security.SecureRandom
import java.util.UUID
import java.util.concurrent.Executors
import io.ktor.http.content.OutgoingContent
import io.ktor.utils.io.writeFully
import io.ktor.utils.io.writeStringUtf8
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

class ApiServerController(
    private val context: Context,
    private val modelManager: ModelManager,
    private val sceneManager: SceneManager,
    private val chatCapability: ChatCapability,
    private val localStore: LocalStore,
    private val sessionStore: SessionStore,
    private val triggerEngine: TriggerEngine,
) {
    @Volatile private var server: EmbeddedServer<*, *>? = null
    @Volatile private var starting = false
    private var port = DEFAULT_PORT
    private var modelId = DEFAULT_MODEL_ID
    private var maxOutputTokens = DEFAULT_MAX_OUTPUT_TOKENS
    private var cpuThreads = MnnRuntime.DEFAULT_CPU_THREADS
    private var contextProfile = RuntimeDefaults.Sessions.CONTEXT_PROFILE_DEFAULT
    private var historyLimit = RuntimeDefaults.Sessions.MODEL_HISTORY_LIMIT
    private var autoStart = false
    private var currentSessionId = DEFAULT_SESSION_ID
    private var authToken = ""
    private var toolExposure = ToolExposure.Action
    private var serverEpoch = 0
    private var lastError: String? = null
    private val serverExecutor = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "mnnode-api-server").apply { isDaemon = true }
    }

    private val chatController = ChatController(chatCapability, sessionStore)
    private val modelMarket by lazy { ModelMarket(context, modelManager) }
    private val notificationTools by lazy { NotificationTools(context) }
    private val toolRegistry: ToolRegistry by lazy {
        ToolRegistry(
            listOf(
                RuntimeTools(context, runtimeState = { runtimeSummary() }),
                ModelTools(
                    modelManager = modelManager,
                    preloadModel = { chatController.preload(it.ifBlank { modelId }) },
                    cancelChat = { chatController.cancelCurrent() },
                ),
                VisionTools(context),
                StorageTools(sessionStore, localStore),
                notificationTools,
            )
        )
    }
    private val mcpController by lazy { McpController(toolRegistry, exposure = { toolExposure }) }

    fun chatController() = chatController
    fun callTool(name: String, args: JSONObject): JSONObject = toolRegistry.call(name, args)
    fun callToolResult(name: String, args: JSONObject = JSONObject()): JSONObject {
        val response = toolRegistry.call(name, args)
        return response.optJSONObject("result") ?: response
    }

    init { loadSettings() }

    // ---- Public API ----

    @Synchronized
    fun command(command: String, payload: JSONObject = JSONObject()): JSONObject {
        when (command) {
            "start" -> start(payload)
            "stop" -> stop()
            "settings" -> {
                if (payload.optBoolean("generateAuthToken", false)) payload.put("authToken", newToken())
                updateSettings(payload)
                if (server != null) tryPreload()
            }
            "model.release" -> {
                chatController.releaseModel()
                lastError = null
            }
            "session.create" -> selectSession(sessionStore.createModelSession(modelId))
            "session.select" -> selectSession(payload.optString("sessionId", payload.optString("id")))
            "session.delete" -> deleteSession(payload.optString("sessionId", payload.optString("id")))
            "session.details" -> return sessionDetails(payload.optString("sessionId", payload.optString("id")))
            "status" -> loadSettings()
        }
        return state()
    }

    @Synchronized fun startForService(payload: JSONObject = JSONObject()) { start(payload) }
    @Synchronized fun stopForService() { stop() }
    fun close() {
        stop()
        runCatching { notificationTools.close() }
    }

    fun state(): JSONObject = buildStateJson("api.server.state", includeSensitive = true)
    fun serviceState(): JSONObject = buildStateJson(null, includeSensitive = true)
    fun uiState(): JSONObject = buildStateJson(null, includeHistory = false, includeSensitive = true)
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
        try {
            runCatching {
                if (!isPortAvailable(port)) {
                    port = java.net.ServerSocket(0).use { it.localPort }
                    saveSettings()
                }
                server = embeddedServer(Netty, host = "0.0.0.0", port = port) {
                    routing {
                        options("/{...}") { call.withCors(); call.respondText("", JsonContentType, HttpStatusCode.NoContent) }
                        get("/health") { call.withCors(); call.respondText(healthJson().toString(), JsonContentType) }
                        get("/v1/scenes") { call.withCors(); call.respondText(sceneManager.listScenesJson(), JsonContentType) }
                        post("/v1/scenes/{sceneId}/load") { call.withCors(); handleSceneLoad(call) }
                        post("/v1/scenes/{sceneId}/delete") { call.withCors(); handleSceneDelete(call) }
                        get("/v1/events/{sceneId}") { call.withCors(); handleEvents(call) }
                        get("/v1/preview") { call.withCors(); handlePreview(call) }
                        get("/v1/preview/stream") { call.withCors(); handlePreviewStream(call) }
                        get("/v1/models") {
                            call.withCors()
                            val response = modelsJson()
                            chatController.recordRequestAsync(call.request.httpMethod.value, call.request.path(), 200, 0, modelId)
                            call.respondText(response.toString(), JsonContentType)
                        }
                        get("/v1/models/full") { call.withCors(); call.respondText(modelManager.listModelsJson(), JsonContentType) }
                        get("/v1/models/market") { call.withCors(); handleModelMarket(call) }
                        get("/v1/models/market/{modelId}/progress") { call.withCors(); handleModelMarketProgress(call) }
                        post("/v1/models/market/{modelId}/install") { call.withCors(); handleModelMarketInstall(call) }
                        post("/v1/models/{modelId}/delete") { call.withCors(); handleModelDelete(call) }
                        get("/v1/store/{namespace}/{key}") { call.withCors(); handleStoreGet(call) }
                        get("/v1/store/{namespace}") { call.withCors(); handleStoreList(call) }
                        post("/v1/store/{namespace}/{key}") { call.withCors(); handleStoreSet(call) }
                        post("/v1/store/{namespace}/{key}/delete") { call.withCors(); handleStoreRemove(call) }
                        get("/v1/sessions") { call.withCors(); handleSessionGet(call) }
                        post("/v1/runtime/{command}") { call.withCors(); if (!call.authorized()) call.respondUnauthorized() else handleRuntimeCommand(call) }
                        get("/v1/tools") {
                            call.withCors()
                            if (!call.authorized()) return@get call.respondUnauthorized()
                            val response = toolRegistry.manifest(toolExposure)
                            chatController.recordRequestAsync(call.request.httpMethod.value, call.request.path(), 200, 0, modelId)
                            call.respondText(response.toString(), JsonContentType)
                        }
                        post("/v1/tools/{name}/call") { call.withCors(); handleToolCall(call) }
                        get("/mcp") { call.withCors(); if (!call.authorized()) call.respondUnauthorized() else mcpController.get(call) }
                        post("/mcp") { call.withCors(); if (!call.authorized()) call.respondUnauthorized() else mcpController.post(call) }
                        post("/v1/chat/completions") { call.withCors(); if (!call.authorized()) call.respondUnauthorized() else handleChat(call, ChatProtocol.OPENAI) }
                        post("/api/chat") { call.withCors(); if (!call.authorized()) call.respondUnauthorized() else handleChat(call, ChatProtocol.OLLAMA) }
                        get("/v1/chat/status/{requestId}") { call.withCors(); if (!call.authorized()) call.respondUnauthorized() else handleAsyncStatus(call) }
                        get("/v1/chat/queue") { call.withCors(); if (!call.authorized()) call.respondUnauthorized() else handleQueueSnapshot(call) }
                        get("/") { call.withCors(); call.respondText(healthJson().toString(), JsonContentType) }
                        get("/{...}") {
                            call.withCors()
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
            }
        } finally {
            starting = false
        }
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
            val includeStreamUsage = protocol == ChatProtocol.OPENAI && ModelApiMapper.openAiStreamIncludesUsage(raw)
            if (protocol == ChatProtocol.OPENAI) {
                handleOpenAiForcedToolRequest(currentRequest)?.let { response ->
                    chatController.recordRequestAsync(call.request.httpMethod.value, endpoint, response.first.value, System.currentTimeMillis() - started, modelId)
                    call.respondText(response.second.toString(), JsonContentType, response.first)
                    return
                }
            }
            val request = chatController.sessionRequest(currentRequest)
            val turnRequest = currentRequest.copy(sessionId = request.sessionId, modelId = request.modelId, persistSession = request.persistSession)
            if (JSONObject(raw).optBoolean("async", false)) {
                val asyncId = chatController.submitAsync(request)
                Log.i(TAG, "request async id=$requestId asyncId=$asyncId")
                call.respondText(JSONObject().put("id", asyncId).put("status", "queued").toString(), JsonContentType, HttpStatusCode.Accepted)
                return
            }
            if (request.stream) {
                call.respond(streamContent(protocol, requestId, request, turnRequest, includeStreamUsage))
                chatController.recordRequestAsync(call.request.httpMethod.value, endpoint, 200, System.currentTimeMillis() - started, modelId)
                Log.i(TAG, "request stream end id=$requestId elapsed=${System.currentTimeMillis() - started}")
                return
            }
            val result = withContext(Dispatchers.IO) {
                chatController.submitSync(request, RuntimeDefaults.Queue.CHAT_TIMEOUT_MS)
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

    private suspend fun handleOpenAiForcedToolRequest(
        request: ModelChatRequest,
    ): Pair<HttpStatusCode, JSONObject>? {
        val toolCall = forcedToolCall(request) ?: return null
        if (!request.executeTools) {
            return HttpStatusCode.OK to ModelApiMapper.openAiToolCallResponse(request.modelId, toolCall)
        }
        if (!toolRegistry.has(toolCall.name)) {
            return HttpStatusCode.BadRequest to ModelApiMapper.error("tool_not_found", "Unknown local Lociant tool: ${toolCall.name}")
        }

        val toolResult = executeToolCall(toolCall)
        val followUp = request.copy(
            toolChoice = ModelToolChoice.None,
            executeTools = false,
            messages = request.messages + listOf(
                ModelApiMapper.toolAssistantMessage(toolCall),
                ModelApiMapper.toolResultMessage(toolCall, toolResult),
            ),
        )
        val sessionRequest = chatController.sessionRequest(followUp)
        val result = withContext(Dispatchers.IO) {
            chatController.submitSync(sessionRequest, RuntimeDefaults.Queue.CHAT_TIMEOUT_MS)
        }
        chatController.saveModelTurn(followUp.copy(sessionId = sessionRequest.sessionId, modelId = sessionRequest.modelId, persistSession = sessionRequest.persistSession), result)
        return (if (result.ok) HttpStatusCode.OK else HttpStatusCode.BadRequest) to responseJson(ChatProtocol.OPENAI, result, sessionRequest.sessionId)
    }

    private fun forcedToolCall(request: ModelChatRequest): ModelToolCall? {
        return when (val choice = request.toolChoice) {
            is ModelToolChoice.Function -> ModelToolCall(
                id = "call_${UUID.randomUUID().toString().take(8)}",
                name = choice.name,
                arguments = choice.arguments.ifBlank { "{}" },
            )
            ModelToolChoice.Required -> firstDeclaredTool(request)
            ModelToolChoice.Auto, ModelToolChoice.None -> null
        }
    }

    private fun firstDeclaredTool(request: ModelChatRequest): ModelToolCall? {
        val tools = request.tools ?: toolRegistry.definitions(toolExposure)
        for (index in 0 until tools.length()) {
            val name = tools.optJSONObject(index)
                ?.optJSONObject("function")
                ?.optString("name")
                .orEmpty()
            if (name.isNotBlank()) {
                return ModelToolCall("call_${UUID.randomUUID().toString().take(8)}", name, "{}")
            }
        }
        return null
    }

    private fun executeToolCall(toolCall: ModelToolCall): JSONObject {
        val args = runCatching { JSONObject(toolCall.arguments.ifBlank { "{}" }) }.getOrDefault(JSONObject())
        return toolRegistry.call(toolCall.name, args, toolExposure)
            .put("tool_call_id", toolCall.id)
    }

    private fun parseChat(protocol: ChatProtocol, raw: String) = when (protocol) {
        ChatProtocol.OPENAI -> ModelApiMapper.parseOpenAiChat(raw)
        ChatProtocol.OLLAMA -> ModelApiMapper.parseOllamaChat(raw)
    }

    private fun streamContent(
        protocol: ChatProtocol,
        requestId: String,
        request: ModelChatRequest,
        turnRequest: ModelChatRequest,
        includeUsage: Boolean,
    ) = when (protocol) {
        ChatProtocol.OPENAI -> chatController.openAiStreamContent(requestId, request, turnRequest, includeUsage)
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

    private suspend fun handleToolCall(call: ApplicationCall) {
        val started = System.currentTimeMillis()
        if (!call.authorized()) return call.respondUnauthorized()
        val name = call.parameters["name"].orEmpty()
        val json = requestJson(call)
        val args = json.optJSONObject("arguments") ?: json
        val response = toolRegistry.call(name, args, toolExposure)
        val status = if (response.optBoolean("ok", false)) HttpStatusCode.OK else HttpStatusCode.BadRequest
        chatController.recordRequestAsync(call.request.httpMethod.value, call.request.path(), status.value, System.currentTimeMillis() - started, modelId)
        call.respondText(response.toString(), JsonContentType, status)
    }

    private suspend fun handleRuntimeCommand(call: ApplicationCall) {
        call.respondText(command(call.parameters["command"].orEmpty(), requestJson(call)).toString(), JsonContentType)
    }

    private suspend fun handleSceneLoad(call: ApplicationCall) {
        val sceneId = call.parameters["sceneId"].orEmpty()
        val scene = sceneManager.findScene(sceneId)
        if (scene == null) {
            call.respondText(errorJson("scene_not_found", "Scene not found: $sceneId").toString(), JsonContentType, HttpStatusCode.NotFound)
            return
        }
        triggerEngine.loadFromJson(scene.triggers)
        call.respondText(JSONObject()
            .put("ok", true)
            .put("sceneId", sceneId)
            .put("triggersLoaded", scene.triggers.length())
            .toString(), JsonContentType)
    }

    private suspend fun handleSceneDelete(call: ApplicationCall) {
        val sceneId = call.parameters["sceneId"].orEmpty()
        val ok = runCatching { sceneManager.uninstallScene(sceneId) }.getOrDefault(false)
        call.respondText(JSONObject().put("ok", ok).put("id", sceneId).toString(), JsonContentType)
    }

    private suspend fun handleEvents(call: ApplicationCall) {
        val sceneId = call.parameters["sceneId"].orEmpty()
        val type = call.request.queryParameters["type"].orEmpty().takeIf { it.isNotBlank() }
        val limit = call.request.queryParameters["limit"]?.toIntOrNull()?.coerceIn(1, 100) ?: 20
        val events = sessionStore.queryEvents(sceneId, type, limit)
        call.respondText(events.toString(), JsonContentType)
    }

    private suspend fun handlePreview(call: ApplicationCall) {
        val bytes = VisionRuntime.previewBytes()
        if (bytes == null) {
            call.respondText("No preview available", ContentType.Text.Plain, HttpStatusCode.NotFound)
            return
        }
        call.respondBytes(bytes, ContentType.Image.JPEG, HttpStatusCode.OK)
    }

    private suspend fun handlePreviewStream(call: ApplicationCall) {
        call.respond(object : OutgoingContent.WriteChannelContent() {
            override val contentType = ContentType.parse("multipart/x-mixed-replace; boundary=MNNodeBoundary")
            override suspend fun writeTo(channel: io.ktor.utils.io.ByteWriteChannel) {
                var first = true
                while (true) {
                    val bytes = VisionRuntime.previewBytes()
                    if (bytes != null) {
                        if (first) { channel.writeStringUtf8("--MNNodeBoundary\r\n"); first = false }
                        else { channel.writeStringUtf8("\r\n--MNNodeBoundary\r\n") }
                        channel.writeStringUtf8("Content-Type: image/jpeg\r\nContent-Length: ${bytes.size}\r\n\r\n")
                        channel.writeFully(bytes)
                    }
                    delay(50)
                }
            }
        })
    }

    private suspend fun handleModelDelete(call: ApplicationCall) {
        call.respondText(modelManager.deleteModel(call.parameters["modelId"].orEmpty()).toString(), JsonContentType)
    }

    private suspend fun handleModelMarket(call: ApplicationCall) {
        val query = call.request.queryParameters["q"].orEmpty()
        val refresh = call.request.queryParameters["refresh"] == "true"
        val response = withContext(Dispatchers.IO) {
            JSONObject().put("source", "modelscope").put("models", modelMarket.catalog(query, refresh))
        }
        call.respondText(response.toString(), JsonContentType)
    }

    private suspend fun handleModelMarketProgress(call: ApplicationCall) {
        val modelId = call.parameters["modelId"].orEmpty()
        val response = withContext(Dispatchers.IO) {
            modelMarket.installProgress(modelId) ?: JSONObject().put("modelId", modelId).put("active", false)
        }
        call.respondText(response.toString(), JsonContentType)
    }

    private suspend fun handleModelMarketInstall(call: ApplicationCall) {
        val id = call.parameters["modelId"].orEmpty()
        val response = withContext(Dispatchers.IO) {
            runCatching {
                modelMarket.installAsync(id)
            }.fold(
                onSuccess = { json -> json },
                onFailure = { error -> JSONObject().put("ok", false).put("message", error.message ?: "Model install failed") },
            )
        }
        call.respondText(response.toString(), JsonContentType, if (response.optBoolean("ok")) HttpStatusCode.Accepted else HttpStatusCode.BadRequest)
    }

    private suspend fun handleStoreGet(call: ApplicationCall) {
        call.respondText(localStore.get(storeNamespace(call), storeKey(call)).toString(), JsonContentType)
    }

    private suspend fun handleStoreSet(call: ApplicationCall) {
        val body = call.receiveText()
        val value = runCatching {
            val json = JSONObject(body.ifBlank { "{}" })
            if (json.has("value")) json.opt("value") else localStore.parseValue(body)
        }.getOrElse { localStore.parseValue(body) }
        call.respondText(localStore.set(storeNamespace(call), storeKey(call), value).toString(), JsonContentType)
    }

    private suspend fun handleStoreRemove(call: ApplicationCall) {
        call.respondText(localStore.remove(storeNamespace(call), storeKey(call)).toString(), JsonContentType)
    }

    private suspend fun handleStoreList(call: ApplicationCall) {
        call.respondText(localStore.list(storeNamespace(call)).toString(), JsonContentType)
    }

    private suspend fun handleSessionGet(call: ApplicationCall) {
        if (!call.authorized()) return call.respondUnauthorized()
        val sessionId = call.request.queryParameters["sessionId"].orEmpty()
        val session = sessionStore.sessionDetails(sessionId)
        call.respondText(session.toString(), JsonContentType)
    }

    // ---- State reporting ----

    private fun buildStateJson(type: String?, includeHistory: Boolean = true, includeSensitive: Boolean = false): JSONObject {
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
            .put("authEnabled", authToken.isNotBlank())
            .apply { if (includeSensitive) put("authToken", authToken) }
            .put("toolExposure", toolExposure.id)
            .put("modelId", modelId)
            .put("modelLoading", chatController.isLoading)
            .put("modelLoaded", chatController.isModelLoaded(modelId))
            .put("activeRequest", JSONObject.NULL)
            .put("defaultOutputTokens", DEFAULT_MAX_OUTPUT_TOKENS)
            .put("maxOutputTokens", maxOutputTokens)
            .put("hardMaxOutputTokens", HARD_MAX_OUTPUT_TOKENS)
            .put("cpuThreads", cpuThreads)
            .put("maxCpuThreads", maxCpuThreads())
            .put("contextProfile", contextProfile)
            .put("historyLimit", historyLimit)
            .put("modelMaxOutputTokens", modelManager.maxNewTokens(modelId) ?: JSONObject.NULL)
            .put("contextWindowTokens", chatController.contextWindowTokens(modelId))
            .put("contextStrategy", "token-budget")
            .put("effectiveMaxOutputTokens", chatController.effectiveMaxTokens(modelId, maxOutputTokens))
            .put("sessionPolicy", sessionPolicyJson())
            .put("autoStart", autoStart)
            .put("currentSessionId", currentSessionId)
            .put("lastError", chatController.lastError ?: lastError ?: JSONObject.NULL)
            .put("message", message())
            .put("packageName", context.packageName)
            .put("device", DeviceInteraction.snapshot(context))
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
        put("ok", true)
        put("name", "Lociant Model Server")
        put("version", "0.1.0")
        put("endpoints", JSONArray(listOf("/health", "/mcp", "/v1/models", "/v1/tools", "/v1/chat/completions", "/api/chat")))
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

    private fun sessionPolicyJson(): JSONObject = JSONObject()
        .put("recentLimit", RuntimeDefaults.Sessions.RECENT_LIMIT)
        .put("historyLimit", historyLimit)
        .put("contextProfile", contextProfile)
        .put("maxHistoryLimit", RuntimeDefaults.Sessions.MODEL_HISTORY_MAX_LIMIT)
        .put("lastTextLimit", RuntimeDefaults.Sessions.LAST_TEXT_LIMIT)
        .put("maxSystemMessages", RuntimeDefaults.Sessions.MAX_SYSTEM_MESSAGES)
        .put("defaultSessionId", RuntimeDefaults.Sessions.DEFAULT_CHAT_ID)
        .put("cache", JSONObject()
            .put("promptCache", RuntimeDefaults.NativeRuntime.PROMPT_CACHE_ENABLED)
            .put("singleActiveSession", true)
            .put("mode", "native-prompt-cache"))

    private fun isApiModel(model: JSONObject): Boolean {
        val runtime = model.optString("runtime")
        val type = model.optString("type")
        return runtime == "mnn" || type == "vlm" || type == "chat" || type == "llm"
    }

    private fun errorJson(code: String, message: String) = JSONObject()
        .put("error", JSONObject().put("message", message).put("type", "invalid_request_error").put("code", code))

    private fun ApplicationCall.withCors() {
        response.header("Access-Control-Allow-Origin", "*")
        response.header("Access-Control-Allow-Headers", "Content-Type, Authorization, X-Lociant-Token, X-MNNode-Session-Id, X-Session-Id, MCP-Protocol-Version, Mcp-Session-Id")
        response.header("Access-Control-Allow-Methods", "GET, POST, OPTIONS")
    }

    // ---- Settings ----

    private fun loadSettings() { applySettings(localStore.getObject(SETTINGS_NAMESPACE, SETTINGS_KEY)) }

    private fun updateSettings(payload: JSONObject) { applySettings(settingsFrom(payload)); saveSettings() }

    private fun saveSettings() {
        localStore.set(SETTINGS_NAMESPACE, SETTINGS_KEY, JSONObject()
            .put("port", port).put("modelId", modelId)
            .put("maxOutputTokens", maxOutputTokens)
            .put("cpuThreads", cpuThreads)
            .put("contextProfile", contextProfile)
            .put("historyLimit", historyLimit)
            .put("authToken", authToken)
            .put("toolExposure", toolExposure.id)
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
            if (chatCapability.configureCpuThreads(cpuThreads)) chatController.resetLoadedModel()
        }
        contextProfile = normalizeContextProfile(settings.optString("contextProfile", RuntimeDefaults.Sessions.CONTEXT_PROFILE_DEFAULT))
        historyLimit = settings.optInt("historyLimit", historyLimitForContextProfile(contextProfile))
            .coerceIn(1, RuntimeDefaults.Sessions.MODEL_HISTORY_MAX_LIMIT)
        chatController.configureHistoryLimit(historyLimit)
        autoStart = settings.optBoolean("autoStart", false)
        currentSessionId = sessionStore.normalizeModelSessionId(settings.optString("currentSessionId", DEFAULT_SESSION_ID))
        authToken = settings.optString("authToken").trim()
        toolExposure = ToolExposure.from(settings.optString("toolExposure", ToolExposure.Action.id))
    }

    private fun maxCpuThreads() = Runtime.getRuntime().availableProcessors()
        .coerceAtLeast(MnnRuntime.MIN_CPU_THREADS)
        .coerceAtMost(MnnRuntime.MAX_CPU_THREADS)

    private fun normalizeContextProfile(value: String): String {
        return when (value.lowercase()) {
            RuntimeDefaults.Sessions.CONTEXT_PROFILE_LIGHT -> RuntimeDefaults.Sessions.CONTEXT_PROFILE_LIGHT
            RuntimeDefaults.Sessions.CONTEXT_PROFILE_DEEP -> RuntimeDefaults.Sessions.CONTEXT_PROFILE_DEEP
            else -> RuntimeDefaults.Sessions.CONTEXT_PROFILE_BALANCED
        }
    }

    private fun historyLimitForContextProfile(value: String): Int {
        return when (normalizeContextProfile(value)) {
            RuntimeDefaults.Sessions.CONTEXT_PROFILE_LIGHT -> RuntimeDefaults.Sessions.MODEL_HISTORY_LIGHT_LIMIT
            RuntimeDefaults.Sessions.CONTEXT_PROFILE_DEEP -> RuntimeDefaults.Sessions.MODEL_HISTORY_DEEP_LIMIT
            else -> RuntimeDefaults.Sessions.MODEL_HISTORY_LIMIT
        }
    }

    // ---- Sessions ----

    private fun selectSession(rawSessionId: String) {
        val previousSessionId = currentSessionId
        currentSessionId = sessionStore.ensureModelSession(rawSessionId, modelId)
        if (currentSessionId != previousSessionId) {
            chatController.resetSessionCache()
        }
        saveSettings()
    }

    private fun deleteSession(rawSessionId: String) {
        val deletedId = sessionStore.normalizeModelSessionId(rawSessionId)
        if (sessionStore.deleteModelSession(deletedId) && deletedId == currentSessionId) {
            currentSessionId = DEFAULT_SESSION_ID
            chatController.resetSessionCache()
            saveSettings()
        }
    }

    private fun sessionDetails(rawSessionId: String): JSONObject {
        val details = sessionStore.sessionDetails(rawSessionId)
        return state().put("session", details)
    }

    // ---- Extensions ----

    private fun ModelChatRequest.withHeaderSession(headerSessionId: String) =
        if (sessionId.isBlank() && headerSessionId.isNotBlank()) copy(sessionId = headerSessionId) else this

    private fun io.ktor.server.request.ApplicationRequest.headerSessionId() =
        header("X-MNNode-Session-Id") ?: header("X-Session-Id") ?: ""

    private fun ApplicationCall.authorized(): Boolean {
        val token = authToken
        if (token.isBlank()) return true
        val header = request.header("Authorization").orEmpty()
        val bearer = if (header.startsWith("Bearer ", ignoreCase = true)) header.substringAfter(' ').trim() else ""
        return bearer == token || request.header("X-Lociant-Token") == token
    }

    private suspend fun ApplicationCall.respondUnauthorized() {
        respondText(errorJson("unauthorized", "Missing or invalid API token").toString(), JsonContentType, HttpStatusCode.Unauthorized)
    }

    // ---- Network ----

    private fun lanAddress(): String = runCatching {
        NetworkInterface.getNetworkInterfaces().asSequence()
            .flatMap { it.inetAddresses.asSequence() }
            .filterIsInstance<Inet4Address>()
            .firstOrNull { !it.isLoopbackAddress && it.hostAddress?.startsWith("169.254.") != true }
            ?.hostAddress
    }.getOrNull() ?: "127.0.0.1"

    // ---- Helpers ----

    private suspend fun requestJson(call: ApplicationCall): JSONObject {
        return runCatching { JSONObject(call.receiveText().ifBlank { "{}" }) }.getOrDefault(JSONObject())
    }

    private fun storeNamespace(call: ApplicationCall) = call.parameters["namespace"].orEmpty()
    private fun storeKey(call: ApplicationCall) = call.parameters["key"].orEmpty()

    private fun isPortAvailable(port: Int): Boolean = runCatching {
        java.net.ServerSocket(port).use { it.close() }; true
    }.getOrDefault(false)

    companion object {
        private const val TAG = "MNNodeApi"
        private const val DEFAULT_PORT = RuntimeDefaults.PORT
        private const val DEFAULT_MODEL_ID = RuntimeDefaults.MODEL_ID
        private const val DEFAULT_SESSION_ID = RuntimeDefaults.Sessions.DEFAULT_CHAT_ID
        private const val DEFAULT_MAX_OUTPUT_TOKENS = com.mnnode.app.model.DEFAULT_OUTPUT_TOKENS
        private const val MIN_OUTPUT_TOKENS = com.mnnode.app.model.MIN_OUTPUT_TOKENS
        private const val HARD_MAX_OUTPUT_TOKENS = com.mnnode.app.model.HARD_MAX_OUTPUT_TOKENS
        private const val SETTINGS_NAMESPACE = RuntimeDefaults.Settings.SERVER_NAMESPACE
        private const val SETTINGS_KEY = RuntimeDefaults.Settings.SERVER_KEY
        private val JsonContentType = ContentType.Application.Json.withParameter("charset", "utf-8")
        fun newToken(): String {
            val bytes = ByteArray(18)
            SecureRandom().nextBytes(bytes)
            return java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
        }
    }

    private enum class ChatProtocol(val idPrefix: String) {
        OPENAI("openai"),
        OLLAMA("ollama"),
    }
}
