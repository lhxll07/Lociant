package com.mnnode.app

import android.Manifest
import android.annotation.SuppressLint
import android.app.PictureInPictureParams
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.util.Rational
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.webkit.PermissionRequest
import android.webkit.WebSettings
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.webkit.WebViewAssetLoader
import com.mnnode.app.model.ModelInstaller
import com.mnnode.app.model.ModelManager
import com.mnnode.app.scene.SceneManager
import com.mnnode.app.scene.ScenePackInstaller
import com.mnnode.app.runtime.TriggerEngine
import com.mnnode.app.runtime.SensorSample
import com.mnnode.app.server.ApiServerController
import com.mnnode.app.runtime.MNNodeRuntime
import com.mnnode.app.runtime.MNNodeRuntimeService
import com.mnnode.app.session.SessionStore
import com.mnnode.app.storage.LocalStore
import com.mnnode.app.vision.VisionAnalysisController
import org.json.JSONObject
import java.util.concurrent.Executors

class MainActivity : ComponentActivity(), MNNodeShellBridge.Host {
    private lateinit var root: FrameLayout
    private lateinit var webView: WebView
    private lateinit var visionController: VisionAnalysisController
    private lateinit var modelManager: ModelManager
    private lateinit var modelInstaller: ModelInstaller
    private lateinit var sceneManager: SceneManager
    private lateinit var scenePackInstaller: ScenePackInstaller
    private lateinit var apiServerController: ApiServerController
    private lateinit var localStore: LocalStore
    private lateinit var sessionStore: SessionStore
    private lateinit var triggerEngine: TriggerEngine
    private val modelInstallExecutor = Executors.newSingleThreadExecutor()

    private var pendingWebPermissionRequest: PermissionRequest? = null
    private var startRuntimeAfterNotificationPermission = false
    private var pendingRuntimePayload = JSONObject()
    private var windowSettings = JSONObject()

    private val requestCameraPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        val webPermissionRequest = pendingWebPermissionRequest
        if (webPermissionRequest != null) {
            pendingWebPermissionRequest = null
            if (granted) webPermissionRequest.grant(webPermissionRequest.resources)
            else webPermissionRequest.deny()
        }
    }

    private val installScenePackage = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) notifySceneInstallResult(false, "cancelled", null) else handleScenePackage(uri)
    }

    private val installModelPackage = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) notifyModelInstallResult(false, "cancelled", null) else handleModelPackage(uri)
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enterImmersiveMode()

        sceneManager = MNNodeRuntime.sceneManager(this)
        scenePackInstaller = MNNodeRuntime.scenePackInstaller(this)
        localStore = MNNodeRuntime.localStore(this)
        sessionStore = MNNodeRuntime.sessionStore(this)
        modelManager = MNNodeRuntime.modelManager(this)
        modelInstaller = ModelInstaller(this, modelManager)
        apiServerController = MNNodeRuntime.apiServer(this)
        apiServerController.startForService(JSONObject().put("source", "activity"))
        triggerEngine = MNNodeRuntime.triggerEngine(this)
        windowSettings = loadWindowSettings()

        visionController = VisionAnalysisController(this, this).also {
            it.setCallbacks(
                onState = { state -> notifyVisionState(state) },
                onFrame = { frame -> notifyVisionFrame(frame) },
            )
        }
        apiServerController.visionController = visionController

        val assetLoader = WebViewAssetLoader.Builder()
            .setHttpAllowed(true)
            .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(this))
            .addPathHandler("/installed-scenes/", WebViewAssetLoader.InternalStoragePathHandler(this, sceneManager.scenesDir()))
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
                MNNodeShellBridge(host = this@MainActivity),
                "MNNodeShell",
            )
            loadUrl("${SceneManager.LOCAL_ORIGIN}/assets/web/index.html")
        }

        root = FrameLayout(this).apply {
            setBackgroundColor(Color.BLACK)
            addView(webView, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        }

        setContentView(root)
    }

    override fun onResume() {
        super.onResume()
        enterImmersiveMode()
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (shouldShowRuntimeWindow()) showRuntimeWindow()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) enterImmersiveMode()
    }

    private val requestNotificationPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted && startRuntimeAfterNotificationPermission) {
            startRuntimeAfterNotificationPermission = false
            MNNodeRuntimeService.startRuntime(this, pendingRuntimePayload)
        } else {
            startRuntimeAfterNotificationPermission = false
        }
        pendingRuntimePayload = JSONObject()
    }

    override fun onDestroy() {
        visionController.close()
        modelInstallExecutor.shutdown()
        webView.destroy()
        super.onDestroy()
    }

    override fun openScenePackPicker() {
        runOnUiThread { installScenePackage.launch(PACKAGE_MIME_TYPES) }
    }

    override fun openModelPackagePicker() {
        runOnUiThread { installModelPackage.launch(PACKAGE_MIME_TYPES) }
    }

    private fun handleScenePackage(uri: Uri) {
        runCatching { contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
        runCatching { scenePackInstaller.installFromUri(uri) }
            .onSuccess { scene -> notifySceneInstallResult(true, "installed", scene.toJson()) }
            .onFailure { error -> notifySceneInstallResult(false, error.message ?: "install failed", null) }
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

    override fun runtimeShellCommand(command: String, payloadJson: String?): String {
        val payload = parseObject(payloadJson)
        return when (command) {
            "start" -> {
                runCatching { startRuntimeService(payload) }
                    .fold(
                        onSuccess = { apiServerController.state().withRuntimeState().put("starting", true) },
                        onFailure = { error -> apiServerController.state().withRuntimeState().put("lastError", error.message ?: "Runtime service start failed") },
                    )
            }
            "stop" -> {
                MNNodeRuntimeService.stopRuntime(this)
                apiServerController.state().withRuntimeState()
            }
            "battery.requestExemption" -> {
                requestBatteryOptimizationExemption()
                apiServerController.state().withRuntimeState()
            }
            "window.show" -> {
                runOnUiThread { showRuntimeWindow() }
                apiServerController.state().withRuntimeState()
            }
            "window.hide" -> {
                MNNodeRuntimeService.hideFloatingWindow(this)
                apiServerController.state().withRuntimeState()
            }
            "window.settings" -> {
                updateWindowSettings(payload)
                if (shouldShowRuntimeWindow()) runOnUiThread { showRuntimeWindow() }
                apiServerController.state().withRuntimeState()
            }
            "window.permission" -> {
                requestOverlayPermission()
                apiServerController.state().withRuntimeState()
            }
            "settings", "session.create", "session.select", "session.delete" ->
                apiServerController.command(command, payload).withRuntimeState()
            "status" -> apiServerController.state().withRuntimeState()
            else -> JSONObject().put("ok", false).put("message", "Unknown shell command: $command")
        }.toString()
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
        MNNodeRuntimeService.startRuntime(this, payload)
    }

    private fun hasPermission(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
    }

    private fun parseObject(raw: String?): JSONObject {
        return runCatching { JSONObject(raw ?: "{}") }.getOrDefault(JSONObject())
    }

    private fun JSONObject.withRuntimeState(): JSONObject {
        return put("runtimeService", true)
            .put("mode", "foreground-service")
            .put("runtimeModes", org.json.JSONArray(listOf("interactive", "service", "headless")))
            .put("headlessCapable", true)
            .put("vision", visionController.stateJson())
            .put("triggers", triggerEngine.snapshot())
            .put("windowSupported", isFloatingWindowSupported())
            .put("windowAutoShow", windowSettings.optBoolean("autoShow", false))
            .put("windowAllowed", canDrawOverlays())
            .put("windowVisible", MNNodeRuntimeService.isFloatingWindowVisible())
            .put("inPictureInPicture", if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) isInPictureInPictureMode else false)
            .put("batteryOptimizationIgnored", isIgnoringBatteryOptimizations())
    }

    private fun updateWindowSettings(payload: JSONObject) {
        windowSettings = JSONObject(windowSettings.toString())
            .put("autoShow", payload.optBoolean("autoShow", windowSettings.optBoolean("autoShow", false)))
        localStore.set(RUNTIME_SETTINGS_NAMESPACE, WINDOW_SETTINGS_KEY, windowSettings)
    }

    private fun shouldShowRuntimeWindow(): Boolean {
        if (!windowSettings.optBoolean("autoShow", false)) return false
        if (!isPipSupported() && !canDrawOverlays()) return false
        val state = apiServerController.runtimeSummary()
        return state.optBoolean("running", false) || state.optBoolean("starting", false)
    }

    private fun loadWindowSettings(): JSONObject {
        val current = localStore.getObject(RUNTIME_SETTINGS_NAMESPACE, WINDOW_SETTINGS_KEY)
        if (current.length() > 0) return current
        val legacy = localStore.getObject(RUNTIME_SETTINGS_NAMESPACE, LEGACY_PIP_SETTINGS_KEY)
        return JSONObject().put("autoShow", legacy.optBoolean("autoEnterPip", false))
    }

    private fun isFloatingWindowSupported(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M

    private fun canDrawOverlays(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(this)

    private fun showRuntimeWindow() {
        if (!canDrawOverlays()) {
            requestOverlayPermission()
            if (isPipSupported()) enterRuntimePip()
            return
        }
        val state = apiServerController.runtimeSummary()
        if (!state.optBoolean("running", false) && !state.optBoolean("starting", false)) {
            startRuntimeService(JSONObject().put("floatingWindow", true))
            return
        }
        MNNodeRuntimeService.showFloatingWindow(this)
    }

    private fun requestOverlayPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return
        val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
        runCatching { startActivity(intent) }
            .onFailure { startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)) }
    }

    private fun isPipSupported(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)
    }

    private fun enterRuntimePip() {
        if (!isPipSupported()) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val params = PictureInPictureParams.Builder()
                .setAspectRatio(Rational(16, 9))
                .build()
            runCatching { enterPictureInPictureMode(params) }
        }
    }

    private fun isIgnoringBatteryOptimizations(): Boolean {
        val powerManager = getSystemService(PowerManager::class.java)
        return powerManager?.isIgnoringBatteryOptimizations(packageName) == true
    }

    private fun requestBatteryOptimizationExemption() {
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
    }

    private fun notifySceneInstallResult(ok: Boolean, message: String, scene: JSONObject?) {
        emitJs("onSceneInstallResult", JSONObject()
            .put("ok", ok)
            .put("message", message)
            .put("scene", scene ?: JSONObject.NULL))
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

    private fun notifyVisionState(state: JSONObject) {
        emitJs("onVisionState", state)
    }

    private fun notifyVisionFrame(frame: JSONObject) {
        feedVisionToTriggers(frame)
        emitJs("onVisionFrame", frame)
    }

    private fun feedVisionToTriggers(frame: JSONObject) {
        val detections = frame.optJSONArray("detections") ?: return
        val confs = mutableMapOf<Int, Double>()
        for (i in 0 until detections.length()) {
            val det = detections.optJSONObject(i) ?: continue
            val cid = det.optInt("classId", -1)
            val score = det.optDouble("score", 0.0)
            if (cid >= 0) confs[cid] = maxOf(confs[cid] ?: 0.0, score)
        }
        triggerEngine.feed(SensorSample(
            source = "camera:yolov8n",
            timestamp = frame.optLong("timestamp", System.currentTimeMillis()),
            confidenceByClass = confs,
        ))
    }

    private fun emitJs(event: String, payload: JSONObject) {
        val script = "window.MNNodeEvents && window.MNNodeEvents.$event(JSON.parse(${JSONObject.quote(payload.toString())}));"
        webView.post { webView.evaluateJavascript(script, null) }
    }

    companion object {
        private val PACKAGE_MIME_TYPES = arrayOf(
            "application/zip",
            "application/octet-stream",
            "application/x-zip-compressed",
            "*/*",
        )
        private const val RUNTIME_SETTINGS_NAMESPACE = "runtime/settings"
        private const val WINDOW_SETTINGS_KEY = "window"
        private const val LEGACY_PIP_SETTINGS_KEY = "pip"
    }
}
