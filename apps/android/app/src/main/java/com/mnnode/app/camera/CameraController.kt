package com.mnnode.app.camera

import android.content.Context
import android.os.Looper
import android.view.View
import android.widget.FrameLayout
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import org.json.JSONObject

class CameraController(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner,
    private val previewView: PreviewView,
) {
    private var cameraProvider: ProcessCameraProvider? = null
    private var state: CameraState = CameraState.Idle

    fun start(onResult: (Boolean, String) -> Unit = { _, _ -> }) {
        if (state == CameraState.Previewing || state == CameraState.Starting) {
            onResult(true, state.value)
            return
        }

        state = CameraState.Starting
        previewView.visibility = View.VISIBLE

        val future = ProcessCameraProvider.getInstance(context)
        future.addListener({
            runCatching {
                val provider = future.get()
                cameraProvider = provider

                val preview = Preview.Builder().build().apply {
                    surfaceProvider = previewView.surfaceProvider
                }

                provider.unbindAll()
                provider.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                )

                state = CameraState.Previewing
                onResult(true, state.value)
            }.onFailure { error ->
                state = CameraState.Error
                previewView.visibility = View.GONE
                onResult(false, error.message ?: "camera start failed")
            }
        }, ContextCompat.getMainExecutor(context))
    }

    fun stop() {
        cameraProvider?.unbindAll()
        previewView.visibility = View.GONE
        state = CameraState.Idle
    }

    fun setPreviewRect(x: Int, y: Int, width: Int, height: Int) {
        val safeWidth = width.coerceAtLeast(1)
        val safeHeight = height.coerceAtLeast(1)
        val applyRect = {
            previewView.layoutParams = FrameLayout.LayoutParams(safeWidth, safeHeight).apply {
                leftMargin = x.coerceAtLeast(0)
                topMargin = y.coerceAtLeast(0)
            }
            previewView.translationX = 0f
            previewView.translationY = 0f
            previewView.requestLayout()
        }

        if (Looper.myLooper() == Looper.getMainLooper()) {
            applyRect()
        } else {
            previewView.post(applyRect)
        }
    }

    fun stateJson(): String {
        return JSONObject()
            .put("state", state.value)
            .put("visible", previewView.visibility == View.VISIBLE)
            .toString()
    }
}

enum class CameraState(val value: String) {
    Idle("idle"),
    Starting("starting"),
    Previewing("previewing"),
    Error("error"),
}
