package io.lociant.core.api

/**
 * Canonical public paths for the three protocols served by Lociant.
 *
 * OpenAI and MCP retain their standard paths. Product-specific operations live
 * under [Control.BASE], so clients never have to guess whether a `/v1` route is
 * an OpenAI extension or a Lociant management operation.
 */
object ApiContract {
    const val VERSION = "1.0.1"

    object OpenAi {
        const val MODELS = "/v1/models"
        const val CHAT_COMPLETIONS = "/v1/chat/completions"
    }

    object Mcp {
        const val ENDPOINT = "/mcp"
    }

    object Control {
        const val BASE = "/api/v1"
        const val RUNTIME = "$BASE/runtime"
        const val SETTINGS = "$BASE/settings"
        const val MODELS = "$BASE/models"
        const val CATALOG_MODELS = "$BASE/catalog/models"
        const val MODEL_INSTALLATIONS = "$BASE/model-installations"
        const val SESSIONS = "$BASE/sessions"
        const val STORE = "$BASE/store"
        const val TOOLS = "$BASE/tools"
        const val CHAT_REQUESTS = "$BASE/chat-requests"
    }

    const val HEALTH = "/health"
    const val SESSION_HEADER = "X-Lociant-Session-Id"
    const val TOKEN_HEADER = "X-Lociant-Token"
}

/** A session identifier is either valid as supplied or rejected; it is never repaired. */
object SessionIds {
    private val pattern = Regex("^[A-Za-z0-9][A-Za-z0-9._-]{0,95}$")

    fun requireValid(raw: String?): String {
        val value = raw.orEmpty()
        require(pattern.matches(value)) {
            "Session id must contain 1-96 ASCII letters, digits, dots, underscores, or hyphens"
        }
        return value
    }

    fun isValid(raw: String?): Boolean = runCatching { requireValid(raw) }.isSuccess
}
