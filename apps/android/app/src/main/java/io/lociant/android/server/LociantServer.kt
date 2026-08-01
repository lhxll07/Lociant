package io.lociant.android.server

import android.content.Context
import android.util.Log
import io.lociant.runtime.model.ChatCapability
import io.lociant.runtime.model.ModelManager
import io.lociant.runtime.model.ModelMarket
import io.lociant.core.model.ModelChatRequest
import io.lociant.core.model.ModelToolCall
import io.lociant.core.model.ModelToolChoice
import io.lociant.core.config.RuntimeDefaults
import io.lociant.runtime.model.MnnRuntime
import io.lociant.tools.runtime.DeviceInteraction
import io.lociant.data.session.SessionStore
import io.lociant.data.storage.LocalStore
import io.lociant.core.api.ApiContract
import io.lociant.core.tools.ToolCallOrigin
import io.lociant.core.tools.ToolExposure
import io.lociant.core.tools.ToolRegistry
import io.lociant.mcp.McpController
import io.lociant.tools.AndroidTools
import io.lociant.tools.LlmTools
import io.lociant.tools.ModelTools
import io.lociant.tools.RuntimeTools
import io.lociant.tools.VisionTools
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.ApplicationRequest
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
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.delete
import io.ktor.server.routing.options
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.routing
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.plugins.statuspages.exception
import io.ktor.server.application.install
import org.json.JSONArray
import org.json.JSONObject
import io.ktor.http.content.OutgoingContent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.Inet4Address
import java.net.NetworkInterface
import java.security.SecureRandom
import java.util.UUID
import java.util.concurrent.Executors

class LociantServer(
    private val context: Context,
    private val modelManager: ModelManager,
    private val chatCapability: ChatCapability,
    private val localStore: LocalStore,
    private val sessionStore: SessionStore,
    private val startVisionRuntime: (JSONObject) -> Unit,
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
        Thread(runnable, "lociant-api-server").apply { isDaemon = true }
    }

    private val chatController = ChatController(chatCapability, sessionStore)
    private val modelMarket = ModelMarket(context, modelManager)
    private val llmToolExecutor = LlmToolExecutor(
        modelManager = modelManager,
        chatController = chatController,
        defaultModelId = { modelId },
        defaultMaxOutputTokens = { maxOutputTokens },
    )
    private val toolRegistry: ToolRegistry by lazy {
        ToolRegistry(
            listOf(
                RuntimeTools(runtimeState = { runtimeSummary() }),
                AndroidTools(context),
                ModelTools(modelManager = modelManager),
                LlmTools(
                    modelManager = modelManager,
                    runtimeState = { runtimeSummary() },
                    chat = llmToolExecutor::execute,
                ),
                VisionTools(context, startVisionRuntime),

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

    init {
        loadSettings()
        sessionStore.createSessionIfAbsent(currentSessionId, modelId)
    }

    // ---- Public API ----

    @Synchronized
    fun updateRuntimeSettings(payload: JSONObject): JSONObject {
        val next = JSONObject(payload.toString())
        if (next.optBoolean("generateAuthToken", false)) next.put("authToken", newToken())
        next.remove("generateAuthToken")
        updateSettings(next)
        if (server != null) tryPreload()
        return state()
    }

    @Synchronized
    fun releaseModel(): JSONObject {
        chatController.releaseModel()
        lastError = null
        return state()
    }

    @Synchronized
    fun createSession(): String {
        val id = sessionStore.createModelSession(modelId)
        selectSession(id)
        return id
    }

    @Synchronized fun startForService(payload: JSONObject = JSONObject()) { start(payload) }
    @Synchronized fun stopForService() { stop() }
    fun close() {
        stop()
    }

    fun state(): JSONObject = buildStateJson("api.server.state", includeSensitive = true)
    fun serviceState(): JSONObject = buildStateJson(null, includeSensitive = true)
    fun uiState(): JSONObject = buildStateJson(null, includeHistory = false, includeSensitive = true)
    fun runtimeSummary(): JSONObject = buildStateJson(null, includeHistory = false)
    fun settingsJson(): JSONObject = JSONObject()
        .put("port", port)
        .put("modelId", modelId)
        .put("maxOutputTokens", maxOutputTokens)
        .put("cpuThreads", cpuThreads)
        .put("contextProfile", contextProfile)
        .put("historyLimit", historyLimit)
        .put("authToken", authToken)
        .put("toolExposure", toolExposure.id)
        .put("autoStart", autoStart)
        .put("currentSessionId", currentSessionId)

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
                check(isPortAvailable(port)) { "Port $port is already in use" }
                server = embeddedServer(Netty, host = "0.0.0.0", port = port) {
                    install(StatusPages) {
                        exception<InvalidRequestException> { call, error ->
                            call.respondProblem(HttpStatusCode.BadRequest, "Invalid request", error.message ?: "Invalid request", "invalid-request")
                        }
                        exception<UnauthorizedRequestException> { call, _ ->
                            call.respondProblem(HttpStatusCode.Unauthorized, "Unauthorized", "Provide a valid bearer token", "unauthorized")
                        }
                        exception<NoSuchElementException> { call, error ->
                            call.respondProblem(HttpStatusCode.NotFound, "Resource not found", error.message ?: "Resource not found", "not-found")
                        }
                        exception<IllegalArgumentException> { call, error ->
                            call.respondProblem(HttpStatusCode.BadRequest, "Invalid request", error.message ?: "Invalid request", "invalid-request")
                        }
                        exception<Throwable> { call, error ->
                            Log.e(TAG, "Unhandled API error", error)
                            call.respondProblem(HttpStatusCode.InternalServerError, "Internal server error", "The request could not be completed", "internal-error")
                        }
                    }
                    routing {
                        options("/{...}") { call.withCors(); call.respondText("", JsonContentType, HttpStatusCode.NoContent) }
                        get(ApiContract.HEALTH) { call.withCors(); call.respondText(healthJson().toString(), JsonContentType) }
                        get(ApiContract.OpenAi.MODELS) {
                            call.withCors()
                            if (!call.authorized()) return@get call.respondUnauthorized()
                            val response = modelsJson()
                            call.respondText(response.toString(), JsonContentType)
                        }
                        post(ApiContract.OpenAi.CHAT_COMPLETIONS) { call.withCors(); if (!call.authorized()) call.respondUnauthorized() else handleChat(call) }
                        post(ApiContract.Mcp.ENDPOINT) { call.withCors(); if (!call.authorized()) call.respondUnauthorized() else mcpController.post(call) }

                        get(ApiContract.Control.RUNTIME) { call.withCors(); requireAuthorized(call); call.respondText(runtimeSummary().toString(), JsonContentType) }
                        get(ApiContract.Control.SETTINGS) { call.withCors(); requireAuthorized(call); call.respondText(settingsJson().toString(), JsonContentType) }
                        put(ApiContract.Control.SETTINGS) { call.withCors(); requireAuthorized(call); updateRuntimeSettings(requestJson(call)); call.respondText(settingsJson().toString(), JsonContentType) }

                        get(ApiContract.Control.MODELS) { call.withCors(); handleModelsFull(call) }
                        delete("${ApiContract.Control.MODELS}/{modelId}") { call.withCors(); handleModelDelete(call) }
                        get(ApiContract.Control.CATALOG_MODELS) { call.withCors(); handleModelMarket(call) }
                        post(ApiContract.Control.MODEL_INSTALLATIONS) { call.withCors(); handleModelInstall(call) }
                        get("${ApiContract.Control.MODEL_INSTALLATIONS}/{jobId}") { call.withCors(); handleModelInstallProgress(call) }

                        get(ApiContract.Control.SESSIONS) { call.withCors(); requireAuthorized(call); call.respondText(JSONObject().put("sessions", sessionStore.recentModelSessions()).toString(), JsonContentType) }
                        post(ApiContract.Control.SESSIONS) { call.withCors(); requireAuthorized(call); val id = createSession(); call.respondText(sessionStore.sessionDetails(id).toString(), JsonContentType, HttpStatusCode.Created) }
                        get("${ApiContract.Control.SESSIONS}/{sessionId}") { call.withCors(); requireAuthorized(call); call.respondText(sessionStore.sessionDetails(call.parameters["sessionId"].orEmpty()).toString(), JsonContentType) }
                        delete("${ApiContract.Control.SESSIONS}/{sessionId}") { call.withCors(); requireAuthorized(call); deleteSession(call.parameters["sessionId"].orEmpty()); call.respondText("", JsonContentType, HttpStatusCode.NoContent) }

                        get("${ApiContract.Control.STORE}/{namespace}") { call.withCors(); requireAuthorized(call); respondStore(call) { localStore.list(call.parameters["namespace"].orEmpty()) } }
                        get("${ApiContract.Control.STORE}/{namespace}/{key}") { call.withCors(); handleStoreGet(call) }
                        put("${ApiContract.Control.STORE}/{namespace}/{key}") { call.withCors(); handleStoreSet(call) }
                        delete("${ApiContract.Control.STORE}/{namespace}/{key}") { call.withCors(); requireAuthorized(call); respondStore(call) { localStore.remove(call.parameters["namespace"].orEmpty(), call.parameters["key"].orEmpty()) } }

                        get(ApiContract.Control.TOOLS) {
                            call.withCors()
                            requireAuthorized(call)
                            val response = toolRegistry.manifest(toolExposure)
                            call.respondText(response.toString(), JsonContentType)
                        }
                        post("${ApiContract.Control.TOOLS}/{name}/calls") { call.withCors(); handleToolCall(call) }
                        get("${ApiContract.Control.CHAT_REQUESTS}/{requestId}") { call.withCors(); requireAuthorized(call); handleAsyncStatus(call) }
                        get(ApiContract.Control.CHAT_REQUESTS) { call.withCors(); requireAuthorized(call); handleQueueSnapshot(call) }
                        get("/{...}") {
                            call.withCors()
                            call.respondProblem(HttpStatusCode.NotFound, "Endpoint not found", "No endpoint matches ${call.request.path()}", "endpoint-not-found")
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

    private suspend fun handleChat(call: ApplicationCall) {
        val started = System.currentTimeMillis()
        val requestId = "openai_$started"
        val endpoint = call.request.path()
        Log.i(TAG, "request start id=$requestId path=$endpoint")

        val raw = receiveTextLimited(call)
        val response = try {
            val parsed = ModelApiMapper.parseOpenAiChat(raw).withHeaderSession(call.request.headerSessionId())
            val currentRequest = chatController.boundRequest(parsed, modelId, maxOutputTokens)
            val includeStreamUsage = ModelApiMapper.openAiStreamIncludesUsage(raw)
            handleOpenAiForcedToolRequest(currentRequest)?.let { response ->
                chatController.recordRequestAsync(call.request.httpMethod.value, endpoint, response.first.value, System.currentTimeMillis() - started, modelId)
                call.respondText(response.second.toString(), JsonContentType, response.first)
                return
            }
            val request = chatController.sessionRequest(currentRequest)
            val turnRequest = currentRequest.copy(sessionId = request.sessionId, modelId = request.modelId, persistSession = request.persistSession)
            if (JSONObject(raw).optBoolean("async", false)) {
                val asyncId = chatController.submitAsync(request, turnRequest)
                Log.i(TAG, "request async id=$requestId asyncId=$asyncId")
                call.respondText(JSONObject().put("id", asyncId).put("status", "queued").toString(), JsonContentType, HttpStatusCode.Accepted)
                return
            }
            if (request.stream) {
                call.respond(chatController.openAiStreamContent(requestId, request, turnRequest, includeStreamUsage))
                chatController.recordRequestAsync(call.request.httpMethod.value, endpoint, 200, System.currentTimeMillis() - started, modelId)
                Log.i(TAG, "request stream end id=$requestId elapsed=${System.currentTimeMillis() - started}")
                return
            }
            val result = withContext(Dispatchers.IO) {
                chatController.submitSync(request, RuntimeDefaults.Queue.CHAT_TIMEOUT_MS)
            }
            chatController.saveModelTurn(turnRequest, result)
            val status = if (result.ok) HttpStatusCode.OK else HttpStatusCode.BadRequest
            status to responseJson(result, request.sessionId)
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
        return (if (result.ok) HttpStatusCode.OK else HttpStatusCode.BadRequest) to responseJson(result, sessionRequest.sessionId)
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
        return toolRegistry.call(toolCall.name, args, toolExposure, ToolCallOrigin.Remote)
            .put("tool_call_id", toolCall.id)
    }

    private fun responseJson(result: io.lociant.core.model.ModelChatResult, sessionId: String): JSONObject {
        if (!result.ok) return ModelApiMapper.error("chat_failed", result.message)
        return ModelApiMapper.openAiResponse(result).put("sessionId", sessionId)
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
        requireAuthorized(call)
        val name = call.parameters["name"].orEmpty()
        val json = requestJson(call)
        val args = json.optJSONObject("arguments") ?: json
        val response = toolRegistry.call(name, args, toolExposure, ToolCallOrigin.Remote)
        val status = if (response.optBoolean("ok", false)) HttpStatusCode.OK else HttpStatusCode.BadRequest
        chatController.recordRequestAsync(call.request.httpMethod.value, call.request.path(), status.value, System.currentTimeMillis() - started, modelId)
        call.respondText(response.toString(), JsonContentType, status)
    }

    private suspend fun handleModelsFull(call: ApplicationCall) {
        requireAuthorized(call)
        val refresh = call.request.queryParameters["refresh"]?.toBooleanStrictOrNull() ?: false
        val models = withContext(Dispatchers.IO) { JSONArray(modelManager.listModelsJson(refresh)) }
        call.respondText(JSONObject().put("models", models).toString(), JsonContentType)
    }

    private suspend fun handleModelMarket(call: ApplicationCall) {
        requireAuthorized(call)
        val query = call.request.queryParameters["q"].orEmpty()
        val refresh = call.request.queryParameters["refresh"]?.toBooleanStrictOrNull() ?: false
        val models = withContext(Dispatchers.IO) { modelMarket.catalog(query, refresh) }
        call.respondText(JSONObject().put("models", models).toString(), JsonContentType)
    }

    private suspend fun handleModelInstall(call: ApplicationCall) {
        requireAuthorized(call)
        val modelId = requestJson(call).optString("modelId")
        if (modelId.isBlank()) throw InvalidRequestException("modelId is required")
        val response = runCatching {
            withContext(Dispatchers.IO) { modelMarket.installAsync(modelId) }
        }.getOrElse { throw InvalidRequestException(it.message ?: "Model install failed", it) }
        if (!response.optBoolean("ok", false)) {
            throw InvalidRequestException(response.optJSONObject("error")?.optString("message") ?: "Model install failed")
        }
        response.remove("ok")
        call.respondText(response.toString(), JsonContentType, HttpStatusCode.Accepted)
    }

    private suspend fun handleModelInstallProgress(call: ApplicationCall) {
        requireAuthorized(call)
        val modelId = call.parameters["jobId"].orEmpty()
        val response = modelMarket.installProgress(modelId)
            ?: throw NoSuchElementException("Model installation not found: $modelId")
        response.remove("ok")
        call.respondText(response.toString(), JsonContentType)
    }

    private suspend fun handleModelDelete(call: ApplicationCall) {
        requireAuthorized(call)
        val response = withContext(Dispatchers.IO) {
            modelManager.deleteModel(call.parameters["modelId"].orEmpty())
        }
        if (!response.optBoolean("ok", false)) {
            throw InvalidRequestException(response.optString("message", "Model could not be deleted"))
        }
        response.remove("ok")
        call.respondText(response.toString(), JsonContentType)
    }

    private suspend fun handleStoreGet(call: ApplicationCall) {
        requireAuthorized(call)
        respondStore(call) {
            localStore.get(call.parameters["namespace"].orEmpty(), call.parameters["key"].orEmpty())
        }
    }

    private suspend fun handleStoreSet(call: ApplicationCall) {
        requireAuthorized(call)
        val request = requestJson(call)
        respondStore(call) {
            require(request.has("value")) { "Store request requires value" }
            localStore.set(
                call.parameters["namespace"].orEmpty(),
                call.parameters["key"].orEmpty(),
                request.opt("value") ?: JSONObject.NULL,
            )
        }
    }

    private suspend fun respondStore(call: ApplicationCall, operation: () -> JSONObject) {
        val response = operation()
        response.remove("ok")
        call.respondText(response.toString(), JsonContentType)
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
        server != null -> "Lociant is serving OpenAI, MCP, and control APIs."
        else -> lastError ?: "Server stopped. Start it to expose model chat endpoints on the local network."
    }

    private fun healthJson(): JSONObject = buildStateJson(null, includeHistory = false).apply {
        put("ok", true)
        put("name", "Lociant Model Server")
        put("version", ApiContract.VERSION)
        put("endpoints", JSONArray(listOf(ApiContract.HEALTH, ApiContract.Mcp.ENDPOINT, ApiContract.OpenAi.MODELS, ApiContract.OpenAi.CHAT_COMPLETIONS, ApiContract.Control.BASE)))
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
                .put("owned_by", model.optString("runtime", "lociant")))
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
        response.header("Access-Control-Allow-Headers", "Content-Type, Authorization, X-Lociant-Token, X-Lociant-Session-Id, MCP-Protocol-Version")
        response.header("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS")
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
        currentSessionId = sessionStore.requireSessionId(settings.optString("currentSessionId", DEFAULT_SESSION_ID))
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

    @Synchronized
    fun selectSession(rawSessionId: String): JSONObject {
        val previousSessionId = currentSessionId
        currentSessionId = sessionStore.requireExistingSession(rawSessionId)
        if (currentSessionId != previousSessionId) {
            chatController.resetSessionCache()
        }
        saveSettings()
        return state()
    }

    @Synchronized
    fun deleteSession(rawSessionId: String): JSONObject {
        val deletedId = sessionStore.requireSessionId(rawSessionId)
        if (!sessionStore.deleteModelSession(deletedId)) {
            throw NoSuchElementException("Session not found: $deletedId")
        }
        if (deletedId == currentSessionId) {
            sessionStore.createSessionIfAbsent(DEFAULT_SESSION_ID, modelId)
            selectSession(DEFAULT_SESSION_ID)
        } else {
            saveSettings()
        }
        return state()
    }

    fun sessionDetails(rawSessionId: String): JSONObject = sessionStore.sessionDetails(rawSessionId)


    private suspend fun ApplicationCall.respondUnauthorized() {
        respondText(errorJson("unauthorized", "Missing or invalid API token").toString(), JsonContentType, HttpStatusCode.Unauthorized)
    }

    private fun requireAuthorized(call: ApplicationCall) {
        if (!call.authorized()) throw UnauthorizedRequestException()
    }

    private fun ApplicationCall.authorized(): Boolean {
        if (authToken.isBlank()) return true
        val bearer = request.header("Authorization")
            ?.trim()
            ?.takeIf { it.startsWith("Bearer ", ignoreCase = true) }
            ?.substringAfter(' ', "")
            ?.trim()
            .orEmpty()
        val direct = request.header("X-Lociant-Token")?.trim().orEmpty()
        return bearer == authToken || direct == authToken
    }

    private fun ApplicationRequest.headerSessionId(): String? {
        return header(ApiContract.SESSION_HEADER)?.takeIf { it.isNotEmpty() }
    }

    private fun ModelChatRequest.withHeaderSession(headerSessionId: String?): ModelChatRequest {
        val session = headerSessionId.orEmpty()
        if (session.isEmpty() || sessionId.isNotEmpty()) return this
        return copy(sessionId = session)
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
        val raw = receiveTextLimited(call)
        if (raw.isBlank()) return JSONObject()
        return runCatching { JSONObject(raw) }
            .getOrElse { throw InvalidRequestException("Request body must be a JSON object", it) }
    }

    private suspend fun receiveTextLimited(call: ApplicationCall): String {
        val declaredLength = call.request.headers[HttpHeaders.ContentLength]?.toLongOrNull()
        require(declaredLength == null || declaredLength <= MAX_JSON_BODY_BYTES) {
            "Request body is too large"
        }
        return call.receiveText().also { body ->
            require(body.length <= MAX_JSON_BODY_BYTES) { "Request body is too large" }
        }
    }


    private fun isPortAvailable(port: Int): Boolean = runCatching {
        java.net.ServerSocket(port).use { it.close() }; true
    }.getOrDefault(false)

    companion object {
        private const val TAG = "LociantApi"
        private const val DEFAULT_PORT = RuntimeDefaults.PORT
        private const val DEFAULT_MODEL_ID = RuntimeDefaults.MODEL_ID
        private const val DEFAULT_SESSION_ID = RuntimeDefaults.Sessions.DEFAULT_CHAT_ID
        private const val DEFAULT_MAX_OUTPUT_TOKENS = io.lociant.core.model.DEFAULT_OUTPUT_TOKENS
        private const val MIN_OUTPUT_TOKENS = io.lociant.core.model.MIN_OUTPUT_TOKENS
        private const val HARD_MAX_OUTPUT_TOKENS = io.lociant.core.model.HARD_MAX_OUTPUT_TOKENS
        private const val MAX_JSON_BODY_BYTES = 4L * 1024L * 1024L
        private const val SETTINGS_NAMESPACE = RuntimeDefaults.Settings.SERVER_NAMESPACE
        private const val SETTINGS_KEY = RuntimeDefaults.Settings.SERVER_KEY
        private val JsonContentType = ContentType.Application.Json.withParameter("charset", "utf-8")
        fun newToken(): String {
            val bytes = ByteArray(18)
            SecureRandom().nextBytes(bytes)
            return java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
        }
    }
}
