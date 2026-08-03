package io.lociant.core.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ToolLoopGuardTest {
    @Test
    fun reorderedJsonArgumentsStillCountAsTheSameCall() {
        val guard = ToolLoopGuard()

        assertFalse(guard.observe(ModelToolCall("1", "tap", "{\"x\":1,\"y\":2}")))
        assertFalse(guard.observe(ModelToolCall("2", "tap", "{\"y\":2,\"x\":1}")))
        assertTrue(guard.observe(ModelToolCall("3", "tap", "{\"x\":1,\"y\":2}")))
    }

    @Test
    fun differentCallBreaksTheConsecutiveLoop() {
        val guard = ToolLoopGuard()

        repeat(2) { assertFalse(guard.observe(ModelToolCall("$it", "tap", "{}"))) }
        assertFalse(guard.observe(ModelToolCall("other", "swipe", "{}")))
        assertFalse(guard.observe(ModelToolCall("next", "tap", "{}")))
    }
}
