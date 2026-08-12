package io.lociant.android.server

import android.content.Context
import android.util.Log
import io.lociant.core.config.RuntimeDefaults
import io.lociant.core.tools.ToolExposure
import io.lociant.core.tools.ToolRegistry
import io.lociant.data.storage.LocalStore
import io.lociant.runtime.model.ChatCapability
import io.lociant.runtime.model.MnnRuntime
import io.lociant.runtime.model.ModelManager
import io.lociant.tools.AndroidTools
import io.lociant.tools.LlmTools
import io.lociant.tools.ModelTools
import io.lociant.tools.RuntimeTools
import io.lociant.tools.SensorTools
import io.lociant.tools.VisionTools
import io.lociant.tools.runtime.DeviceInteraction
import org.json.JSONObject
import java.io.File
import java.net.Inet4Address
import java.net.NetworkInterface

/**
 * Device-layer composition root. The HTTP server and its routes moved to the
 * Rust backend (apps/rust-backend); this class only wires the phone
 * capabilities Rust calls through the device IPC: tools, local MNN chat,
 * vision and persisted settings.
 */
class LociantServer(
    private val context: Context,
    private val modelManager: ModelManager,
    private val chatCapability: ChatCapability,
    private val localStore: LocalStore,
    private val startVisionRuntime: (JSONObject) -> Unit,
) {
    private var port = DEFAULT_PORT
    private var modelId = DEFAULT_MODEL_ID
    private var maxOutputTokens = DEFAULT_MAX_OUTPUT_TOKENS
    private var cpuThreads = MnnRuntime.DEFAULT_CPU_THREADS
    private var inferenceBackend = MnnRuntime.DEFAULT_INFERENCE_BACKEND
    @Volatile private var backendFallbackApplied = false
    private var contextProfile = RuntimeDefaults.Sessions.CONTEXT_PROFILE_DEFAULT
    private var historyLimit = RuntimeDefaults.Sessions.MODEL_HISTORY_LIMIT
    private var agentMaxRounds = RuntimeDefaults.Agent.ROUNDS_DEFAULT
    private var autoStart = false
    private var currentSessionId = DEFAULT_SESSION_ID
    private var authToken = ""
    private var toolExposure = ToolExposure.Action
    private var lastError: String? = null

    private val chatController = ChatController(chatCapability)
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
                SensorTools(context),
            )
        )
    }

    init {
        loadSettings()
    }

    fun chatController() = chatController
    fun toolRegistry(): ToolRegistry = toolRegistry
    fun modelManager(): ModelManager = modelManager
    fun callTool(name: String, args: JSONObject): JSONObject = toolRegistry.call(name, args)
    fun callToolResult(name: String, args: JSONObject = JSONObject()): JSONObject {
        val response = toolRegistry.call(name, args)
        return response.optJSONObject("result") ?: response
    }

    fun state(): JSONObject = buildStateJson("api.server.state", includeSensitive = true)
    fun uiState(): JSONObject = buildStateJson(null, includeSensitive = true)
    fun runtimeSummary(): JSONObject = buildStateJson(null)

    // ---- State reporting ----

    private fun buildStateJson(
        type: String?,
        includeSensitive: Boolean = false,
    ): JSONObject {
        val json = JSONObject()
        if (type != null) json.put("type", type)
        // The Rust server is the API owner; the device layer reports itself
        // as running whenever this composition exists.
        json.put("running", true)
            .put("starting", false)
            .put("host", "127.0.0.1")
            .put("port", port)
            .put("url", "http://127.0.0.1:$port")
            .put("lanUrl", "http://${lanAddress()}:$port")
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
            .put("inferenceBackend", inferenceBackend)
            .put("inferenceBackendFallback", backendFallbackApplied)
            .put("contextProfile", contextProfile)
            .put("historyLimit", historyLimit)
            .put("modelMaxOutputTokens", modelManager.maxNewTokens(modelId) ?: JSONObject.NULL)
            .put("contextWindowTokens", chatController.contextWindowTokens(modelId))
            .put("contextStrategy", "token-budget")
            .put("effectiveMaxOutputTokens", chatController.effectiveMaxTokens(modelId, maxOutputTokens))
            .put("sessionPolicy", sessionPolicyJson())
            .put("agentMaxRounds", agentMaxRounds)
            .put("agentPolicy", agentPolicyJson())
            .put("autoStart", autoStart)
            .put("currentSessionId", currentSessionId)
            .put("lastError", chatController.lastError ?: lastError ?: JSONObject.NULL)
            .put("message", message())
            .put("packageName", context.packageName)
            .put("device", DeviceInteraction.snapshot(context))
        return json
    }

    private fun message(): String = when {
        chatController.isLoading -> "Model server is running. Loading the selected model before first inference."
        else -> lastError ?: "Device layer active; the Rust backend serves the API."
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

    private fun agentPolicyJson(): JSONObject = JSONObject()
        .put("maxRounds", agentMaxRounds)
        .put("roundsMin", RuntimeDefaults.Agent.ROUNDS_MIN)
        .put("roundsMax", RuntimeDefaults.Agent.ROUNDS_MAX)
        .put("maxToolCalls", RuntimeDefaults.Agent.MAX_TOOL_CALLS)

    // ---- Settings ----

    private fun loadSettings() {
        val settings = localStore.getObject(SETTINGS_NAMESPACE, SETTINGS_KEY)
        // A leftover marker means a previous run started with a non-CPU inference
        // backend and did not end cleanly (crash / OOM / process kill). Fall back to
        // the safe default so a bad GPU backend cannot make the app unusable.
        val hadRiskyMarker = inferenceBackendMarker().isFile
        applySettings(settings)
        if (hadRiskyMarker && MnnRuntime.isRiskyBackend(inferenceBackend)) {
            inferenceBackend = MnnRuntime.DEFAULT_INFERENCE_BACKEND
            if (chatCapability.configureBackend(inferenceBackend)) chatController.resetLoadedModel()
            backendFallbackApplied = true
            inferenceBackendMarker().delete()
            saveSettings()
            Log.w(TAG, "Risky inference backend marker left by a previous run; fell back to ${MnnRuntime.DEFAULT_INFERENCE_BACKEND}")
        }
    }

    private fun inferenceBackendMarker(): File = File(context.filesDir, "inference_backend_marker")

    private fun saveSettings() {
        localStore.set(SETTINGS_NAMESPACE, SETTINGS_KEY, JSONObject()
            .put("port", port).put("modelId", modelId)
            .put("maxOutputTokens", maxOutputTokens)
            .put("cpuThreads", cpuThreads)
            .put("inferenceBackend", inferenceBackend)
            .put("contextProfile", contextProfile)
            .put("historyLimit", historyLimit)
            .put("agentMaxRounds", agentMaxRounds)
            .put("authToken", authToken)
            .put("toolExposure", toolExposure.id)
            .put("autoStart", autoStart).put("currentSessionId", currentSessionId))
    }

    private fun applySettings(settings: JSONObject) {
        port = settings.optInt("port", DEFAULT_PORT).coerceIn(1024, 65535)
        modelId = ModelManager.normalizeId(settings.optString("modelId", DEFAULT_MODEL_ID)).ifBlank { DEFAULT_MODEL_ID }
        maxOutputTokens = settings.optInt("maxOutputTokens", DEFAULT_MAX_OUTPUT_TOKENS)
            .coerceIn(MIN_OUTPUT_TOKENS, HARD_MAX_OUTPUT_TOKENS)
        val nextCpuThreads = settings.optInt("cpuThreads", MnnRuntime.DEFAULT_CPU_THREADS)
            .coerceIn(MnnRuntime.MIN_CPU_THREADS, maxCpuThreads())
        if (cpuThreads != nextCpuThreads) {
            cpuThreads = nextCpuThreads
            if (chatCapability.configureCpuThreads(cpuThreads)) chatController.resetLoadedModel()
        }
        val nextBackend = MnnRuntime.normalizeBackend(
            settings.optString("inferenceBackend", MnnRuntime.DEFAULT_INFERENCE_BACKEND)
        )
        if (inferenceBackend != nextBackend) {
            inferenceBackend = nextBackend
            if (chatCapability.configureBackend(inferenceBackend)) chatController.resetLoadedModel()
        }
        val marker = inferenceBackendMarker()
        if (MnnRuntime.isRiskyBackend(inferenceBackend)) {
            if (!marker.isFile) marker.writeText(inferenceBackend)
        } else {
            marker.delete()
        }
        contextProfile = normalizeContextProfile(
            settings.optString("contextProfile", RuntimeDefaults.Sessions.CONTEXT_PROFILE_DEFAULT)
        )
        historyLimit = settings.optInt("historyLimit", historyLimitForContextProfile(contextProfile))
            .coerceIn(1, RuntimeDefaults.Sessions.MODEL_HISTORY_MAX_LIMIT)
        chatController.configureHistoryLimit(historyLimit)
        agentMaxRounds = settings.optInt("agentMaxRounds", RuntimeDefaults.Agent.ROUNDS_DEFAULT)
            .coerceIn(RuntimeDefaults.Agent.ROUNDS_MIN, RuntimeDefaults.Agent.ROUNDS_MAX)
        autoStart = settings.optBoolean("autoStart", false)
        currentSessionId = settings.optString("currentSessionId", DEFAULT_SESSION_ID)
        authToken = settings.optString("authToken").trim()
        toolExposure = ToolExposure.from(settings.optString("toolExposure", ToolExposure.Action.id))
    }

    private fun maxCpuThreads() = Runtime.getRuntime().availableProcessors()
        .coerceAtLeast(MnnRuntime.MIN_CPU_THREADS)
        .coerceAtMost(MnnRuntime.MAX_CPU_THREADS)

    private fun normalizeContextProfile(value: String): String = when (value.lowercase()) {
        RuntimeDefaults.Sessions.CONTEXT_PROFILE_LIGHT -> RuntimeDefaults.Sessions.CONTEXT_PROFILE_LIGHT
        RuntimeDefaults.Sessions.CONTEXT_PROFILE_DEEP -> RuntimeDefaults.Sessions.CONTEXT_PROFILE_DEEP
        else -> RuntimeDefaults.Sessions.CONTEXT_PROFILE_BALANCED
    }

    private fun historyLimitForContextProfile(value: String): Int = when (normalizeContextProfile(value)) {
        RuntimeDefaults.Sessions.CONTEXT_PROFILE_LIGHT -> RuntimeDefaults.Sessions.MODEL_HISTORY_LIGHT_LIMIT
        RuntimeDefaults.Sessions.CONTEXT_PROFILE_DEEP -> RuntimeDefaults.Sessions.MODEL_HISTORY_DEEP_LIMIT
        else -> RuntimeDefaults.Sessions.MODEL_HISTORY_LIMIT
    }

    // ---- Network ----

    private fun lanAddress(): String = runCatching {
        NetworkInterface.getNetworkInterfaces().asSequence()
            .flatMap { it.inetAddresses.asSequence() }
            .filterIsInstance<Inet4Address>()
            .firstOrNull { !it.isLoopbackAddress && it.hostAddress?.startsWith("169.254.") != true }
            ?.hostAddress
    }.getOrNull() ?: "127.0.0.1"

    companion object {
        private const val TAG = "LociantApi"
        private const val DEFAULT_PORT = RuntimeDefaults.PORT
        private const val DEFAULT_MODEL_ID = RuntimeDefaults.MODEL_ID
        private const val DEFAULT_SESSION_ID = RuntimeDefaults.Sessions.DEFAULT_CHAT_ID
        private const val DEFAULT_MAX_OUTPUT_TOKENS = io.lociant.core.model.DEFAULT_OUTPUT_TOKENS
        private const val MIN_OUTPUT_TOKENS = io.lociant.core.model.MIN_OUTPUT_TOKENS
        private const val HARD_MAX_OUTPUT_TOKENS = io.lociant.core.model.HARD_MAX_OUTPUT_TOKENS
        private const val SETTINGS_NAMESPACE = RuntimeDefaults.Settings.SERVER_NAMESPACE
        private const val SETTINGS_KEY = RuntimeDefaults.Settings.SERVER_KEY
    }
}
