package com.mnnode.app.server

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
import com.mnnode.app.LociantAccessibilityService
import com.mnnode.app.runtime.DeviceInteraction
import org.json.JSONObject
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class AndroidTools(
    private val context: Context,
) : ToolProvider {
    private val readScreenPolicy = ToolPolicy(requiresActivity = true)
    private val actionPolicy = ToolPolicy(sideEffect = true)

    override fun tools(): List<ToolDefinition> = listOf(
        ToolDefinition(
            name = "device_status",
            description = "Return Android device state, battery, network, screen, and permission readiness.",
            parameters = objectSchema(),
        ) { deviceStatus() },
        ToolDefinition(
            name = "clipboard_read",
            description = "Read current Android clipboard text when Android allows clipboard access.",
            parameters = objectSchema(),
            policy = readScreenPolicy,
        ) { clipboardRead() },
        ToolDefinition(
            name = "clipboard_write",
            description = "Write plain text into the Android clipboard.",
            parameters = objectSchema(JSONObject()
                .put("text", stringParam("Text to copy to clipboard."))
                .put("label", stringParam("Optional clipboard label."))),
            policy = actionPolicy,
        ) { args -> clipboardWrite(args) },
        ToolDefinition(
            name = "app_open",
            description = "Open an installed Android app by package name, or open a safe deep link / URL.",
            parameters = objectSchema(JSONObject()
                .put("packageName", stringParam("Android package name to launch."))
                .put("uri", stringParam("Optional http, https, geo, tel, mailto, or package URI."))),
            policy = actionPolicy,
        ) { args -> appOpen(args) },
        ToolDefinition(
            name = "ui_screen_text",
            description = "Read visible text from the current Android screen via AccessibilityService.",
            parameters = objectSchema(JSONObject()
                .put("maxDepth", intParam("Maximum accessibility tree depth to scan. Default 8."))),
            policy = readScreenPolicy,
        ) { args -> uiScreenText(args) },
        ToolDefinition(
            name = "ui_screen_structure",
            description = "Read the current Android accessibility tree as a stable JSON snapshot.",
            parameters = objectSchema(JSONObject()
                .put("maxDepth", intParam("Maximum tree depth. Default 5, capped at 12."))),
            policy = readScreenPolicy,
        ) { args -> uiScreenStructure(args) },
        ToolDefinition(
            name = "ui_clickable_elements",
            description = "List visible clickable or long-clickable elements with labels and tap coordinates.",
            parameters = objectSchema(JSONObject()
                .put("maxDepth", intParam("Maximum tree depth to scan. Default 8."))),
            policy = readScreenPolicy,
        ) { args -> uiClickableElements(args) },
        ToolDefinition(
            name = "ui_find_text",
            description = "Find visible text on the current Android screen and return stable bounds for matching elements.",
            parameters = objectSchema(JSONObject()
                .put("query", stringParam("Text to search for."))
                .put("exact", boolParam("If true, match exact text. Default false."))
                .put("maxDepth", intParam("Maximum tree depth to scan. Default 8."))),
            policy = readScreenPolicy,
        ) { args -> uiFindText(args) },
        ToolDefinition(
            name = "ui_click",
            description = "Tap the current Android screen at the specified coordinates.",
            parameters = objectSchema(JSONObject()
                .put("x", intParam("Screen x coordinate."))
                .put("y", intParam("Screen y coordinate."))),
            policy = actionPolicy,
        ) { args -> uiClick(args) },
        ToolDefinition(
            name = "ui_long_click",
            description = "Long press the current Android screen at the specified coordinates.",
            parameters = objectSchema(JSONObject()
                .put("x", intParam("Screen x coordinate."))
                .put("y", intParam("Screen y coordinate."))),
            policy = actionPolicy,
        ) { args -> uiLongClick(args) },
        ToolDefinition(
            name = "ui_swipe",
            description = "Swipe or drag from one screen coordinate to another.",
            parameters = objectSchema(JSONObject()
                .put("x1", intParam("Start x coordinate."))
                .put("y1", intParam("Start y coordinate."))
                .put("x2", intParam("End x coordinate."))
                .put("y2", intParam("End y coordinate."))
                .put("durationMs", intParam("Gesture duration in milliseconds. Default 260."))),
            policy = actionPolicy,
        ) { args -> uiSwipe(args) },
        ToolDefinition(
            name = "ui_swipe_up",
            description = "Swipe upward on the current screen.",
            parameters = objectSchema(),
            policy = actionPolicy,
        ) { uiSwipeUp() },
        ToolDefinition(
            name = "ui_swipe_down",
            description = "Swipe downward on the current screen.",
            parameters = objectSchema(),
            policy = actionPolicy,
        ) { uiSwipeDown() },
        ToolDefinition(
            name = "ui_back",
            description = "Press the Android Back button.",
            parameters = objectSchema(),
            policy = actionPolicy,
        ) { globalAction("back") { it.pressBack() } },
        ToolDefinition(
            name = "ui_home",
            description = "Press the Android Home button.",
            parameters = objectSchema(),
            policy = actionPolicy,
        ) { globalAction("home") { it.pressHome() } },
        ToolDefinition(
            name = "ui_recent_apps",
            description = "Open the Android recent apps overview.",
            parameters = objectSchema(),
            policy = actionPolicy,
        ) { globalAction("recent_apps") { it.pressRecentApps() } },
        ToolDefinition(
            name = "ui_notifications",
            description = "Open the Android notification shade.",
            parameters = objectSchema(),
            policy = actionPolicy,
        ) { globalAction("notifications") { it.openNotifications() } },
        ToolDefinition(
            name = "ui_quick_settings",
            description = "Open Android quick settings.",
            parameters = objectSchema(),
            policy = actionPolicy,
        ) { globalAction("quick_settings") { it.openQuickSettings() } },
        ToolDefinition(
            name = "ui_find_and_click",
            description = "Find visible text on the current screen and click the matching element or its actionable parent.",
            parameters = objectSchema(JSONObject()
                .put("text", stringParam("Text to search for and click."))
                .put("exact", boolParam("If true, match exact text. Default false."))
                .put("maxDepth", intParam("Maximum tree depth to scan. Default 8."))),
            policy = actionPolicy,
        ) { args -> uiFindAndClick(args) },
    )

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

    private fun uiScreenText(args: JSONObject): JSONObject =
        requireAccessibility().readScreenText(maxDepth(args, 8))

    private fun uiScreenStructure(args: JSONObject): JSONObject =
        requireAccessibility().readScreenStructure(maxDepth(args, 5))

    private fun uiClickableElements(args: JSONObject): JSONObject =
        requireAccessibility().readClickableElements(maxDepth(args, 8))

    private fun uiFindText(args: JSONObject): JSONObject {
        val query = args.optString("query").trim()
        require(query.isNotBlank()) { "query is required" }
        return requireAccessibility().findText(query, args.optBoolean("exact", false), maxDepth(args, 8))
    }

    private fun uiClick(args: JSONObject): JSONObject {
        val x = requiredCoordinate(args, "x")
        val y = requiredCoordinate(args, "y")
        val success = requireAccessibility().gestureClick(x, y)
        return gestureResult("click", success)
            .put("x", x)
            .put("y", y)
    }

    private fun uiLongClick(args: JSONObject): JSONObject {
        val x = requiredCoordinate(args, "x")
        val y = requiredCoordinate(args, "y")
        val success = requireAccessibility().gestureLongClick(x, y)
        return gestureResult("long_click", success)
            .put("x", x)
            .put("y", y)
    }

    private fun uiSwipe(args: JSONObject): JSONObject {
        val x1 = requiredCoordinate(args, "x1")
        val y1 = requiredCoordinate(args, "y1")
        val x2 = requiredCoordinate(args, "x2")
        val y2 = requiredCoordinate(args, "y2")
        val durationMs = args.optLong("durationMs", 260L).coerceIn(50L, 2000L)
        val success = requireAccessibility().gestureSwipe(x1, y1, x2, y2, durationMs)
        return gestureResult("swipe", success)
            .put("from", JSONObject().put("x", x1).put("y", y1))
            .put("to", JSONObject().put("x", x2).put("y", y2))
            .put("durationMs", durationMs)
    }

    private fun uiSwipeUp(): JSONObject =
        gestureResult("swipe_up", requireAccessibility().gestureSwipeUp())

    private fun uiSwipeDown(): JSONObject =
        gestureResult("swipe_down", requireAccessibility().gestureSwipeDown())

    private fun uiFindAndClick(args: JSONObject): JSONObject {
        val text = args.optString("text").trim()
        require(text.isNotBlank()) { "text is required" }
        return requireAccessibility().clickText(text, args.optBoolean("exact", false), maxDepth(args, 8))
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

    private fun stringParam(description: String): JSONObject =
        JSONObject().put("type", "string").put("description", description)

    private fun intParam(description: String): JSONObject =
        JSONObject().put("type", "integer").put("description", description)

    private fun boolParam(description: String): JSONObject =
        JSONObject().put("type", "boolean").put("description", description)

    companion object {
        private val SAFE_SCHEMES = setOf("http", "https", "geo", "tel", "mailto", "package")
    }
}
