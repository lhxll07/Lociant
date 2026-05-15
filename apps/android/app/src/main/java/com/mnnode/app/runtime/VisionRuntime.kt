package com.mnnode.app.runtime

import com.mnnode.app.camera.CameraController
import com.mnnode.app.vision.VisionAnalysisController
import com.mnnode.app.vision.VisionConfig
import org.json.JSONObject

class VisionRuntime(
    private val cameraController: CameraController,
    private val visionController: VisionAnalysisController,
    private val onCameraState: (JSONObject) -> Unit,
    private val onVisionState: (JSONObject) -> Unit,
    private val onVisionFrame: (JSONObject) -> Unit,
) {
    fun setPreviewRect(x: Int, y: Int, width: Int, height: Int) {
        cameraController.setPreviewRect(x, y, width, height)
    }

    fun startCamera(): JSONObject {
        visionController.stop()
        cameraController.start { ok, message ->
            onCameraState(
                JSONObject()
                    .put("ok", ok)
                    .put("message", message)
                    .put("state", if (ok) "previewing" else "error")
            )
        }
        return state("camera", "starting")
    }

    fun stopCamera(): JSONObject {
        cameraController.stop()
        return state("camera", "idle")
    }

    fun cameraState(): String = cameraController.stateJson()

    fun startVision(config: VisionConfig = VisionConfig()): JSONObject {
        cameraController.stop()
        visionController.start(
            config = config,
            onState = onVisionState,
            onFrame = onVisionFrame,
        )
        return state("vision", "starting")
    }

    fun stopVision(): JSONObject {
        visionController.stop()
        onVisionState(visionController.stateJson())
        return state("vision", "idle")
    }

    fun visionState(): JSONObject = visionController.stateJson()

    fun close() {
        visionController.close()
        cameraController.stop()
    }

    private fun state(domain: String, state: String): JSONObject {
        return JSONObject()
            .put("ok", true)
            .put("domain", domain)
            .put("state", state)
            .put("headlessPreviewSupported", false)
    }
}
