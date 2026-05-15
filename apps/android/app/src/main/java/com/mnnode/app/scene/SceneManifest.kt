package com.mnnode.app.scene

import org.json.JSONArray
import org.json.JSONObject

data class SceneManifest(
    val id: String,
    val name: String,
    val version: String,
    val description: String,
    val entry: String,
    val source: String,
    val entryUrl: String,
    val capabilities: List<String> = emptyList(),
    val permissions: List<String> = emptyList(),
    val runtime: JSONObject? = null,
) {
    fun toJson(): JSONObject {
        return JSONObject()
            .put("id", id)
            .put("name", name)
            .put("version", version)
            .put("description", description)
            .put("entry", entry)
            .put("source", source)
            .put("entryUrl", entryUrl)
            .put("capabilities", JSONArray(capabilities))
            .put("permissions", JSONArray(permissions))
            .apply {
                runtime?.let { put("runtime", JSONObject(it.toString())) }
            }
    }

    companion object {
        fun fromJson(json: JSONObject, source: String, baseUrl: String): SceneManifest {
            val id = json.getString("id")
            val entry = json.getString("entry")
            return SceneManifest(
                id = id,
                name = json.optString("name", id),
                version = json.optString("version", "0.0.0"),
                description = json.optString("description", ""),
                entry = entry,
                source = source,
                entryUrl = baseUrl.trimEnd('/') + "/" + entry.trimStart('/'),
                capabilities = json.optJSONArray("capabilities").toStringList(),
                permissions = json.optJSONArray("permissions").toStringList(),
                runtime = json.optJSONObject("runtime")?.let { JSONObject(it.toString()) },
            )
        }
    }
}

private fun JSONArray?.toStringList(): List<String> {
    if (this == null) return emptyList()
    return List(length()) { index -> optString(index) }.filter { it.isNotBlank() }
}
