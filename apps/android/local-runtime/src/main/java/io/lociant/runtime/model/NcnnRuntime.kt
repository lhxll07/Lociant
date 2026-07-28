package io.lociant.runtime.model

import android.content.Context
import androidx.camera.core.ImageProxy
import org.json.JSONObject
import java.nio.ByteBuffer

class NcnnRuntime(context: Context) : AutoCloseable {
    private val appContext = context.applicationContext
    private var handle: Long = nativeCreate(appContext.assets)

    @Synchronized
    fun loadModel(
        model: ModelSpec,
        backend: InferenceBackend = InferenceBackend.Auto,
        numThreads: Int = 2,
    ): JSONObject {
        check(handle != 0L) { "NCNN runtime is closed" }
        val raw = nativeLoadModel(
            handle,
            model.id,
            model.paramAsset,
            model.binAsset,
            backend.id,
            numThreads.coerceIn(1, 8),
        )
        return JSONObject(raw)
    }

    @Synchronized
    fun detectBytes(
        width: Int, height: Int, rotation: Int,
        y: ByteArray, u: ByteArray, v: ByteArray,
        yRowStride: Int, uRowStride: Int, vRowStride: Int,
        yPixelStride: Int, uPixelStride: Int, vPixelStride: Int,
        model: ModelSpec, confidenceThreshold: Float = 0.50f,
    ): JSONObject {
        check(handle != 0L) { "NCNN runtime is closed" }
        return JSONObject(
            nativeDetectYuv420(
                handle, width, height, rotation,
                y.toDirectBuffer(), u.toDirectBuffer(), v.toDirectBuffer(),
                yRowStride, uRowStride, vRowStride,
                yPixelStride, uPixelStride, vPixelStride,
                model.inputName, model.outputName, model.inputSize,
                confidenceThreshold.coerceIn(0.05f, 0.95f),
            )
        )
    }

    @Synchronized
    fun detect(
        image: ImageProxy,
        model: ModelSpec,
        confidenceThreshold: Float = 0.50f,
    ): JSONObject {
        check(handle != 0L) { "NCNN runtime is closed" }
        require(image.planes.size >= 3) { "YUV_420_888 image is required" }
        val y = image.planes[0]
        val u = image.planes[1]
        val v = image.planes[2]
        return JSONObject(
            nativeDetectYuv420(
                handle,
                image.width,
                image.height,
                image.imageInfo.rotationDegrees,
                y.buffer,
                u.buffer,
                v.buffer,
                y.rowStride,
                u.rowStride,
                v.rowStride,
                y.pixelStride,
                u.pixelStride,
                v.pixelStride,
                model.inputName,
                model.outputName,
                model.inputSize,
                confidenceThreshold.coerceIn(0.05f, 0.95f),
            )
        )
    }

    @Synchronized
    override fun close() {
        if (handle != 0L) {
            nativeRelease(handle)
            handle = 0L
        }
    }

    companion object {
        init {
            System.loadLibrary("lociant_ncnn")
            nativeInit()
        }

        fun gpuCount(): Int = nativeGpuCount()

        @JvmStatic private external fun nativeInit()
        @JvmStatic private external fun nativeGpuCount(): Int
        @JvmStatic private external fun nativeCreate(assetManager: android.content.res.AssetManager): Long
        @JvmStatic private external fun nativeRelease(handle: Long)
        @JvmStatic private external fun nativeLoadModel(
            handle: Long,
            modelId: String,
            paramAsset: String,
            binAsset: String,
            backend: String,
            numThreads: Int,
        ): String
        @JvmStatic private external fun nativeDetectYuv420(
            handle: Long,
            width: Int,
            height: Int,
            rotation: Int,
            yBuffer: java.nio.ByteBuffer,
            uBuffer: java.nio.ByteBuffer,
            vBuffer: java.nio.ByteBuffer,
            yRowStride: Int,
            uRowStride: Int,
            vRowStride: Int,
            yPixelStride: Int,
            uPixelStride: Int,
            vPixelStride: Int,
            inputName: String,
            outputName: String,
            inputSize: Int,
            confidenceThreshold: Float,
        ): String
        @JvmStatic private external fun nativeState(handle: Long): String
    }
}

private fun ByteArray.toDirectBuffer(): ByteBuffer {
    return ByteBuffer.allocateDirect(size).apply {
        put(this@toDirectBuffer)
        rewind()
    }
}
