package io.lociant.core

import io.lociant.core.model.AutomaticSessionCache
import io.lociant.core.model.ModelChatMessage
import io.lociant.core.model.ModelChatPart
import io.lociant.core.model.ModelChatRequest
import io.lociant.core.model.ModelChatResult
import io.lociant.core.model.ModelToolCall
import io.lociant.core.tools.ToolCallOrigin
import io.lociant.core.tools.ToolDefinition
import io.lociant.core.tools.ToolPolicy
import io.lociant.core.tools.ToolProvider
import io.lociant.core.tools.ToolRegistry
import io.lociant.core.tools.tool
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CoreBehaviorTest {
    private val cache = AutomaticSessionCache()

    @Test
    fun everyTurnKeepsTheFullPromptForNativePrefixMatching() {
        val first = request(system = "You are concise.", user = "one")
        val preparedFirst = cache.prepare(first, "cpu:4")

        assertTrue(preparedFirst.useSessionCache)
        assertEquals(first.messages, preparedFirst.messages)
        cache.commit(preparedFirst, result("reply one"))

        val second = request(system = "You are concise.", user = "two", previous = "reply one")
        val preparedSecond = cache.prepare(second, "cpu:4")

        assertEquals(preparedFirst.sessionId, preparedSecond.sessionId)
        assertEquals(second.messages, preparedSecond.messages)
    }

    @Test
    fun clientReasoningMetadataDoesNotBreakNativePrefixMatching() {
        val first = cache.prepare(request(user = "one"), "cpu:4")
        cache.commit(first, result("reply one"))

        val second = request(user = "two", previous = "reply one").let { request ->
            request.copy(messages = request.messages.map { message ->
                if (message.role == "assistant") {
                    message.copy(reasoning = "The client preserved this reasoning metadata.")
                } else {
                    message
                }
            })
        }
        val preparedSecond = cache.prepare(second, "cpu:4")

        assertEquals(first.sessionId, preparedSecond.sessionId)
        assertTrue(preparedSecond.useSessionCache)
        assertEquals(second.messages, preparedSecond.messages)
    }

    @Test
    fun samePromptDoesNotPretendToBeAnAppend() {
        val first = cache.prepare(request(user = "one"), "cpu:4")
        cache.commit(first, result("reply one"))

        val repeated = cache.prepare(request(user = "one"), "cpu:4")

        assertTrue(repeated.useSessionCache)
        assertNotEquals(first.sessionId, repeated.sessionId)
        assertEquals(request(user = "one").messages, repeated.messages)
    }

    @Test
    fun modelAndRuntimeChangesStartANewGeneration() {
        val first = cache.prepare(request(user = "one"), "cpu:4")
        cache.commit(first, result("reply one"))
        val continuation = request(user = "two", previous = "reply one")

        val differentRuntime = cache.prepare(continuation, "cpu:8")
        assertNotEquals(first.sessionId, differentRuntime.sessionId)
        assertEquals(continuation.messages, differentRuntime.messages)

        cache.commit(differentRuntime, result("reply two"))
        val differentModel = cache.prepare(continuation.copy(modelId = "other"), "cpu:8")
        assertNotEquals(differentRuntime.sessionId, differentModel.sessionId)
        assertEquals(continuation.messages, differentModel.messages)
    }

    @Test
    fun stableToolDefinitionsAllowAutomaticReuse() {
        val tools = toolDefinitions()
        val first = cache.prepare(request(user = "one").copy(tools = tools), "cpu:4")
        cache.commit(first, result("reply one"))

        val continuation = request(user = "two", previous = "reply one").copy(
            tools = toolDefinitions(),
        )
        val preparedContinuation = cache.prepare(continuation, "cpu:4")
        assertEquals(first.sessionId, preparedContinuation.sessionId)
        assertTrue(preparedContinuation.useSessionCache)

        val changedTools = continuation.copy(tools = org.json.JSONArray(
            "[{\"type\":\"function\",\"function\":{\"name\":\"other\"}}]",
        ))
        val preparedWithChangedTools = cache.prepare(changedTools, "cpu:4")
        assertNotEquals(first.sessionId, preparedWithChangedTools.sessionId)
    }

    @Test
    fun toolCallMessagesDisableAutomaticReuse() {
        val first = cache.prepare(request(user = "one"), "cpu:4")
        cache.commit(first, result("reply one"))

        val withToolCall = request(user = "two", previous = "reply one").copy(
            messages = request(user = "two", previous = "reply one").messages.dropLast(1) +
                ModelChatMessage(
                    role = "assistant",
                    parts = emptyList(),
                    toolCalls = listOf(ModelToolCall("call_1", "noop")),
                ) + ModelChatMessage(
                    role = "user",
                    parts = listOf(ModelChatPart.Text("two")),
                ),
        )
        val toolRequest = cache.prepare(withToolCall, "cpu:4")
        assertEquals("", toolRequest.sessionId)
        assertTrue(!toolRequest.useSessionCache)

        val afterTool = cache.prepare(request(user = "three"), "cpu:4")
        assertNotEquals(first.sessionId, afterTool.sessionId)
        assertEquals(request(user = "three").messages, afterTool.messages)
    }

    @Test
    fun failedOrCancelledTurnsAreNotCommitted() {
        val first = cache.prepare(request(user = "one"), "cpu:4")
        cache.commit(first, result("", ok = false))
        val afterFailure = cache.prepare(request(user = "two"), "cpu:4")
        assertEquals(request(user = "two").messages, afterFailure.messages)

        cache.commit(afterFailure, result("reply two", cancelled = true))
        val afterCancellation = cache.prepare(request(user = "three"), "cpu:4")
        assertEquals(request(user = "three").messages, afterCancellation.messages)
    }

    @Test
    fun queuedAutomaticRequestsDoNotReuseAnUnfinishedTurn() {
        val first = cache.prepare(request(user = "one"), "cpu:4")
        val second = cache.prepare(request(user = "two"), "cpu:4")

        assertNotEquals(first.sessionId, second.sessionId)
        assertEquals(request(user = "two").messages, second.messages)
    }

    @Test
    fun remoteToolPolicyIsEnforcedAtTheExecutionBoundary() {
        val registry = ToolRegistry(listOf(provider(remoteAllowed = false)))

        val local = registry.call("sample", origin = ToolCallOrigin.Local)
        val remote = registry.call("sample", origin = ToolCallOrigin.Remote)

        assertTrue(local.optBoolean("ok"))
        assertFalse(remote.optBoolean("ok"))
        assertEquals("tool_remote_denied", remote.getJSONObject("error").getString("code"))
    }

    private fun request(
        system: String? = null,
        user: String,
        previous: String? = null,
        modelId: String = "qwen3.5-4b-mnn",
    ): ModelChatRequest {
        val messages = buildList {
            system?.let { add(message("system", it)) }
            add(message("user", "one"))
            previous?.let {
                add(message("assistant", it))
                add(message("user", user))
            }
        }
        return ModelChatRequest(modelId = modelId, messages = messages)
    }

    private fun message(role: String, text: String) =
        ModelChatMessage(role, listOf(ModelChatPart.Text(text)))

    private fun toolDefinitions() = org.json.JSONArray(
        "[{\"type\":\"function\",\"function\":{\"name\":\"noop\",\"description\":\"Do nothing\",\"parameters\":{\"type\":\"object\",\"properties\":{}}}}]",
    )

    private fun result(text: String, ok: Boolean = true, cancelled: Boolean = false) =
        ModelChatResult(ok = ok, modelId = "qwen3.5-4b-mnn", text = text, cancelled = cancelled)

    private fun provider(remoteAllowed: Boolean) = object : ToolProvider {
        override fun tools(): List<ToolDefinition> = listOf(
            tool(
                name = "sample",
                description = "Test tool",
                policy = ToolPolicy(remoteAllowed = remoteAllowed),
            ) { JSONObject().put("value", 1) },
        )
    }
}
