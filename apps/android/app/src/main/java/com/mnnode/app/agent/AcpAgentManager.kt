package com.mnnode.app.agent

import com.mnnode.app.config.RuntimeDefaults
import com.mnnode.app.session.SessionStore
import com.mnnode.app.storage.LocalStore
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

class AcpAgentManager(
    private val localStore: LocalStore,
    private val sessionStore: SessionStore,
) {
    private val httpClient = OkHttpClient.Builder()
        .pingInterval(25, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .build()
    private val heartbeat: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor { runnable ->
        Thread(runnable, "lociant-acp-heartbeat").apply { isDaemon = true }
    }
    private val lock = Any()
    private val nextRequestId = AtomicInteger(1)
    private val pending = ConcurrentHashMap<Int, PendingCall>()

    @Volatile private var webSocket: WebSocket? = null
    @Volatile private var connectionState = "idle"
    @Volatile private var lastError = ""
    @Volatile private var initialized = false
    @Volatile private var supportsLoadSession = false
    @Volatile private var activeNode = defaultLocalNode()
    @Volatile private var activeSession = AcpSession()
    @Volatile private var activePromptSessionId = ""
    private val activePromptText = StringBuilder()

    init {
        loadActiveNode()
        heartbeat.scheduleAtFixedRate({
            val socket = webSocket
            if (socket != null && connectionState == "connected") {
                socket.send(JSONObject().put("jsonrpc", "2.0").put("method", "$/ping").toString() + "\n")
            }
        }, 25, 25, TimeUnit.SECONDS)
    }

    fun command(command: String, payload: JSONObject = JSONObject()): JSONObject {
        return runCatching {
            when (command) {
                "agent.status" -> state()
                "agent.saveNode" -> {
                    if (saveNode(payload)) reconnectIfActive(payload)
                    state()
                }
                "agent.selectNode" -> selectNode(payload.optString("nodeId", payload.optString("id", LOCAL_NODE_ID)))
                "agent.connect" -> connect()
                "agent.disconnect" -> disconnect()
                "agent.session.create" -> createSession(payload)
                "agent.session.select" -> selectSession(payload)
                "agent.prompt" -> prompt(payload)
                else -> JSONObject().put("ok", false).put("message", "Unknown agent command: $command")
            }
        }.getOrElse { error ->
            lastError = error.message ?: "ACP command failed"
            state().put("ok", false).put("message", lastError)
        }
    }

    fun state(): JSONObject = JSONObject()
        .put("ok", true)
        .put("activeNodeId", activeNode.id)
        .put("activeNode", activeNode.toJson(includeSensitive = false))
        .put("nodes", nodesJson())
        .put("agent", JSONObject()
            .put("state", connectionState)
            .put("connected", connectionState == "connected")
            .put("initialized", initialized)
            .put("supportsLoadSession", supportsLoadSession)
            .put("lastError", lastError.ifBlank { JSONObject.NULL })
            .put("sessionId", activeSession.localSessionId)
            .put("remoteSessionId", activeSession.remoteSessionId))

    private fun saveNode(payload: JSONObject): Boolean {
        val existing = nodes().toMutableList()
        val node = NodeProfile.fromJson(payload.optJSONObject("node") ?: payload)
        if (node.kind == ACP_KIND && node.url.isBlank()) {
            lastError = "ACP URL is empty"
            return false
        }
        lastError = ""
        val index = existing.indexOfFirst { it.id == node.id }
        if (index >= 0) existing[index] = node else existing.add(node)
        saveNodes(existing)
        if (payload.optBoolean("active", false)) {
            activeNode = node
            saveActiveNodeId(node.id)
        }
        return true
    }

    private fun selectNode(nodeId: String): JSONObject {
        val node = nodes().find { it.id == nodeId } ?: defaultLocalNode()
        if (activeNode.id != node.id) disconnect()
        activeNode = node
        activeSession = AcpSession()
        saveActiveNodeId(node.id)
        return state()
    }

    private fun reconnectIfActive(payload: JSONObject) {
        val node = NodeProfile.fromJson(payload.optJSONObject("node") ?: payload)
        if (node.id == activeNode.id) {
            activeNode = node
            activeSession = AcpSession()
            if (connectionState == "connected") {
                disconnect()
                connect()
            }
        }
    }

    private fun connect(): JSONObject {
        if (activeNode.kind == LOCAL_KIND) return state()
        if (connectionState == "connected" && initialized) return state()
        synchronized(lock) {
            disconnectLocked()
            connectionState = "connecting"
            lastError = ""
            initialized = false
        }
        if (activeNode.url.isBlank()) {
            connectionState = "error"
            lastError = "ACP URL is empty"
            return state()
        }
        val openLatch = CountDownLatch(1)
        val request = Request.Builder()
            .url(normalizeWebSocketUrl(activeNode.url))
            .apply {
                if (activeNode.token.isNotBlank()) header("Authorization", "Bearer ${activeNode.token}")
                if (activeNode.clientId.isNotBlank()) header("X-Client-Id", activeNode.clientId)
            }
            .build()
        val socket = httpClient.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                if (this@AcpAgentManager.webSocket !== webSocket) return
                connectionState = "connected"
                openLatch.countDown()
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                if (this@AcpAgentManager.webSocket !== webSocket) return
                handleIncoming(text)
            }

            override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                if (this@AcpAgentManager.webSocket !== webSocket) return
                lastError = "ACP sent binary data, ignored."
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                if (this@AcpAgentManager.webSocket !== webSocket) return
                connectionState = "closing"
                webSocket.close(code, reason)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                if (this@AcpAgentManager.webSocket !== webSocket) return
                connectionState = "idle"
                initialized = false
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                if (this@AcpAgentManager.webSocket !== webSocket) return
                lastError = t.message ?: "ACP connection failed"
                connectionState = "error"
                initialized = false
                openLatch.countDown()
            }
        })
        webSocket = socket
        if (!openLatch.await(15, TimeUnit.SECONDS)) {
            lastError = "ACP connect timed out"
            connectionState = "error"
            socket.cancel()
            return state()
        }
        if (connectionState != "connected") return state()
        return initialize()
    }

    private fun initialize(): JSONObject {
        if (connectionState != "connected") return state()
        if (initialized) return state()
        return runCatching {
            val init = request("initialize", JSONObject()
                .put("protocolVersion", 1)
                .put("clientCapabilities", JSONObject()
                    .put("fs", JSONObject()
                        .put("readTextFile", false)
                        .put("writeTextFile", false)))
                .put("clientInfo", JSONObject()
                    .put("name", "lociant")
                    .put("title", "Lociant")
                    .put("version", "0.4.0")))
            val capabilities = init.optJSONObject("agentCapabilities") ?: JSONObject()
            supportsLoadSession = capabilities.optBoolean("loadSession", false)
            initialized = true
            state()
        }.getOrElse { error ->
            lastError = error.message ?: "ACP initialize failed"
            initialized = false
            state()
        }
    }

    private fun disconnect(): JSONObject {
        synchronized(lock) { disconnectLocked() }
        return state()
    }

    private fun disconnectLocked() {
        pending.values.forEach { it.error = "Connection closed"; it.latch.countDown() }
        pending.clear()
        webSocket?.close(1000, "client closed")
        webSocket = null
        connectionState = "idle"
        initialized = false
    }

    private fun createSession(payload: JSONObject): JSONObject {
        if (activeNode.kind == LOCAL_KIND) return state()
        ensureConnected()
        val cwd = payload.optString("cwd", activeNode.cwd).ifBlank { activeNode.cwd }
        val response = request("session/new", JSONObject()
            .put("cwd", cwd)
            .put("mcpServers", JSONArray()))
        val remoteSessionId = response.optString("sessionId")
        if (remoteSessionId.isBlank()) throw IllegalStateException("ACP did not return sessionId")
        val localSessionId = sessionStore.createAcpSession(activeNode.id, remoteSessionId)
        activeSession = AcpSession(localSessionId, remoteSessionId)
        return state().put("currentSessionId", localSessionId)
    }

    private fun selectSession(payload: JSONObject): JSONObject {
        val sessionId = payload.optString("sessionId", payload.optString("id")).trim()
        if (sessionId.isBlank()) return state()
        val details = sessionStore.sessionDetails(sessionId)
        val metadata = details.optJSONObject("metadata") ?: JSONObject()
        if (details.optString("kind") == RuntimeDefaults.Sessions.AGENT_ACP_KIND) {
            val nodeId = metadata.optString("nodeId", activeNode.id)
            val node = nodes().find { it.id == nodeId }
            if (node != null && activeNode.id != node.id) {
                disconnect()
                activeNode = node
                saveActiveNodeId(node.id)
            }
            activeSession = AcpSession(sessionId, metadata.optString("remoteSessionId"))
            if (activeSession.remoteSessionId.isNotBlank()) {
                runCatching {
                    ensureConnected()
                    if (supportsLoadSession) {
                        request("session/load", JSONObject()
                            .put("sessionId", activeSession.remoteSessionId)
                            .put("cwd", activeNode.cwd)
                            .put("mcpServers", JSONArray()))
                    }
                }.onFailure { error ->
                    lastError = error.message ?: "ACP session load failed"
                }
            }
        }
        return state().put("currentSessionId", sessionId).put("session", details)
    }

    private fun prompt(payload: JSONObject): JSONObject {
        if (activeNode.kind == LOCAL_KIND) {
            return JSONObject().put("ok", false).put("message", "Active node is local.")
        }
        ensureConnected()
        if (activeSession.remoteSessionId.isBlank()) createSession(JSONObject())
        val text = payload.optString("text").trim()
        if (text.isBlank()) return state()
        sessionStore.appendAcpMessage(activeSession.localSessionId, activeNode.id, activeSession.remoteSessionId, "user", text)
        synchronized(lock) {
            activePromptSessionId = activeSession.localSessionId
            activePromptText.setLength(0)
        }
        val response = request("session/prompt", JSONObject()
            .put("sessionId", activeSession.remoteSessionId)
            .put("prompt", JSONArray().put(JSONObject().put("type", "text").put("text", text))))
        val reply = synchronized(lock) {
            activePromptText.toString().also {
                activePromptText.setLength(0)
                activePromptSessionId = ""
            }
        }.ifBlank { response.optString("text") }
        if (reply.isNotBlank()) {
            sessionStore.appendAcpMessage(activeSession.localSessionId, activeNode.id, activeSession.remoteSessionId, "assistant", reply)
        }
        return state()
            .put("currentSessionId", activeSession.localSessionId)
            .put("reply", reply)
            .put("stopReason", response.optString("stopReason", ""))
    }

    private fun ensureConnected() {
        if (connectionState != "connected") {
            val next = connect()
            if (next.optJSONObject("agent")?.optBoolean("connected", false) != true) {
                throw IllegalStateException(lastError.ifBlank { "ACP is not connected" })
            }
        }
        if (!initialized) {
            val next = initialize()
            if (next.optJSONObject("agent")?.optBoolean("initialized", false) != true) {
                throw IllegalStateException(lastError.ifBlank { "ACP is not initialized" })
            }
        }
    }

    private fun request(method: String, params: JSONObject): JSONObject {
        val socket = webSocket ?: throw IllegalStateException("ACP socket is not connected")
        val id = nextRequestId.getAndIncrement()
        val call = PendingCall(method)
        pending[id] = call
        val frame = JSONObject()
            .put("jsonrpc", "2.0")
            .put("id", id)
            .put("method", method)
            .put("params", params)
            .toString() + "\n"
        if (!socket.send(frame)) {
            pending.remove(id)
            throw IllegalStateException("ACP send failed")
        }
        if (!call.latch.await(120, TimeUnit.SECONDS)) {
            pending.remove(id)
            throw IllegalStateException("ACP request timed out: $method")
        }
        call.error?.let { throw IllegalStateException(it) }
        return call.result ?: JSONObject()
    }

    private fun handleIncoming(raw: String) {
        raw.split('\n')
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .forEach { line ->
                val json = runCatching { JSONObject(line) }.getOrNull() ?: return@forEach
                if (json.has("id") && !json.has("method")) handleResponse(json)
                else if (json.has("id") && json.has("method")) handleRequest(json)
                else if (json.has("method")) handleNotification(json)
                else handleTransportEvent(json)
            }
    }

    private fun handleResponse(json: JSONObject) {
        val id = json.optInt("id")
        val call = pending.remove(id) ?: return
        val error = json.optJSONObject("error")
        if (error != null) call.error = error.optString("message", error.toString())
        else call.result = json.optJSONObject("result") ?: JSONObject()
        call.latch.countDown()
    }

    private fun handleRequest(json: JSONObject) {
        val id = json.opt("id")
        val method = json.optString("method")
        val result = when (method) {
            "session/request_permission" -> JSONObject()
                .put("outcome", JSONObject()
                    .put("outcome", "selected")
                    .put("optionId", firstPermissionOption(json.optJSONObject("params"))))
            else -> null
        }
        val response = JSONObject().put("jsonrpc", "2.0").put("id", id)
        if (result == null) {
            response.put("error", JSONObject().put("code", -32601).put("message", "Method not found: $method"))
        } else {
            response.put("result", result)
        }
        webSocket?.send(response.toString() + "\n")
    }

    private fun handleNotification(json: JSONObject) {
        if (json.optString("method") != "session/update") return
        val update = json.optJSONObject("params")?.optJSONObject("update") ?: return
        val kind = update.optString("sessionUpdate")
        val content = update.optJSONObject("content")
        val text = content?.optString("text").orEmpty()
        if (text.isBlank()) return
        if (kind == "agent_message_chunk") {
            synchronized(lock) {
                if (activePromptSessionId == activeSession.localSessionId) {
                    activePromptText.append(text)
                    return
                }
            }
            sessionStore.appendAcpMessage(activeSession.localSessionId, activeNode.id, activeSession.remoteSessionId, "assistant", text)
        }
    }

    private fun handleTransportEvent(json: JSONObject) {
        if (json.optString("type") != "connected") return
        val clientId = json.optString("clientId").trim()
        if (clientId.isBlank() || activeNode.kind != ACP_KIND || activeNode.clientId == clientId) return
        activeNode = activeNode.copy(clientId = clientId)
        saveNodes(nodes().map { if (it.id == activeNode.id) activeNode else it })
    }

    private fun firstPermissionOption(params: JSONObject?): String {
        val options = params?.optJSONArray("options")
        var fallback = ""
        for (index in 0 until (options?.length() ?: 0)) {
            val option = options?.optJSONObject(index) ?: continue
            val id = option.optString("optionId")
            val kind = option.optString("kind")
            if (kind.equals("allow", ignoreCase = true) || kind.equals("approve", ignoreCase = true)) return id
            if (fallback.isBlank() && id.isNotBlank()) fallback = id
        }
        return fallback.ifBlank { "allow" }
    }

    private fun nodes(): List<NodeProfile> {
        val saved = localStore.getObject(RuntimeDefaults.Settings.NODES_NAMESPACE, RuntimeDefaults.Settings.NODES_KEY)
        val raw = saved.optJSONArray("nodes") ?: JSONArray()
        val values = mutableListOf(defaultLocalNode())
        for (index in 0 until raw.length()) {
            val node = NodeProfile.fromJson(raw.optJSONObject(index) ?: continue)
            if (node.id != LOCAL_NODE_ID) values.add(node)
        }
        if (values.none { it.id == activeNode.id }) activeNode = values.first()
        return values.distinctBy { it.id }
    }

    private fun saveNodes(nodes: List<NodeProfile>) {
        val raw = JSONArray()
        nodes.filter { it.id != LOCAL_NODE_ID }.forEach { raw.put(it.toJson(includeSensitive = true)) }
        localStore.set(RuntimeDefaults.Settings.NODES_NAMESPACE, RuntimeDefaults.Settings.NODES_KEY, JSONObject().put("nodes", raw))
    }

    private fun nodesJson(): JSONArray = JSONArray().also { array -> nodes().forEach { array.put(it.toJson(includeSensitive = false)) } }

    private fun loadActiveNode() {
        val saved = localStore.getObject(RuntimeDefaults.Settings.NODES_NAMESPACE, "active")
        val nodeId = saved.optString("nodeId", LOCAL_NODE_ID)
        activeNode = nodes().find { it.id == nodeId } ?: defaultLocalNode()
    }

    private fun saveActiveNodeId(nodeId: String) {
        localStore.set(RuntimeDefaults.Settings.NODES_NAMESPACE, "active", JSONObject().put("nodeId", nodeId))
    }

    private data class PendingCall(
        val method: String,
        val latch: CountDownLatch = CountDownLatch(1),
        @Volatile var result: JSONObject? = null,
        @Volatile var error: String? = null,
    )

    private data class AcpSession(
        val localSessionId: String = "",
        val remoteSessionId: String = "",
    )

    private data class NodeProfile(
        val id: String,
        val kind: String,
        val name: String,
        val url: String,
        val token: String,
        val cwd: String,
        val clientId: String,
    ) {
        fun toJson(includeSensitive: Boolean): JSONObject = JSONObject()
            .put("id", id)
            .put("kind", kind)
            .put("name", name)
            .put("url", url)
            .put("cwd", cwd)
            .put("clientId", clientId)
            .apply {
                if (includeSensitive) put("token", token)
            }

        companion object {
            fun fromJson(json: JSONObject): NodeProfile {
                val kind = json.optString("kind", ACP_KIND).ifBlank { ACP_KIND }
                return NodeProfile(
                    id = json.optString("id").ifBlank { if (kind == LOCAL_KIND) LOCAL_NODE_ID else "codex-${UUID.randomUUID().toString().take(8)}" },
                    kind = kind,
                    name = json.optString("name").ifBlank { if (kind == LOCAL_KIND) "Local Lociant" else "Desktop Codex" },
                    url = json.optString("url").trim(),
                    token = json.optString("token").trim(),
                    cwd = json.optString("cwd").trim(),
                    clientId = json.optString("clientId").trim(),
                )
            }
        }
    }

    companion object {
        private const val LOCAL_NODE_ID = "local"
        private const val LOCAL_KIND = "local"
        private const val ACP_KIND = "acp"

        private fun defaultLocalNode() = NodeProfile(
            id = LOCAL_NODE_ID,
            kind = LOCAL_KIND,
            name = "Local Lociant",
            url = "",
            token = "",
            cwd = "",
            clientId = "",
        )

        private fun normalizeWebSocketUrl(raw: String): String {
            val value = raw.trim()
            if (value.startsWith("ws://", ignoreCase = true) || value.startsWith("wss://", ignoreCase = true)) {
                return value
            }
            if (value.startsWith("http://", ignoreCase = true)) return "ws://" + value.substringAfter("://")
            if (value.startsWith("https://", ignoreCase = true)) return "wss://" + value.substringAfter("://")
            return "ws://$value"
        }
    }
}
