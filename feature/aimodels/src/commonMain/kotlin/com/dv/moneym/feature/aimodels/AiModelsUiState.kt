package com.dv.moneym.feature.aimodels

import com.dv.moneym.data.aiproviders.AiProviderId

data class AiModelsUiState(
    val models: List<ModelRowUi> = emptyList(),
    val providers: List<ProviderRowUi> = emptyList(),
    val pendingDeleteId: String? = null,
    val error: AiModelsError? = null,
)

data class ModelRowUi(
    val id: String,
    val displayNameKey: String,
    val sizeLabel: String,
    val status: ModelStatus,
)

sealed interface ModelStatus {
    data object NotDownloaded : ModelStatus
    data class Downloading(
        val progress: Float,
        val percentText: String,
        val sizeText: String,
        val speedText: String,
        val etaSeconds: Long?,
    ) : ModelStatus
    data object Downloaded : ModelStatus
    data object Active : ModelStatus
}

data class ProviderRowUi(
    val id: AiProviderId,
    val displayName: String,
    val configured: Boolean,
    val apiKeyInput: String,
    val selectedModelId: String,
    val models: List<ProviderModelUi>,
    val isRefreshing: Boolean,
    val isTesting: Boolean,
)

data class ProviderModelUi(
    val id: String,
    val displayName: String,
)

enum class AiModelsError { Download, Delete, SaveKey, DeleteKey, RefreshModels, TestConnection, SelectModel }

sealed interface AiModelsIntent {
    data class Download(val id: String) : AiModelsIntent
    data class Cancel(val id: String) : AiModelsIntent
    data class Delete(val id: String) : AiModelsIntent
    data object DeleteConfirmed : AiModelsIntent
    data object DeleteCancelled : AiModelsIntent
    data class SetActive(val id: String) : AiModelsIntent
    data class ApiKeyChanged(val provider: AiProviderId, val value: String) : AiModelsIntent
    data class SaveApiKey(val provider: AiProviderId) : AiModelsIntent
    data class DeleteApiKey(val provider: AiProviderId) : AiModelsIntent
    data class RefreshModels(val provider: AiProviderId) : AiModelsIntent
    data class TestConnection(val provider: AiProviderId) : AiModelsIntent
    data class SelectRemoteModel(val provider: AiProviderId, val modelId: String) : AiModelsIntent
    data object ClearError : AiModelsIntent
}
