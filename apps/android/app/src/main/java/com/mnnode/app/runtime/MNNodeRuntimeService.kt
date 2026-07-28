package com.mnnode.app.runtime

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import com.mnnode.app.MainActivity
import com.mnnode.app.R
import com.mnnode.app.vision.VisionAnalysisController
import org.json.JSONObject
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MNNodeRuntimeService : Service(), LifecycleOwner {
    private val tag = "LociantRuntimeService"
    private val lifecycleRegistry = LifecycleRegistry(this)
    private val eventExecutor: ExecutorService = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "lociant-runtime-events").apply { isDaemon = true }
    }
    private var visionController: VisionAnalysisController? = null
    @Volatile private var serviceMode = MODE_SERVICE
    @Volatile private var visionEnabled = false

    override val lifecycle: Lifecycle
        get() = lifecycleRegistry

    override fun onCreate() {
        super.onCreate()
        lifecycleRegistry.currentState = Lifecycle.State.CREATED
        ensureChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        lifecycleRegistry.currentState = Lifecycle.State.STARTED
        when (intent?.action ?: ACTION_START_RUNTIME) {
            ACTION_START_RUNTIME -> {
                startRuntime(payload(intent))
                if (visionEnabled) attachVisionRuntime()
            }
            ACTION_STOP_RUNTIME -> stopRuntime()
        }
        return START_STICKY
    }

    override fun onDestroy() {
        runtimeWindow().hide()
        visionController?.let { VisionRuntime.detach(it) }
        visionController?.close()
        visionController = null
        runCatching { MNNodeRuntime.apiServer(this).stopForService() }
        eventExecutor.shutdown()
        lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun startRuntime(payload: JSONObject) {
        serviceMode = payload.optString("mode", MODE_HEADLESS).ifBlank { MODE_HEADLESS }
        visionEnabled = payload.optBoolean("visionEnabled", false)
        runCatching {
            startForegroundCompat(
                notification("Starting Lociant runtime"),
                includeCamera = visionEnabled && DeviceInteraction.snapshot(this).optBoolean("visionInteractive", false)
            )
            recordLifecycle("runtime.start", payload)
            MNNodeRuntime.apiServer(this).startForService(payload)
            if (payload.optBoolean("floatingWindow", false)) runtimeWindow().show()
            updateNotification()
        }.onFailure { error ->
            val message = error.message ?: "start failed"
            recordLifecycle("runtime.error", JSONObject().put("message", message))
            runCatching { startForegroundCompat(notification("Runtime service error: $message")) }
        }
    }

    private fun attachVisionRuntime() {
        if (visionController != null) return
        if (!visionEnabled) return
        if (!DeviceInteraction.snapshot(this).optBoolean("visionInteractive", false)) return
        Log.i(tag, "attachVisionRuntime lifecycle=${lifecycle.currentState}")
        visionController = VisionAnalysisController(this, this).also {
            it.setCallbacks(
                onState = { state ->
                    recordRuntimeEvent("runtime", "vision.state", payload = state)
                    runtimeWindow().refresh()
                },
                onFrame = { runtimeWindow().refresh() },
            )
        }
        visionController?.let { VisionRuntime.attach(it) }
    }

    private fun stopRuntime() {
        recordLifecycle("runtime.stop", JSONObject().put("mode", serviceMode))
        runtimeWindow().hide()
        visionEnabled = false
        runCatching { MNNodeRuntime.apiServer(this).stopForService() }
        stopForegroundCompat()
        stopSelf()
    }

    private fun payload(intent: Intent?): JSONObject {
        val raw = intent?.getStringExtra(EXTRA_PAYLOAD).orEmpty()
        return runCatching { JSONObject(raw.ifBlank { "{}" }) }.getOrDefault(JSONObject())
    }

    private fun updateNotification() {
        notificationManager().notify(NOTIFICATION_ID, notification(statusText()))
        runtimeWindow().refresh()
    }

    private fun recordLifecycle(type: String, payload: JSONObject) {
        recordRuntimeEvent(
            sceneId = "runtime",
            type = type,
            payload = JSONObject(payload.toString()).put("serviceMode", serviceMode),
        )
    }

    private fun recordRuntimeEvent(sceneId: String, type: String, level: String = "info", payload: JSONObject = JSONObject()) {
        val eventPayload = JSONObject(payload.toString())
        eventExecutor.execute {
            runCatching {
                MNNodeRuntime.sessionStore(this).recordRuntimeEvent(
                    sceneId = sceneId,
                    type = type,
                    level = level,
                    payload = eventPayload,
                )
            }
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
        return "$label - ${state.optString("lanUrl", "LAN API")}"
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
        notificationManager().createNotificationChannel(NotificationChannel(
            CHANNEL_ID,
            "Lociant Runtime",
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = "Keeps Lociant runtime services visible while running."
        })
    }

    private fun notificationManager(): NotificationManager = getSystemService(NotificationManager::class.java)

    private fun runtimeWindow() = RuntimeWindowController.get(this)

    private fun stopForegroundCompat() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) stopForeground(STOP_FOREGROUND_REMOVE)
        else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
    }

    private fun startForegroundCompat(notification: Notification, includeCamera: Boolean = false) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                if (includeCamera) {
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC or ServiceInfo.FOREGROUND_SERVICE_TYPE_CAMERA
                } else {
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
                },
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    companion object {
        private const val CHANNEL_ID = "mnnode_runtime"
        private const val NOTIFICATION_ID = 1001
        private const val EXTRA_PAYLOAD = "payload"
        private const val MODE_SERVICE = "service"
        private const val MODE_HEADLESS = "headless"
        private const val ACTION_START_RUNTIME = "com.mnnode.app.runtime.START_RUNTIME"
        private const val ACTION_STOP_RUNTIME = "com.mnnode.app.runtime.STOP_RUNTIME"

        fun startRuntime(context: Context, payload: JSONObject = JSONObject()) {
            val intent = Intent(context, MNNodeRuntimeService::class.java)
                .setAction(ACTION_START_RUNTIME)
                .putExtra(EXTRA_PAYLOAD, withDefaultMode(payload).toString())
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) context.startForegroundService(intent)
            else context.startService(intent)
        }

        fun stopRuntime(context: Context) {
            context.startService(Intent(context, MNNodeRuntimeService::class.java).setAction(ACTION_STOP_RUNTIME))
        }

        fun showFloatingWindow(context: Context): JSONObject =
            RuntimeWindowController.get(context).show()

        fun hideFloatingWindow(context: Context): JSONObject =
            RuntimeWindowController.get(context).hide()

        fun collapseFloatingWindow(context: Context): JSONObject =
            RuntimeWindowController.get(context).collapse()

        fun expandFloatingWindow(context: Context): JSONObject =
            RuntimeWindowController.get(context).expand()

        fun floatingWindowState(context: Context): JSONObject =
            RuntimeWindowController.get(context).state()

        private fun withDefaultMode(payload: JSONObject): JSONObject {
            val copy = JSONObject(payload.toString())
            if (!copy.has("mode")) copy.put("mode", MODE_HEADLESS)
            return copy
        }
    }
}
