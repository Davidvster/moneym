package com.dv.moneym.data.aiproviders

import com.dv.moneym.core.ai.AiEngineId
import com.dv.moneym.core.datastore.PrefKeys
import com.dv.moneym.core.security.SecurityKeys

enum class AiProviderId(
    val engineId: AiEngineId,
    val displayName: String,
    val defaultModelId: String,
    val modelPrefKey: String,
    val apiKeySecurityKey: String,
) {
    OPENAI(
        engineId = AiEngineId("remote:openai"),
        displayName = "OpenAI",
        defaultModelId = "gpt-4.1-mini",
        modelPrefKey = PrefKeys.AI_PROVIDER_OPENAI_MODEL_ID,
        apiKeySecurityKey = SecurityKeys.AI_PROVIDER_OPENAI_API_KEY,
    ),
    ANTHROPIC(
        engineId = AiEngineId("remote:anthropic"),
        displayName = "Anthropic",
        defaultModelId = "claude-3-5-haiku-latest",
        modelPrefKey = PrefKeys.AI_PROVIDER_ANTHROPIC_MODEL_ID,
        apiKeySecurityKey = SecurityKeys.AI_PROVIDER_ANTHROPIC_API_KEY,
    ),
    GEMINI(
        engineId = AiEngineId("remote:gemini"),
        displayName = "Gemini",
        defaultModelId = "gemini-2.0-flash",
        modelPrefKey = PrefKeys.AI_PROVIDER_GEMINI_MODEL_ID,
        apiKeySecurityKey = SecurityKeys.AI_PROVIDER_GEMINI_API_KEY,
    ),
    OPENROUTER(
        engineId = AiEngineId("remote:openrouter"),
        displayName = "OpenRouter",
        defaultModelId = "openai/gpt-4.1-mini",
        modelPrefKey = PrefKeys.AI_PROVIDER_OPENROUTER_MODEL_ID,
        apiKeySecurityKey = SecurityKeys.AI_PROVIDER_OPENROUTER_API_KEY,
    ),
}

data class RemoteAiModel(
    val id: String,
    val displayName: String = id,
)

data class AiProviderState(
    val provider: AiProviderId,
    val configured: Boolean,
    val selectedModelId: String,
    val models: List<RemoteAiModel>,
    val isRefreshing: Boolean = false,
    val isTesting: Boolean = false,
)

enum class AiProviderError {
    MissingApiKey,
    RequestFailed,
}
