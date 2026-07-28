package io.lociant.runtime.model

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import java.io.File
import java.io.FilterInputStream
import java.io.InputStream
import java.util.zip.ZipInputStream

class ModelInstaller(
    private val context: Context,
    private val modelManager: ModelManager,
) {
    fun installFromUri(uri: Uri, onProgress: (Double?, String) -> Unit = { _, _ -> }): ModelStatus {
        val installParent = File((context.getExternalFilesDir(null) ?: context.filesDir), "models").apply { mkdirs() }
        val tempRoot = File(installParent, ".install-${System.currentTimeMillis()}").apply { mkdirs() }
        var targetDir: File? = null
        var backupDir: File? = null
        var targetPrepared = false

        return try {
            unzipSafely(uri, tempRoot, onProgress)
            val modelRoot = findModelRoot(tempRoot)
            val spec = modelManager.inferMnnSpec(modelRoot) ?: error("unknown MNN model package")
            val missing = spec.requiredFiles.filterNot { File(modelRoot, it).isFile }
            require(missing.isEmpty()) { "Missing model files: ${missing.joinToString(", ")}" }
            targetDir = modelManager.externalModelDir(spec)
            backupDir = File(installParent, ".backup-${spec.id}").apply { deleteRecursively() }

            onProgress(0.96, "Preparing model")
            if (targetDir.exists()) {
                require(targetDir.renameTo(backupDir)) { "Cannot replace existing model" }
            }
            targetPrepared = true
            if (!modelRoot.renameTo(targetDir)) {
                onProgress(null, "Writing model files")
                modelRoot.copyRecursively(targetDir, overwrite = true)
            }
            tempRoot.deleteRecursively()
            backupDir.deleteRecursively()
            modelManager.invalidateCache()
            onProgress(1.0, "Model installed")
            modelManager.resolve(spec.id)
        } catch (error: Throwable) {
            if (targetPrepared) targetDir?.deleteRecursively()
            val target = targetDir
            if (target != null) backupDir?.takeIf { it.exists() }?.renameTo(target)
            tempRoot.deleteRecursively()
            modelManager.invalidateCache()
            throw error
        }
    }

    private fun findModelRoot(tempRoot: File): File {
        return modelManager.findMnnModelDirs(tempRoot).singleOrNull()
            ?: error("model package must contain exactly one MNN config.json model root")
    }

    private fun unzipSafely(uri: Uri, destDir: File, onProgress: (Double?, String) -> Unit) {
        val totalBytes = contentLength(uri)
        var lastProgressAt = 0L

        context.contentResolver.openInputStream(uri).use { input ->
            requireNotNull(input) { "Cannot open model package" }
            val countingInput = CountingInputStream(input)
            ZipInputStream(countingInput.buffered()).use { zip ->
                while (true) {
                    val entry = zip.nextEntry ?: break
                    val name = entry.name.replace('\\', '/')
                    require(isSafeRelativePath(name)) { "Invalid zip entry: $name" }

                    val outFile = File(destDir, name)
                    require(isInside(destDir, outFile)) { "Zip entry escaped target dir" }

                    if (entry.isDirectory) {
                        outFile.mkdirs()
                    } else {
                        outFile.parentFile?.mkdirs()
                        outFile.outputStream().use { output ->
                            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                            while (true) {
                                val read = zip.read(buffer)
                                if (read < 0) break
                                output.write(buffer, 0, read)

                                val now = System.currentTimeMillis()
                                if (now - lastProgressAt >= PROGRESS_INTERVAL_MS) {
                                    lastProgressAt = now
                                    onProgress(readProgress(countingInput.bytesRead, totalBytes), "Extracting model")
                                }
                            }
                        }
                    }
                    zip.closeEntry()
                }
            }
        }
    }

    private fun contentLength(uri: Uri): Long {
        context.contentResolver.query(uri, arrayOf(OpenableColumns.SIZE), null, null, null).use { cursor ->
            if (cursor != null && cursor.moveToFirst()) {
                val index = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (index >= 0) return cursor.getLong(index)
            }
        }
        return -1L
    }

    private fun readProgress(readBytes: Long, totalBytes: Long): Double? {
        if (totalBytes <= 0L) return null
        return (readBytes.toDouble() / totalBytes.toDouble()).coerceIn(0.0, 0.95)
    }

    private fun isSafeRelativePath(path: String): Boolean {
        val normalized = path.replace('\\', '/').trimEnd('/')
        return normalized.isNotBlank() &&
            !normalized.startsWith('/') &&
            !normalized.contains(":") &&
            normalized.split('/').none { it == ".." || it.isBlank() }
    }

    private fun isInside(root: File, child: File): Boolean {
        return child.canonicalFile.toPath().startsWith(root.canonicalFile.toPath())
    }

    companion object {
        private const val PROGRESS_INTERVAL_MS = 200L
    }
}

private class CountingInputStream(input: InputStream) : FilterInputStream(input) {
    var bytesRead: Long = 0L
        private set

    override fun read(): Int {
        val value = super.read()
        if (value >= 0) bytesRead += 1
        return value
    }

    override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
        val count = super.read(buffer, offset, length)
        if (count > 0) bytesRead += count.toLong()
        return count
    }
}
