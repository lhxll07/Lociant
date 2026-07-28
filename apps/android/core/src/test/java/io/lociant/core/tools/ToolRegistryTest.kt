package io.lociant.core.tools

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ToolRegistryTest {
    @Test
    fun remotePolicyIsEnforcedAtTheExecutionBoundary() {
        val registry = ToolRegistry(listOf(provider(remoteAllowed = false)))

        val local = registry.call("sample", origin = ToolCallOrigin.Local)
        val remote = registry.call("sample", origin = ToolCallOrigin.Remote)

        assertTrue(local.optBoolean("ok"))
        assertFalse(remote.optBoolean("ok"))
        assertEquals("tool_remote_denied", remote.getJSONObject("error").getString("code"))
    }

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
