package com.mnnode.app.scene

import org.json.JSONArray
import org.json.JSONObject

class SceneRuntimeManager {
    private var runtime: VisionRuleRuntime? = null

    fun activate(scene: SceneManifest): JSONObject {
        val config = scene.runtime
        runtime = if (config?.optString("type") == "vision-rules") {
            VisionRuleRuntime(scene.id, config)
        } else {
            null
        }
        return snapshot()
    }

    fun onVisionFrame(frame: JSONObject): List<JSONObject> {
        return runtime?.onVisionFrame(frame).orEmpty()
    }

    fun command(sceneId: String, command: String, payload: JSONObject = JSONObject()): JSONObject {
        val active = runtime
        if (active == null || active.sceneId != sceneId) {
            return JSONObject()
                .put("type", "runtime.snapshot")
                .put("active", false)
                .put("sceneId", sceneId)
                .put("message", "runtime not active")
        }
        return active.command(command, payload)
    }

    fun snapshot(): JSONObject {
        return runtime?.snapshot() ?: JSONObject()
            .put("type", "runtime.snapshot")
            .put("active", false)
            .put("state", "idle")
            .put("events", JSONArray())
    }
}

private class VisionRuleRuntime(
    val sceneId: String,
    config: JSONObject,
) {
    private val initialState = config.optJSONObject("state")?.optString("initial", "idle").orEmpty().ifBlank { "idle" }
    private val rules = config.optJSONArray("rules").toRuleList()
    private val events = ArrayDeque<JSONObject>()
    private val ruleStates = mutableMapOf<String, RuleRuntimeState>()
    private val cooldowns = mutableMapOf<String, Long>()
    private val trackDurations = config.optJSONObject("session")
        ?.optJSONArray("trackDurations")
        .toStringList()
        .ifEmpty { config.optJSONObject("state")?.optJSONArray("states").toStringList() }
        .ifEmpty { listOf(initialState) }
    private val durations = trackDurations.associateWith { 0L }.toMutableMap()
    private var state = initialState
    private var stateStartedAt = System.currentTimeMillis()
    private var sessionState = "idle"
    private var sessionStartedAt = 0L
    private var elapsedMs = 0L
    private var elapsedTick = System.currentTimeMillis()
    private var lastDurationTick = System.currentTimeMillis()
    private var lastFrameAt = 0L
    private var lastDetections = JSONArray()

    fun onVisionFrame(frame: JSONObject): List<JSONObject> {
        val nowMs = System.currentTimeMillis()
        lastFrameAt = nowMs
        accrue(nowMs)
        if (sessionState != "running") return emptyList()
        val detections = frame.optJSONArray("detections") ?: JSONArray()
        lastDetections = detections
        val emitted = mutableListOf<JSONObject>()

        rules.forEach { rule ->
            if (rule.fromStates.isNotEmpty() && state !in rule.fromStates) return@forEach

            val ruleState = ruleStates.getOrPut(rule.id) { RuleRuntimeState() }
            val matched = rule.conditions.all { it.matches(detections) }

            if (!matched) {
                ruleState.since = 0L
                return@forEach
            }

            if (ruleState.since == 0L) ruleState.since = nowMs
            val heldMs = nowMs - ruleState.since
            val lastTriggeredAt = cooldowns[rule.cooldownKey] ?: 0L
            val cooledDown = rule.cooldownMs <= 0L || nowMs - lastTriggeredAt >= rule.cooldownMs
            if (heldMs < rule.forMs || !cooledDown) return@forEach

            ruleState.lastTriggeredAt = nowMs
            cooldowns[rule.cooldownKey] = nowMs
            emitted += runActions(rule, nowMs)
        }

        return emitted
    }

    fun command(command: String, payload: JSONObject): JSONObject {
        val nowMs = System.currentTimeMillis()
        when (command) {
            "start" -> {
                if (sessionState == "idle" || sessionState == "stopped") {
                    reset(nowMs)
                    sessionStartedAt = nowMs
                }
                sessionState = "running"
                lastDurationTick = nowMs
                elapsedTick = nowMs
                rememberEvent(event("session.start", "command", nowMs))
            }
            "pause" -> transitionSession("paused", "session.pause", nowMs)
            "reset" -> {
                reset(nowMs)
                rememberEvent(event("session.reset", "command", nowMs))
            }
            "stop" -> transitionSession("stopped", "session.stop", nowMs)
            "setState" -> {
                val next = payload.optString("state", "")
                if (next.isNotBlank()) setState(next, nowMs)
            }
            "sync" -> Unit
        }
        return snapshot()
    }

    fun snapshot(): JSONObject {
        accrue(System.currentTimeMillis())
        return runtimeMessage("runtime.snapshot")
            .put("active", true)
            .put("sessionState", sessionState)
            .put("running", sessionState == "running")
            .put("state", state)
            .put("initialState", initialState)
            .put("sessionStartedAt", sessionStartedAt)
            .put("elapsedMs", elapsedMs)
            .put("stateStartedAt", stateStartedAt)
            .put("lastFrameAt", lastFrameAt)
            .put("durations", JSONObject(durations.mapValues { it.value }))
            .put("events", JSONArray(events.toList()))
            .put("rules", rulesDebug(System.currentTimeMillis()))
    }

    private fun transitionSession(nextState: String, eventName: String, nowMs: Long) {
        accrue(nowMs)
        sessionState = nextState
        rememberEvent(event(eventName, "command", nowMs))
    }

    private fun reset(nowMs: Long) {
        state = initialState
        stateStartedAt = nowMs
        sessionState = "idle"
        sessionStartedAt = 0L
        elapsedMs = 0L
        elapsedTick = nowMs
        lastDurationTick = nowMs
        durations.keys.forEach { durations[it] = 0L }
        ruleStates.clear()
        cooldowns.clear()
        events.clear()
        lastDetections = JSONArray()
    }

    private fun accrue(nowMs: Long) {
        if (sessionState != "running") {
            lastDurationTick = nowMs
            elapsedTick = nowMs
            return
        }
        val elapsedDelta = (nowMs - elapsedTick).coerceAtLeast(0L)
        elapsedMs += elapsedDelta
        elapsedTick = nowMs
        val delta = (nowMs - lastDurationTick).coerceAtLeast(0L)
        durations[state] = (durations[state] ?: 0L) + delta
        lastDurationTick = nowMs
    }

    private fun setState(nextState: String, nowMs: Long): JSONObject {
        accrue(nowMs)
        state = nextState
        stateStartedAt = nowMs
        return runtimeMessage("runtime.state").putRuntimeState()
    }

    private fun runActions(rule: VisionRule, nowMs: Long): List<JSONObject> {
        val emitted = mutableListOf<JSONObject>()
        val pendingAlert = rule.actions
            .firstOrNull { it.type == "alert" }
            ?.payload
            ?.let { JSONObject(it.toString()) }

        rule.actions.forEach { action ->
            when (action.type) {
                "setState" -> {
                    val next = action.payload.optString("state", "")
                    if (next.isNotBlank() && next != state) emitted += setState(next, nowMs)
                }
                "alert" -> Unit
                "emit" -> {
                    val name = action.payload.optString("name", "")
                    if (name.isBlank()) return@forEach
                    val event = event(name, rule.id, nowMs)
                        .apply {
                            pendingAlert?.let { put("alert", JSONObject(it.toString())) }
                        }
                    rememberEvent(event)
                    emitted += runtimeMessage("runtime.event").put("event", event)
                }
            }
        }

        return emitted
    }

    private fun rulesDebug(nowMs: Long): JSONArray {
        return JSONArray(rules.map { rule ->
            val ruleState = ruleStates[rule.id]
            val stateAllowed = rule.fromStates.isEmpty() || state in rule.fromStates
            val conditions = rule.conditions.map { it.toJson().put("matched", it.matches(lastDetections)) }
            val matched = stateAllowed && conditions.all { it.optBoolean("matched") }
            val heldMs = if (matched && ruleState?.since != null && ruleState.since > 0L) nowMs - ruleState.since else 0L
            val lastCooldownAt = cooldowns[rule.cooldownKey] ?: 0L
            val cooldownRemainingMs = cooldownRemaining(rule, lastCooldownAt, nowMs)
            JSONObject()
                .put("id", rule.id)
                .put("matched", matched)
                .put("stateAllowed", stateAllowed)
                .put("heldMs", heldMs)
                .put("forMs", rule.forMs)
                .put("ready", matched && heldMs >= rule.forMs && cooldownRemainingMs == 0L)
                .put("cooldownKey", rule.cooldownKey)
                .put("cooldownMs", rule.cooldownMs)
                .put("cooldownRemainingMs", cooldownRemainingMs)
                .put("lastTriggeredAt", ruleState?.lastTriggeredAt ?: 0L)
                .put("setState", rule.actions.firstOrNull { it.type == "setState" }?.payload?.optString("state", "") ?: "")
                .put("emit", rule.actions.firstOrNull { it.type == "emit" }?.payload?.optString("name", "") ?: "")
                .put("actions", JSONArray(rule.actions.map { it.toJson() }))
                .put("fromStates", JSONArray(rule.fromStates.toList()))
                .put("conditions", JSONArray(conditions))
        })
    }

    private fun runtimeMessage(type: String): JSONObject {
        return JSONObject()
            .put("type", type)
            .put("sceneId", sceneId)
            .put("timestamp", System.currentTimeMillis())
    }

    private fun cooldownRemaining(rule: VisionRule, lastCooldownAt: Long, nowMs: Long): Long {
        return if (rule.cooldownMs <= 0L || lastCooldownAt <= 0L) 0L
        else (rule.cooldownMs - (nowMs - lastCooldownAt)).coerceAtLeast(0L)
    }

    private fun JSONObject.putRuntimeState(): JSONObject {
        return put("state", state)
            .put("sessionState", sessionState)
            .put("stateStartedAt", stateStartedAt)
            .put("elapsedMs", elapsedMs)
            .put("durations", JSONObject(durations.mapValues { it.value }))
    }

    private fun rememberEvent(event: JSONObject) {
        events.addFirst(event)
        while (events.size > MAX_EVENTS) events.removeLast()
    }

    private fun event(name: String, ruleId: String, nowMs: Long): JSONObject {
        return JSONObject()
            .put("id", "$ruleId-$nowMs")
            .put("sceneId", sceneId)
            .put("ruleId", ruleId)
            .put("name", name)
            .put("state", state)
            .put("timestamp", nowMs)
    }

    companion object {
        private const val MAX_EVENTS = 50
    }
}

private data class VisionRule(
    val id: String,
    val conditions: List<RuleCondition>,
    val forMs: Long,
    val cooldownMs: Long,
    val cooldownKey: String,
    val fromStates: Set<String>,
    val actions: List<RuleAction>,
)

private data class RuleAction(
    val type: String,
    val payload: JSONObject,
) {
    fun toJson(): JSONObject {
        return JSONObject(payload.toString()).put("type", type)
    }
}

private data class RuleCondition(
    val present: String?,
    val missing: String?,
    val classId: Int?,
    val confidenceGte: Double,
) {
    fun matches(detections: JSONArray): Boolean {
        val target = present ?: missing ?: return false
        val found = (0 until detections.length()).any { index ->
            val item = detections.optJSONObject(index) ?: return@any false
            val labelMatches = item.optString("label").equals(target, ignoreCase = true)
            val classMatches = classId != null && item.optInt("classId", Int.MIN_VALUE) == classId
            (labelMatches || classMatches) && item.optDouble("score", 0.0) >= confidenceGte
        }
        return if (missing != null) !found else found
    }

    fun toJson(): JSONObject {
        return JSONObject()
            .put("present", present ?: JSONObject.NULL)
            .put("missing", missing ?: JSONObject.NULL)
            .put("classId", classId ?: JSONObject.NULL)
            .put("confidenceGte", confidenceGte)
    }
}

private data class RuleRuntimeState(
    var since: Long = 0L,
    var lastTriggeredAt: Long = 0L,
)

private fun JSONArray?.toRuleList(): List<VisionRule> {
    if (this == null) return emptyList()
    return (0 until length()).mapNotNull { index ->
        val json = optJSONObject(index) ?: return@mapNotNull null
        val ruleId = json.optString("id", "rule-$index")
        val conditions = json.toRuleConditions()
        if (conditions.isEmpty()) return@mapNotNull null

        VisionRule(
            id = ruleId,
            conditions = conditions,
            forMs = json.optLong("forMs", json.optJSONObject("when")?.optLong("forMs", 0L) ?: 0L).coerceAtLeast(0L),
            cooldownMs = json.optLong("cooldownMs", json.optJSONObject("when")?.optLong("cooldownMs", 0L) ?: 0L).coerceAtLeast(0L),
            cooldownKey = json.optString("cooldownKey", ruleId).ifBlank { ruleId },
            fromStates = json.optJSONArray("fromStates").toStringList().toSet(),
            actions = json.toRuleActions(),
        )
    }
}

private fun JSONObject.toRuleConditions(): List<RuleCondition> {
    val conditionsJson = optJSONArray("conditions")
    if (conditionsJson != null) {
        return (0 until conditionsJson.length()).mapNotNull { index ->
            conditionsJson.optJSONObject(index).toRuleCondition()
        }
    }
    return listOfNotNull(optJSONObject("when").toRuleCondition())
}

private fun JSONObject?.toRuleCondition(): RuleCondition? {
    if (this == null) return null
    val present = optString("present", "").ifBlank { null }
    val missing = optString("missing", "").ifBlank { null }
    val classId = takeIf { has("classId") }?.optInt("classId")
    if (present == null && missing == null && classId == null) return null
    return RuleCondition(
        present = present,
        missing = missing,
        classId = classId,
        confidenceGte = optDouble("confidenceGte", 0.0),
    )
}

private fun JSONObject.toRuleActions(): List<RuleAction> {
    val actionsJson = optJSONArray("actions")
    if (actionsJson != null) {
        return (0 until actionsJson.length()).mapNotNull { index ->
            actionsJson.optJSONObject(index).toRuleAction()
        }
    }

    val actions = mutableListOf<RuleAction>()
    optString("setState", "").takeIf { it.isNotBlank() }?.let { state ->
        actions += RuleAction("setState", JSONObject().put("state", state))
    }
    optJSONObject("alert")?.let { alert ->
        actions += RuleAction("alert", JSONObject(alert.toString()))
    }
    optString("emit", "").takeIf { it.isNotBlank() }?.let { name ->
        actions += RuleAction("emit", JSONObject().put("name", name))
    }
    return actions
}

private fun JSONObject?.toRuleAction(): RuleAction? {
    if (this == null) return null
    val type = optString("type", "").ifBlank { return null }
    val payload = JSONObject(toString()).apply { remove("type") }
    return RuleAction(type, payload)
}

private fun JSONArray?.toStringList(): List<String> {
    if (this == null) return emptyList()
    return (0 until length()).mapNotNull { index -> optString(index).takeIf { it.isNotBlank() } }
}
