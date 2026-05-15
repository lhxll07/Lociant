package com.mnnode.app.scene

import android.content.Context
import android.net.Uri
import org.json.JSONObject
import java.io.File
import java.util.zip.ZipInputStream

class ScenePackInstaller(private val context: Context) {
    private val sceneIdRegex = Regex("^[A-Za-z0-9._-]+$")

    fun installFromUri(uri: Uri): SceneManifest {
        val scenesDir = File(context.filesDir, "scenes").apply { mkdirs() }
        val tempRoot = File(context.cacheDir, "scene-pack-install").apply {
            deleteRecursively()
            mkdirs()
        }

        unzipSafely(uri, tempRoot)

        val manifestFile = File(tempRoot, "manifest.json")
        require(manifestFile.isFile) { "manifest.json not found" }

        val manifestJson = JSONObject(manifestFile.readText(Charsets.UTF_8))
        val id = manifestJson.getString("id")
        require(sceneIdRegex.matches(id)) { "Invalid scene id" }

        val entry = manifestJson.getString("entry")
        require(isSafeRelativePath(entry)) { "Invalid entry path" }
        val entryFile = File(tempRoot, entry)
        require(entryFile.isFile) { "Scene entry not found" }
        require(isInside(tempRoot, entryFile)) { "Invalid entry location" }

        val targetDir = File(scenesDir, id)
        val backupDir = File(context.cacheDir, "scene-pack-backup-$id").apply { deleteRecursively() }

        if (targetDir.exists()) {
            if (!targetDir.renameTo(backupDir)) {
                targetDir.deleteRecursively()
            }
        }

        try {
            if (!tempRoot.renameTo(targetDir)) {
                tempRoot.copyRecursively(targetDir, overwrite = true)
                tempRoot.deleteRecursively()
            }
            backupDir.deleteRecursively()
        } catch (error: Throwable) {
            targetDir.deleteRecursively()
            if (backupDir.exists()) backupDir.renameTo(targetDir)
            throw error
        }

        val baseUrl = targetDir.toURI().toString().trimEnd('/')
        return SceneManifest.fromJson(manifestJson, source = "installed", baseUrl = baseUrl)
    }

    private fun unzipSafely(uri: Uri, destDir: File) {
        context.contentResolver.openInputStream(uri).use { input ->
            requireNotNull(input) { "Cannot open scene pack" }
            ZipInputStream(input.buffered()).use { zip ->
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
                        outFile.outputStream().use { output -> zip.copyTo(output) }
                    }
                    zip.closeEntry()
                }
            }
        }
    }

    private fun isSafeRelativePath(path: String): Boolean {
        val normalized = path.replace('\\', '/')
        return normalized.isNotBlank() &&
            !normalized.startsWith('/') &&
            !normalized.contains(":") &&
            normalized.split('/').none { it == ".." || it.isBlank() }
    }

    private fun isInside(root: File, child: File): Boolean {
        val rootPath = root.canonicalFile.toPath()
        val childPath = child.canonicalFile.toPath()
        return childPath.startsWith(rootPath)
    }
}
