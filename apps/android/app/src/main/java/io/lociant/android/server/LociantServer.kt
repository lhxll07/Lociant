package io.lociant.android.server

import android.content.Context
import io.lociant.data.storage.LocalStore
import io.lociant.core.config.RuntimeDefaults
import io.lociant.core.tools.ToolExposure
import io.lociant.core.tools.ToolRegistry
import io.lociant.runtime.model.ModelManager
import io.lociant.tools.AndroidTools
import io.lociant.tools.ModelTools
import io.lociant.tools.RuntimeTools
import io.lociant.tools.SensorTools
import io.lociant.tools.VisionTools
import io.lociant.tools.runtime.DeviceInteraction
import org.json.JSONObject
import java.net.Inet4Address
import java.net.NetworkInterface

/**
 * Device-layer composition root. The HTTP server and its routes moved to the
 * Rust backend (apps/rust-backend); this class only wires the phone
 * capabilities Rust calls through the device IPC: phone tools and vision.
 * Server-side settings are owned by Rust and pushed via `settings.sync`.
 */
class LociantServer(
    private val context: Context,
    private val modelManager: ModelManager,
    private val startVisionRuntime: (JSONObject) -> Unit,
) {
    private val localStore = LocalStore(context)
    private var port = DEFAULT_PORT
    private var autoStart = false
    private var authToken = ""
    private var toolExposure = ToolExposure.Action
    private var lastError: String? = null

    private val toolRegistry: ToolRegistry by lazy {
        ToolRegistry(
            listOf(
                RuntimeTools(runtimeState = { runtimeSummary() }),
                AndroidTools(context),
                ModelTools(modelManager = modelManager),
                VisionTools(context, startVisionRuntime),
                SensorTools(context),
            )
        )
    }

    init {
        // The Rust backend owns server-side settings. Apply defaults here and
        // let `settings.sync` push any persisted Rust snapshot over the IPC.
        applySettings(JSONObject())
    }

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
            .put("activeRequest", JSONObject.NULL)
            .put("autoStart", autoStart)
            .put("lastError", lastError ?: JSONObject.NULL)
            .put("message", message())
            .put("packageName", context.packageName)
            .put("device", DeviceInteraction.snapshot(context))
        return json
    }

    private fun message(): String = lastError ?: "Device layer active; the Rust backend serves the API."

    // ---- Settings ----

    private fun applySettings(settings: JSONObject) {
        port = settings.optInt("port", DEFAULT_PORT).coerceIn(1024, 65535)
        autoStart = settings.optBoolean("autoStart", false)
        authToken = settings.optString("authToken").trim()
        toolExposure = ToolExposure.from(settings.optString("toolExposure", ToolExposure.Action.id))
    }

    /**
     * Applies a full Rust-side settings snapshot pushed through the device IPC.
     * The Rust backend is the settings owner; this keeps the device summary
     * aligned with what the UI edited over HTTP.
     */
    fun applyRuntimeSettings(settings: JSONObject) {
        applySettings(settings)
        // BootReceiver only needs the auto-start flag; the rest of the server
        // settings stay in Rust SQLite.
        localStore.set(
            SERVER_SETTINGS_NAMESPACE,
            SERVER_SETTINGS_KEY,
            JSONObject().put("autoStart", autoStart),
        )
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
        private const val DEFAULT_PORT = RuntimeDefaults.PORT
        private const val SERVER_SETTINGS_NAMESPACE = RuntimeDefaults.Settings.SERVER_NAMESPACE
        private const val SERVER_SETTINGS_KEY = RuntimeDefaults.Settings.SERVER_KEY
    }
}
