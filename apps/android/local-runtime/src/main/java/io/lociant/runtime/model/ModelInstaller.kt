package io.lociant.runtime.model

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FilterInputStream
import java.io.InputStream
import java.util.zip.ZipInputStream

/** Imports a GGUF file for the Rust-owned llama.cpp runtime. */
class ModelInstaller(private val context: Context) {
    fun installFromUri(uri: Uri, onProgress: (Double?, String) -> Unit = { _, _ -> }): JSONObject {
        val installParent = File(
            context.getExternalFilesDir(null) ?: context.filesDir,
            "models",
        ).apply { mkdirs() }
        val tempRoot = File(
            installParent,
            ".install-${System.currentTimeMillis()}-${Thread.currentThread().id}",
        ).apply { mkdirs() }

        return try {
            val displayName = displayName(uri)
            val source = if (displayName.endsWith(".gguf", ignoreCase = true)) {
                copyGguf(uri, tempRoot, displayName, onProgress)
            } else {
                extractGguf(uri, tempRoot, onProgress)
            }
            val target = File(installParent, safeGgufName(source.name))
            val staging = File(installParent, ".${target.name}.part")
            staging.delete()
            source.copyTo(staging, overwrite = true)
            require(staging.renameTo(target)) { "Cannot commit model file" }
            onProgress(1.0, "Model installed")
            modelJson(target)
        } finally {
            tempRoot.deleteRecursively()
        }
    }

    private fun copyGguf(
        uri: Uri,
        tempRoot: File,
        name: String,
        onProgress: (Double?, String) -> Unit,
    ): File {
        val target = File(tempRoot, safeGgufName(name))
        copyStream(uri, target, onProgress)
        return target
    }

    private fun extractGguf(
        uri: Uri,
        tempRoot: File,
        onProgress: (Double?, String) -> Unit,
    ): File {
        val totalBytes = contentLength(uri)
        require(totalBytes <= MAX_ARCHIVE_BYTES || totalBytes < 0L) { "Model package is too large" }
        var lastProgressAt = 0L
        var entryCount = 0
        var totalUncompressed = 0L
        val entryNames = HashSet<String>()

        context.contentResolver.openInputStream(uri).use { input ->
            requireNotNull(input) { "Cannot open model package" }
            val countingInput = CountingInputStream(input)
            ZipInputStream(countingInput.buffered()).use { zip ->
                while (true) {
                    val entry = zip.nextEntry ?: break
                    require(countingInput.bytesRead <= MAX_ARCHIVE_BYTES) { "Model package is too large" }
                    require(++entryCount <= MAX_ZIP_ENTRIES) { "Model package has too many files" }
                    val name = entry.name.replace('\\', '/')
                    require(isSafeRelativePath(name)) { "Invalid zip entry: $name" }
                    require(entryNames.add(name)) { "Duplicate zip entry: $name" }

                    if (!entry.isDirectory && name.endsWith(".gguf", ignoreCase = true)) {
                        require(totalUncompressed == 0L) { "Model package must contain exactly one GGUF file" }
                        val output = File(tempRoot, safeGgufName(name.substringAfterLast('/')))
                        output.outputStream().use { stream ->
                            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                            while (true) {
                                val read = zip.read(buffer)
                                if (read < 0) break
                                totalUncompressed += read.toLong()
                                require(totalUncompressed <= MAX_MODEL_BYTES) { "Model file is too large" }
                                stream.write(buffer, 0, read)
                                val now = System.currentTimeMillis()
                                if (now - lastProgressAt >= PROGRESS_INTERVAL_MS) {
                                    lastProgressAt = now
                                    onProgress(readProgress(countingInput.bytesRead, totalBytes), "Extracting model")
                                }
                            }
                        }
                    } else {
                        while (zip.read() >= 0) {
                            // Consume non-model entries without materializing them.
                        }
                    }
                    zip.closeEntry()
                }
            }
        }

        return tempRoot.listFiles()?.singleOrNull { it.isFile && it.extension.equals("gguf", true) }
            ?: error("Model package must contain exactly one GGUF file")
    }

    private fun copyStream(uri: Uri, target: File, onProgress: (Double?, String) -> Unit) {
        val totalBytes = contentLength(uri)
        require(totalBytes <= MAX_MODEL_BYTES || totalBytes < 0L) { "Model file is too large" }
        context.contentResolver.openInputStream(uri).use { input ->
            requireNotNull(input) { "Cannot open model file" }
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            var copied = 0L
            var lastProgressAt = 0L
            target.outputStream().use { output ->
                while (true) {
                    val read = input.read(buffer)
                    if (read < 0) break
                    copied += read.toLong()
                    require(copied <= MAX_MODEL_BYTES) { "Model file is too large" }
                    output.write(buffer, 0, read)
                    val now = System.currentTimeMillis()
                    if (now - lastProgressAt >= PROGRESS_INTERVAL_MS) {
                        lastProgressAt = now
                        onProgress(readProgress(copied, totalBytes), "Importing model")
                    }
                }
            }
        }
    }

    private fun modelJson(file: File): JSONObject = JSONObject()
        .put("id", file.nameWithoutExtension)
        .put("name", file.nameWithoutExtension)
        .put("runtime", "llama")
        .put("type", "chat")
        .put("source", "external")
        .put("entry", file.name)
        .put("ready", true)
        .put("installed", true)
        .put("path", file.absolutePath)
        .put("missingFiles", JSONArray())

    private fun displayName(uri: Uri): String = context.contentResolver.query(
        uri,
        arrayOf(OpenableColumns.DISPLAY_NAME),
        null,
        null,
        null,
    )?.use { cursor ->
        val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        if (index >= 0 && cursor.moveToFirst()) cursor.getString(index) else null
    }?.takeIf { it.isNotBlank() } ?: "model.gguf"

    private fun contentLength(uri: Uri): Long = context.contentResolver.query(
        uri,
        arrayOf(OpenableColumns.SIZE),
        null,
        null,
        null,
    )?.use { cursor ->
        val index = cursor.getColumnIndex(OpenableColumns.SIZE)
        if (index >= 0 && cursor.moveToFirst()) cursor.getLong(index) else -1L
    } ?: -1L

    private fun safeGgufName(value: String): String {
        val base = value.substringAfterLast('/').replace(Regex("[^A-Za-z0-9._-]"), "-")
        val name = base.ifBlank { "model.gguf" }
        return if (name.endsWith(".gguf", ignoreCase = true)) name else "$name.gguf"
    }

    private fun readProgress(readBytes: Long, totalBytes: Long): Double? =
        if (totalBytes <= 0L) null else (readBytes.toDouble() / totalBytes).coerceIn(0.0, 0.95)

    private fun isSafeRelativePath(path: String): Boolean {
        val normalized = path.trimEnd('/')
        return normalized.isNotBlank() &&
            !normalized.startsWith('/') &&
            !normalized.contains(':') &&
            normalized.split('/').none { it == ".." || it.isBlank() }
    }

    companion object {
        private const val PROGRESS_INTERVAL_MS = 200L
        private const val MAX_ZIP_ENTRIES = 4096
        private const val MAX_MODEL_BYTES = 16L * 1024L * 1024L * 1024L
        private const val MAX_ARCHIVE_BYTES = 16L * 1024L * 1024L * 1024L
    }
}

private class CountingInputStream(input: InputStream) : FilterInputStream(input) {
    var bytesRead: Long = 0L
        private set

    override fun read(): Int {
        val value = super.read()
        if (value >= 0) bytesRead++
        return value
    }

    override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
        val count = super.read(buffer, offset, length)
        if (count > 0) bytesRead += count.toLong()
        return count
    }
}
