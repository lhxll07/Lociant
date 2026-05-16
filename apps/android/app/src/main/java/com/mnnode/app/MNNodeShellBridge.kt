package com.mnnode.app

import android.webkit.JavascriptInterface
import org.json.JSONObject

class MNNodeShellBridge(private val host: Host) {
    interface Host {
        fun openScenePackPicker()
        fun openModelPackagePicker()
        fun runtimeShellCommand(command: String, payloadJson: String?): String
    }

    @JavascriptInterface
    fun installScenePack(): String {
        host.openScenePackPicker()
        return ok("picker_opened")
    }

    @JavascriptInterface
    fun installModelPackage(): String {
        host.openModelPackagePicker()
        return ok("picker_opened")
    }

    @JavascriptInterface
    fun runtimeShellCommand(command: String, payloadJson: String?): String {
        return host.runtimeShellCommand(command, payloadJson)
    }

    private fun ok(state: String): String {
        return JSONObject().put("ok", true).put("state", state).toString()
    }
}
