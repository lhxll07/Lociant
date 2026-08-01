package io.lociant.tools

import android.Manifest
import android.app.KeyguardManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.BatteryManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.provider.Settings
import android.text.TextUtils
import androidx.core.content.ContextCompat
import io.lociant.core.tools.*
import io.lociant.tools.LociantAccessibilityService
import io.lociant.tools.runtime.DeviceInteraction
import org.json.JSONObject
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class AndroidTools(
    private val context: Context,
) : ToolProvider {
    private val readScreenPolicy = ToolPolicy(requiresActivity = true)
    private val actionPolicy = ToolPolicy(sideEffect = true, openWorld = true)

    override fun tools(): List<ToolDefinition> = listOf(
        tool(
            name = "device_status",
            description = "Return Android device state, battery, network, screen, and permission readiness.",
        ) { deviceStatus() },
        tool(
            name = "clipboard_read",
            description = "Read current Android clipboard text when Android allows clipboard access.",
            policy = readScreenPolicy,
        ) { clipboardRead() },
        tool(
            name = "clipboard_write",
            description = "Write plain text into the Android clipboard.",
            properties = JSONObject()
                .put("text", stringParam("Text to copy to clipboard."))
                .put("label", stringParam("Optional clipboard label.")),
            policy = actionPolicy,
        ) { args -> clipboardWrite(args) },
        tool(
            name = "app_open",
            description = "Open an installed Android app by package name, or open a safe deep link / URL.",
            properties = JSONObject()
                .put("packageName", stringParam("Android package name to launch."))
                .put("uri", stringParam("Optional http, https, geo, tel, mailto, or package URI.")),
            policy = actionPolicy,
        ) { args -> appOpen(args) },
        tool(
            name = "ui_screen_state",
            description = "Read the current Android UI as a compact actionable state. Prefer this before UI actions; it returns stable nodeId values for ui_click_node and can include a screenshot fallback.",
            properties = JSONObject()
                .put("query", stringParam("Optional text filter for visible node labels."))
                .put("exact", boolParam("If true, query must match the full label. Default false."))
                .put("maxDepth", intParam("Maximum tree depth to scan. Default 8."))
                .put("maxNodes", intParam("Maximum visible nodes to return. Default 80."))
                .put("includeScreenshot", boolParam("Include a compressed screenshot data URL. Default false."))
                .put("screenshotMaxWidth", intParam("Maximum screenshot width. Default 720."))
                .put("quality", intParam("JPEG quality from 45 to 95. Default 82.")),
            policy = readScreenPolicy,
        ) { args -> uiScreenState(args) },
        tool(
            name = "ui_click_node",
            description = "Click a nodeId returned by ui_screen_state. Uses Accessibility click first, then falls back to a tap at the node center.",
            properties = JSONObject()
                .put("nodeId", stringParam("nodeId returned by ui_screen_state."))
                .put("longClick", boolParam("If true, perform a long click. Default false.")),
            policy = actionPolicy,
        ) { args -> uiClickNode(args) },
        tool(
            name = "ui_tap",
            description = "Tap or long press the current Android screen at coordinates. Use only when ui_click_node is not possible.",
            properties = JSONObject()
                .put("x", intParam("Screen x coordinate."))
                .put("y", intParam("Screen y coordinate."))
                .put("longClick", boolParam("If true, perform a long click. Default false.")),
            policy = actionPolicy,
        ) { args -> uiTap(args) },
        tool(
            name = "ui_swipe",
            description = "Swipe or drag from one screen coordinate to another.",
            properties = JSONObject()
                .put("x1", intParam("Start x coordinate."))
                .put("y1", intParam("Start y coordinate."))
                .put("x2", intParam("End x coordinate."))
                .put("y2", intParam("End y coordinate."))
                .put("durationMs", intParam("Gesture duration in milliseconds. Default 260.")),
            policy = actionPolicy,
        ) { args -> uiSwipe(args) },
        tool(
            name = "ui_wait",
            description = "Wait until Android UI becomes idle or until visible text appears. Use after actions before reading the next screen state.",
            properties = JSONObject()
                .put("text", stringParam("Optional visible text to wait for. If omitted, wait for idle."))
                .put("exact", boolParam("If true, text must match exactly. Default false."))
                .put("timeoutMs", intParam("Maximum wait time. Default 3000."))
                .put("idleMs", intParam("Required idle duration without accessibility events. Default 500."))
                .put("maxDepth", intParam("Maximum tree depth to scan for text. Default 8.")),
            policy = readScreenPolicy,
        ) { args -> uiWait(args) },
        tool(
            name = "ui_paste",
            description = "Paste the current Android clipboard into the focused input field. First write the text with clipboard_write, then focus the target input, then call this tool.",
            policy = actionPolicy,
        ) { requireAccessibility().pasteIntoFocusedText() },
        tool(
            name = "ui_set_text",
            description = "Replace the text of an editable nodeId returned by ui_screen_state, bypassing the clipboard.",
            properties = JSONObject()
                .put("nodeId", stringParam("nodeId of an editable node returned by ui_screen_state."))
                .put("text", stringParam("Text to write into the field."))
                .put("submit", boolParam("Request the editor to perform its IME action after writing. Default false.")),
            policy = actionPolicy,
        ) { args -> setNodeText(args) },
        globalActionTool("ui_back", "Press the Android Back button.", "back") { it.pressBack() },
        globalActionTool("ui_home", "Press the Android Home button.", "home") { it.pressHome() },
        globalActionTool("ui_recent_apps", "Open the Android recent apps overview.", "recent_apps") { it.pressRecentApps() },
        globalActionTool("ui_notifications", "Open the Android notification shade.", "notifications") { it.openNotifications() },
        globalActionTool("ui_quick_settings", "Open Android quick settings.", "quick_settings") { it.openQuickSettings() },
    )

    private fun globalActionTool(
        name: String,
        description: String,
        action: String,
        handler: (LociantAccessibilityService) -> Boolean,
    ): ToolDefinition = tool(
        name = name,
        description = description,
        policy = actionPolicy,
    ) { globalAction(action, handler) }

    private fun deviceStatus(): JSONObject {
        val battery = context.registerReceiver(null, android.content.IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val power = context.getSystemService(PowerManager::class.java)
        val keyguard = context.getSystemService(KeyguardManager::class.java)
        val chargingStatus = battery?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val plugged = battery?.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) ?: 0
        val level = battery?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = battery?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        val device = DeviceInteraction.snapshot(context)
        return JSONObject()
            .put("ok", true)
            .put("device", device)
            .put("battery", JSONObject()
                .put("level", if (level >= 0 && scale > 0) (level * 100 / scale) else JSONObject.NULL)
                .put("charging", chargingStatus == BatteryManager.BATTERY_STATUS_CHARGING || chargingStatus == BatteryManager.BATTERY_STATUS_FULL)
                .put("plugged", plugged != 0)
                .put("powerSave", power?.isPowerSaveMode == true))
            .put("network", networkStatus())
            .put("permissions", JSONObject()
                .put("camera", hasPermission(Manifest.permission.CAMERA))
                .put("notification", notificationPermissionGranted())
                .put("overlay", canDrawOverlays())
                .put("batteryOptimizationIgnored", isIgnoringBatteryOptimizations())
                .put("accessibility", isAccessibilityServiceEnabled()))
            .put("screen", JSONObject()
                .put("interactive", device.optBoolean("interactive"))
                .put("locked", keyguard?.isKeyguardLocked == true))
            .put("sdk", Build.VERSION.SDK_INT)
            .put("packageName", context.packageName)
    }

    private fun clipboardRead(): JSONObject {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && !DeviceInteraction.snapshot(context).optBoolean("activityForeground")) {
            return JSONObject()
                .put("ok", false)
                .put("text", "")
                .put("code", "clipboard_foreground_required")
                .put("message", "Android only allows clipboard reads while Lociant is foreground and focused.")
        }
        return runOnMain {
            val clipboard = context.getSystemService(ClipboardManager::class.java)
            val item = clipboard?.primaryClip?.takeIf { it.itemCount > 0 }?.getItemAt(0)
            val text = item?.coerceToText(context)?.toString().orEmpty()
            JSONObject()
                .put("ok", text.isNotEmpty())
                .put("text", text)
                .put("length", text.length)
                .put("message", if (text.isEmpty()) "Clipboard is empty or blocked by Android privacy rules." else "Clipboard text read.")
        }
    }

    private fun clipboardWrite(args: JSONObject): JSONObject {
        val text = args.optString("text").trim()
        require(text.isNotBlank()) { "text is required" }
        val label = args.optString("label", "Lociant")
        return runOnMain {
            val clipboard = requireNotNull(context.getSystemService(ClipboardManager::class.java)) {
                "clipboard service unavailable"
            }
            clipboard.setPrimaryClip(ClipData.newPlainText(label, text))
            JSONObject()
                .put("ok", true)
                .put("action", "clipboard_write")
                .put("length", text.length)
                .put("message", "Clipboard text written.")
        }
    }

    private fun appOpen(args: JSONObject): JSONObject {
        val packageName = args.optString("packageName").trim()
        val uri = args.optString("uri").trim()
        require(packageName.isNotBlank() || uri.isNotBlank()) { "packageName or uri is required" }
        val uriObject = uri.takeIf { it.isNotBlank() }?.let { safeUri(it) }
        val targetPackage = packageName.ifBlank {
            uriObject?.takeIf { it.scheme.equals("package", ignoreCase = true) }?.schemeSpecificPart.orEmpty()
        }.trim()
        val intent = if (targetPackage.isNotBlank()) {
            context.packageManager.getLaunchIntentForPackage(targetPackage)
                ?: throw IllegalArgumentException("No launchable app found for package: $targetPackage")
        } else {
            Intent(Intent.ACTION_VIEW, requireNotNull(uriObject) { "uri is required" })
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { context.startActivity(intent) }
            .getOrElse { error -> throw IllegalArgumentException(error.message ?: "No matching activity found.") }
        return JSONObject()
            .put("ok", true)
            .put("action", "app_open")
            .put("packageName", targetPackage)
            .put("uri", uri)
            .put("message", if (targetPackage.isNotBlank()) "App opened." else "URI opened.")
    }

    private fun uiScreenState(args: JSONObject): JSONObject =
        requireAccessibility().readScreenState(
            maxDepth(args, 8),
            args.optInt("maxNodes", 80).coerceIn(8, 200),
            args.optString("query").trim(),
            args.optBoolean("exact", false),
            args.optBoolean("includeScreenshot", false),
            args.optInt("screenshotMaxWidth", 720).coerceIn(320, 1440),
            args.optInt("quality", 82).coerceIn(45, 95),
        )

    private fun uiClickNode(args: JSONObject): JSONObject =
        requireAccessibility().clickNode(
            args.optString("nodeId").trim(),
            args.optBoolean("longClick", false),
        )

    private fun uiTap(args: JSONObject): JSONObject {
        val x = requiredCoordinate(args, "x")
        val y = requiredCoordinate(args, "y")
        return if (args.optBoolean("longClick", false)) {
            requireAccessibility().gestureLongClick(x, y)
        } else {
            requireAccessibility().gestureClick(x, y)
        }
    }

    private fun uiSwipe(args: JSONObject): JSONObject {
        val x1 = requiredCoordinate(args, "x1")
        val y1 = requiredCoordinate(args, "y1")
        val x2 = requiredCoordinate(args, "x2")
        val y2 = requiredCoordinate(args, "y2")
        val durationMs = args.optLong("durationMs", 260L).coerceIn(50L, 2000L)
        return requireAccessibility().gestureSwipe(x1, y1, x2, y2, durationMs)
    }

    private fun uiWait(args: JSONObject): JSONObject =
        requireAccessibility().waitForUi(
            args.optString("text").trim(),
            args.optBoolean("exact", false),
            args.optLong("timeoutMs", 3000L).coerceIn(100L, 15000L),
            args.optLong("idleMs", 500L).coerceIn(100L, 3000L),
            maxDepth(args, 8),
        )

    private fun setNodeText(args: JSONObject): JSONObject {
        val nodeId = args.optString("nodeId").trim()
        if (nodeId.isEmpty()) throw IllegalArgumentException("nodeId is required")
        val text = args.optString("text", "")
        val submit = args.optBoolean("submit", false)
        return requireAccessibility().setNodeText(nodeId, text, submit)
    }

    private fun globalAction(action: String, block: (LociantAccessibilityService) -> Boolean): JSONObject =
        gestureResult(action, block(requireAccessibility()))

    private fun gestureResult(action: String, success: Boolean): JSONObject =
        JSONObject()
            .put("ok", success)
            .put("action", action)
            .put("message", if (success) "$action performed." else "$action failed or was cancelled.")

    private fun requireAccessibility(): LociantAccessibilityService {
        if (!isAccessibilityServiceEnabled()) {
            throw IllegalStateException("AccessibilityService permission is not granted.")
        }
        return LociantAccessibilityService.instance
            ?: throw IllegalStateException("AccessibilityService is not connected. Open Android Accessibility settings and enable Lociant.")
    }

    private fun networkStatus(): JSONObject {
        val cm = context.getSystemService(ConnectivityManager::class.java)
        val network = cm?.activeNetwork
        val caps = network?.let { cm.getNetworkCapabilities(it) }
        return JSONObject()
            .put("connected", caps != null)
            .put("wifi", caps?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true)
            .put("cellular", caps?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true)
            .put("ethernet", caps?.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) == true)
            .put("vpn", caps?.hasTransport(NetworkCapabilities.TRANSPORT_VPN) == true)
    }

    private fun safeUri(raw: String): Uri {
        val uri = Uri.parse(raw)
        val scheme = uri.scheme?.lowercase().orEmpty()
        require(scheme in SAFE_SCHEMES) { "Unsupported URI scheme: $scheme" }
        return uri
    }

    private fun requiredCoordinate(args: JSONObject, key: String): Int {
        val value = args.optInt(key, -1)
        require(value >= 0) { "$key is required" }
        return value
    }

    private fun maxDepth(args: JSONObject, defaultValue: Int): Int =
        args.optInt("maxDepth", defaultValue).coerceIn(1, 12)

    private fun hasPermission(permission: String): Boolean =
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED

    private fun notificationPermissionGranted(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            hasPermission(Manifest.permission.POST_NOTIFICATIONS)

    private fun canDrawOverlays(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(context)

    private fun isIgnoringBatteryOptimizations(): Boolean {
        val powerManager = context.getSystemService(PowerManager::class.java)
        return powerManager?.isIgnoringBatteryOptimizations(context.packageName) == true
    }

    private fun isAccessibilityServiceEnabled(): Boolean {
        val serviceId = "${context.packageName}/${LociantAccessibilityService::class.java.name}"
        val enabled = Settings.Secure.getString(context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
            ?: return false
        return enabled.split(':').any { TextUtils.equals(it, serviceId) }
    }

    private fun runOnMain(block: () -> JSONObject): JSONObject {
        if (Looper.myLooper() == Looper.getMainLooper()) return block()
        val latch = CountDownLatch(1)
        var result = JSONObject()
        var failure: Throwable? = null
        Handler(Looper.getMainLooper()).post {
            runCatching { block() }
                .onSuccess { result = it }
                .onFailure { failure = it }
            latch.countDown()
        }
        if (!latch.await(2, TimeUnit.SECONDS)) throw IllegalStateException("main thread command timed out")
        failure?.let { throw it }
        return result
    }

    companion object {
        private val SAFE_SCHEMES = setOf("http", "https", "geo", "tel", "mailto", "package")
    }
}
