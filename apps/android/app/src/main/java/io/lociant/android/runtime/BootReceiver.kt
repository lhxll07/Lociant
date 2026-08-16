package io.lociant.android.runtime

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.util.Log
import io.lociant.core.config.RuntimeDefaults
import io.lociant.data.storage.LocalStore
import org.json.JSONObject

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != Intent.ACTION_BOOT_COMPLETED) return
        val appContext = context.applicationContext
        val store = localStore(appContext)
        val settings = store.getObject(SERVER_SETTINGS_NAMESPACE, SERVER_SETTINGS_KEY)
        if (settings.optBoolean("autoStart", false)) {
            val windowSettings = store.getObject(WINDOW_SETTINGS_NAMESPACE, WINDOW_SETTINGS_KEY)
            val payload = JSONObject()
                .put("mode", "headless")
            if (windowSettings.optBoolean("autoShow", false) && Settings.canDrawOverlays(appContext)) {
                payload.put("floatingWindow", true)
            }
            runCatching { LociantRuntimeService.startRuntime(appContext, payload) }
                .onFailure { error -> Log.w(TAG, "runtime boot start failed", error) }
        }

    }

    private fun localStore(context: Context): LocalStore = LociantRuntime.localStore(context)

    companion object {
        private const val TAG = "LociantBootReceiver"
        private const val SERVER_SETTINGS_NAMESPACE = RuntimeDefaults.Settings.SERVER_NAMESPACE
        private const val SERVER_SETTINGS_KEY = RuntimeDefaults.Settings.SERVER_KEY
        private const val WINDOW_SETTINGS_NAMESPACE = RuntimeDefaults.Settings.WINDOW_NAMESPACE
        private const val WINDOW_SETTINGS_KEY = RuntimeDefaults.Settings.WINDOW_KEY
    }
}
