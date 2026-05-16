package com.mnnode.app.vision

import com.mnnode.app.model.InferenceBackend
import org.json.JSONObject

data class VisionConfig(
    val modelId: String = "yolov8n",
    val backend: InferenceBackend = InferenceBackend.Auto,
    val inferenceIntervalMs: Long = 250L,
    val previewIntervalMs: Long = 250L,
    val confidenceThreshold: Float = 0.50f,
) {
    fun normalized(): VisionConfig {
        return copy(
            inferenceIntervalMs = inferenceIntervalMs.coerceIn(50L, 2000L),
            previewIntervalMs = previewIntervalMs.coerceIn(33L, 2000L),
            confidenceThreshold = confidenceThreshold.coerceIn(0.05f, 0.95f),
        )
    }

    companion object {
        fun fromJson(raw: String?): VisionConfig {
            if (raw.isNullOrBlank()) return VisionConfig()
            return runCatching {
                val json = JSONObject(raw)
                VisionConfig(
                    modelId = json.optString("modelId", "yolov8n"),
                    backend = InferenceBackend.from(json.optString("backend", "auto")),
                    inferenceIntervalMs = json.optLong("inferenceIntervalMs", 250L),
                    previewIntervalMs = json.optLong("previewIntervalMs", 250L),
                    confidenceThreshold = json.optDouble("confidenceThreshold", 0.50).toFloat(),
                ).normalized()
            }.getOrDefault(VisionConfig())
        }
    }
}
