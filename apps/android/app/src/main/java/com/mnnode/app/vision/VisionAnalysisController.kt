package com.mnnode.app.vision

import android.content.Context
import android.os.SystemClock
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.mnnode.app.model.ModelManager
import com.mnnode.app.model.ModelSpec
import com.mnnode.app.model.NcnnRuntime
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class VisionAnalysisController(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner,
) {
    private val analyzerExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    private val modelManager = ModelManager(context)
    private val ncnnRuntime: NcnnRuntime = NcnnRuntime(context)
    private var cameraProvider: ProcessCameraProvider? = null
    private var state: VisionState = VisionState.Idle
    private var config: VisionConfig = VisionConfig()
    private var modelSpec: ModelSpec = modelManager.defaultVisionModel()
    private var modelState: JSONObject = JSONObject().put("loaded", false)
    private var lastDetection: JSONObject = JSONObject().put("ok", false).put("detections", JSONArray())
    private var frameCount = 0L
    private var fps = 0.0
    private var lastSampleFrame = 0L
    private var lastSampleMs = 0L
    private var lastPublishMs = 0L
    private var lastInferMs = 0L
    private var lastPreviewMs = 0L
    private var lastPreviewBase64: String? = null

    fun start(
        config: VisionConfig = VisionConfig(),
        onState: (JSONObject) -> Unit = {},
        onFrame: (JSONObject) -> Unit = {},
    ) {
        if (state == VisionState.Running || state == VisionState.Starting) {
            onState(stateJson())
            return
        }

        resetStats()
        this.config = config.normalized()
        state = VisionState.Starting
        onState(stateJson())

        val future = ProcessCameraProvider.getInstance(context)
        future.addListener({
            runCatching {
                loadModel(this.config)

                val provider = future.get()
                cameraProvider = provider

                val analysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                    .apply {
                        setAnalyzer(analyzerExecutor) { image ->
                            handleImage(image, onFrame)
                        }
                    }

                provider.unbindAll()
                provider.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    analysis,
                )

                state = VisionState.Running
                onState(stateJson())
            }.onFailure { error ->
                state = VisionState.Error
                onState(stateJson(error.message ?: "vision start failed"))
            }
        }, ContextCompat.getMainExecutor(context))
    }

    fun stop() {
        cameraProvider?.unbindAll()
        state = VisionState.Idle
        resetStats()
    }

    fun close() {
        stop()
        analyzerExecutor.shutdown()
        ncnnRuntime.close()
    }

    fun stateJson(message: String? = null): JSONObject {
        return JSONObject()
            .put("state", state.value)
            .put("running", state == VisionState.Running)
            .put("frameCount", frameCount)
            .put("fps", fps)
            .put("modelId", config.modelId)
            .put("backend", config.backend.id)
            .put("inferenceIntervalMs", config.inferenceIntervalMs)
            .put("previewIntervalMs", config.previewIntervalMs)
            .put("confidenceThreshold", config.confidenceThreshold)
            .put("model", modelState)
            .apply {
                if (!message.isNullOrBlank()) put("message", message)
            }
    }

    private fun loadModel(config: VisionConfig) {
        val model = modelManager.getSpec(config.modelId)
            ?: error("unknown model: ${config.modelId}")
        modelSpec = model
        val result = ncnnRuntime.loadModel(model, config.backend)
        modelState = result
        if (!result.optBoolean("ok", false)) {
            error(result.optString("message", "model load failed"))
        }
    }

    private fun handleImage(image: ImageProxy, onFrame: (JSONObject) -> Unit) {
        try {
            val nowMs = SystemClock.elapsedRealtime()
            frameCount += 1

            val elapsedMs = nowMs - lastSampleMs
            if (elapsedMs >= FPS_SAMPLE_INTERVAL_MS) {
                val deltaFrames = frameCount - lastSampleFrame
                fps = if (elapsedMs > 0) deltaFrames * 1000.0 / elapsedMs else 0.0
                lastSampleFrame = frameCount
                lastSampleMs = nowMs
            }

            if (nowMs - lastPublishMs >= publishIntervalMs()) {
                lastPublishMs = nowMs

                if (nowMs - lastPreviewMs >= config.previewIntervalMs) {
                    lastPreviewMs = nowMs
                    lastPreviewBase64 = runCatching { YuvPreviewEncoder.encodeJpegBase64(image) }.getOrNull()
                }

                if (modelState.optBoolean("ok", false) && nowMs - lastInferMs >= config.inferenceIntervalMs) {
                    lastInferMs = nowMs
                    lastDetection = runCatching { ncnnRuntime.detect(image, modelSpec, config.confidenceThreshold) }
                        .getOrElse { error ->
                            JSONObject()
                                .put("ok", false)
                                .put("message", error.message ?: "detect failed")
                                .put("detections", JSONArray())
                        }
                }

                onFrame(
                    JSONObject()
                        .put("state", state.value)
                        .put("frameCount", frameCount)
                        .put("width", image.width)
                        .put("height", image.height)
                        .put("rotation", image.imageInfo.rotationDegrees)
                        .put("timestamp", image.imageInfo.timestamp)
                        .put("fps", fps)
                        .put("modelId", config.modelId)
                        .put("backend", modelState.optString("actualBackend", config.backend.id))
                        .put("inferenceIntervalMs", config.inferenceIntervalMs)
                        .put("previewIntervalMs", config.previewIntervalMs)
                        .put("confidenceThreshold", config.confidenceThreshold)
                        .put("modelLoaded", modelState.optBoolean("ok", false))
                        .put("inference", lastDetection)
                        .put("detections", lastDetection.optJSONArray("detections") ?: JSONArray())
                        .apply {
                            lastPreviewBase64?.let { put("preview", "data:image/jpeg;base64,$it") }
                        }
                )
            }
        } finally {
            image.close()
        }
    }

    private fun resetStats() {
        val nowMs = SystemClock.elapsedRealtime()
        frameCount = 0L
        fps = 0.0
        lastSampleFrame = 0L
        lastSampleMs = nowMs
        lastPublishMs = 0L
        lastInferMs = 0L
        lastPreviewMs = 0L
        lastPreviewBase64 = null
        lastDetection = JSONObject().put("ok", false).put("detections", JSONArray())
    }

    private fun publishIntervalMs(): Long {
        return minOf(config.inferenceIntervalMs, config.previewIntervalMs).coerceIn(100L, 2000L)
    }

    companion object {
        private const val FPS_SAMPLE_INTERVAL_MS = 1000L
    }
}

enum class VisionState(val value: String) {
    Idle("idle"),
    Starting("starting"),
    Running("running"),
    Error("error"),
}
