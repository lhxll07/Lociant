package io.lociant.core.util

import org.json.JSONObject

/**
 * Build a success JSON object with ok=true and optional key-value pairs.
 *
 * Null values are converted to [JSONObject.NULL] to ensure the key appears
 * in the serialized output with a JSON null value (rather than being removed).
 * If you need to omit a key, omit the pair entirely.
 */
fun jsonOk(vararg pairs: Pair<String, Any?>): JSONObject {
    val json = JSONObject().put("ok", true)
    for ((key, value) in pairs) {
        json.put(key, value ?: JSONObject.NULL)
    }
    return json
}

/**
 * Build an error JSON object with ok=false, a code identifier, a human-readable
 * message, and optional extra key-value pairs.
 *
 * Null values are converted to [JSONObject.NULL] (see [jsonOk] for rationale).
 */
fun jsonError(code: String, message: String, vararg extras: Pair<String, Any?>): JSONObject {
    val json = JSONObject()
        .put("ok", false)
        .put("code", code)
        .put("message", message)
    for ((key, value) in extras) {
        json.put(key, value ?: JSONObject.NULL)
    }
    return json
}

/**
 * Build a JSON object from a list of key-value pairs.
 * Null values are converted to [JSONObject.NULL].
 */
fun jsonOf(vararg pairs: Pair<String, Any?>): JSONObject {
    val json = JSONObject()
    for ((key, value) in pairs) {
        json.put(key, value ?: JSONObject.NULL)
    }
    return json
}
