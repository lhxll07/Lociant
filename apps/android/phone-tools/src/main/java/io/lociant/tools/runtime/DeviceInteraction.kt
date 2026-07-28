package io.lociant.tools.runtime

import android.app.KeyguardManager
import android.content.Context
import android.os.Build
import android.os.PowerManager
import org.json.JSONObject

object DeviceInteraction {
    @Volatile private var activityForeground = false

    fun setActivityForeground(value: Boolean) {
        activityForeground = value
    }

    fun snapshot(context: Context): JSONObject {
        val power = context.getSystemService(PowerManager::class.java)
        val keyguard = context.getSystemService(KeyguardManager::class.java)
        val interactive = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH) {
            power?.isInteractive == true
        } else {
            @Suppress("DEPRECATION")
            power?.isScreenOn == true
        }
        val locked = keyguard?.isKeyguardLocked == true
        return JSONObject()
            .put("interactive", interactive)
            .put("screenOn", interactive)
            .put("keyguardLocked", locked)
            .put("activityForeground", activityForeground)
            .put("visionInteractive", interactive && !locked)
    }
}
