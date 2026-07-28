package io.lociant.core.api

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ApiContractTest {
    @Test
    fun standardProtocolsDoNotShareTheControlNamespace() {
        assertEquals("/v1/models", ApiContract.OpenAi.MODELS)
        assertEquals("/mcp", ApiContract.Mcp.ENDPOINT)
        assertTrue(ApiContract.Control.SESSIONS.startsWith("/api/v1/"))
        assertFalse(ApiContract.OpenAi.MODELS.startsWith(ApiContract.Control.BASE))
    }

    @Test
    fun sessionIdsAreValidatedWithoutFallbackGeneration() {
        assertEquals("chat_20260729", SessionIds.requireValid("chat_20260729"))
        assertFalse(SessionIds.isValid(null))
        assertFalse(SessionIds.isValid(""))
        assertFalse(SessionIds.isValid(" chat_20260729 "))
        assertFalse(SessionIds.isValid("contains/slash"))
        assertFalse(SessionIds.isValid("contains space"))
        assertFalse(SessionIds.isValid("x".repeat(97)))
    }

    @Test
    fun controlPathsAreExplicitAndVersioned() {
        assertEquals("/api/v1/runtime", ApiContract.Control.RUNTIME)
        assertEquals("/api/v1/settings", ApiContract.Control.SETTINGS)
        assertEquals("/api/v1/models", ApiContract.Control.MODELS)
        assertEquals("/api/v1/catalog/models", ApiContract.Control.CATALOG_MODELS)
        assertEquals("/api/v1/model-installations", ApiContract.Control.MODEL_INSTALLATIONS)
        assertEquals("/api/v1/sessions", ApiContract.Control.SESSIONS)
        assertEquals("/api/v1/store", ApiContract.Control.STORE)
        assertEquals("/api/v1/tools", ApiContract.Control.TOOLS)
        assertEquals("/api/v1/chat-requests", ApiContract.Control.CHAT_REQUESTS)
    }
}
