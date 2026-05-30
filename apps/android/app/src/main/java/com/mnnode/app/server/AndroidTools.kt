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
    override fun tools(): List<ToolDefinition> = listOf(
        ToolDefinition(
            name = "device_status",
            description = "Return Android device state, battery, network, screen, and permission readiness.",
            parameters = objectSchema(),
        ) { deviceStatus() },
        ToolDefinition(
            name = "clipboard_read",
            description = "Read current Android clipboard text when the system allows app clipboard access.",
            parameters = objectSchema(),
            policy = ToolPolicy(requiresActivity = true),
        ) { clipboardRead() },
        ToolDefinition(
            name = "clipboard_write",
            description = "Write plain text into the Android clipboard.",
            parameters = objectSchema(JSONObject()
                .put("text", JSONObject().put("type", "string").put("description", "Text to copy to clipboard"))
                .put("label", JSONObject().put("type", "string").put("description", "Optional clipboard label"))),
            policy = ToolPolicy(sideEffect = true),
        ) { args -> clipboardWrite(args) },
        ToolDefinition(
            name = "app_open",
            description = "Open an installed Android app by package name, or open a safe deep link / URL.",
            parameters = objectSchema(JSONObject()
                .put("packageName", JSONObject().put("type", "string").put("description", "Android package name to launch"))
                .put("uri", JSONObject().put("type", "string").put("description", "Optional http, https, geo, tel, mailto, or package URI"))),
            policy = ToolPolicy(sideEffect = true),
        ) { args -> appOpen(args) },
    )

    private fun deviceStatus(): JSONObject {
        val battery = context.registerReceiver(null, android.content.IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val power = context.getSystemService(PowerManager::class.java)
        val keyguard = context.getSystemService(KeyguardManager::class.java)
        val chargingStatus = battery?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val plugged = battery?.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) ?: 0
        val level = battery?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = battery?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        return JSONObject()
            .put("device", DeviceInteraction.snapshot(context))
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
                .put("interactive", DeviceInteraction.snapshot(context).optBoolean("interactive"))
                .put("locked", keyguard?.isKeyguardLocked == true))
            .put("sdk", Build.VERSION.SDK_INT)
            .put("packageName", context.packageName)
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

    private fun clipboardRead(): JSONObject {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && !DeviceInteraction.snapshot(context).optBoolean("activityForeground")) {
            return JSONObject()
                .put("ok", false)
                .put("text", "")
                .put("code", "clipboard_foreground_required")
                .put("message", "Android 10+ only allows clipboard reads while the app is foreground and focused.")
        }
        return runOnMain {
            val clipboard = context.getSystemService(ClipboardManager::class.java)
            val item = clipboard?.primaryClip?.takeIf { it.itemCount > 0 }?.getItemAt(0)
            val text = item?.coerceToText(context)?.toString().orEmpty()
            JSONObject()
                .put("ok", text.isNotEmpty())
                .put("text", text)
                .put("message", if (text.isEmpty()) "Clipboard is empty or blocked by Android privacy rules." else "clipboard read")
        }
    }

    private fun clipboardWrite(args: JSONObject): JSONObject {
        val text = args.optString("text")
        require(text.isNotBlank()) { "text is required" }
        val label = args.optString("label", "Lociant")
        return runOnMain {
            val clipboard = context.getSystemService(ClipboardManager::class.java)
            require(clipboard != null) { "clipboard service unavailable" }
            clipboard.setPrimaryClip(ClipData.newPlainText(label, text))
            JSONObject().put("ok", true).put("action", "clipboard_written").put("length", text.length)
        }
    }

    private fun appOpen(args: JSONObject): JSONObject {
        val packageName = args.optString("packageName").trim()
        val uri = args.optString("uri").trim()
        val intent = when {
            packageName.isNotBlank() -> launchIntentForPackage(packageName)
            uri.isNotBlank() -> Intent(Intent.ACTION_VIEW, safeUri(uri))
            else -> throw IllegalArgumentException("packageName or uri is required")
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { context.startActivity(intent) }
            .getOrElse { error ->
                throw IllegalArgumentException("Cannot open app or URI: ${error.message ?: "no matching activity"}")
            }
        return JSONObject()
            .put("ok", true)
            .put("action", "opened")
            .put("packageName", packageName)
            .put("uri", uri)
    }

    private fun launchIntentForPackage(packageName: String): Intent {
        context.packageManager.getLaunchIntentForPackage(packageName)?.let { return it }
        return Intent(Intent.ACTION_MAIN)
            .addCategory(Intent.CATEGORY_LAUNCHER)
            .setPackage(packageName)
    }

    private fun safeUri(raw: String): Uri {
        val uri = Uri.parse(raw)
        val scheme = uri.scheme?.lowercase().orEmpty()
        require(scheme in SAFE_SCHEMES) { "Unsupported URI scheme: $scheme" }
        return uri
    }

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
        if (!latch.await(2, TimeUnit.SECONDS)) {
            throw IllegalStateException("main thread command timed out")
        }
        failure?.let { throw it }
        return result
    }

    companion object {
        private val SAFE_SCHEMES = setOf("http", "https", "geo", "tel", "mailto", "package")
    }
}
