package com.mnnode.app.model

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors

class ModelMarket(
    private val context: Context,
    private val modelManager: ModelManager,
) {
    private val installTasks = ConcurrentHashMap<String, InstallTask>()
    @Volatile private var cachedCatalog: List<MarketRepo>? = null
    private val installExecutor = Executors.newCachedThreadPool { runnable ->
        Thread(runnable, "mnnode-market-install").apply { isDaemon = true }
    }

    fun catalog(query: String = "", refresh: Boolean = false): JSONArray {
        val items = loadMarketData(refresh)
            .filter { matchesQuery(it, query) }
            .sortedWith(compareByDescending<MarketRepo> { it.fileSize }.thenBy { it.vendor }.thenBy { it.modelName })
        val result = JSONArray()
        items.forEach { repo ->
            result.put(repo.toJson().put("installed", modelManager.getSpec(ModelManager.normalizeId(repo.modelName)) != null))
        }
        return result
    }

    fun install(repoId: String, onProgress: (Double?, String) -> Unit = { _, _ -> }): ModelStatus {
        val repo = loadMarketData().firstOrNull {
            it.modelId.equals(repoId, ignoreCase = true) ||
                it.modelName.equals(repoId, ignoreCase = true) ||
                it.repoPath.equals(repoId, ignoreCase = true)
        } ?: error("Unknown ModelScope model: $repoId")
        updateTask(repo.modelId, 0.0, "Starting")
        return installRepo(repo, onProgress)
    }

    fun installProgress(modelId: String): JSONObject? {
        val task = installTasks[normalizeId(modelId)] ?: return null
        return JSONObject()
            .put("modelId", normalizeId(modelId))
            .put("progress", task.progress)
            .put("message", task.message)
            .put("active", task.active)
    }

    fun installAsync(repoId: String): JSONObject {
        val repo = loadMarketData().firstOrNull {
            it.modelId.equals(repoId, ignoreCase = true) ||
                it.modelName.equals(repoId, ignoreCase = true) ||
                it.repoPath.equals(repoId, ignoreCase = true)
        } ?: error("Unknown ModelScope model: $repoId")
        updateTask(repo.modelId, 0.0, "Starting")
        installExecutor.execute {
            try {
                installRepo(repo) { progress, message ->
                    updateTask(repo.modelId, progress, message)
                }
            } catch (_: Throwable) {
                // task state already updated by installRepo
            }
        }
        return JSONObject()
            .put("ok", true)
            .put("modelId", repo.modelId)
            .put("message", "installing")
    }

    private fun installRepo(repo: MarketRepo, onProgress: (Double?, String) -> Unit = { _, _ -> }): ModelStatus {
        val files = repoFiles(repo.repoPath)
        val selected = selectMnnFiles(files)
        require(selected.any { it.path == "config.json" }) { "ModelScope repo has no config.json" }
        require(selected.any { it.path.endsWith(".mnn") && !it.path.contains("visual", ignoreCase = true) }) { "ModelScope repo has no LLM MNN file" }
        require(selected.any { it.path.endsWith(".weight") && !it.path.contains("visual", ignoreCase = true) }) { "ModelScope repo has no LLM weight file" }

        val installParent = File((context.getExternalFilesDir(null) ?: context.filesDir), "models").apply { mkdirs() }
        val tempRoot = File(installParent, ".market-${System.currentTimeMillis()}").apply { mkdirs() }
        var targetDir: File? = null
        var backupDir: File? = null
        var targetPrepared = false

        try {
            val total = selected.sumOf { it.size.coerceAtLeast(0L) }.takeIf { it > 0L }
            var downloaded = 0L
            selected.forEach { file ->
                val outFile = File(tempRoot, file.path)
                require(isInside(tempRoot, outFile)) { "Invalid model file path: ${file.path}" }
                outFile.parentFile?.mkdirs()
                downloadToFile(resolveUrl(repo.repoPath, file.path), outFile, file.sha256) { delta ->
                    downloaded += delta
                    updateTask(repo.modelId, total?.let { downloaded.toDouble() / it.toDouble() }?.coerceIn(0.0, 0.94), "Downloading ${file.path.substringAfterLast('/')}")
                    onProgress(total?.let { downloaded.toDouble() / it.toDouble() }?.coerceIn(0.0, 0.94), "Downloading ${file.path.substringAfterLast('/')}")
                }
            }
            val modelRoot = modelManager.findMnnModelDirs(tempRoot).singleOrNull()
                ?: error("Downloaded files do not form a valid MNN model")
            modelManager.writeMnnDisplayName(modelRoot, repo.modelName)
            val spec = modelManager.inferMnnSpec(modelRoot, repo.modelName) ?: error("unknown MNN model package")
            targetDir = modelManager.externalModelDir(spec)
            backupDir = File(installParent, ".backup-${spec.id}").apply { deleteRecursively() }

            updateTask(repo.modelId, 0.96, "Preparing model")
            onProgress(0.96, "Preparing model")
            if (targetDir.exists()) require(targetDir.renameTo(backupDir)) { "Cannot replace existing model" }
            targetPrepared = true
            if (!modelRoot.renameTo(targetDir)) {
                updateTask(repo.modelId, null, "Writing model files")
                onProgress(null, "Writing model files")
                modelRoot.copyRecursively(targetDir, overwrite = true)
            }
            tempRoot.deleteRecursively()
            backupDir.deleteRecursively()
            updateTask(repo.modelId, 1.0, "Model installed", false)
            onProgress(1.0, "Model installed")
            return modelManager.resolve(spec.id)
        } catch (error: Throwable) {
            if (targetPrepared) targetDir?.deleteRecursively()
            val target = targetDir
            if (target != null) backupDir?.takeIf { it.exists() }?.renameTo(target)
            tempRoot.deleteRecursively()
            updateTask(repo.modelId, null, error.message ?: "Model install failed", false)
            throw error
        }
    }

    private fun loadMarketData(refresh: Boolean = false): List<MarketRepo> {
        if (!refresh) cachedCatalog?.let { return it }
        val json = JSONObject(httpGet(MARKET_JSON_URL))
        val all = mutableListOf<MarketRepo>()
        all += parseSection(json.optJSONArray("models"), "models")
        all += parseSection(json.optJSONArray("libs"), "libs")
        return all.distinctBy { it.modelId }.also { cachedCatalog = it }
    }

    private fun parseSection(array: JSONArray?, section: String): List<MarketRepo> {
        if (array == null) return emptyList()
        return List(array.length()) { index -> array.optJSONObject(index) }
            .filterNotNull()
            .mapNotNull { item ->
                val modelName = item.optString("modelName").trim()
                val sources = item.optJSONObject("sources") ?: return@mapNotNull null
                val repoPath = sources.optString("ModelScope").trim()
                if (modelName.isBlank() || repoPath.isBlank()) return@mapNotNull null
                val modelId = normalizeId(modelName)
                MarketRepo(
                    modelId = modelId,
                    repoPath = repoPath,
                    modelName = modelName,
                    description = item.optString("description").ifBlank { buildDescription(item, section) },
                    tags = readTags(item),
                    vendor = item.optString("vendor").ifBlank { item.optString("modelType").ifBlank { section } },
                    sizeGb = item.optDouble("size_gb", 0.0),
                    fileSize = item.optLong("file_size", 0L),
                    source = "modelscope",
                )
            }
    }

    private fun readTags(item: JSONObject): List<String> {
        val tags = mutableListOf<String>()
        val categories = item.optJSONArray("categories")
        val extraTags = item.optJSONArray("tags")
        for (array in listOf(categories, extraTags)) {
            if (array == null) continue
            for (i in 0 until array.length()) {
                val tag = array.optString(i).trim()
                if (tag.isNotBlank()) tags += tag
            }
        }
        return tags.distinct()
    }

    private fun buildDescription(item: JSONObject, section: String): String {
        val tags = readTags(item)
        val size = item.optDouble("size_gb", 0.0)
        return buildString {
            append(if (section == "libs") "ModelScope runtime library" else "ModelScope MNN model")
            if (size > 0) append(" · ").append(size).append("B")
            if (tags.isNotEmpty()) append(" · ").append(tags.take(4).joinToString(", "))
        }
    }

    private fun matchesQuery(repo: MarketRepo, query: String): Boolean {
        val q = query.trim().lowercase()
        if (q.isBlank()) return true
        return listOf(repo.modelId, repo.repoPath, repo.modelName, repo.vendor, repo.description, repo.tags.joinToString(" "))
            .joinToString(" ")
            .lowercase()
            .contains(q)
    }

    private fun updateTask(modelId: String, progress: Double?, message: String, active: Boolean = true) {
        installTasks[normalizeId(modelId)] = InstallTask(progress, message, active)
    }

    private fun repoFiles(repo: String): List<MarketFile> {
        val json = JSONObject(httpGet("https://modelscope.cn/api/v1/models/$repo/repo/files?Revision=master&Recursive=true"))
        val files = json.optJSONObject("Data")?.optJSONArray("Files") ?: JSONArray()
        return List(files.length()) { index -> files.optJSONObject(index) }
            .filterNotNull()
            .filter { it.optString("Type") == "blob" }
            .map {
                MarketFile(
                    path = it.optString("Path").replace('\\', '/').trimStart('/'),
                    size = it.optLong("Size", 0L),
                    sha256 = it.optString("Sha256"),
                )
            }
            .filter { it.path.isNotBlank() && isSafeRelativePath(it.path) }
    }

    private fun selectMnnFiles(files: List<MarketFile>): List<MarketFile> {
        val names = setOf("config.json", "configuration.json", "llm_config.json", "tokenizer.txt", "tokenizer.json")
        return files.filter { file ->
            val lower = file.path.lowercase()
            lower in names ||
                lower.endsWith(".mnn") ||
                lower.endsWith(".mnn.weight") ||
                lower.endsWith(".weight")
        }
    }

    private fun httpGet(url: String): String {
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.connectTimeout = NETWORK_TIMEOUT_MS
        conn.readTimeout = NETWORK_TIMEOUT_MS
        conn.requestMethod = "GET"
        conn.setRequestProperty("User-Agent", "Lociant/${context.packageName}")
        return conn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
    }

    private fun downloadToFile(url: String, target: File, sha256: String, onBytes: (Long) -> Unit) {
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.connectTimeout = NETWORK_TIMEOUT_MS
        conn.readTimeout = NETWORK_TIMEOUT_MS
        conn.requestMethod = "GET"
        conn.setRequestProperty("User-Agent", "Lociant/${context.packageName}")
        val digest = MessageDigest.getInstance("SHA-256")
        conn.inputStream.use { input ->
            target.outputStream().use { output ->
                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                while (true) {
                    val read = input.read(buffer)
                    if (read < 0) break
                    output.write(buffer, 0, read)
                    digest.update(buffer, 0, read)
                    onBytes(read.toLong())
                }
            }
        }
        if (sha256.isNotBlank()) {
            val actual = digest.digest().joinToString("") { "%02x".format(it) }
            require(actual.equals(sha256, ignoreCase = true)) {
                "Checksum mismatch for ${target.name}"
            }
        }
    }

    private fun resolveUrl(repo: String, path: String): String {
        return "https://modelscope.cn/models/$repo/resolve/master/${path.split('/').joinToString("/") { encodePathSegment(it) }}"
    }

    private fun encodePathSegment(value: String): String {
        return java.net.URLEncoder.encode(value, "UTF-8").replace("+", "%20")
    }

    private fun isSafeRelativePath(path: String): Boolean {
        return path.isNotBlank() &&
            !path.startsWith('/') &&
            !path.contains(":") &&
            path.split('/').none { it.isBlank() || it == ".." }
    }

    private fun isInside(root: File, child: File): Boolean {
        return child.canonicalFile.toPath().startsWith(root.canonicalFile.toPath())
    }

    private fun normalizeId(value: String): String {
        return ModelManager.normalizeId(value)
    }

    private data class MarketFile(val path: String, val size: Long, val sha256: String)

    private data class MarketRepo(
        val modelId: String,
        val repoPath: String,
        val modelName: String,
        val description: String,
        val tags: List<String>,
        val vendor: String,
        val sizeGb: Double,
        val fileSize: Long,
        val source: String,
    ) {
        fun toJson(): JSONObject = JSONObject()
            .put("id", modelId)
            .put("repo", repoPath)
            .put("name", modelName)
            .put("description", description)
            .put("vendor", vendor)
            .put("sizeGb", sizeGb)
            .put("fileSize", fileSize)
            .put("runtime", "mnn")
            .put("source", source)
            .put("tags", JSONArray(tags))
    }

    private data class InstallTask(
        val progress: Double?,
        val message: String,
        val active: Boolean,
    )

    companion object {
        private const val NETWORK_TIMEOUT_MS = 30_000
        private const val MARKET_JSON_URL = "https://meta.alicdn.com/data/mnn/apis/model_market.json"
    }
}
