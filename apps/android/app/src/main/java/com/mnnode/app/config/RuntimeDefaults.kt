package com.mnnode.app.config

object RuntimeDefaults {
    const val MODEL_ID = "qwen3.5-2b-mnn"
    const val PORT = 11434

    object Tokens {
        const val OUTPUT_DEFAULT = 512
        const val OUTPUT_MIN = 8
        const val OUTPUT_MAX = 32768
        const val CONTEXT_DEFAULT = 4096
        const val CONTEXT_MIN = 1024
        const val CONTEXT_MAX = 32768
        const val CONTEXT_SAFETY_MARGIN = 256
        const val INPUT_BUDGET_MIN = 512
        const val MESSAGE_OVERHEAD = 8
        const val IMAGE_ESTIMATE = 512
    }

    object Sessions {
        const val MODEL_SERVER_SCENE_ID = "model-server"
        const val MODEL_SERVER_SESSION_ID = "model-server/default"
        const val MODEL_SERVER_KIND = "model-server"
        const val MODEL_CHAT_KIND = "model-chat"
        const val CHAT_PREFIX = "model-server/chat/"
        const val DEFAULT_CHAT_ID = "${CHAT_PREFIX}default"
        const val RECENT_LIMIT = 8
        const val API_REQUEST_LIMIT = 12
        const val MODEL_HISTORY_LIMIT = 64
        const val LAST_TEXT_LIMIT = 120
        const val MAX_SYSTEM_MESSAGES = 4
        const val DATABASE_NAME = "mnnode-sessions.db"
    }

    object Settings {
        const val SERVER_NAMESPACE = "runtime/model-server/settings"
        const val SERVER_KEY = "server"
        const val WINDOW_NAMESPACE = "runtime/settings"
        const val WINDOW_KEY = "window"
        const val FLOATING_WINDOW_KEY = "floating-window"
    }

    object NativeRuntime {
        const val PROMPT_CACHE_ENABLED = true
        const val THINKING_ENABLED = false
        const val CHAT_CACHE_DIR = "mnn-chat"
        const val CONFIG_CACHE_DIR = "mnn-chat-config"
    }

    object Queue {
        const val MAX_QUEUED_REQUESTS = 16
        const val CHAT_TIMEOUT_MS = 300_000L
        const val STREAM_HEARTBEAT_MS = 10_000L
        const val TASK_RETENTION_MS = 300_000L
    }
}
