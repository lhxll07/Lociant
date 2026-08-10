package io.lociant.android

import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.EventChannel
import io.flutter.plugin.common.MethodChannel
import org.json.JSONObject

/**
 * The single native surface exposed to the Flutter UI.
 *
 * It replaces the old WebView `LociantBridge`: one method channel for
 * Android/Activity-bound operations and one event channel for push events
 * (model install progress and runtime state refreshes). All results keep the
 * same JSON shapes the web UI used, so the Flutter client can parse them with
 * plain `dart:convert`.
 */
class LociantPlatformChannel(
    private val host: MainActivity,
    engine: FlutterEngine,
) {
    private val events = EventChannel(engine.dartExecutor.binaryMessenger, EVENTS_CHANNEL)
    private var eventSink: EventChannel.EventSink? = null

    init {
        events.setStreamHandler(object : EventChannel.StreamHandler {
            override fun onListen(arguments: Any?, events: EventChannel.EventSink) {
                eventSink = events
            }

            override fun onCancel(arguments: Any?) {
                eventSink = null
            }
        })

        MethodChannel(engine.dartExecutor.binaryMessenger, METHODS_CHANNEL)
            .setMethodCallHandler { call, result ->
                runCatching { dispatch(call.method, call.arguments as? String) }
                    .onSuccess { result.success(it) }
                    .onFailure { error ->
                        result.error("platform_error", error.message ?: "platform call failed", null)
                    }
            }
    }

    fun emitModelInstallResult(payload: JSONObject) = emit("modelInstallResult", payload)

    fun emitRuntimeMessage(payload: JSONObject) = emit("runtimeMessage", payload)

    private fun emit(type: String, payload: JSONObject) {
        val envelope = JSONObject()
            .put("type", type)
            .put("payload", payload)
        runCatching { eventSink?.success(envelope.toString()) }
    }

    private fun dispatch(method: String, payloadJson: String?): String = when (method) {
        "installModelPackage" -> host.installModelPackage()
        "requestCameraPermission" -> host.requestCameraPermission()
        "requestNotificationPermission" -> host.requestNotificationPermission()
        "requestOverlayPermission" -> host.requestOverlayPermission()
        "requestBatteryOptimizationExemption" -> host.requestBatteryOptimizationExemption()
        "requestAccessibilityPermission" -> host.requestAccessibilityPermission()
        "openAppSettings" -> host.openAppSettings()
        "openExternalUrl" -> host.openExternalUrl(stringArg(payloadJson, "url"))
        "openPermissionSettings" -> host.openPermissionSettings(stringArg(payloadJson, "kind"))
        "deviceState" -> host.deviceState()
        "startRuntime" -> host.startRuntime(payloadJson)
        "stopRuntime" -> host.stopRuntime()
        "startVision" -> host.startVision(payloadJson)
        "stopVision" -> host.stopVision()
        "showRuntimeWindow" -> host.showRuntimeWindow()
        "hideRuntimeWindow" -> host.hideRuntimeWindow()
        "updateRuntimeWindow" -> host.updateRuntimeWindow(payloadJson)
        else -> throw IllegalArgumentException("Unknown platform method: $method")
    }

    /**
     * Dart sends single-string arguments as a JSON object (`{"sessionId": "…"}`)
     * for methods that expect a plain string. Fall back to the raw payload so
     * both shapes work.
     */
    private fun stringArg(payloadJson: String?, key: String): String {
        val raw = payloadJson.orEmpty()
        if (raw.isBlank()) return ""
        val parsed = runCatching { JSONObject(raw) }.getOrNull()
        val value = parsed?.optString(key).orEmpty()
        return value.ifEmpty { raw }
    }

    companion object {
        const val METHODS_CHANNEL = "io.lociant.android/platform"
        const val EVENTS_CHANNEL = "io.lociant.android/events"
    }
}
