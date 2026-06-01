package com.mnnode.app.model

enum class InferenceBackend(val id: String) {
    Auto("auto"),
    Cpu("cpu"),
    Vulkan("vulkan");

    companion object {
        fun from(value: String?): InferenceBackend {
            return values().firstOrNull { it.id == value?.lowercase() } ?: Auto
        }
    }
}

