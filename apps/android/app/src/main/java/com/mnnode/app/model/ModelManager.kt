package com.mnnode.app.model

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
    fun toJson(): JSONObject {
        return JSONObject()
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
}

class ModelManager(context: Context) {
    private val appContext = context.applicationContext

    fun defaultVisionModel(): ModelSpec = knownSpecs.getValue(YOLO_ID)

    fun knownSpec(id: String): ModelSpec? = knownSpecs[normalizeId(id)]

    fun getSpec(id: String): ModelSpec? {
        return knownSpecs[normalizeId(id)] ?: scanExternalSpecs().firstOrNull { it.id == normalizeId(id) }
    }

    fun resolve(id: String): ModelStatus {
        val modelId = normalizeId(id)
        val spec = getSpec(modelId) ?: unknownSpec(modelId)
        val external = findInstalledDir(spec)
        val path = when {
            spec.source == SOURCE_ASSET -> spec.entry.substringBeforeLast('/', missingDelimiterValue = spec.entry)
            external != null -> external.absolutePath
            else -> null
        }
        val missing = missingFiles(spec, external)
        return ModelStatus(
            spec = spec,
            ready = missing.isEmpty(),
            path = path,
            missingFiles = missing,
            source = if (spec.source == SOURCE_ASSET) SOURCE_ASSET else if (external != null) SOURCE_EXTERNAL else spec.source,
        )
    }

    fun resolveDir(id: String): File? {
        val status = resolve(id)
        if (!status.ready || status.source == SOURCE_ASSET) return null
        return status.path?.let(::File)
    }

    fun listModelsJson(): String {
        val ids = (knownSpecs.keys + scanExternalSpecs().map { it.id }).distinct()
        return JSONArray(ids.map { resolve(it).toJson() }).toString()
    }

    fun deleteModel(id: String): JSONObject {
        val spec = getSpec(id) ?: return JSONObject()
            .put("ok", false)
            .put("message", "Model not found")
            .put("id", normalizeId(id))

        if (spec.source == SOURCE_ASSET) {
            return JSONObject()
                .put("ok", false)
                .put("message", "Built-in model cannot be deleted")
                .put("id", spec.id)
        }

        val dirs = externalDirs(spec).filter { it.exists() }
        if (dirs.isEmpty()) {
            return JSONObject()
                .put("ok", false)
                .put("message", "Model not installed")
                .put("id", spec.id)
        }

        val failed = dirs.filterNot { it.deleteRecursively() }
        return JSONObject()
            .put("ok", failed.isEmpty())
            .put("message", if (failed.isEmpty()) "deleted" else "Delete failed")
            .put("id", spec.id)
    }


    fun installHint(id: String): String {
        val spec = getSpec(id) ?: unknownSpec(normalizeId(id))
        return "Place model files under app external files: Android/data/${appContext.packageName}/files/models/${dirName(spec)}/"
    }

    fun maxNewTokens(id: String): Int? {
        val dir = resolveDir(id) ?: return null
        val value = runCatching {
            JSONObject(File(dir, "config.json").readText(Charsets.UTF_8)).optInt("max_new_tokens", 0)
        }.getOrDefault(0)
        return value.takeIf { it > 0 }
    }


    fun externalModelDir(spec: ModelSpec): File {
        val root = appContext.getExternalFilesDir(null) ?: appContext.filesDir
        return File(root, "models/${dirName(spec)}")
    }

    private fun missingFiles(spec: ModelSpec, externalDir: File?): List<String> {
        if (spec.source == SOURCE_ASSET) {
            return spec.requiredFiles.filterNot { assetExists(it) }
        }
        if (externalDir == null) return spec.requiredFiles
        return spec.requiredFiles.filterNot { File(externalDir, it).isFile }
    }

    private fun findInstalledDir(spec: ModelSpec): File? {
        val dirs = externalDirs(spec).filter { it.isDirectory }
        return dirs.firstOrNull { dir -> missingFiles(spec, dir).isEmpty() }
            ?: dirs.firstOrNull()
    }

    private fun externalDirs(spec: ModelSpec): List<File> {
        val dir = dirName(spec)
        return listOfNotNull(
            File(appContext.filesDir, "models/$dir"),
            appContext.getExternalFilesDir(null)?.let { File(it, "models/$dir") },
        )
    }

    private fun scanExternalSpecs(): List<ModelSpec> {
        return modelRoots()
            .flatMap { root -> root.listFiles()?.filter { it.isDirectory }.orEmpty() }
            .mapNotNull { readModelJson(it) }
            .distinctBy { it.id }
    }

    private fun modelRoots(): List<File> {
        return listOfNotNull(
            File(appContext.filesDir, "models"),
            appContext.getExternalFilesDir(null)?.let { File(it, "models") },
        )
    }

    private fun readModelJson(dir: File): ModelSpec? {
        val json = runCatching { JSONObject(File(dir, "model.json").readText()) }.getOrNull() ?: return null
        val id = normalizeId(json.optString("id"))
        if (id.isBlank()) return null
        val required = json.optJSONArray("requiredFiles")?.let { array ->
            List(array.length()) { array.optString(it) }.filter { it.isNotBlank() }
        }.orEmpty()
        return ModelSpec(
            id = id,
            name = json.optString("name", id),
            runtime = json.optString("runtime", "unknown"),
            type = json.optString("type", "unknown"),
            source = SOURCE_EXTERNAL,
            entry = json.optString("entry", required.firstOrNull().orEmpty()),
            requiredFiles = required,
        )
    }

    private fun assetExists(path: String): Boolean {
        return runCatching { appContext.assets.open(path).close() }.isSuccess
    }

    private fun dirName(spec: ModelSpec): String {
        return knownDirs[spec.id] ?: spec.id
    }

    private fun unknownSpec(id: String): ModelSpec {
        return ModelSpec(
            id = id,
            name = id,
            runtime = "unknown",
            type = "unknown",
            source = SOURCE_EXTERNAL,
            entry = "",
            requiredFiles = emptyList(),
        )
    }

    companion object {
        private const val YOLO_ID = "yolov8n"
        private const val QWEN_ID = "qwen3.5-2b-mnn"
        private const val SOURCE_ASSET = "asset"
        private const val SOURCE_EXTERNAL = "external"

        private val knownDirs = mapOf(
            YOLO_ID to "yolov8n",
            QWEN_ID to "Qwen3.5-2B-MNN",
        )

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
            QWEN_ID to ModelSpec(
                id = QWEN_ID,
                name = "Qwen 3.5 2B MNN",
                runtime = "mnn",
                type = "vlm",
                source = SOURCE_EXTERNAL,
                entry = "config.json",
                requiredFiles = listOf(
                    "config.json",
                    "llm.mnn",
                    "llm.mnn.weight",
                    "llm_config.json",
                    "tokenizer.txt",
                    "visual.mnn",
                    "visual.mnn.weight",
                ),
            ),
        )

        fun normalizeId(value: String): String {
            return value.trim().lowercase().replace('_', '-')
        }
    }
}

