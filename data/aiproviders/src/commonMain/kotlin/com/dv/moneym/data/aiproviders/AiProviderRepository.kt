package com.dv.moneym.data.aiproviders

import kotlinx.coroutines.flow.Flow

interface AiProviderRepository {
    fun observeProviders(): Flow<List<AiProviderState>>
    suspend fun saveApiKey(provider: AiProviderId, apiKey: String)
    suspend fun deleteApiKey(provider: AiProviderId)
    suspend fun refreshModels(provider: AiProviderId): Result<Unit>
    suspend fun testConnection(provider: AiProviderId): Result<Unit>
    suspend fun setSelectedModel(provider: AiProviderId, modelId: String)
    suspend fun apiKey(provider: AiProviderId): String?
    suspend fun selectedModel(provider: AiProviderId): String
}
