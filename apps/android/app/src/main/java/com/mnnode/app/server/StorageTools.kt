package com.mnnode.app.server

import com.mnnode.app.session.SessionStore
import com.mnnode.app.storage.LocalStore
import org.json.JSONObject

class StorageTools(
    private val sessionStore: SessionStore,
    private val localStore: LocalStore,
) : ToolProvider {
    override fun tools(): List<ToolDefinition> = listOf(
        ToolDefinition(
            name = "event_record",
            description = "Record a runtime event to local persistent storage (Room). Useful for sensor-driven data logging.",
            parameters = objectSchema(JSONObject()
                .put("sceneId", JSONObject().put("type", "string").put("description", "Scene identifier for this event"))
                .put("type", JSONObject().put("type", "string").put("description", "Event type label"))
                .put("level", JSONObject().put("type", "string").put("description", "info | warn | error"))
                .put("payload", JSONObject().put("type", "object").put("description", "Arbitrary payload JSON"))),
            policy = ToolPolicy(sideEffect = true),
        ) { args -> recordEvent(args) },
        ToolDefinition(
            name = "store_increment",
            description = "Read-modify-write a numeric value (delta) in the local persistent key-value store. Use for trigger-driven accumulators like focus_seconds.",
            parameters = objectSchema(JSONObject()
                .put("namespace", JSONObject().put("type", "string").put("description", "Store namespace"))
                .put("key", JSONObject().put("type", "string").put("description", "Key to increment"))
                .put("value", JSONObject().put("type", "number").put("description", "Delta value to add"))),
            policy = ToolPolicy(sideEffect = true),
        ) { args -> storeValue(args) },
    )

    private fun recordEvent(args: JSONObject): JSONObject {
        val sceneId = args.optString("sceneId", "runtime")
        val type = args.optString("type", "trigger.event")
        val level = args.optString("level", "info")
        val payload = args.optJSONObject("payload") ?: JSONObject()
        sessionStore.recordRuntimeEvent(sceneId, type, level, payload)
        return JSONObject().put("ok", true).put("action", "recorded").put("sceneId", sceneId)
    }

    private fun storeValue(args: JSONObject): JSONObject {
        val ns = args.optString("namespace", "default")
        val key = args.optString("key", "value")
        val delta = args.optDouble("value", 0.0)
        val cur = localStore.getObject(ns, key).optDouble("value", 0.0)
        val next = cur + delta
        localStore.set(ns, key, JSONObject().put("value", next).put("updatedAt", System.currentTimeMillis()))
        return JSONObject().put("ok", true).put("key", key).put("value", next).put("delta", delta)
    }
}
