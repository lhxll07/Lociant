package com.mnnode.app.server

import android.content.Context
import com.mnnode.app.runtime.MNNodeRuntimeService
import com.mnnode.app.runtime.VisionRuntime
import com.mnnode.app.vision.VisionConfig
import org.json.JSONObject

class VisionTools(
    private val context: Context,
) : ToolProvider {
    override fun tools(): List<ToolDefinition> = listOf(
        ToolDefinition(
            name = "vision_status",
            description = "Return camera and object detection runtime status.",
            parameters = objectSchema(),
            policy = ToolPolicy(requiresActivity = true),
        ) { VisionRuntime.status() },
        ToolDefinition(
            name = "vision_start",
            description = "Start continuous camera analysis and YOLO object detection.",
            parameters = objectSchema(JSONObject()
                .put("modelId", JSONObject().put("type", "string"))
                .put("backend", JSONObject().put("type", "string"))
                .put("inferenceIntervalMs", JSONObject().put("type", "integer"))
                .put("confidenceThreshold", JSONObject().put("type", "number"))),
            policy = ToolPolicy(requiresActivity = true, sideEffect = true),
        ) { args ->
            val config = VisionConfig.fromJson(args.toString())
            val state = VisionRuntime.start(config)
            if (!state.optBoolean("running", false) && state.optString("state") == "starting") {
                MNNodeRuntimeService.startRuntime(context, JSONObject().put("floatingWindow", true))
            }
            state
        },
        ToolDefinition(
            name = "camera_capture",
            description = "Capture the latest camera frame from the Android phone. Use this when the user asks what the phone sees or needs a current photo from the device.",
            parameters = objectSchema(),
            policy = ToolPolicy(requiresActivity = true),
        ) { VisionRuntime.snapshot() },
        ToolDefinition(
            name = "vision_stop",
            description = "Stop continuous camera analysis.",
            parameters = objectSchema(),
            policy = ToolPolicy(requiresActivity = true, sideEffect = true),
        ) { VisionRuntime.stop() },
    )
}
