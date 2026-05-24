package com.mnnode.app.runtime

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Settings
import com.mnnode.app.storage.LocalStore
import org.json.JSONObject

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != Intent.ACTION_BOOT_COMPLETED) return
        val appContext = context.applicationContext
        val store = localStore(appContext)
        val settings = store.getObject(SERVER_SETTINGS_NAMESPACE, SERVER_SETTINGS_KEY)
        if (!settings.optBoolean("autoStart", false)) return

        val windowSettings = store.getObject(WINDOW_SETTINGS_NAMESPACE, WINDOW_SETTINGS_KEY)
        val payload = JSONObject()
            .put("mode", "headless")
        if (windowSettings.optBoolean("autoShow", false) && Settings.canDrawOverlays(appContext)) {
            payload.put("floatingWindow", true)
        }
        MNNodeRuntimeService.startRuntime(appContext, payload)
    }

    private fun localStore(context: Context): LocalStore = MNNodeRuntime.localStore(context)

    companion object {
        private const val SERVER_SETTINGS_NAMESPACE = "runtime/model-server/settings"
        private const val SERVER_SETTINGS_KEY = "server"
        private const val WINDOW_SETTINGS_NAMESPACE = "runtime/settings"
        private const val WINDOW_SETTINGS_KEY = "window"
    }
}
