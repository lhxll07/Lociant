package com.mnnode.app.runtime

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import com.mnnode.app.MainActivity
import com.mnnode.app.R
import org.json.JSONObject

class MNNodeRuntimeService : Service() {
    @Volatile private var serviceMode = MODE_SERVICE
    private var floatingWindow: TextView? = null
    private var floatingParams: WindowManager.LayoutParams? = null
    private var lastFloatingTapAt = 0L

    override fun onCreate() {
        super.onCreate()
        ensureChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action ?: ACTION_START_RUNTIME) {
            ACTION_START_RUNTIME -> startRuntime(payload(intent))
            ACTION_STOP_RUNTIME -> stopRuntime()
            ACTION_SHOW_FLOATING_WINDOW -> showFloatingWindow()
            ACTION_HIDE_FLOATING_WINDOW -> hideFloatingWindow()
        }
        return START_STICKY
    }

    override fun onDestroy() {
        hideFloatingWindow()
        runCatching { MNNodeRuntime.apiServer(this).stopForService() }
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun startRuntime(payload: JSONObject) {
        serviceMode = payload.optString("mode", MODE_HEADLESS).ifBlank { MODE_HEADLESS }
        runCatching {
            startForeground(NOTIFICATION_ID, notification("Starting Lociant runtime"))
            recordLifecycle("runtime.start", payload)
            MNNodeRuntime.apiServer(this).startForService(payload)
            if (payload.optBoolean("floatingWindow", false)) showFloatingWindow()
            updateNotification()
        }.onFailure { error ->
            runCatching {
                startForeground(NOTIFICATION_ID, notification("Runtime service error: ${error.message ?: "start failed"}"))
            }
        }
    }

    private fun stopRuntime() {
        recordLifecycle("runtime.stop", JSONObject().put("mode", serviceMode))
        hideFloatingWindow()
        runCatching { MNNodeRuntime.apiServer(this).stopForService() }
        stopForegroundCompat()
        stopSelf()
    }

    private fun payload(intent: Intent?): JSONObject {
        val raw = intent?.getStringExtra(EXTRA_PAYLOAD).orEmpty()
        return runCatching { JSONObject(raw) }.getOrDefault(JSONObject())
    }

    private fun updateNotification() {
        notificationManager().notify(NOTIFICATION_ID, notification(statusText()))
        floatingWindow?.text = floatingText()
    }

    private fun recordLifecycle(type: String, payload: JSONObject) {
        runCatching {
            MNNodeRuntime.sessionStore(this).recordRuntimeEvent(
                sceneId = "runtime",
                type = type,
                payload = JSONObject(payload.toString()).put("serviceMode", serviceMode),
            )
        }
    }

    private fun statusText(): String {
        val state = MNNodeRuntime.runtimeSummary(this)
        val label = when {
            state.optBoolean("starting") -> "Starting"
            state.optBoolean("running") -> "Model server active"
            state.optString("lastError").isNotBlank() -> "Runtime error"
            else -> "Runtime service active"
        }
        return "$label · $serviceMode · ${state.optString("lanUrl", "LAN API")}"
    }

    private fun notification(text: String): Notification {
        val openIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        return Notification.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_mnnode)
            .setContentTitle("Lociant Runtime")
            .setContentText(text)
            .setContentIntent(openIntent)
            .setOngoing(true)
            .setLocalOnly(true)
            .setShowWhen(false)
            .build()
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Lociant Runtime",
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = "Keeps Lociant runtime services visible while running."
        }
        notificationManager().createNotificationChannel(channel)
    }

    private fun notificationManager(): NotificationManager {
        return getSystemService(NotificationManager::class.java)
    }

    private fun showFloatingWindow() {
        if (floatingWindow != null || !canDrawOverlay()) return
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            overlayType(),
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 24
            y = 96
        }
        val view = TextView(this).apply {
            text = floatingText()
            setTextColor(Color.WHITE)
            setTextSize(13f)
            setPadding(18, 12, 18, 12)
            setBackgroundColor(Color.argb(220, 18, 18, 18))
            setOnTouchListener(FloatingDragListener(params))
            setOnClickListener { handleFloatingClick() }
            setOnLongClickListener { hideFloatingWindow(); true }
        }
        runCatching {
            windowManager().addView(view, params)
            floatingWindow = view
            floatingParams = params
            floatingVisible = true
            updateNotification()
        }
    }

    private fun hideFloatingWindow() {
        val view = floatingWindow ?: return
        runCatching { windowManager().removeView(view) }
        floatingWindow = null
        floatingParams = null
        floatingVisible = false
    }

    private fun floatingText(): String {
        val state = MNNodeRuntime.runtimeSummary(this)
        val status = when {
            state.optBoolean("starting") -> "Starting"
            state.optBoolean("running") -> "Running"
            state.optString("lastError").isNotBlank() -> "Error"
            else -> "Stopped"
        }
        val model = state.optString("modelId", "").ifBlank { "no model" }
        val url = state.optString("lanUrl", "").ifBlank { "LAN API unavailable" }
        return "Lociant · $status\n$model · $url"
    }

    private fun canDrawOverlay() =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(this)

    private fun overlayType() =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        else WindowManager.LayoutParams.TYPE_PHONE

    private fun windowManager() = getSystemService(WindowManager::class.java)

    private fun openMainActivity() {
        startActivity(Intent(this, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP))
    }

    private fun handleFloatingClick() {
        val now = System.currentTimeMillis()
        if (now - lastFloatingTapAt < DOUBLE_TAP_MS) {
            lastFloatingTapAt = 0L
            toggleRuntime()
        } else {
            lastFloatingTapAt = now
            floatingWindow?.postDelayed({
                if (lastFloatingTapAt == now) openMainActivity()
            }, DOUBLE_TAP_MS)
        }
    }

    private fun toggleRuntime() {
        val state = MNNodeRuntime.runtimeSummary(this)
        if (state.optBoolean("running") || state.optBoolean("starting")) {
            stopRuntime()
        } else {
            startRuntime(JSONObject().put("floatingWindow", true))
        }
    }

    private fun stopForegroundCompat() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
    }

    companion object {
        private const val CHANNEL_ID = "mnnode_runtime"
        private const val NOTIFICATION_ID = 1001
        private const val EXTRA_PAYLOAD = "payload"
        private const val DOUBLE_TAP_MS = 320L
        private const val MODE_SERVICE = "service"
        private const val MODE_HEADLESS = "headless"
        private const val ACTION_START_RUNTIME = "com.mnnode.app.runtime.START_RUNTIME"
        private const val ACTION_STOP_RUNTIME = "com.mnnode.app.runtime.STOP_RUNTIME"
        private const val ACTION_SHOW_FLOATING_WINDOW = "com.mnnode.app.runtime.SHOW_FLOATING_WINDOW"
        private const val ACTION_HIDE_FLOATING_WINDOW = "com.mnnode.app.runtime.HIDE_FLOATING_WINDOW"
        @Volatile private var floatingVisible = false

        fun startRuntime(context: Context, payload: JSONObject = JSONObject()) {
            val intent = Intent(context, MNNodeRuntimeService::class.java)
                .setAction(ACTION_START_RUNTIME)
                .putExtra(EXTRA_PAYLOAD, withDefaultMode(payload).toString())
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopRuntime(context: Context) {
            context.startService(Intent(context, MNNodeRuntimeService::class.java).setAction(ACTION_STOP_RUNTIME))
        }

        fun showFloatingWindow(context: Context) {
            context.startService(Intent(context, MNNodeRuntimeService::class.java).setAction(ACTION_SHOW_FLOATING_WINDOW))
        }

        fun hideFloatingWindow(context: Context) {
            context.startService(Intent(context, MNNodeRuntimeService::class.java).setAction(ACTION_HIDE_FLOATING_WINDOW))
        }

        fun isFloatingWindowVisible() = floatingVisible

        private fun withDefaultMode(payload: JSONObject): JSONObject {
            val copy = JSONObject(payload.toString())
            if (!copy.has("mode")) copy.put("mode", MODE_HEADLESS)
            return copy
        }
    }

    private inner class FloatingDragListener(
        private val params: WindowManager.LayoutParams,
    ) : View.OnTouchListener {
        private var downRawX = 0f
        private var downRawY = 0f
        private var startX = 0
        private var startY = 0

        override fun onTouch(view: View, event: MotionEvent): Boolean {
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    downRawX = event.rawX
                    downRawY = event.rawY
                    startX = params.x
                    startY = params.y
                    return false
                }
                MotionEvent.ACTION_MOVE -> {
                    params.x = startX + (event.rawX - downRawX).toInt()
                    params.y = startY + (event.rawY - downRawY).toInt()
                    runCatching { windowManager().updateViewLayout(view, params) }
                    return true
                }
            }
            return false
        }
    }
}
