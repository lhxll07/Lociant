package io.lociant.runtime.vision

import android.content.Context
import android.graphics.BitmapFactory
import android.os.SystemClock
import android.util.Base64
import android.util.Size
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import io.lociant.runtime.model.ModelManager
import io.lociant.runtime.model.ModelSpec
import io.lociant.runtime.model.NcnnRuntime
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

class VisionAnalysisController(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner,
) {
    private val analyzerExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    private val inferenceExecutor: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor { r ->
        Thread(r, "lociant-inference").apply { isDaemon = true }
    }
    private val inferenceFrame = AtomicReference<InferenceFrame?>(null)
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
    private var lastPreviewBytes: ByteArray? = null
    private var lastPreviewWidth = 0
    private var lastPreviewHeight = 0
    private var lastPreviewRotation = 0
    private var lastPreviewTimestamp = 0L
    private var lastPreviewSourceWidth = 0
    private var lastPreviewSourceHeight = 0
    private var onState: (JSONObject) -> Unit = {}
    private var onFrame: (JSONObject) -> Unit = {}

    fun setCallbacks(onState: (JSONObject) -> Unit, onFrame: (JSONObject) -> Unit) {
        this.onState = onState; this.onFrame = onFrame
    }

    private fun copyInferenceFrame(image: ImageProxy) {
        if (!modelState.optBoolean("ok", false)) return
        val model = modelSpec
        val threshold = config.confidenceThreshold
        val yPlane = image.planes[0]; val uPlane = image.planes[1]; val vPlane = image.planes[2]
        inferenceFrame.set(InferenceFrame(
            width = image.width, height = image.height, rotation = image.imageInfo.rotationDegrees,
            y = ByteArray(yPlane.buffer.remaining()).also { yPlane.buffer.duplicate().get(it) },
            u = ByteArray(uPlane.buffer.remaining()).also { uPlane.buffer.duplicate().get(it) },
            v = ByteArray(vPlane.buffer.remaining()).also { vPlane.buffer.duplicate().get(it) },
            yRowStride = yPlane.rowStride, uRowStride = uPlane.rowStride, vRowStride = vPlane.rowStride,
            yPixelStride = yPlane.pixelStride, uPixelStride = uPlane.pixelStride, vPixelStride = vPlane.pixelStride,
            model = model, threshold = threshold,
        ))
    }

    private var inferenceFuture: java.util.concurrent.Future<*>? = null

    private fun startInferenceLoop() {
        inferenceFuture?.cancel(false)
        inferenceFuture = inferenceExecutor.scheduleAtFixedRate({
            val frame = inferenceFrame.getAndSet(null)
            if (frame != null) {
                val nowMs = SystemClock.elapsedRealtime()
                if (nowMs - lastInferMs >= config.inferenceIntervalMs) {
                    lastInferMs = nowMs
                    val result = runCatching {
                        ncnnRuntime.detectBytes(frame.width, frame.height, frame.rotation,
                            frame.y, frame.u, frame.v,
                            frame.yRowStride, frame.uRowStride, frame.vRowStride,
                            frame.yPixelStride, frame.uPixelStride, frame.vPixelStride,
                            frame.model, frame.threshold)
                    }.getOrElse { error ->
                        JSONObject().put("ok", false).put("message", error.message ?: "detect failed").put("detections", JSONArray())
                    }
                    lastDetection = result
                }
            }
        }, 0, config.inferenceIntervalMs, TimeUnit.MILLISECONDS)
    }

    fun previewBytes(): ByteArray? = lastPreviewBytes

    fun snapshotJson(): JSONObject {
        val image = lastPreviewBase64
        if (state != VisionState.Running) {
            return JSONObject()
                .put("ok", false)
                .put("code", "camera_not_running")
                .put("message", "Camera capture requires vision runtime to be running")
                .put("state", state.value)
        }
        if (image.isNullOrBlank()) {
            return JSONObject()
                .put("ok", false)
                .put("code", "camera_frame_unavailable")
                .put("message", "No camera frame is available yet")
                .put("state", state.value)
                .put("frameCount", frameCount)
        }
        return JSONObject()
            .put("ok", true)
            .put("state", state.value)
            .put("image", "data:image/jpeg;base64,$image")
            .put("mimeType", "image/jpeg")
            .put("width", lastPreviewWidth)
            .put("height", lastPreviewHeight)
            .put("sourceWidth", lastPreviewSourceWidth)
            .put("sourceHeight", lastPreviewSourceHeight)
            .put("rotation", lastPreviewRotation)
            .put("timestamp", lastPreviewTimestamp)
            .put("frameCount", frameCount)
            .put("fps", fps)
    }

    fun start(config: VisionConfig = VisionConfig()) {
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
                    .setTargetResolution(Size(640, 480))
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
                startInferenceLoop()
            }.onFailure { error ->
                state = VisionState.Error
                onState(stateJson(error.message ?: "vision start failed"))
            }
        }, ContextCompat.getMainExecutor(context))
    }

    fun stop() {
        runCatching { cameraProvider?.unbindAll() }
        inferenceFuture?.cancel(false)
        inferenceFuture = null
        state = VisionState.Idle
        resetStats()
    }

    fun close() {
        stop()
        analyzerExecutor.shutdown()
        inferenceExecutor.shutdown()
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
            .put("lastDetection", lastDetection)
            .apply {
                if (lastPreviewBase64 != null) put("preview", "data:image/jpeg;base64,$lastPreviewBase64")
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
            copyInferenceFrame(image)

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
                    lastPreviewBytes = runCatching { YuvPreviewEncoder.encodeJpegBytes(image) }.getOrNull()
                    lastPreviewBase64 = lastPreviewBytes?.let { Base64.encodeToString(it, Base64.NO_WRAP) }
                    if (lastPreviewBytes != null) {
                        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                        BitmapFactory.decodeByteArray(lastPreviewBytes, 0, lastPreviewBytes?.size ?: 0, bounds)
                        lastPreviewWidth = bounds.outWidth.takeIf { it > 0 } ?: image.width
                        lastPreviewHeight = bounds.outHeight.takeIf { it > 0 } ?: image.height
                        lastPreviewSourceWidth = image.width
                        lastPreviewSourceHeight = image.height
                        lastPreviewRotation = image.imageInfo.rotationDegrees
                        lastPreviewTimestamp = image.imageInfo.timestamp
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
        lastPublishMs = nowMs
        lastInferMs = nowMs
        lastPreviewMs = nowMs
        lastPreviewBase64 = null
        lastPreviewBytes = null
        lastPreviewWidth = 0
        lastPreviewHeight = 0
        lastPreviewSourceWidth = 0
        lastPreviewSourceHeight = 0
        lastPreviewRotation = 0
        lastPreviewTimestamp = 0L
        lastDetection = JSONObject().put("ok", false).put("detections", JSONArray())
    }

    private fun publishIntervalMs(): Long {
        return minOf(config.inferenceIntervalMs, config.previewIntervalMs).coerceIn(16L, 2000L)
    }

    companion object {
        private const val FPS_SAMPLE_INTERVAL_MS = 1000L
    }
}

private data class InferenceFrame(
    val width: Int, val height: Int, val rotation: Int,
    val y: ByteArray, val u: ByteArray, val v: ByteArray,
    val yRowStride: Int, val uRowStride: Int, val vRowStride: Int,
    val yPixelStride: Int, val uPixelStride: Int, val vPixelStride: Int,
    val model: io.lociant.runtime.model.ModelSpec, val threshold: Float,
)

enum class VisionState(val value: String) {
    Idle("idle"),
    Starting("starting"),
    Running("running"),
    Error("error"),
}
