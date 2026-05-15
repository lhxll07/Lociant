package com.mnnode.app.scene

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

class SceneManager(private val context: Context) {
    fun listScenes(): List<SceneManifest> {
        val merged = LinkedHashMap<String, SceneManifest>()
        listBuiltinScenes().forEach { merged[it.id] = it }
        listInstalledScenes().forEach { merged[it.id] = it }
        return merged.values.sortedWith(compareBy<SceneManifest> { it.source }.thenBy { it.id })
    }

    fun listScenesJson(): String {
        val array = JSONArray()
        listScenes().forEach { array.put(it.toJson()) }
        return array.toString()
    }

    fun findScene(id: String): SceneManifest? {
        return listScenes().firstOrNull { it.id == id }
    }

    fun scenesDir(): File {
        return File(context.filesDir, "scenes").apply { mkdirs() }
    }

    fun uninstallScene(id: String): Boolean {
        require(Regex("^[A-Za-z0-9._-]+$").matches(id)) { "Invalid scene id" }
        val target = File(scenesDir(), id)
        if (!target.exists()) return false
        return target.deleteRecursively()
    }

    private fun listBuiltinScenes(): List<SceneManifest> {
        val root = ""
        val sceneIds = context.assets.list(root)
            ?.filter { id -> Regex("^[A-Za-z0-9._-]+$").matches(id) }
            ?.filter { id -> context.assets.list(id).orEmpty().contains("manifest.json") }
            .orEmpty()
        return sceneIds.mapNotNull { sceneId ->
            runCatching {
                val manifestPath = "$sceneId/manifest.json"
                val manifestText = context.assets.open(manifestPath).bufferedReader(Charsets.UTF_8).use { it.readText() }
                val baseUrl = "$LOCAL_ORIGIN/assets/$sceneId"
                SceneManifest.fromJson(JSONObject(manifestText), source = "builtin", baseUrl = baseUrl)
            }.getOrNull()
        }
    }

    private fun listInstalledScenes(): List<SceneManifest> {
        return scenesDir().listFiles()
            ?.filter { it.isDirectory }
            ?.mapNotNull { sceneDir ->
                runCatching {
                    val manifestFile = File(sceneDir, "manifest.json")
                    if (!manifestFile.isFile) return@runCatching null
                    val baseUrl = "$LOCAL_ORIGIN/installed-scenes/${sceneDir.name}"
                    SceneManifest.fromJson(JSONObject(manifestFile.readText(Charsets.UTF_8)), source = "installed", baseUrl = baseUrl)
                }.getOrNull()
            }
            ?.filterNotNull()
            .orEmpty()
    }

    companion object {
        const val LOCAL_ORIGIN = "https://appassets.androidplatform.net"
    }
}
