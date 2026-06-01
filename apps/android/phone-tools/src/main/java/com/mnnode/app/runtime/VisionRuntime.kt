package com.mnnode.app.runtime

import android.util.Log
import com.mnnode.app.vision.VisionAnalysisController
import com.mnnode.app.vision.VisionConfig
import org.json.JSONObject

object VisionRuntime {
    private const val TAG = "LociantVisionRuntime"
    @Volatile private var controller: VisionAnalysisController? = null
    @Volatile private var pendingStartConfig: VisionConfig? = null

    fun attach(controller: VisionAnalysisController) {
        this.controller = controller
        Log.i(TAG, "attach controller pending=${pendingStartConfig != null}")
        pendingStartConfig?.let {
            pendingStartConfig = null
            Log.i(TAG, "consume pending start model=${it.modelId}")
            controller.start(it)
        }
    }

    fun detach(controller: VisionAnalysisController) {
        Log.i(TAG, "detach controller")
        if (this.controller === controller) this.controller = null
    }

    fun status(): JSONObject =
        controller?.stateJson()?.put("owner", "service")?.put("pendingStart", pendingStartConfig != null) ?: JSONObject()
            .put("state", "unavailable")
            .put("running", false)
            .put("pendingStart", pendingStartConfig != null)
            .put("message", "Vision requires the interactive Activity")

    fun start(config: VisionConfig = VisionConfig()): JSONObject {
        val current = controller ?: run {
            pendingStartConfig = config
            Log.i(TAG, "queue start model=${config.modelId}")
            return status().put("state", "starting").put("message", "Vision runtime service is starting")
        }
        Log.i(TAG, "start attached model=${config.modelId}")
        current.start(config)
        return current.stateJson()
    }

    fun stop(): JSONObject {
        Log.i(TAG, "stop")
        pendingStartConfig = null
        val current = controller ?: return status()
        current.stop()
        return current.stateJson()
    }

    fun snapshot(): JSONObject {
        val current = controller
            ?: return JSONObject()
                .put("ok", false)
                .put("code", "vision_unavailable")
                .put("message", "Vision requires the interactive Activity")
        return current.snapshotJson()
    }

    fun previewBytes(): ByteArray? = controller?.previewBytes()
}
