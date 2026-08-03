package io.lociant.core.model

import org.json.JSONArray
import org.json.JSONObject
import java.util.ArrayDeque

/**
 * Detects a tool doom loop without limiting legitimate multi-step work.
 *
 * The guard follows the same rule used by OpenCode: only a short consecutive
 * run of the exact same tool input is considered a loop. JSON object keys are
 * sorted before comparison so providers cannot evade the guard by reordering
 * arguments.
 */
class ToolLoopGuard(
    private val threshold: Int = DEFAULT_THRESHOLD,
) {
    private val recentCalls = ArrayDeque<String>(threshold.coerceAtLeast(1))

    fun observe(call: ModelToolCall): Boolean {
        val key = "${call.name}\u0000${canonicalArguments(call.arguments)}"
        recentCalls.addLast(key)
        while (recentCalls.size > threshold) recentCalls.removeFirst()
        return recentCalls.size == threshold && recentCalls.all { it == key }
    }

    fun clear() = recentCalls.clear()

    private fun canonicalArguments(raw: String): String {
        val value = runCatching { JSONObject(raw.ifBlank { "{}" }) }.getOrElse { raw.trim() }
        return canonicalJson(value)
    }

    private fun canonicalJson(value: Any?): String = when (value) {
        is JSONObject -> buildString {
            append('{')
            value.keys().asSequence().toList().sorted().forEachIndexed { index, key ->
                if (index > 0) append(',')
                append(JSONObject.quote(key)).append(':').append(canonicalJson(value.opt(key)))
            }
            append('}')
        }
        is JSONArray -> buildString {
            append('[')
            for (index in 0 until value.length()) {
                if (index > 0) append(',')
                append(canonicalJson(value.opt(index)))
            }
            append(']')
        }
        JSONObject.NULL -> "null"
        is String -> JSONObject.quote(value)
        else -> value.toString()
    }

    companion object {
        const val DEFAULT_THRESHOLD = 3
    }
}
