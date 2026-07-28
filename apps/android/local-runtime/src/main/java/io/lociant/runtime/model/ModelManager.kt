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
    private val cacheLock = Any()
    @Volatile private var cachedExternalModels: ExternalModelSnapshot? = null

    fun defaultVisionModel(): ModelSpec = knownSpecs.getValue(YOLO_ID)

    fun getSpec(id: String): ModelSpec? {
        val modelId = normalizeId(id)
        return knownSpecs[modelId] ?: externalModelSnapshot().specsById[modelId]
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

    fun listModelsJson(refresh: Boolean = false): String {
        if (refresh) invalidateCache()
        val ids = (knownSpecs.keys + externalModelSnapshot().specsById.keys).distinct()
        return JSONArray(ids.map { resolve(it).toJson() }).toString()
    }

    fun invalidateCache() {
        synchronized(cacheLock) {
            cachedExternalModels = null
        }
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

        val dirs = listOfNotNull(findInstalledDir(spec)).ifEmpty { externalDirs(spec).filter { it.exists() } }
        if (dirs.isEmpty()) {
            return JSONObject()
                .put("ok", false)
                .put("message", "Model not installed")
                .put("id", spec.id)
        }

        val failed = dirs.filterNot { it.deleteRecursively() }
        invalidateCache()
        return JSONObject()
            .put("ok", failed.isEmpty())
            .put("message", if (failed.isEmpty()) "deleted" else "Delete failed")
            .put("id", spec.id)
    }


    fun installHint(id: String): String {
        val spec = getSpec(id) ?: unknownSpec(normalizeId(id))
        return "Place model files under app external files: Android/data/${appContext.packageName}/files/models/${modelDirName(spec)}/"
    }

    fun maxNewTokens(id: String): Int? {
        val dir = resolveDir(id) ?: return null
        val value = runCatching {
            JSONObject(File(dir, "config.json").readText(Charsets.UTF_8)).optInt("max_new_tokens", 0)
        }.getOrDefault(0)
        return value.takeIf { it > 0 }
    }

    fun contextWindowTokens(id: String): Int? {
        val dir = resolveDir(id) ?: return null
        val config = runCatching { JSONObject(File(dir, "config.json").readText(Charsets.UTF_8)) }.getOrNull() ?: return null
        return listOf(
            "max_context_tokens",
            "context_window",
            "max_context_length",
            "max_position_embeddings",
            "seq_len",
            "max_seq_len",
        ).firstNotNullOfOrNull { key ->
            config.optInt(key, 0).takeIf { it > 0 }
        }
    }


    fun externalModelDir(spec: ModelSpec): File {
        val root = appContext.getExternalFilesDir(null) ?: appContext.filesDir
        return File(root, "models/${modelDirName(spec)}")
    }

    private fun missingFiles(spec: ModelSpec, externalDir: File?): List<String> {
        if (spec.source == SOURCE_ASSET) {
            return spec.requiredFiles.filterNot { assetExists(it) }
        }
        if (externalDir == null) return spec.requiredFiles
        return spec.requiredFiles.filterNot { File(externalDir, it).isFile }
    }

    private fun findInstalledDir(spec: ModelSpec): File? {
        if (spec.source != SOURCE_ASSET) {
            findExternalModelDirById(spec.id)?.let { return it }
        }
        val dirs = externalDirs(spec).filter { it.isDirectory }
        return dirs.firstOrNull { dir -> missingFiles(spec, dir).isEmpty() }
            ?: dirs.firstOrNull()
    }

    private fun externalDirs(spec: ModelSpec): List<File> {
        val dir = modelDirName(spec)
        return listOfNotNull(
            File(appContext.filesDir, "models/$dir"),
            appContext.getExternalFilesDir(null)?.let { File(it, "models/$dir") },
        )
    }

    private fun externalModelSnapshot(): ExternalModelSnapshot {
        cachedExternalModels?.let { return it }
        return synchronized(cacheLock) {
            cachedExternalModels ?: scanExternalModels().also { cachedExternalModels = it }
        }
    }

    private fun scanExternalModels(): ExternalModelSnapshot {
        val specsById = linkedMapOf<String, ModelSpec>()
        val dirsById = linkedMapOf<String, File>()
        modelRoots().flatMap(::findMnnModels).forEach { (dir, spec) ->
            if (!specsById.containsKey(spec.id)) {
                specsById[spec.id] = spec
                dirsById[spec.id] = dir
            }
        }
        return ExternalModelSnapshot(specsById.toMap(), dirsById.toMap())
    }

    private fun modelRoots(): List<File> {
        return listOfNotNull(
            File(appContext.filesDir, "models"),
            appContext.getExternalFilesDir(null)?.let { File(it, "models") },
        )
    }

    fun inferMnnSpec(dir: File, preferredName: String = ""): ModelSpec? {
        val configFile = File(dir, "config.json")
        if (!configFile.isFile) return null
        val config = runCatching { JSONObject(configFile.readText(Charsets.UTF_8)) }.getOrNull() ?: return null
        if (!looksLikeMnnConfig(dir, config)) return null
        val required = inferMnnRequiredFiles(dir, config)
        if (!required.any { it.endsWith(".mnn") && !it.contains("visual") } ||
            !required.any { it.endsWith(".weight") && !it.contains("visual") }) return null
        val baseName = cleanModelName(config.optString("model_name")
            .ifBlank { config.optString("name") }
            .ifBlank { config.optString("display_name") }
            .ifBlank { preferredName }
            .ifBlank { config.optString("model") }
            .ifBlank { readAuxModelName(dir) }
            .ifBlank { dir.name })
        val hasVisual = required.any { it.startsWith("visual") || it.contains("/visual") }
        return ModelSpec(
            id = normalizeId(baseName),
            name = baseName.ifBlank { normalizeId(dir.name) },
            runtime = "mnn",
            type = if (hasVisual) "vlm" else "llm",
            source = SOURCE_EXTERNAL,
            entry = "config.json",
            requiredFiles = required,
        )
    }

    fun writeMnnDisplayName(dir: File, preferredName: String) {
        val name = cleanModelName(preferredName)
        if (name.isBlank()) return
        val configFile = File(dir, "config.json")
        val config = runCatching { JSONObject(configFile.readText(Charsets.UTF_8)) }.getOrNull() ?: return
        val existing = cleanModelName(config.optString("model_name")
            .ifBlank { config.optString("name") }
            .ifBlank { config.optString("display_name") })
        if (existing.isBlank() || existing.startsWith("market-", ignoreCase = true) || existing.startsWith("install-", ignoreCase = true)) {
            config.put("model_name", name)
            configFile.writeText(config.toString(2), Charsets.UTF_8)
        }
    }

    private fun inferMnnRequiredFiles(dir: File, config: JSONObject): List<String> {
        val files = linkedSetOf("config.json")
        addConfiguredOrFallback(files, dir, config.optString("llm_model"), "llm.mnn", required = true)
        addConfiguredOrFallback(files, dir, config.optString("llm_weight"), "llm.mnn.weight", required = true)
        if (files.none { it.endsWith(".mnn") && !it.contains("visual") }) {
            dir.listFiles()?.firstOrNull { it.isFile && it.name.endsWith(".mnn") && !it.name.contains("visual", ignoreCase = true) }
                ?.let { files += it.name }
        }
        if (files.none { it.endsWith(".weight") && !it.contains("visual") }) {
            dir.listFiles()?.firstOrNull { it.isFile && it.name.endsWith(".weight") && !it.name.contains("visual", ignoreCase = true) }
                ?.let { files += it.name }
        }
        addConfiguredOrFallback(files, dir, config.optString("tokenizer_file").ifBlank { config.optString("tokenizer") }, "tokenizer.txt", required = true)
        if (File(dir, "llm_config.json").isFile) files += "llm_config.json"
        val visual = config.optString("visual_model").trim().replace('\\', '/').trimStart('/')
        if (visual.isNotBlank() && File(dir, visual).isFile) {
            files += visual
            val visualWeight = "$visual.weight"
            if (File(dir, visualWeight).isFile) files += visualWeight
        } else if (File(dir, "visual.mnn").isFile) {
            files += "visual.mnn"
            if (File(dir, "visual.mnn.weight").isFile) files += "visual.mnn.weight"
        }
        return files.toList()
    }

    private fun looksLikeMnnConfig(dir: File, config: JSONObject): Boolean {
        return listOf("llm_model", "llm_weight", "tokenizer_file", "tokenizer").any { config.optString(it).isNotBlank() } ||
            File(dir, "llm.mnn").isFile ||
            File(dir, "llm.mnn.weight").isFile ||
            File(dir, "llm_config.json").isFile
    }

    private fun readAuxModelName(dir: File): String {
        return listOf("configuration.json", "llm_config.json")
            .asSequence()
            .map { File(dir, it) }
            .filter { it.isFile }
            .mapNotNull { file -> runCatching { JSONObject(file.readText(Charsets.UTF_8)) }.getOrNull() }
            .mapNotNull { json ->
                json.optString("model_name")
                    .ifBlank { json.optString("name") }
                    .ifBlank { json.optString("_name_or_path").substringAfterLast('/') }
                    .ifBlank { json.optJSONArray("architectures")?.optString(0).orEmpty() }
                    .takeIf { it.isNotBlank() }
            }
            .firstOrNull()
            .orEmpty()
    }

    private fun addConfiguredOrFallback(files: MutableSet<String>, dir: File, configured: String, fallback: String, required: Boolean) {
        val value = configured.trim().replace('\\', '/').trimStart('/')
        when {
            value.isNotBlank() -> files += value
            File(dir, fallback).isFile -> files += fallback
            required -> files += fallback
        }
    }

    private fun cleanModelName(value: String): String {
        return value.trim()
            .replace("___", ".")
            .replace('_', '-')
            .replace(Regex("-+"), "-")
            .trim('-', '.', ' ')
    }

    private fun assetExists(path: String): Boolean {
        return runCatching { appContext.assets.open(path).close() }.isSuccess
    }

    fun findMnnModelDirs(root: File): List<File> {
        return findMnnModels(root).map { it.first }
    }

    private fun findMnnModels(root: File): List<Pair<File, ModelSpec>> {
        if (!root.isDirectory) return emptyList()
        val result = mutableListOf<Pair<File, ModelSpec>>()
        fun visit(dir: File, depth: Int) {
            if (depth > MAX_MODEL_SCAN_DEPTH) return
            val spec = inferMnnSpec(dir)
            if (spec != null) {
                result += dir to spec
                return
            }
            dir.listFiles()
                ?.filter { it.isDirectory && !it.name.startsWith(".") }
                ?.forEach { visit(it, depth + 1) }
        }
        visit(root, 0)
        return result
    }

    private fun findExternalModelDirById(id: String): File? {
        return externalModelSnapshot().dirsById[normalizeId(id)]
    }

    private fun modelDirName(spec: ModelSpec): String {
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
        private const val SOURCE_ASSET = "asset"
        private const val SOURCE_EXTERNAL = "external"
        private const val MAX_MODEL_SCAN_DEPTH = 3

        private val knownDirs = mapOf(
            YOLO_ID to "yolov8n",
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
        )

        fun normalizeId(value: String): String {
            return value.trim()
                .lowercase()
                .replace(Regex("[\\s_]+"), "-")
                .replace(Regex("[^a-z0-9.\\-]+"), "-")
                .replace(Regex("-+"), "-")
                .trim('-', '.')
        }
    }

    private data class ExternalModelSnapshot(
        val specsById: Map<String, ModelSpec>,
        val dirsById: Map<String, File>,
    )
}
