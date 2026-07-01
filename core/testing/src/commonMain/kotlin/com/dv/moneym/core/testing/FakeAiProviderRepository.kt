package com.dv.moneym.core.testing

import com.dv.moneym.data.aiproviders.AiProviderId
import com.dv.moneym.data.aiproviders.AiProviderRepository
import com.dv.moneym.data.aiproviders.AiProviderState
import com.dv.moneym.data.aiproviders.RemoteAiModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeAiProviderRepository : AiProviderRepository {
    private val keys = mutableMapOf<AiProviderId, String>()
    private val selected = AiProviderId.entries.associateWith { it.defaultModelId }.toMutableMap()
    private val modelMap = AiProviderId.entries.associateWith { listOf(RemoteAiModel(it.defaultModelId)) }.toMutableMap()
    private val states = MutableStateFlow(buildStates())

    override fun observeProviders(): Flow<List<AiProviderState>> = states

    override suspend fun saveApiKey(provider: AiProviderId, apiKey: String) {
        keys[provider] = apiKey
        emit()
    }

    override suspend fun deleteApiKey(provider: AiProviderId) {
        keys.remove(provider)
        emit()
    }

    override suspend fun refreshModels(provider: AiProviderId): Result<Unit> {
        modelMap[provider] = listOf(RemoteAiModel(selectedModel(provider)))
        emit()
        return Result.success(Unit)
    }

    override suspend fun testConnection(provider: AiProviderId): Result<Unit> = refreshModels(provider)

    override suspend fun setSelectedModel(provider: AiProviderId, modelId: String) {
        selected[provider] = modelId
        modelMap[provider] = modelMap[provider].orEmpty().filterNot { it.id == modelId } + RemoteAiModel(modelId)
        emit()
    }

    override suspend fun apiKey(provider: AiProviderId): String? = keys[provider]

    override suspend fun selectedModel(provider: AiProviderId): String = selected[provider] ?: provider.defaultModelId

    private fun emit() {
        states.value = buildStates()
    }

    private fun buildStates(): List<AiProviderState> =
        AiProviderId.entries.map { provider ->
            AiProviderState(
                provider = provider,
                configured = keys.containsKey(provider),
                selectedModelId = selected[provider] ?: provider.defaultModelId,
                models = modelMap[provider].orEmpty(),
            )
        }
}
