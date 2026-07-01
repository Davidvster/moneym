package com.dv.moneym.data.aiproviders.internal

import com.dv.moneym.core.datastore.AppSettings
import com.dv.moneym.core.security.SecureStore
import com.dv.moneym.data.aiproviders.AiProviderId
import com.dv.moneym.data.aiproviders.AiProviderRepository
import com.dv.moneym.data.aiproviders.AiProviderState
import com.dv.moneym.data.aiproviders.RemoteAiModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

class DefaultAiProviderRepository(
    private val appSettings: AppSettings,
    private val secureStore: SecureStore,
    private val client: RemoteAiClient,
) : AiProviderRepository {

    private val revision = MutableStateFlow(0)
    private val modelCache = mutableMapOf<AiProviderId, List<RemoteAiModel>>()
    private val refreshing = MutableStateFlow<Set<AiProviderId>>(emptySet())
    private val testing = MutableStateFlow<Set<AiProviderId>>(emptySet())

    override fun observeProviders(): Flow<List<AiProviderState>> = revision.map {
        AiProviderId.entries.map { provider ->
            AiProviderState(
                provider = provider,
                configured = secureStore.get(provider.apiKeySecurityKey) != null,
                selectedModelId = selectedModel(provider),
                models = modelsFor(provider),
                isRefreshing = provider in refreshing.value,
                isTesting = provider in testing.value,
            )
        }
    }

    override suspend fun saveApiKey(provider: AiProviderId, apiKey: String) {
        val trimmed = apiKey.trim()
        if (trimmed.isEmpty()) {
            deleteApiKey(provider)
        } else {
            secureStore.put(provider.apiKeySecurityKey, trimmed.encodeToByteArray())
            bump()
        }
    }

    override suspend fun deleteApiKey(provider: AiProviderId) {
        secureStore.remove(provider.apiKeySecurityKey)
        modelCache.remove(provider)
        bump()
    }

    override suspend fun refreshModels(provider: AiProviderId): Result<Unit> = withFlag(refreshing, provider) {
        val key = apiKey(provider) ?: return@withFlag Result.failure(IllegalStateException("Missing API key"))
        runCatching {
            modelCache[provider] = client.models(provider, key)
            bump()
        }
    }

    override suspend fun testConnection(provider: AiProviderId): Result<Unit> = withFlag(testing, provider) {
        val key = apiKey(provider) ?: return@withFlag Result.failure(IllegalStateException("Missing API key"))
        runCatching {
            modelCache[provider] = client.models(provider, key)
            bump()
        }
    }

    override suspend fun setSelectedModel(provider: AiProviderId, modelId: String) {
        val trimmed = modelId.trim()
        if (trimmed.isNotEmpty()) {
            appSettings.putString(provider.modelPrefKey, trimmed)
            modelCache[provider] = modelsFor(provider)
                .filterNot { it.id == trimmed } + RemoteAiModel(trimmed, trimmed)
            bump()
        }
    }

    override suspend fun apiKey(provider: AiProviderId): String? =
        secureStore.get(provider.apiKeySecurityKey)?.decodeToString()?.takeIf { it.isNotBlank() }

    override suspend fun selectedModel(provider: AiProviderId): String =
        appSettings.getString(provider.modelPrefKey, provider.defaultModelId) ?: provider.defaultModelId

    private suspend fun modelsFor(provider: AiProviderId): List<RemoteAiModel> {
        val selected = selectedModel(provider)
        val cached = modelCache[provider].orEmpty()
        return if (cached.any { it.id == selected }) {
            cached
        } else {
            listOf(RemoteAiModel(selected, selected)) + cached
        }
    }

    private suspend fun withFlag(
        flags: MutableStateFlow<Set<AiProviderId>>,
        provider: AiProviderId,
        block: suspend () -> Result<Unit>,
    ): Result<Unit> {
        flags.update { it + provider }
        bump()
        return try {
            block()
        } finally {
            flags.update { it - provider }
            bump()
        }
    }

    private fun bump() {
        revision.update { it + 1 }
    }
}
