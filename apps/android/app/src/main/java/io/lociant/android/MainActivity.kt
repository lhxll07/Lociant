package io.lociant.android

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Looper
import android.os.PowerManager
import android.provider.Settings
import android.text.TextUtils
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import io.flutter.embedding.android.FlutterFragmentActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugins.GeneratedPluginRegistrant
import io.lociant.android.runtime.LociantRuntime
import io.lociant.android.runtime.LociantRuntimeService
import io.lociant.core.config.RuntimeDefaults
import io.lociant.runtime.model.ModelInstaller
import io.lociant.runtime.model.ModelManager
import io.lociant.tools.LociantAccessibilityService
import io.lociant.tools.runtime.DeviceInteraction
import io.lociant.tools.runtime.VisionRuntime
import org.json.JSONObject
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class MainActivity : FlutterFragmentActivity() {
    private var platformChannel: LociantPlatformChannel? = null
    private lateinit var modelManager: ModelManager
    private lateinit var modelInstaller: ModelInstaller
    private lateinit var lociantServer: io.lociant.android.server.LociantServer
    private lateinit var localStore: io.lociant.data.storage.LocalStore
    private val modelInstallExecutor = Executors.newSingleThreadExecutor()

    private var startVisionAfterCameraPermission = false
    private var pendingVisionPayload = JSONObject()
    private var startRuntimeAfterNotificationPermission = false
    private var pendingRuntimePayload = JSONObject()
    private var windowSettings = JSONObject()

    private val requestCameraPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (startVisionAfterCameraPermission) {
            startVisionAfterCameraPermission = false
            if (granted) {
                lociantServer.callToolResult("vision_start", pendingVisionPayload)
            }
            pendingVisionPayload = JSONObject()
        }
        refreshRuntimeStateIfNeeded()
    }

    private val installModelPackage = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) notifyModelInstallResult(false, "cancelled", null) else handleModelPackage(uri)
    }

    private val requestNotificationPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            if (startRuntimeAfterNotificationPermission) {
                LociantRuntimeService.startRuntime(this, pendingRuntimePayload)
            }
        }
        startRuntimeAfterNotificationPermission = false
        pendingRuntimePayload = JSONObject()
        refreshRuntimeStateIfNeeded()
    }

    private val requestSensorPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) {
        refreshRuntimeStateIfNeeded()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        DeviceInteraction.setActivityForeground(true)
        localStore = LociantRuntime.localStore(this)
        modelManager = LociantRuntime.modelManager(this)
        modelInstaller = ModelInstaller(this)
        lociantServer = LociantRuntime.server(this)
        windowSettings = loadWindowSettings()
        ensureRuntimeStarted()
    }

    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)
        GeneratedPluginRegistrant.registerWith(flutterEngine)
        platformChannel = LociantPlatformChannel(this, flutterEngine)
    }

    override fun onResume() {
        super.onResume()
        DeviceInteraction.setActivityForeground(true)
        refreshRuntimeStateIfNeeded()
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (shouldShowRuntimeWindow()) runCatching {
            LociantRuntimeService.showFloatingWindow(this)
        }
    }

    override fun onDestroy() {
        DeviceInteraction.setActivityForeground(false)
        modelInstallExecutor.shutdown()
        super.onDestroy()
    }

    // ---- Platform channel host operations (mirror the old LociantBridge) ----

    fun installModelPackage(): String {
        runOnUiThread { installModelPackage.launch(PACKAGE_MIME_TYPES) }
        return ok("picker_opened")
    }

    fun requestCameraPermission(): String {
        runOnUiThread { requestCameraPermission.launch(Manifest.permission.CAMERA) }
        return ok("permission_requested")
    }

    fun requestNotificationPermission(): String {
        runOnUiThread {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                refreshRuntimeStateIfNeeded()
            }
        }
        return ok("permission_requested")
    }

    fun requestSensorPermission(): String {
        runOnUiThread {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                requestSensorPermission.launch(Manifest.permission.ACTIVITY_RECOGNITION)
            } else {
                refreshRuntimeStateIfNeeded()
            }
        }
        return ok("permission_requested")
    }

    fun requestOverlayPermission(): String {
        runOnUiThread { launchOverlayPermissionSettings() }
        return ok("permission_requested")
    }

    fun requestBatteryOptimizationExemption(): String {
        runOnUiThread { launchBatteryOptimizationExemption() }
        return ok("permission_requested")
    }

    fun requestAccessibilityPermission(): String {
        runOnUiThread { launchAccessibilitySettings() }
        return ok("permission_requested")
    }

    fun openAppSettings(): String {
        runOnUiThread { openAppSettingsScreen() }
        return ok("settings_opened")
    }

    fun openExternalUrl(url: String): String {
        val uri = runCatching { Uri.parse(url) }.getOrNull() ?: return ok("external_url_rejected")
        if (!uri.scheme.equals("https", ignoreCase = true)) return ok("external_url_rejected")
        runOnUiThread {
            runCatching { startActivity(Intent(Intent.ACTION_VIEW, uri)) }
        }
        return ok("external_url_opened")
    }

    fun openPermissionSettings(kind: String): String {
        runOnUiThread {
            when (kind) {
                "overlay" -> launchOverlayPermissionSettings()
                "battery" -> launchBatteryOptimizationSettings()
                "accessibility" -> launchAccessibilitySettings()
                else -> openAppSettingsScreen()
            }
        }
        return ok("settings_opened")
    }

    fun startRuntime(payloadJson: String?): String = runCatching {
        startRuntimeService(parseObject(payloadJson))
        runtimeSummaryWithWindow().put("starting", true)
    }.getOrElse { error ->
        runtimeSummaryWithWindow().put("lastError", error.message ?: "Runtime service start failed")
    }.toString()

    fun stopRuntime(): String {
        LociantRuntimeService.stopRuntime(this)
        return runtimeSummaryWithWindow(LociantRuntimeService.hideFloatingWindow(this))
            .put("running", false)
            .put("starting", false)
            .toString()
    }

    /**
     * Android-only runtime state for the merged UI snapshot. The Flutter UI
     * reads core server state (sessions/settings/models) from the Rust server
     * over HTTP and overlays these device fields (permissions, floating
     * window, vision) from here.
     */
    fun deviceState(): String = JSONObject().withRuntimeState().toString()

    fun startVision(payloadJson: String?): String = startVisionFromShell(parseObject(payloadJson)).toString()

    fun stopVision(): String = runtimeSummaryWithWindow()
        .put("vision", lociantServer.callToolResult("vision_stop"))
        .toString()

    fun showRuntimeWindow(): String = runtimeSummaryWithWindow(runUiCommand { showRuntimeWindowState() }).toString()

    fun hideRuntimeWindow(): String = runtimeSummaryWithWindow(
        runUiCommand { LociantRuntimeService.hideFloatingWindow(this) },
    ).toString()

    fun updateRuntimeWindow(payloadJson: String?): String {
        updateWindowSettings(parseObject(payloadJson))
        val windowState = if (shouldShowRuntimeWindow()) {
            runUiCommand { showRuntimeWindowState() }
        } else {
            LociantRuntimeService.floatingWindowState(this)
        }
        return runtimeSummaryWithWindow(windowState).toString()
    }

    // ---- Private helpers ----

    private fun ensureRuntimeStarted() {
        if (LociantRuntimeService.isActive()) return
        startRuntimeService(JSONObject().put("mode", "interactive"))
    }

    private fun startRuntimeService(payload: JSONObject = JSONObject()) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU &&
            !hasPermission(Manifest.permission.POST_NOTIFICATIONS)
        ) {
            startRuntimeAfterNotificationPermission = true
            pendingRuntimePayload = payload
            requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
            return
        }
        LociantRuntimeService.startRuntime(this, payload)
    }

    private fun startVisionFromShell(payload: JSONObject): JSONObject {
        val deviceState = DeviceInteraction.snapshot(this)
        if (!deviceState.optBoolean("visionInteractive", false)) {
            return runtimeSummaryWithWindow()
                .put("vision", VisionRuntime.status()
                    .put("state", "locked")
                    .put("running", false)
                    .put("message", "Vision requires the screen to be on and the device unlocked."))
        }
        if (!hasPermission(Manifest.permission.CAMERA)) {
            startVisionAfterCameraPermission = true
            pendingVisionPayload = JSONObject(payload.toString())
            requestCameraPermission.launch(Manifest.permission.CAMERA)
            return runtimeSummaryWithWindow()
                .put("vision", VisionRuntime.status().put("message", "Camera permission requested."))
        }
        val vision = lociantServer.callToolResult("vision_start", payload)
        return runtimeSummaryWithWindow().put("vision", vision)
    }

    private fun handleModelPackage(uri: Uri) {
        runCatching { contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
        notifyModelInstallResult(true, "installing", null, "installing", null)
        modelInstallExecutor.execute {
            runCatching {
                modelInstaller.installFromUri(uri) { progress, message ->
                    notifyModelInstallResult(true, message, null, "installing", progress)
                }
            }
                .onSuccess { model ->
                    notifyModelInstallResult(true, "installed", model)
                    LociantRuntimeService.restartRuntime(this@MainActivity)
                }
                .onFailure { error -> notifyModelInstallResult(false, error.message ?: "install failed", null) }
        }
    }

    private fun hasPermission(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
    }

    private fun parseObject(raw: String?): JSONObject {
        if (raw.isNullOrBlank()) return JSONObject()
        return runCatching { JSONObject(raw) }
            .getOrElse { throw IllegalArgumentException("Platform payload must be a JSON object", it) }
    }

    private fun JSONObject.withRuntimeState(window: JSONObject = LociantRuntimeService.floatingWindowState(this@MainActivity)): JSONObject {
        return put("runtimeService", true)
            .put("mode", "foreground-service")
            .put("runtimeModes", org.json.JSONArray(listOf("interactive", "service", "headless")))
            .put("headlessCapable", true)
            .put("vision", VisionRuntime.status())
            .put("device", DeviceInteraction.snapshot(this@MainActivity))
            .put("cameraPermissionGranted", hasPermission(Manifest.permission.CAMERA))
            .put("windowSupported", isFloatingWindowSupported())
            .put("windowAutoShow", windowSettings.optBoolean("autoShow", false))
            .put("windowAllowed", canDrawOverlays())
            .put("windowVisible", window.optBoolean("visible"))
            .put("windowState", window.optString("state", "hidden"))
            .put("window", window)
            .put("notificationPermissionGranted", notificationPermissionGranted())
            .put("sensorPermissionGranted", sensorPermissionGranted())
            .put("batteryOptimizationIgnored", isIgnoringBatteryOptimizations())
            .put("accessibilityPermissionGranted", isAccessibilityServiceEnabled())
    }

    private fun runtimeSummaryWithWindow(window: JSONObject = LociantRuntimeService.floatingWindowState(this)): JSONObject =
        lociantServer.state().withRuntimeState(window)

    private fun updateWindowSettings(payload: JSONObject) {
        windowSettings = JSONObject(windowSettings.toString())
            .put("autoShow", payload.optBoolean("autoShow", windowSettings.optBoolean("autoShow", false)))
        localStore.set(RUNTIME_SETTINGS_NAMESPACE, WINDOW_SETTINGS_KEY, windowSettings)
    }

    private fun shouldShowRuntimeWindow(): Boolean {
        if (!windowSettings.optBoolean("autoShow", false)) return false
        if (!canDrawOverlays()) return false
        val state = lociantServer.runtimeSummary()
        return state.optBoolean("running", false) || state.optBoolean("starting", false)
    }

    private fun loadWindowSettings(): JSONObject {
        val current = localStore.getObject(RUNTIME_SETTINGS_NAMESPACE, WINDOW_SETTINGS_KEY)
        return if (current.length() > 0) current else JSONObject().put("autoShow", false)
    }

    private fun isFloatingWindowSupported(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M

    private fun canDrawOverlays(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(this)

    private fun notificationPermissionGranted(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            hasPermission(Manifest.permission.POST_NOTIFICATIONS)

    private fun sensorPermissionGranted(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.Q ||
            hasPermission(Manifest.permission.ACTIVITY_RECOGNITION)

    private fun showRuntimeWindowState(): JSONObject {
        if (!canDrawOverlays()) {
            return LociantRuntimeService.floatingWindowState(this)
                .put("state", "error")
                .put("error", "Floating window permission is not granted")
        }
        val state = lociantServer.runtimeSummary()
        if (!state.optBoolean("running", false) && !state.optBoolean("starting", false)) {
            startRuntimeService(JSONObject().put("floatingWindow", true))
            return LociantRuntimeService.floatingWindowState(this)
        }
        return LociantRuntimeService.showFloatingWindow(this)
    }

    private fun runUiCommand(block: () -> JSONObject): JSONObject {
        if (Looper.myLooper() == Looper.getMainLooper()) return block()
        var result = JSONObject()
        val latch = CountDownLatch(1)
        runOnUiThread {
            result = runCatching { block() }
                .getOrElse { error -> JSONObject().put("state", "error").put("error", error.message ?: "UI command failed") }
            latch.countDown()
        }
        if (!latch.await(2, TimeUnit.SECONDS)) {
            return JSONObject().put("state", "error").put("error", "UI command timed out")
        }
        return result
    }

    private fun launchOverlayPermissionSettings() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return
        val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
        runCatching { startActivity(intent) }
            .onFailure { startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)) }
            .also { refreshRuntimeStateIfNeeded() }
    }

    private fun openAppSettingsScreen() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName"))
        runCatching { startActivity(intent) }
            .also { refreshRuntimeStateIfNeeded() }
    }

    private fun isIgnoringBatteryOptimizations(): Boolean {
        val powerManager = getSystemService(PowerManager::class.java)
        return powerManager?.isIgnoringBatteryOptimizations(packageName) == true
    }

    private fun launchBatteryOptimizationExemption() {
        if (isIgnoringBatteryOptimizations()) return
        val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
            data = Uri.parse("package:$packageName")
        }
        runCatching { startActivity(intent) }
            .onFailure {
                runCatching {
                    startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
                }
            }
            .also { refreshRuntimeStateIfNeeded() }
    }

    private fun launchAccessibilitySettings() {
        runCatching { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }
            .onFailure { openAppSettingsScreen() }
            .also { refreshRuntimeStateIfNeeded() }
    }

    private fun isAccessibilityServiceEnabled(): Boolean {
        val serviceId = "$packageName/${LociantAccessibilityService::class.java.name}"
        val enabled = Settings.Secure.getString(contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
            ?: return false
        return enabled.split(':').any { TextUtils.equals(it, serviceId) }
    }

    private fun launchBatteryOptimizationSettings() {
        runCatching { startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)) }
            .onFailure { openAppSettingsScreen() }
            .also { refreshRuntimeStateIfNeeded() }
    }

    private fun refreshRuntimeStateIfNeeded() {
        runCatching {
            platformChannel?.emitRuntimeMessage(lociantServer.uiState().withRuntimeState())
        }
    }

    private fun notifyModelInstallResult(
        ok: Boolean,
        message: String,
        model: JSONObject?,
        state: String = if (ok) "done" else "error",
        progress: Double? = if (state == "done") 1.0 else null,
    ) {
        platformChannel?.emitModelInstallResult(JSONObject()
            .put("ok", ok)
            .put("state", state)
            .put("message", message)
            .put("progress", progress ?: JSONObject.NULL)
            .put("model", model ?: JSONObject.NULL))
    }

    private fun ok(state: String): String {
        return JSONObject().put("ok", true).put("state", state).toString()
    }

    companion object {
        private val PACKAGE_MIME_TYPES = arrayOf(
            "application/zip",
            "application/octet-stream",
            "application/x-zip-compressed",
            "*/*",
        )
        private const val RUNTIME_SETTINGS_NAMESPACE = RuntimeDefaults.Settings.WINDOW_NAMESPACE
        private const val WINDOW_SETTINGS_KEY = RuntimeDefaults.Settings.WINDOW_KEY
    }
}
