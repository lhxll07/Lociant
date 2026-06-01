package com.mnnode.app.server

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

class NotificationTools(
    private val context: Context,
) : ToolProvider, AutoCloseable {
    private val notifId = AtomicInteger(2001)
    private val webhookExecutor = Executors.newSingleThreadExecutor { r ->
        Thread(r, "mnnode-tool-webhook").apply { isDaemon = true }
    }

    init { ensureNotificationChannel() }

    override fun tools(): List<ToolDefinition> = listOf(
        tool(
            name = "notification_post",
            description = "Post a local Android notification. Useful for alerts triggered by sensor conditions.",
            properties = JSONObject()
                .put("title", stringParam("Notification title"))
                .put("body", stringParam("Notification body text")),
            policy = ToolPolicy(sideEffect = true),
        ) { args -> postNotification(args) },
        tool(
            name = "webhook_post",
            description = "POST JSON to an external URL. Used by triggers to notify external agents or a custom server.",
            properties = JSONObject()
                .put("url", stringParam("Target webhook URL"))
                .put("payload", objectParam("JSON payload to POST")),
            policy = ToolPolicy(sideEffect = true),
        ) { args -> callWebhook(args) },
    )

    override fun close() {
        webhookExecutor.shutdownNow()
    }

    private fun postNotification(args: JSONObject): JSONObject {
        val title = args.optString("title", "Lociant")
        val body = args.optString("body", "")
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val n = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            android.app.Notification.Builder(context, NOTIFICATION_CHANNEL_ID)
        } else {
            @Suppress("DEPRECATION")
            android.app.Notification.Builder(context)
        }
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .build()
        nm.notify(notifId.incrementAndGet(), n)
        return JSONObject().put("ok", true).put("action", "notified")
    }

    private fun callWebhook(args: JSONObject): JSONObject {
        val rawUrl = args.optString("url", "")
        val payload = args.optJSONObject("payload") ?: JSONObject()
        require(rawUrl.isNotBlank()) { "webhook url is required" }
        webhookExecutor.execute {
            runCatching {
                val conn = URL(rawUrl).openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.setRequestProperty("Content-Type", "application/json")
                conn.doOutput = true
                conn.connectTimeout = 5000
                conn.readTimeout = 5000
                conn.outputStream.use { it.write(payload.toString().toByteArray(Charsets.UTF_8)) }
                conn.responseCode
                conn.disconnect()
            }
        }
        return JSONObject().put("ok", true).put("action", "webhook_queued")
    }

    private fun ensureNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(NotificationChannel(
            NOTIFICATION_CHANNEL_ID, "Lociant Triggers", NotificationManager.IMPORTANCE_DEFAULT,
        ).apply { description = "Scene trigger notifications" })
    }

    companion object {
        private const val NOTIFICATION_CHANNEL_ID = "mnnode_triggers"
    }
}
