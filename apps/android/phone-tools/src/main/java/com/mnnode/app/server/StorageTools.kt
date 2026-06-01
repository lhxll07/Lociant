package com.mnnode.app.server

import com.mnnode.app.session.SessionStore
import com.mnnode.app.storage.LocalStore
import org.json.JSONObject

class StorageTools(
    private val sessionStore: SessionStore,
    private val localStore: LocalStore,
) : ToolProvider {
    override fun tools(): List<ToolDefinition> = listOf(
        tool(
            name = "store_get",
            description = "Read a value from Lociant's local persistent key-value store.",
            properties = storeKeyParams("Key to read"),
        ) { args ->
            localStore.get(args.optString("namespace", "default"), args.optString("key", "value"))
        },
        tool(
            name = "store_set",
            description = "Write a JSON-compatible value to Lociant's local persistent key-value store.",
            properties = storeKeyParams("Key to write")
                .put("value", JSONObject().put("description", "JSON-compatible value")),
            policy = ToolPolicy(sideEffect = true),
        ) { args ->
            val value = if (args.has("value")) args.opt("value") else JSONObject.NULL
            localStore.set(args.optString("namespace", "default"), args.optString("key", "value"), value)
        },
        tool(
            name = "store_list",
            description = "List all values in a Lociant local persistent key-value namespace.",
            properties = JSONObject().put("namespace", stringParam("Store namespace")),
        ) { args ->
            localStore.list(args.optString("namespace", "default"))
        },
        tool(
            name = "event_record",
            description = "Record a runtime event to local persistent storage (Room). Useful for sensor-driven data logging.",
            properties = JSONObject()
                .put("sceneId", stringParam("Scene identifier for this event"))
                .put("type", stringParam("Event type label"))
                .put("level", stringParam("info | warn | error"))
                .put("payload", objectParam("Arbitrary payload JSON")),
            policy = ToolPolicy(sideEffect = true),
        ) { args -> recordEvent(args) },
        tool(
            name = "store_increment",
            description = "Read-modify-write a numeric value (delta) in the local persistent key-value store. Use for trigger-driven accumulators like focus_seconds.",
            properties = storeKeyParams("Key to increment")
                .put("value", numberParam("Delta value to add")),
            policy = ToolPolicy(sideEffect = true),
        ) { args -> storeValue(args) },
    )

    private fun storeKeyParams(keyDescription: String): JSONObject = JSONObject()
        .put("namespace", stringParam("Store namespace"))
        .put("key", stringParam(keyDescription))

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
