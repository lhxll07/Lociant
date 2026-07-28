package io.lociant.android

import android.webkit.JavascriptInterface
import org.json.JSONObject

class LociantBridge(private val host: Host) {
    interface Host {
        fun openModelPackagePicker()
        fun requestCameraPermission()
        fun requestNotificationPermission()
        fun requestOverlayPermission()
        fun requestBatteryOptimizationExemption()
        fun requestAccessibilityPermission()
        fun openAppSettings()
        fun openPermissionSettings(kind: String)
        fun runtimeState(): String
        fun startRuntime(payloadJson: String?): String
        fun stopRuntime(): String
        fun updateRuntimeSettings(payloadJson: String?): String
        fun releaseRuntimeModel(): String
        fun createSession(): String
        fun selectSession(sessionId: String): String
        fun deleteSession(sessionId: String): String
        fun sessionDetails(sessionId: String): String
        fun startVision(payloadJson: String?): String
        fun stopVision(): String
        fun showRuntimeWindow(): String
        fun hideRuntimeWindow(): String
        fun updateRuntimeWindow(payloadJson: String?): String
    }

    @JavascriptInterface
    fun installModelPackage(): String {
        host.openModelPackagePicker()
        return ok("picker_opened")
    }

    @JavascriptInterface
    fun requestCameraPermission(): String {
        host.requestCameraPermission()
        return ok("permission_requested")
    }

    @JavascriptInterface
    fun requestNotificationPermission(): String {
        host.requestNotificationPermission()
        return ok("permission_requested")
    }

    @JavascriptInterface
    fun requestOverlayPermission(): String {
        host.requestOverlayPermission()
        return ok("permission_requested")
    }

    @JavascriptInterface
    fun requestBatteryOptimizationExemption(): String {
        host.requestBatteryOptimizationExemption()
        return ok("permission_requested")
    }

    @JavascriptInterface
    fun requestAccessibilityPermission(): String {
        host.requestAccessibilityPermission()
        return ok("permission_requested")
    }

    @JavascriptInterface
    fun openAppSettings(): String {
        host.openAppSettings()
        return ok("settings_opened")
    }

    @JavascriptInterface
    fun openPermissionSettings(kind: String): String {
        host.openPermissionSettings(kind)
        return ok("settings_opened")
    }

    @JavascriptInterface
    fun runtimeState(): String = host.runtimeState()

    @JavascriptInterface
    fun startRuntime(payloadJson: String?): String = host.startRuntime(payloadJson)

    @JavascriptInterface
    fun stopRuntime(): String = host.stopRuntime()

    @JavascriptInterface
    fun updateRuntimeSettings(payloadJson: String?): String = host.updateRuntimeSettings(payloadJson)

    @JavascriptInterface
    fun releaseRuntimeModel(): String = host.releaseRuntimeModel()

    @JavascriptInterface
    fun createSession(): String = host.createSession()

    @JavascriptInterface
    fun selectSession(sessionId: String): String = host.selectSession(sessionId)

    @JavascriptInterface
    fun deleteSession(sessionId: String): String = host.deleteSession(sessionId)

    @JavascriptInterface
    fun sessionDetails(sessionId: String): String = host.sessionDetails(sessionId)

    @JavascriptInterface
    fun startVision(payloadJson: String?): String = host.startVision(payloadJson)

    @JavascriptInterface
    fun stopVision(): String = host.stopVision()

    @JavascriptInterface
    fun showRuntimeWindow(): String = host.showRuntimeWindow()

    @JavascriptInterface
    fun hideRuntimeWindow(): String = host.hideRuntimeWindow()

    @JavascriptInterface
    fun updateRuntimeWindow(payloadJson: String?): String = host.updateRuntimeWindow(payloadJson)

    private fun ok(state: String): String {
        return JSONObject().put("ok", true).put("state", state).toString()
    }
}
