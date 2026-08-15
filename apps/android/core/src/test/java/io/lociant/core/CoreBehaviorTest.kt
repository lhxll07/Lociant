package io.lociant.core

import io.lociant.core.tools.ToolDefinition
import io.lociant.core.tools.ToolPolicy
import io.lociant.core.tools.ToolProvider
import io.lociant.core.tools.ToolRegistry
import io.lociant.core.tools.tool
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CoreBehaviorTest {
    @Test
    fun toolMetadataStillPublishesRemotePolicyForRust() {
        val registry = ToolRegistry(listOf(provider(remoteAllowed = false)))

        val definition = registry.definition("sample") ?: error("missing tool")
        val policy = definition.getJSONObject("x_policy")
        val execution = registry.call("sample")

        assertFalse(policy.getBoolean("remoteAllowed"))
        assertTrue(execution.optBoolean("ok"))
        assertEquals(1, execution.getJSONObject("result").getInt("value"))
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
