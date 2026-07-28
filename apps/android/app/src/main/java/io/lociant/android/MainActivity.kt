package io.lociant.android

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Looper
import android.os.PowerManager
import android.provider.Settings
import android.text.TextUtils
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import android.webkit.PermissionRequest
import android.webkit.WebSettings
import android.webkit.WebChromeClient
import android.webkit.ValueCallback
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.webkit.WebViewAssetLoader
import io.lociant.core.config.RuntimeDefaults
import io.lociant.runtime.model.ModelInstaller
import io.lociant.runtime.model.ModelManager
import io.lociant.tools.runtime.DeviceInteraction
import io.lociant.android.runtime.LociantRuntime
import io.lociant.tools.runtime.VisionRuntime
import io.lociant.tools.LociantAccessibilityService
import io.lociant.android.server.LociantServer
import io.lociant.android.runtime.LociantRuntimeService
import io.lociant.data.session.SessionStore
import io.lociant.data.storage.LocalStore
import org.json.JSONObject
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity(), LociantBridge.Host {
    private lateinit var root: FrameLayout
    private lateinit var webView: WebView
    private lateinit var modelManager: ModelManager
    private lateinit var modelInstaller: ModelInstaller
    private lateinit var lociantServer: LociantServer
    private lateinit var localStore: LocalStore
    private lateinit var sessionStore: SessionStore
    private val modelInstallExecutor = Executors.newSingleThreadExecutor()

    private var pendingWebPermissionRequest: PermissionRequest? = null
    private var startVisionAfterCameraPermission = false
    private var pendingVisionPayload = JSONObject()
    private var startRuntimeAfterNotificationPermission = false
    private var pendingRuntimePayload = JSONObject()
    private var pendingVisionPermissionRefresh = false
    private var pendingPermissionRefresh = false
    private var windowSettings = JSONObject()
    private var pendingFileChooser: ValueCallback<Array<Uri>>? = null
    private var lastKeyboardInset = -1

    private val requestCameraPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        val webPermissionRequest = pendingWebPermissionRequest
        if (webPermissionRequest != null) {
            pendingWebPermissionRequest = null
            if (granted) webPermissionRequest.grant(webPermissionRequest.resources)
            else webPermissionRequest.deny()
        }
        if (startVisionAfterCameraPermission) {
            startVisionAfterCameraPermission = false
            if (granted) {
                lociantServer.callToolResult("vision_start", pendingVisionPayload)
            }
            pendingVisionPayload = JSONObject()
            pendingVisionPermissionRefresh = true
        }
        refreshRuntimeStateIfNeeded()
    }


    private val installModelPackage = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) notifyModelInstallResult(false, "cancelled", null) else handleModelPackage(uri)
    }

    private val pickWebFile = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        pendingFileChooser?.onReceiveValue(uri?.let { arrayOf(it) } ?: emptyArray())
        pendingFileChooser = null
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        DeviceInteraction.setActivityForeground(true)
        @Suppress("DEPRECATION")
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        enterImmersiveMode()

        localStore = LociantRuntime.localStore(this)
        sessionStore = LociantRuntime.sessionStore(this)
        modelManager = LociantRuntime.modelManager(this)
        modelInstaller = ModelInstaller(this, modelManager)
        lociantServer = LociantRuntime.server(this)
        windowSettings = loadWindowSettings()

        val assetLoader = WebViewAssetLoader.Builder()
            .setHttpAllowed(true)
            .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(this))
            .build()

        webView = WebView(this).apply {
            webViewClient = object : WebViewClient() {
                override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? {
                    return assetLoader.shouldInterceptRequest(request.url)
                }
            }
            webChromeClient = object : WebChromeClient() {
                override fun onPermissionRequest(request: PermissionRequest) {
                    val needsCamera = request.resources.contains(PermissionRequest.RESOURCE_VIDEO_CAPTURE)
                    if (!needsCamera) {
                        request.deny()
                        return
                    }

                    runOnUiThread {
                        if (hasPermission(Manifest.permission.CAMERA)) {
                            request.grant(request.resources)
                        } else {
                            pendingWebPermissionRequest = request
                            requestCameraPermission.launch(Manifest.permission.CAMERA)
                        }
                    }
                }

                override fun onPermissionRequestCanceled(request: PermissionRequest) {
                    if (pendingWebPermissionRequest == request) pendingWebPermissionRequest = null
                }

                override fun onShowFileChooser(
                    webView: WebView,
                    filePathCallback: ValueCallback<Array<Uri>>,
                    fileChooserParams: FileChooserParams,
                ): Boolean {
                    pendingFileChooser?.onReceiveValue(emptyArray())
                    pendingFileChooser = filePathCallback
                    val types = fileChooserParams.acceptTypes
                        .filter { it.isNotBlank() }
                        .ifEmpty { listOf("image/*") }
                        .toTypedArray()
                    runOnUiThread {
                        runCatching { pickWebFile.launch(types) }
                            .onFailure {
                                pendingFileChooser?.onReceiveValue(emptyArray())
                                pendingFileChooser = null
                            }
                    }
                    return true
                }
            }
            setBackgroundColor(Color.TRANSPARENT)

            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.mediaPlaybackRequiresUserGesture = false
            settings.defaultTextEncodingName = "utf-8"
            settings.allowFileAccess = true
            settings.allowContentAccess = true
            settings.mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE

            addJavascriptInterface(
                LociantBridge(host = this@MainActivity),
                "LociantBridge",
            )
            loadUrl("${"https://appassets.androidplatform.net"}/assets/web/index.html")
        }

        root = FrameLayout(this).apply {
            setBackgroundColor(Color.BLACK)
            addView(webView, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        }
        installKeyboardInsetBridge()

        setContentView(root)
    }

    override fun onResume() {
        super.onResume()
        DeviceInteraction.setActivityForeground(true)
        enterImmersiveMode()
        if (::webView.isInitialized) refreshRuntimeStateIfNeeded()
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (shouldShowRuntimeWindow()) runCatching {
            LociantRuntimeService.showFloatingWindow(this)
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            enterImmersiveMode()
            ViewCompat.requestApplyInsets(root)
        }
    }

    private fun installKeyboardInsetBridge() {
        ViewCompat.setOnApplyWindowInsetsListener(root) { _, insets ->
            val ime = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom
            val navigation = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom
            val keyboardInset = (ime - navigation).coerceAtLeast(0)
            if (keyboardInset != lastKeyboardInset) {
                lastKeyboardInset = keyboardInset
                dispatchKeyboardInset(keyboardInset)
            }
            insets
        }
        root.post { ViewCompat.requestApplyInsets(root) }
    }

    private fun dispatchKeyboardInset(insetPx: Int) {
        if (!::webView.isInitialized) return
        val density = resources.displayMetrics.density.takeIf { it > 0f } ?: 1f
        val insetCssPx = (insetPx / density).toInt()
        runOnUiThread {
            webView.evaluateJavascript(
                "window.__lociantKeyboardInset && window.__lociantKeyboardInset($insetCssPx);",
                null,
            )
        }
    }

    private val requestNotificationPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted && startRuntimeAfterNotificationPermission) {
            startRuntimeAfterNotificationPermission = false
            LociantRuntimeService.startRuntime(this, pendingRuntimePayload)
        } else {
            startRuntimeAfterNotificationPermission = false
        }
        pendingRuntimePayload = JSONObject()
        pendingPermissionRefresh = true
        refreshRuntimeStateIfNeeded()
    }

    override fun onDestroy() {
        DeviceInteraction.setActivityForeground(false)
        modelInstallExecutor.shutdown()
        webView.destroy()
        super.onDestroy()
    }

    override fun openModelPackagePicker() {
        runOnUiThread { installModelPackage.launch(PACKAGE_MIME_TYPES) }
    }

    override fun requestCameraPermission() {
        runOnUiThread { requestCameraPermission.launch(Manifest.permission.CAMERA) }
    }

    override fun requestNotificationPermission() {
        runOnUiThread {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                refreshRuntimeStateIfNeeded()
            }
        }
    }

    override fun requestOverlayPermission() {
        runOnUiThread { launchOverlayPermissionSettings() }
    }

    override fun requestBatteryOptimizationExemption() {
        runOnUiThread { launchBatteryOptimizationExemption() }
    }

    override fun requestAccessibilityPermission() {
        runOnUiThread { launchAccessibilitySettings() }
    }

    override fun openAppSettings() {
        runOnUiThread {
            openAppSettingsScreen()
        }
    }

    override fun openPermissionSettings(kind: String) {
        runOnUiThread {
            when (kind) {
                "overlay" -> launchOverlayPermissionSettings()
                "battery" -> launchBatteryOptimizationSettings()
                "accessibility" -> launchAccessibilitySettings()
                else -> openAppSettingsScreen()
            }
        }
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
                .onSuccess { model -> notifyModelInstallResult(true, "installed", model.toJson()) }
                .onFailure { error -> notifyModelInstallResult(false, error.message ?: "install failed", null) }
        }
    }

    private fun enterImmersiveMode() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT

        WindowInsetsControllerCompat(window, window.decorView).apply {
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            hide(WindowInsetsCompat.Type.systemBars())
        }
    }

    override fun runtimeState(): String = runtimeSummaryWithWindow().toString()

    override fun startRuntime(payloadJson: String?): String = runCatching {
        startRuntimeService(parseObject(payloadJson))
        runtimeSummaryWithWindow().put("starting", true)
    }.getOrElse { error ->
        runtimeSummaryWithWindow().put("lastError", error.message ?: "Runtime service start failed")
    }.toString()

    override fun stopRuntime(): String {
        LociantRuntimeService.stopRuntime(this)
        return runtimeSummaryWithWindow(LociantRuntimeService.hideFloatingWindow(this))
            .put("running", false)
            .put("starting", false)
            .toString()
    }

    override fun updateRuntimeSettings(payloadJson: String?): String =
        lociantServer.updateRuntimeSettings(parseObject(payloadJson)).withRuntimeState().toString()

    override fun releaseRuntimeModel(): String = lociantServer.releaseModel().withRuntimeState().toString()

    override fun createSession(): String {
        lociantServer.createSession()
        return runtimeSummaryWithWindow().toString()
    }

    override fun selectSession(sessionId: String): String = lociantServer.selectSession(sessionId).withRuntimeState().toString()

    override fun deleteSession(sessionId: String): String = lociantServer.deleteSession(sessionId).withRuntimeState().toString()

    override fun sessionDetails(sessionId: String): String = runtimeSummaryWithWindow()
        .put("session", lociantServer.sessionDetails(sessionId))
        .toString()

    override fun startVision(payloadJson: String?): String = startVisionFromShell(parseObject(payloadJson)).toString()

    override fun stopVision(): String = runtimeSummaryWithWindow()
        .put("vision", lociantServer.callToolResult("vision_stop"))
        .toString()

    override fun showRuntimeWindow(): String = runtimeSummaryWithWindow(runUiCommand { showRuntimeWindowState() }).toString()

    override fun hideRuntimeWindow(): String = runtimeSummaryWithWindow(
        runUiCommand { LociantRuntimeService.hideFloatingWindow(this) },
    ).toString()

    override fun updateRuntimeWindow(payloadJson: String?): String {
        updateWindowSettings(parseObject(payloadJson))
        val windowState = if (shouldShowRuntimeWindow()) {
            runUiCommand { showRuntimeWindowState() }
        } else {
            LociantRuntimeService.floatingWindowState(this)
        }
        return runtimeSummaryWithWindow(windowState).toString()
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
            pendingVisionPermissionRefresh = true
            requestCameraPermission.launch(Manifest.permission.CAMERA)
            return runtimeSummaryWithWindow()
                .put("vision", VisionRuntime.status().put("message", "Camera permission requested."))
        }
        val vision = lociantServer.callToolResult("vision_start", payload)
        return runtimeSummaryWithWindow().put("vision", vision)
    }

    private fun hasPermission(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
    }

    private fun parseObject(raw: String?): JSONObject {
        if (raw.isNullOrBlank()) return JSONObject()
        return runCatching { JSONObject(raw) }
            .getOrElse { throw IllegalArgumentException("Bridge payload must be a JSON object", it) }
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
            .put("batteryOptimizationIgnored", isIgnoringBatteryOptimizations())
            .put("accessibilityPermissionGranted", isAccessibilityServiceEnabled())
    }

    private fun runtimeSummaryWithWindow(window: JSONObject = LociantRuntimeService.floatingWindowState(this)): JSONObject =
        lociantServer.state().withRuntimeState(window)

    private fun JSONObject.toRuntimeUiState(): JSONObject {
        return runtimeSummaryWithWindow()
    }

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
        if (!pendingVisionPermissionRefresh && !startRuntimeAfterNotificationPermission && !pendingPermissionRefresh) {
            webView.post {
                runCatching {
                    emitJs("onRuntimeMessage", lociantServer.uiState().withRuntimeState())
                }
            }
        } else {
            pendingVisionPermissionRefresh = false
            pendingPermissionRefresh = false
            webView.post {
                runCatching {
                    emitJs("onRuntimeMessage", lociantServer.uiState().withRuntimeState())
                }
            }
        }
    }

    private fun notifyModelInstallResult(
        ok: Boolean,
        message: String,
        model: JSONObject?,
        state: String = if (ok) "done" else "error",
        progress: Double? = if (state == "done") 1.0 else null,
    ) {
        emitJs("onModelInstallResult", JSONObject()
            .put("ok", ok)
            .put("state", state)
            .put("message", message)
            .put("progress", progress ?: JSONObject.NULL)
            .put("model", model ?: JSONObject.NULL))
    }

    private fun emitJs(event: String, payload: JSONObject) {
        val script = "window.LociantEvents && window.LociantEvents.$event(JSON.parse(${JSONObject.quote(payload.toString())}));"
        webView.post { webView.evaluateJavascript(script, null) }
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
