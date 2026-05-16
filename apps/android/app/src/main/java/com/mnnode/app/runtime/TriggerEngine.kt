package com.mnnode.app.runtime

import android.os.SystemClock
import org.json.JSONArray
import org.json.JSONObject

class TriggerEngine {
    @Volatile private var callTool: ((String, JSONObject) -> JSONObject)? = null
    companion object { private const val DEBOUNCE_MS = 2000L }

    fun setCallTool(fn: (String, JSONObject) -> JSONObject) { callTool = fn }

    private val triggers = mutableListOf<Trigger>()
    private val hitTimers = mutableMapOf<String, Long>()
    private val debounceTimers = mutableMapOf<String, Long>()

    fun loadFromJson(json: JSONArray) {
        triggers.clear()
        hitTimers.clear()
        debounceTimers.clear()
        for (i in 0 until json.length()) {
            json.optJSONObject(i)?.let { triggers.add(Trigger.fromJson(it)) }
        }
    }

    fun activeSourceIds(): Set<String> = triggers.map { it.source }.toSet()

    fun snapshot(): JSONArray = JSONArray(triggers.map { t ->
        JSONObject()
            .put("id", t.id).put("source", t.source)
            .put("active", true).put("forMs", t.forMs).put("tool", t.toolName)
    })

    fun feed(sample: SensorSample) {
        val nowMs = SystemClock.elapsedRealtime()
        triggers.filter { it.source == sample.source }.forEach { trigger ->
            if (trigger.matches(sample)) {
                debounceTimers.remove(trigger.id)
                val since = hitTimers.getOrPut(trigger.id) { nowMs }
                if (nowMs - since >= trigger.forMs) {
                    hitTimers.remove(trigger.id)
                    callTool?.invoke(trigger.toolName, trigger.toolArguments)
                }
            } else {
                val debounceSince = debounceTimers.getOrPut(trigger.id) { nowMs }
                if (nowMs - debounceSince >= DEBOUNCE_MS) {
                    hitTimers.remove(trigger.id)
                    debounceTimers.remove(trigger.id)
                }
            }
        }
    }
}

data class Trigger(
    val id: String,
    val source: String,
    val condition: JSONObject,
    val forMs: Long,
    val toolName: String,
    val toolArguments: JSONObject,
) {
    fun matches(sample: SensorSample): Boolean {
        if (condition.length() == 0) return true
        val type = condition.optString("type", "")
        return when (type) {
            "detection" -> matchDetection(sample)
            "numeric" -> matchNumeric(sample)
            else -> false
        }
    }

    private fun matchDetection(sample: SensorSample): Boolean {
        val classId = condition.optInt("classId", -1)
        if (classId < 0) return false
        val minConf = condition.optDouble("minConfidence", 0.0)
        val absent = condition.optBoolean("absent", false)
        val found = (sample.confidenceByClass[classId] ?: 0.0) >= minConf
        return if (absent) !found else found
    }

    private fun matchNumeric(sample: SensorSample): Boolean {
        val value = sample.numericValue ?: return false
        condition.optDouble("gt", Double.NaN).takeIf { !it.isNaN() }?.let { if (value <= it) return false }
        condition.optDouble("lt", Double.NaN).takeIf { !it.isNaN() }?.let { if (value >= it) return false }
        condition.optDouble("gte", Double.NaN).takeIf { !it.isNaN() }?.let { if (value < it) return false }
        condition.optDouble("lte", Double.NaN).takeIf { !it.isNaN() }?.let { if (value > it) return false }
        return true
    }

    companion object {
        fun fromJson(json: JSONObject): Trigger = Trigger(
            id = json.getString("id"),
            source = json.getString("source"),
            condition = json.optJSONObject("condition") ?: JSONObject(),
            forMs = json.optLong("forMs", 0L),
            toolName = json.getString("tool"),
            toolArguments = json.optJSONObject("arguments") ?: JSONObject(),
        )
    }
}

data class SensorSample(
    val source: String,
    val timestamp: Long,
    val numericValue: Double? = null,
    val confidenceByClass: Map<Int, Double> = emptyMap(),
)
