package com.mnnode.app.server

import com.mnnode.app.vision.VisionAnalysisController
import com.mnnode.app.vision.VisionConfig
import org.json.JSONObject

class VisionTools(
    private val visionController: () -> VisionAnalysisController?,
) : ToolProvider {
    override fun tools(): List<ToolDefinition> = listOf(
        ToolDefinition(
            name = "vision_status",
            description = "Return camera and object detection runtime status.",
            parameters = objectSchema(),
            policy = ToolPolicy(requiresActivity = true),
        ) { withVision { it.stateJson() } },
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
            withVision {
                it.start(config)
                it.stateJson()
            }
        },
        ToolDefinition(
            name = "camera_capture",
            description = "Capture the latest camera frame from the Android phone. Use this when the user asks what the phone sees or needs a current photo from the device.",
            parameters = objectSchema(),
            policy = ToolPolicy(requiresActivity = true),
        ) { withVision { it.snapshotJson() } },
        ToolDefinition(
            name = "vision_stop",
            description = "Stop continuous camera analysis.",
            parameters = objectSchema(),
            policy = ToolPolicy(requiresActivity = true, sideEffect = true),
        ) { withVision { it.stop(); it.stateJson() } },
    )

    private fun withVision(block: (VisionAnalysisController) -> JSONObject): JSONObject {
        val controller = visionController()
            ?: return JSONObject()
                .put("ok", false)
                .put("code", "vision_unavailable")
                .put("message", "Vision tools require the interactive Activity to be running")
        return block(controller)
    }
}
