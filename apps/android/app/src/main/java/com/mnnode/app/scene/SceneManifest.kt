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
    val permissions: List<String> = emptyList(),
    val triggers: JSONArray = JSONArray(),
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
            .put("permissions", JSONArray(permissions))
            .put("triggers", JSONArray(triggers.toString()))
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
                permissions = json.optJSONArray("permissions").toStringList(),
                triggers = json.optJSONArray("triggers") ?: JSONArray(),
            )
        }
    }
}

private fun JSONArray?.toStringList(): List<String> {
    if (this == null) return emptyList()
    return List(length()) { index -> optString(index) }.filter { it.isNotBlank() }
}
