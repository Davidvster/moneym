package com.dv.moneym.feature.aimodels

data class AiModelsUiState(
    val models: List<ModelRowUi> = emptyList(),
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

enum class AiModelsError { Download, Delete }

sealed interface AiModelsIntent {
    data class Download(val id: String) : AiModelsIntent
    data class Cancel(val id: String) : AiModelsIntent
    data class Delete(val id: String) : AiModelsIntent
    data object DeleteConfirmed : AiModelsIntent
    data object DeleteCancelled : AiModelsIntent
    data class SetActive(val id: String) : AiModelsIntent
    data object ClearError : AiModelsIntent
}
