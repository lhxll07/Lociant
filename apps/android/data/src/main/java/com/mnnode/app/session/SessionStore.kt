package com.mnnode.app.session

import android.content.Context
import com.mnnode.app.model.ModelChatMessage
import com.mnnode.app.model.ModelChatPart
import com.mnnode.app.config.RuntimeDefaults
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

class SessionStore(context: Context) {
    private val dao = SessionDatabase.get(context).sessionDao()

    fun normalizeModelSessionId(raw: String?): String {
        val value = raw.orEmpty().trim()
        return value.takeIf { it.matches(SAFE_SESSION_ID) }
            ?: "session-${UUID.randomUUID().toString().take(12)}"
    }

    fun createModelSession(modelId: String?, title: String? = null): String {
        val now = System.currentTimeMillis()
        val id = "${RuntimeDefaults.Sessions.CHAT_PREFIX}$now"
        upsertModelSession(id, modelId.orEmpty(), now, title?.takeIf { it.isNotBlank() } ?: "Chat ${now % 100000}")
        return id
    }

    fun ensureModelSession(sessionId: String?, modelId: String?): String {
        val id = normalizeModelSessionId(sessionId)
        if (dao.session(id) == null) upsertModelSession(id, modelId.orEmpty(), System.currentTimeMillis())
        return id
    }

    fun deleteModelSession(sessionId: String): Boolean {
        val id = normalizeModelSessionId(sessionId)
        val session = dao.session(id) ?: return false
        if (session.kind != RuntimeDefaults.Sessions.MODEL_CHAT_KIND) return false
        dao.deleteSession(id)
        return true
    }

    fun recentModelSessions(limit: Int = RuntimeDefaults.Sessions.RECENT_LIMIT): JSONArray {
        val result = JSONArray()
        dao.recentSessions(listOf(RuntimeDefaults.Sessions.MODEL_CHAT_KIND), limit).forEach { session ->
            val latest = dao.latestMessage(session.id)
            result.put(
                JSONObject()
                    .put("id", session.id)
                    .put("title", session.title)
                    .put("kind", session.kind)
                    .put("modelId", session.modelId ?: JSONObject.NULL)
                    .put("updatedAt", session.updatedAt)
                    .put("messageCount", dao.messageCount(session.id))
                    .put("lastRole", latest?.role ?: JSONObject.NULL)
                    .put("lastText", latest?.text?.take(RuntimeDefaults.Sessions.LAST_TEXT_LIMIT) ?: "")
            )
        }
        return result
    }

    fun sessionDetails(sessionId: String): JSONObject {
        val id = normalizeModelSessionId(sessionId)
        val session = dao.session(id)
        val metadata = session?.metadataJson?.let { runCatching { JSONObject(it) }.getOrDefault(JSONObject()) } ?: JSONObject()
        val result = JSONObject()
            .put("id", id)
            .put("title", session?.title ?: id)
            .put("kind", session?.kind ?: JSONObject.NULL)
            .put("modelId", session?.modelId ?: JSONObject.NULL)
            .put("metadata", metadata)
            .put("updatedAt", session?.updatedAt ?: 0L)
            .put("messages", JSONArray())
        if (session != null) {
            val messages = JSONArray()
            dao.messages(id).forEach { message ->
                messages.put(
                    JSONObject()
                        .put("id", message.id)
                        .put("role", message.role)
                        .put("text", message.text)
                        .put("contentJson", runCatching { JSONObject(message.contentJson) }.getOrDefault(JSONObject()))
                        .put("status", message.status)
                        .put("createdAt", message.createdAt)
                )
            }
            result.put("messages", messages)
        }
        return result
    }

    fun recordApiRequest(
        method: String,
        endpoint: String,
        status: Int,
        elapsedMs: Long,
        modelId: String?,
        message: String? = null,
    ) {
        val now = System.currentTimeMillis()
        val sessionId = RuntimeDefaults.Sessions.MODEL_SERVER_SESSION_ID
        val statusText = status.toString()
        val payload = JSONObject()
            .put("method", method)
            .put("endpoint", endpoint)
            .put("status", status)
            .put("elapsedMs", elapsedMs)
            .put("modelId", modelId ?: JSONObject.NULL)
        if (!message.isNullOrBlank()) payload.put("message", message)

        dao.upsertSession(
            SessionEntity(
                id = sessionId,
                sceneId = RuntimeDefaults.Sessions.MODEL_SERVER_SCENE_ID,
                kind = RuntimeDefaults.Sessions.MODEL_SERVER_KIND,
                title = "Model Server",
                modelId = modelId,
                createdAt = dao.session(sessionId)?.createdAt ?: now,
                updatedAt = now,
                metadataJson = JSONObject().put("requestCountUpdatedAt", now).toString(),
            )
        )
        dao.insertEvent(
            EventEntity(
                sessionId = sessionId,
                sceneId = RuntimeDefaults.Sessions.MODEL_SERVER_SCENE_ID,
                type = "api.request",
                level = if (status in 200..399) "info" else "error",
                payloadJson = payload.toString(),
                createdAt = now,
            )
        )
        dao.insertMessage(
            MessageEntity(
                sessionId = sessionId,
                role = "system",
                text = "$method $endpoint $statusText",
                contentJson = payload.toString(),
                status = statusText,
                createdAt = now,
            )
        )
    }

    fun recentApiRequests(limit: Int = RuntimeDefaults.Sessions.API_REQUEST_LIMIT): JSONArray {
        val result = JSONArray()
        dao.recentEventsBySceneAndType(RuntimeDefaults.Sessions.MODEL_SERVER_SCENE_ID, "api.request", limit).forEach { event ->
            val payload = runCatching { JSONObject(event.payloadJson) }.getOrDefault(JSONObject())
            result.put(
                JSONObject()
                    .put("method", payload.optString("method"))
                    .put("endpoint", payload.optString("endpoint"))
                    .put("status", payload.optInt("status"))
                    .put("elapsedMs", payload.optLong("elapsedMs"))
                    .put("time", event.createdAt)
            )
        }
        return result
    }

    fun apiRequestCount(): Int {
        return dao.eventCountBySceneAndType(RuntimeDefaults.Sessions.MODEL_SERVER_SCENE_ID, "api.request")
    }

    fun recordRuntimeEvent(sceneId: String, type: String, level: String = "info", payload: JSONObject = JSONObject()) {
        val cleanedSceneId = sceneId.trim().ifBlank { "runtime" }
        val cleanedType = type.trim().ifBlank { "runtime.event" }
        dao.insertEvent(
            EventEntity(
                sessionId = null,
                sceneId = cleanedSceneId,
                type = cleanedType,
                level = level.ifBlank { "info" },
                payloadJson = JSONObject(payload.toString()).toString(),
                createdAt = System.currentTimeMillis(),
            )
        )
    }

    fun modelHistory(sessionId: String, limit: Int = RuntimeDefaults.Sessions.MODEL_HISTORY_LIMIT): List<ModelChatMessage> {
        return dao.messages(sessionId)
            .filter { it.role == "user" || it.role == "assistant" || it.role == "system" }
            .takeLast(limit.coerceAtLeast(1))
            .mapNotNull { message ->
                val text = message.text.trim()
                if (text.isBlank()) null else ModelChatMessage(message.role, listOf(ModelChatPart.Text(text)))
            }
    }

    fun appendModelTurn(sessionId: String, modelId: String, requestMessages: List<ModelChatMessage>, resultText: String, ok: Boolean) {
        val now = System.currentTimeMillis()
        upsertModelSession(sessionId, modelId, now)
        requestMessages.forEach { message ->
            val text = message.text()
            val imageCount = message.parts.count { it is ModelChatPart.Image }
            if (text.isBlank() && imageCount == 0) return@forEach
            val content = JSONObject()
                .put("source", "api")
                .put("imageCount", imageCount)
            dao.insertMessage(
                MessageEntity(
                    sessionId = sessionId,
                    role = message.role.ifBlank { "user" },
                    text = text.ifBlank { "[image]" },
                    contentJson = content.toString(),
                    status = "ok",
                    createdAt = now,
                )
            )
        }
        if (resultText.isNotBlank() || !ok) {
            dao.insertMessage(
                MessageEntity(
                    sessionId = sessionId,
                    role = "assistant",
                    text = resultText,
                    contentJson = JSONObject().put("ok", ok).toString(),
                    status = if (ok) "ok" else "error",
                    createdAt = System.currentTimeMillis(),
                )
            )
        }
    }

    private fun upsertModelSession(sessionId: String, modelId: String, now: Long, title: String? = null) {
        val existing = dao.session(sessionId)
        dao.upsertSession(
            SessionEntity(
                id = sessionId,
                sceneId = RuntimeDefaults.Sessions.MODEL_SERVER_SCENE_ID,
                kind = RuntimeDefaults.Sessions.MODEL_CHAT_KIND,
                title = title ?: existing?.title ?: sessionId.substringAfterLast('/').ifBlank { "Default Chat" },
                modelId = modelId,
                createdAt = existing?.createdAt ?: now,
                updatedAt = now,
                metadataJson = JSONObject().put("modelId", modelId).toString(),
            )
        )
    }

    companion object {
        private val SAFE_SESSION_ID = Regex("^[\\w.:/@\\-]+$")
    }
}
