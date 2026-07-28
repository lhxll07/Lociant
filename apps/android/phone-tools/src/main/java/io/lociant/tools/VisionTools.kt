package io.lociant.tools

import android.content.Context
import io.lociant.core.tools.*
import io.lociant.tools.runtime.DeviceInteraction
import io.lociant.tools.runtime.VisionRuntime
import io.lociant.runtime.vision.VisionConfig
import org.json.JSONObject

class VisionTools(
    private val context: Context,
    private val startVisionRuntime: (JSONObject) -> Unit = {},
) : ToolProvider {
    override fun tools(): List<ToolDefinition> = listOf(
        tool(
            name = "vision_status",
            description = "Return camera and object detection runtime status.",
            policy = ToolPolicy(requiresActivity = true),
        ) {
            VisionRuntime.status().put("device", DeviceInteraction.snapshot(context))
        },
        tool(
            name = "vision_start",
            description = "Start continuous camera analysis and YOLO object detection.",
            properties = JSONObject()
                .put("modelId", stringParam())
                .put("backend", stringParam())
                .put("inferenceIntervalMs", intParam())
                .put("confidenceThreshold", numberParam()),
            policy = ToolPolicy(requiresActivity = true, sideEffect = true, openWorld = true),
        ) { args ->
            val device = DeviceInteraction.snapshot(context)
            if (!device.optBoolean("visionInteractive", false)) {
                VisionRuntime.status()
                    .put("state", "locked")
                    .put("running", false)
                    .put("message", "Vision requires an interactive unlocked device.")
                    .put("device", device)
            } else {
                val config = VisionConfig.fromJson(args.toString())
                val state = VisionRuntime.start(config)
                if (!state.optBoolean("running", false) && state.optString("state") == "starting") {
                    startVisionRuntime(JSONObject().put("visionEnabled", true))
                }
                state.put("device", device)
            }
        },
        tool(
            name = "camera_capture",
            description = "Capture the latest camera frame from the Android phone. Use this when the user asks what the phone sees or needs a current photo from the device.",
            policy = ToolPolicy(requiresActivity = true),
        ) {
            val device = DeviceInteraction.snapshot(context)
            val snapshot = VisionRuntime.snapshot()
            if (snapshot.optBoolean("ok", false)) snapshot.put("device", device)
            else snapshot.put("device", device).put("state", if (device.optBoolean("visionInteractive", false)) snapshot.optString("state", "unavailable") else "locked")
        },
        tool(
            name = "vision_stop",
            description = "Stop continuous camera analysis.",
            policy = ToolPolicy(requiresActivity = true, sideEffect = true, openWorld = true),
        ) { VisionRuntime.stop() },
    )
}
