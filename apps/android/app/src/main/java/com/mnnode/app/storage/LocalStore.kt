package com.mnnode.app.storage

import android.content.Context
import android.util.AtomicFile
import org.json.JSONArray
import org.json.JSONTokener
import org.json.JSONObject
import java.io.File

class LocalStore(context: Context) {
    private val file = AtomicFile(File(context.filesDir, "store/local-store.json"))
    private val lock = Any()

    init {
        file.baseFile.parentFile?.mkdirs()
    }

    fun get(namespace: String, key: String): JSONObject {
        val ns = cleanNamespace(namespace)
        val name = cleanKey(key)
        synchronized(lock) {
            val value = getValueLocked(ns, name)
            return JSONObject()
                .put("ok", true)
                .put("namespace", ns)
                .put("key", name)
                .put("value", value)
        }
    }

    fun getObject(namespace: String, key: String): JSONObject {
        val ns = cleanNamespace(namespace)
        val name = cleanKey(key)
        synchronized(lock) {
            return when (val value = getValueLocked(ns, name)) {
                is JSONObject -> JSONObject(value.toString())
                else -> JSONObject()
            }
        }
    }

    fun set(namespace: String, key: String, value: Any): JSONObject {
        val ns = cleanNamespace(namespace)
        val name = cleanKey(key)
        synchronized(lock) {
            val root = readRoot()
            val bucket = root.optJSONObject(ns) ?: JSONObject().also { root.put(ns, it) }
            bucket.put(name, copyJsonValue(value))
            writeRoot(root)
            return JSONObject()
                .put("ok", true)
                .put("namespace", ns)
                .put("key", name)
                .put("value", copyJsonValue(value))
        }
    }

    fun remove(namespace: String, key: String): JSONObject {
        val ns = cleanNamespace(namespace)
        val name = cleanKey(key)
        synchronized(lock) {
            val root = readRoot()
            val bucket = root.optJSONObject(ns)
            val existed = bucket?.has(name) == true
            bucket?.remove(name)
            if (bucket != null && bucket.length() == 0) root.remove(ns)
            writeRoot(root)
            return JSONObject()
                .put("ok", true)
                .put("namespace", ns)
                .put("key", name)
                .put("removed", existed)
        }
    }

    fun list(namespace: String): JSONObject {
        val ns = cleanNamespace(namespace)
        synchronized(lock) {
            val values = readRoot().optJSONObject(ns) ?: JSONObject()
            return JSONObject()
                .put("ok", true)
                .put("namespace", ns)
                .put("values", JSONObject(values.toString()))
        }
    }

    private fun readRoot(): JSONObject {
        return runCatching {
            if (!file.baseFile.isFile) return JSONObject()
            JSONObject(file.openRead().bufferedReader(Charsets.UTF_8).use { it.readText() })
        }.getOrDefault(JSONObject())
    }

    private fun getValueLocked(namespace: String, key: String): Any {
        return readRoot()
            .optJSONObject(namespace)
            ?.opt(key)
            ?.let { copyJsonValue(it) }
            ?: JSONObject.NULL
    }

    private fun writeRoot(root: JSONObject) {
        val stream = file.startWrite()
        runCatching {
            stream.write(root.toString().toByteArray(Charsets.UTF_8))
            stream.flush()
            file.finishWrite(stream)
        }.onFailure { error ->
            file.failWrite(stream)
            throw error
        }
    }

    fun parseValue(raw: String?): Any {
        if (raw.isNullOrBlank()) return JSONObject.NULL
        return normalizeJsonValue(JSONTokener(raw).nextValue())
    }

    private fun copyJsonValue(value: Any?): Any {
        return when (value) {
            null, JSONObject.NULL -> JSONObject.NULL
            is JSONObject -> JSONObject(value.toString())
            is JSONArray -> JSONArray(value.toString())
            is String, is Number, is Boolean -> value
            else -> value.toString()
        }
    }

    private fun normalizeJsonValue(value: Any?): Any {
        return when (value) {
            null, JSONObject.NULL -> JSONObject.NULL
            is JSONObject, is JSONArray, is String, is Number, is Boolean -> value
            else -> value.toString()
        }
    }

    private fun cleanNamespace(value: String): String {
        val cleaned = value.trim()
        require(cleaned.matches(NAME_PATTERN)) { "Invalid namespace" }
        return cleaned
    }

    private fun cleanKey(value: String): String {
        val cleaned = value.trim()
        require(cleaned.matches(NAME_PATTERN)) { "Invalid key" }
        return cleaned
    }

    companion object {
        private val NAME_PATTERN = Regex("^[A-Za-z0-9._:/-]{1,96}$")
    }
}
