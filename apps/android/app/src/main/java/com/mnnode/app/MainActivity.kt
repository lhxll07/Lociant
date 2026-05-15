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
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.webkit.WebViewAssetLoader
import com.mnnode.app.model.ModelInstaller
import com.mnnode.app.model.ModelManager
import com.mnnode.app.camera.CameraController
import com.mnnode.app.scene.SceneManager
import com.mnnode.app.scene.ScenePackInstaller
import com.mnnode.app.scene.SceneRuntimeManager
import com.mnnode.app.server.ApiServerController
import com.mnnode.app.server.ModelApiMapper
import com.mnnode.app.runtime.MNNodeRuntime
import com.mnnode.app.runtime.MNNodeRuntimeService
import com.mnnode.app.runtime.VisionRuntime
import com.mnnode.app.session.SessionStore
import com.mnnode.app.storage.LocalStore
import com.mnnode.app.vision.VisionAnalysisController
import com.mnnode.app.vision.VisionConfig
import org.json.JSONObject
import java.util.concurrent.Executors

class MainActivity : ComponentActivity(), MNNodeBridge.Host {
    private lateinit var root: FrameLayout
    private lateinit var previewView: PreviewView
    private lateinit var webView: WebView
    private lateinit var cameraController: CameraController
    private lateinit var visionController: VisionAnalysisController
    private lateinit var visionRuntime: VisionRuntime
    private lateinit var modelManager: ModelManager
    private lateinit var modelInstaller: ModelInstaller
    private lateinit var sceneManager: SceneManager
    private lateinit var scenePackInstaller: ScenePackInstaller
    private lateinit var apiServerController: ApiServerController
    private lateinit var localStore: LocalStore
    private lateinit var sessionStore: SessionStore
    private lateinit var sceneRuntimeManager: SceneRuntimeManager
    private val aiExecutor = Executors.newSingleThreadExecutor()
    private val modelInstallExecutor = Executors.newSingleThreadExecutor()

    private var startCameraAfterPermission = false
    private var startVisionAfterPermission = false
    private var pendingWebPermissionRequest: PermissionRequest? = null
    private var startRuntimeAfterNotificationPermission = false
    private var pendingRuntimePayload = JSONObject()
    private var windowSettings = JSONObject()

    private val requestCameraPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        val webPermissionRequest = pendingWebPermissionRequest
        if (webPermissionRequest != null) {
            pendingWebPermissionRequest = null
            if (granted) {
                webPermissionRequest.grant(webPermissionRequest.resources)
            } else {
                webPermissionRequest.deny()
            }
            return@registerForActivityResult
        }

        val cameraPending = startCameraAfterPermission
        val visionPending = startVisionAfterPermission
        startCameraAfterPermission = false
        startVisionAfterPermission = false
        when {
            granted && visionPending -> startVisionInternal()
            granted && cameraPending -> startCameraInternal()
            cameraPending -> notifyCameraState(errorJson("permission denied").put("state", "error"))
            visionPending -> notifyVisionState(JSONObject().put("state", "error").put("message", "permission denied"))
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
        sceneRuntimeManager = MNNodeRuntime.sceneRuntimeManager(this)
        windowSettings = loadWindowSettings()

        previewView = PreviewView(this).apply {
            visibility = View.GONE
            scaleType = PreviewView.ScaleType.FIT_CENTER
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            setBackgroundColor(Color.BLACK)
            layoutParams = FrameLayout.LayoutParams(1, 1)
        }

        cameraController = CameraController(this, this, previewView)
        visionController = VisionAnalysisController(this, this)
        visionRuntime = VisionRuntime(
            cameraController = cameraController,
            visionController = visionController,
            onCameraState = { state -> notifyCameraState(state) },
            onVisionState = { state -> notifyVisionState(state) },
            onVisionFrame = { frame -> notifyVisionFrame(frame) },
        )

        val assetLoader = WebViewAssetLoader.Builder()
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

            addJavascriptInterface(
                MNNodeBridge(
                    host = this@MainActivity,
                    sceneManager = sceneManager,
                    sceneRuntimeManager = sceneRuntimeManager,
                    modelManager = modelManager,
                    localStore = localStore,
                ),
                "MNNode",
            )
            loadUrl("${SceneManager.LOCAL_ORIGIN}/assets/web/index.html")
        }

        root = FrameLayout(this).apply {
            setBackgroundColor(Color.BLACK)
            addView(previewView)
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
        visionRuntime.close()
        aiExecutor.shutdown()
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

    override fun setCameraPreviewRect(x: Int, y: Int, width: Int, height: Int) {
        runOnUiThread { visionRuntime.setPreviewRect(x, y, width, height) }
    }

    override fun viewportMetrics(): JSONObject {
        return JSONObject()
            .put("width", root.width)
            .put("height", root.height)
    }

    override fun startCamera(): String {
        runOnUiThread {
            if (hasPermission(Manifest.permission.CAMERA)) {
                startCameraInternal()
            } else {
                startCameraAfterPermission = true
                requestCameraPermission.launch(Manifest.permission.CAMERA)
            }
        }
        return okState("starting")
    }

    private fun startCameraInternal() {
        visionRuntime.startCamera()
    }

    override fun stopCamera(): String {
        runOnUiThread { visionRuntime.stopCamera() }
        return okState("idle")
    }

    override fun cameraState(): String = visionRuntime.cameraState()

    override fun startVision(configRaw: String?): String {
        val config = VisionConfig.fromJson(configRaw)
        runOnUiThread {
            if (hasPermission(Manifest.permission.CAMERA)) {
                startVisionInternal(config)
            } else {
                startVisionAfterPermission = true
                pendingVisionConfig = config
                requestCameraPermission.launch(Manifest.permission.CAMERA)
            }
        }
        return okState("starting")
    }

    private var pendingVisionConfig: VisionConfig? = null

    private fun startVisionInternal(config: VisionConfig = pendingVisionConfig ?: VisionConfig()) {
        pendingVisionConfig = null
        visionRuntime.startVision(config)
    }

    override fun stopVision(): String {
        runOnUiThread { visionRuntime.stopVision() }
        return okState("idle")
    }

    override fun visionState(): String = visionRuntime.visionState().toString()

    override fun modelChat(requestRaw: String?): String {
        aiExecutor.execute {
            val result = runCatching { handleModelChat(requestRaw) }
                .getOrElse { error ->
                    errorJson(error.message ?: "Model chat failed")
                }
            notifyModelChatResult(result)
        }
        return okState("queued")
    }

    private fun handleModelChat(requestRaw: String?): JSONObject {
        val json = parseObject(requestRaw)
        val requestId = json.optString("requestId", "")
        val sceneId = json.optString("sceneId", "")
        val chatController = MNNodeRuntime.apiServer(this).chatController()
        val request = chatController.sessionRequest(ModelApiMapper.parseSceneChat(json))
        val result = chatController.submitSync(request, com.mnnode.app.server.ChatController.CHAT_TIMEOUT_MS)
        return result.toJson()
            .put("requestId", requestId)
            .put("sceneId", sceneId)
            .put("result", parseJsonObject(result.text) ?: JSONObject().put("text", result.text))
    }

    override fun runtimeServiceCommand(command: String, payloadJson: String?): String {
        return runtimeCommand(command, payloadJson).toString()
    }

    private fun runtimeCommand(command: String, payloadRaw: String?): JSONObject {
        val payload = parseObject(payloadRaw)
        return when (command) {
            "start" -> {
                runCatching { startRuntimeService(payload) }
                    .fold(
                        onSuccess = { runtimeServiceState().put("starting", true) },
                        onFailure = { error -> runtimeServiceState().put("lastError", error.message ?: "Runtime service start failed") },
                    )
            }
            "stop" -> {
                MNNodeRuntimeService.stopRuntime(this)
                runtimeServiceState()
            }
            "battery.requestExemption" -> {
                requestBatteryOptimizationExemption()
                runtimeServiceState()
            }
            "window.show" -> {
                runOnUiThread { showRuntimeWindow() }
                runtimeServiceState()
            }
            "window.hide" -> {
                MNNodeRuntimeService.hideFloatingWindow(this)
                runtimeServiceState()
            }
            "window.settings" -> {
                updateWindowSettings(payload)
                if (shouldShowRuntimeWindow()) runOnUiThread { showRuntimeWindow() }
                runtimeServiceState()
            }
            "window.permission" -> {
                requestOverlayPermission()
                runtimeServiceState()
            }
            else -> apiServerController.command(command, payload).withRuntimeState()
        }
    }

    private fun runtimeServiceState(): JSONObject {
        return apiServerController.state().withRuntimeState()
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

    private fun okState(state: String): String {
        return JSONObject().put("ok", true).put("state", state).toString()
    }

    private fun errorJson(message: String): JSONObject {
        return JSONObject().put("ok", false).put("message", message)
    }

    private fun JSONObject.withRuntimeState(): JSONObject {
        return put("runtimeService", true)
            .put("mode", "foreground-service")
            .put("runtimeModes", org.json.JSONArray(listOf("interactive", "service", "headless")))
            .put("headlessCapable", true)
            .put("vision", visionRuntime.visionState())
            .put("sceneRuntime", sceneRuntimeManager.snapshot())
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

    private fun notifyCameraState(state: JSONObject) {
        emitJs("onCameraResult", state)
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
        sceneRuntimeManager.onVisionFrame(frame).forEach { event ->
            recordRuntimeMessage(event)
            notifyRuntimeMessage(event)
        }
        emitJs("onVisionFrame", frame)
    }

    private fun recordRuntimeMessage(message: JSONObject) {
        val sceneId = message.optString("sceneId", "runtime")
        val type = message.optString("type", "runtime.event")
        val level = message.optJSONObject("event")
            ?.optJSONObject("alert")
            ?.optString("level", "info")
            ?: "info"
        runCatching { sessionStore.recordRuntimeEvent(sceneId, type, level, message) }
    }

    private fun notifyRuntimeMessage(message: JSONObject) {
        emitJs("onRuntimeMessage", message)
    }

    private fun notifyModelChatResult(result: JSONObject) {
        emitJs("onModelChatResult", result)
    }

    private fun emitJs(event: String, payload: JSONObject) {
        val script = "window.MNNodeEvents && window.MNNodeEvents.$event(JSON.parse(${JSONObject.quote(payload.toString())}));"
        webView.post { webView.evaluateJavascript(script, null) }
    }

    private fun parseJsonObject(text: String): JSONObject? {
        val range = lastJsonObjectRange(text) ?: return null
        return runCatching { JSONObject(text.substring(range.first, range.last + 1)) }.getOrNull()
            ?.put("raw", text)
    }

    private fun lastJsonObjectRange(text: String): IntRange? {
        var best: IntRange? = null
        var depth = 0
        var start = -1
        var inString = false
        var escaped = false

        text.forEachIndexed { index, char ->
            if (escaped) { escaped = false; return@forEachIndexed }
            if (char == '\\' && inString) { escaped = true; return@forEachIndexed }
            if (char == '"') { inString = !inString; return@forEachIndexed }
            if (inString) return@forEachIndexed

            when (char) {
                '{' -> { if (depth++ == 0) start = index }
                '}' -> {
                    if (depth > 0 && --depth == 0) best = start..index
                }
            }
        }
        return best
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
