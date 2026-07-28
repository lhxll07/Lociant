package com.mnnode.app

import android.webkit.JavascriptInterface
import org.json.JSONObject

class MNNodeShellBridge(private val host: Host) {
    interface Host {
        fun openModelPackagePicker()
        fun requestCameraPermission()
        fun requestNotificationPermission()
        fun requestOverlayPermission()
        fun requestBatteryOptimizationExemption()
        fun requestAccessibilityPermission()
        fun openAppSettings()
        fun openPermissionSettings(kind: String)
        fun runtimeShellCommand(command: String, payloadJson: String?): String
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
    fun runtimeShellCommand(command: String, payloadJson: String?): String {
        return host.runtimeShellCommand(command, payloadJson)
    }

    private fun ok(state: String): String {
        return JSONObject().put("ok", true).put("state", state).toString()
    }
}
