package com.mnnode.app.model

import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

object ToolTemplateContract {
    fun parse(raw: String): List<ModelToolCall> {
        val text = stripFences(raw)
        return parseJson(text).ifEmpty { parseQwenXml(text) }
    }

    private fun stripFences(raw: String): String {
        var text = raw.trim()
        if (text.startsWith("```")) {
            text = text.substringAfter('\n', text)
            text = text.removeSuffix("```").trim()
        }
        return text
    }

    private fun parseJson(text: String): List<ModelToolCall> {
        val json = runCatching { JSONObject(text) }.getOrNull()
        if (json != null) {
            json.optJSONArray("tool_calls")?.let { return parseOpenAiToolCalls(it) }
            val function = json.optJSONObject("function")
            val name = function?.optString("name").orEmpty().ifBlank { json.optString("name") }
            if (name.isNotBlank()) {
                return listOf(ModelToolCall(
                    id = json.optString("id").ifBlank { newCallId() },
                    name = name,
                    arguments = normalizeArguments(function?.opt("arguments") ?: json.opt("arguments")),
                ))
            }
        }

        val array = runCatching { JSONArray(text) }.getOrNull()
        return if (array != null) parseOpenAiToolCalls(array) else emptyList()
    }

    private fun parseOpenAiToolCalls(array: JSONArray): List<ModelToolCall> {
        return List(array.length()) { array.optJSONObject(it) }
            .filterNotNull()
            .mapNotNull { item ->
                val function = item.optJSONObject("function") ?: item
                val name = function.optString("name")
                if (name.isBlank()) null else ModelToolCall(
                    id = item.optString("id").ifBlank { newCallId() },
                    name = name,
                    arguments = normalizeArguments(function.opt("arguments")),
                )
            }
    }

    private fun parseQwenXml(text: String): List<ModelToolCall> {
        val blocks = Regex("(?s)<tool_call>\\s*(.*?)\\s*</tool_call>").findAll(text).map { it.groupValues[1] }
        return blocks.mapNotNull { block ->
            val fn = Regex("(?s)<function=([^>\\s]+)>\\s*(.*?)\\s*</function>").find(block) ?: return@mapNotNull null
            val name = fn.groupValues[1].trim()
            val body = fn.groupValues[2]
            if (name.isBlank()) return@mapNotNull null
            ModelToolCall(id = newCallId(), name = name, arguments = parseQwenParameters(body).toString())
        }.toList()
    }

    private fun parseQwenParameters(body: String): JSONObject {
        val args = JSONObject()
        Regex("(?s)<parameter=([^>\\s]+)>\\s*(.*?)\\s*</parameter>")
            .findAll(body)
            .forEach { match ->
                val key = match.groupValues[1].trim()
                val value = match.groupValues[2].trim()
                if (key.isNotBlank()) args.put(key, parseJsonValue(value))
            }
        return args
    }

    private fun parseJsonValue(value: String): Any {
        if (value.isBlank()) return ""
        runCatching { return JSONObject(value) }
        runCatching { return JSONArray(value) }
        return when (value.lowercase()) {
            "true" -> true
            "false" -> false
            "null" -> JSONObject.NULL
            else -> value.toDoubleOrNull()?.let { if (it % 1.0 == 0.0) it.toLong() else it } ?: value
        }
    }

    private fun normalizeArguments(value: Any?): String {
        return when (value) {
            null, JSONObject.NULL -> "{}"
            is JSONObject, is JSONArray -> value.toString()
            is String -> value.ifBlank { "{}" }
            else -> JSONObject().put("value", value).toString()
        }
    }

    private fun newCallId(): String = "call_${UUID.randomUUID().toString().take(8)}"
}
