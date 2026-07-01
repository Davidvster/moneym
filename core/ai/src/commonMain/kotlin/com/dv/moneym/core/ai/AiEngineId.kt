package com.dv.moneym.core.ai

import kotlinx.serialization.Serializable

@Serializable
@JvmInline
value class AiEngineId(val value: String) {
    val name: String get() = value

    companion object {
        val GEMINI_NANO = AiEngineId("GEMINI_NANO")
        val APPLE_INTELLIGENCE = AiEngineId("APPLE_INTELLIGENCE")
        val LOCAL_LLM = AiEngineId("LOCAL_LLM")

        fun valueOf(value: String): AiEngineId = AiEngineId(value)
    }
}
