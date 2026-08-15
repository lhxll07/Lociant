package io.lociant.runtime.model

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

data class ModelSpec(
    val id: String,
    val name: String,
    val runtime: String,
    val type: String,
    val source: String,
    val entry: String,
    val requiredFiles: List<String>,
    val paramAsset: String = "",
    val binAsset: String = "",
    val inputName: String = "in0",
    val outputName: String = "out0",
    val inputSize: Int = 640,
)

data class ModelStatus(
    val spec: ModelSpec,
    val ready: Boolean,
    val path: String?,
    val missingFiles: List<String>,
    val source: String = spec.source,
) {
    fun toJson(): JSONObject = JSONObject()
        .put("id", spec.id)
        .put("name", spec.name)
        .put("runtime", spec.runtime)
        .put("type", spec.type)
        .put("source", source)
        .put("entry", spec.entry)
        .put("ready", ready)
        .put("installed", path != null)
        .put("path", path ?: JSONObject.NULL)
        .put("missingFiles", JSONArray(missingFiles))
}

/**
 * Describes Android-native vision assets. LLM/GGUF models are owned by the
 * Rust llama.cpp runtime and are intentionally not parsed by this class.
 */
class ModelManager(context: Context) {
    private val appContext = context.applicationContext

    fun defaultVisionModel(): ModelSpec = knownSpecs.getValue(YOLO_ID)

    fun getSpec(id: String): ModelSpec? = knownSpecs[normalizeId(id)]

    fun resolve(id: String): ModelStatus {
        val modelId = normalizeId(id)
        val spec = getSpec(modelId) ?: unknownSpec(modelId)
        val missing = spec.requiredFiles.filterNot(::assetExists)
        return ModelStatus(
            spec = spec,
            ready = missing.isEmpty(),
            path = if (spec.source == SOURCE_ASSET) spec.entry else null,
            missingFiles = missing,
            source = spec.source,
        )
    }

    fun listModelsJson(refresh: Boolean = false): String {
        // `refresh` remains part of the IPC contract; native assets are static.
        return JSONArray(knownSpecs.keys.map { resolve(it).toJson() }).toString()
    }

    fun invalidateCache() = Unit

    private fun assetExists(path: String): Boolean =
        runCatching { appContext.assets.open(path).close() }.isSuccess

    private fun unknownSpec(id: String): ModelSpec = ModelSpec(
        id = id,
        name = id,
        runtime = "unknown",
        type = "unknown",
        source = SOURCE_EXTERNAL,
        entry = "",
        requiredFiles = emptyList(),
    )

    companion object {
        private const val YOLO_ID = "yolov8n"
        private const val SOURCE_ASSET = "asset"
        private const val SOURCE_EXTERNAL = "external"

        private val knownSpecs = mapOf(
            YOLO_ID to ModelSpec(
                id = YOLO_ID,
                name = "YOLOv8n NCNN",
                runtime = "ncnn",
                type = "detection",
                source = SOURCE_ASSET,
                entry = "models/yolov8n/yolov8n.ncnn.param",
                requiredFiles = listOf(
                    "models/yolov8n/yolov8n.ncnn.param",
                    "models/yolov8n/yolov8n.ncnn.bin",
                ),
                paramAsset = "models/yolov8n/yolov8n.ncnn.param",
                binAsset = "models/yolov8n/yolov8n.ncnn.bin",
            ),
        )

        fun normalizeId(value: String): String = value.trim()
            .lowercase()
            .replace(Regex("[\\s_]+"), "-")
            .replace(Regex("[^a-z0-9.\\-]+"), "-")
            .replace(Regex("-+"), "-")
            .trim('-', '.')
    }
}
