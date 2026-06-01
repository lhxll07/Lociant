package com.mnnode.app.vision

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Matrix
import android.graphics.Rect
import android.graphics.YuvImage
import android.util.Base64
import androidx.camera.core.ImageProxy
import java.io.ByteArrayOutputStream
import kotlin.math.roundToInt

object YuvPreviewEncoder {
    fun encodeJpegBase64(
        image: ImageProxy,
        maxWidth: Int = 640,
        quality: Int = 58,
    ): String? {
        val bytes = encodeJpegBytes(image, maxWidth, quality) ?: return null
        return Base64.encodeToString(bytes, Base64.NO_WRAP)
    }

    fun encodeJpegBytes(
        image: ImageProxy,
        maxWidth: Int = 640,
        quality: Int = 58,
    ): ByteArray? {
        val jpeg = yuvToJpeg(image, quality.coerceIn(1, 100)) ?: return null
        return rotateAndScale(jpeg, image.imageInfo.rotationDegrees, maxWidth, quality) ?: jpeg
    }

    private fun yuvToJpeg(image: ImageProxy, quality: Int): ByteArray? {
        val nv21 = toNv21(image)
        val out = ByteArrayOutputStream()
        val ok = YuvImage(nv21, ImageFormat.NV21, image.width, image.height, null)
            .compressToJpeg(Rect(0, 0, image.width, image.height), quality, out)
        return if (ok) out.toByteArray() else null
    }

    private fun rotateAndScale(
        jpeg: ByteArray,
        rotation: Int,
        maxWidth: Int,
        quality: Int,
    ): ByteArray? {
        val source = BitmapFactory.decodeByteArray(jpeg, 0, jpeg.size) ?: return null
        val rotated = if (rotation % 360 != 0) {
            Bitmap.createBitmap(source, 0, 0, source.width, source.height, Matrix().apply {
                postRotate(rotation.toFloat())
            }, true)
        } else {
            source
        }

        val finalBitmap = if (rotated.width > maxWidth) {
            val scale = maxWidth.toFloat() / rotated.width
            Bitmap.createScaledBitmap(rotated, maxWidth, (rotated.height * scale).roundToInt(), true)
        } else {
            rotated
        }

        val out = ByteArrayOutputStream()
        finalBitmap.compress(Bitmap.CompressFormat.JPEG, quality, out)

        if (finalBitmap !== rotated) finalBitmap.recycle()
        if (rotated !== source) rotated.recycle()
        source.recycle()

        return out.toByteArray()
    }

    private fun toNv21(image: ImageProxy): ByteArray {
        val width = image.width
        val height = image.height
        val yPlane = image.planes[0]
        val uPlane = image.planes[1]
        val vPlane = image.planes[2]
        val yBuffer = yPlane.buffer.duplicate()
        val uBuffer = uPlane.buffer.duplicate()
        val vBuffer = vPlane.buffer.duplicate()

        val nv21 = ByteArray(width * height * 3 / 2)
        var output = 0

        for (row in 0 until height) {
            val rowStart = row * yPlane.rowStride
            for (col in 0 until width) {
                nv21[output++] = yBuffer.get(rowStart + col * yPlane.pixelStride)
            }
        }

        val chromaHeight = height / 2
        val chromaWidth = width / 2
        for (row in 0 until chromaHeight) {
            val uRowStart = row * uPlane.rowStride
            val vRowStart = row * vPlane.rowStride
            for (col in 0 until chromaWidth) {
                nv21[output++] = vBuffer.get(vRowStart + col * vPlane.pixelStride)
                nv21[output++] = uBuffer.get(uRowStart + col * uPlane.pixelStride)
            }
        }

        return nv21
    }
}
