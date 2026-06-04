package com.dv.moneym.feature.aimodels

data class AiModelsUiState(
    val models: List<ModelRowUi> = emptyList(),
    val hfToken: String = "",
    val tokenSaved: Boolean = false,
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
    data class Downloading(val progress: Float) : ModelStatus
    data object Downloaded : ModelStatus
    data object Active : ModelStatus
}

enum class AiModelsError { Download, Delete, Token }

sealed interface AiModelsIntent {
    data class Download(val id: String) : AiModelsIntent
    data class Cancel(val id: String) : AiModelsIntent
    data class Delete(val id: String) : AiModelsIntent
    data class SetActive(val id: String) : AiModelsIntent
    data class HfTokenChanged(val text: String) : AiModelsIntent
    data object SaveToken : AiModelsIntent
    data object ClearError : AiModelsIntent
}
