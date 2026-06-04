package com.dv.moneym.feature.aimodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dv.moneym.data.llmmodels.LlmModelRepository
import com.dv.moneym.data.llmmodels.LlmModelState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.math.round

class AiModelsViewModel(
    private val repository: LlmModelRepository,
) : ViewModel() {

    private val hfToken = MutableStateFlow("")
    private val tokenSaved = MutableStateFlow(false)
    private val error = MutableStateFlow<AiModelsError?>(null)

    internal val state: StateFlow<AiModelsUiState> = combine(
        repository.observeModels(),
        repository.observeHasToken(),
        hfToken,
        tokenSaved,
        error,
    ) { models, hasToken, token, saved, err ->
        AiModelsUiState(
            models = models.map { it.toRow() },
            hfToken = token,
            tokenSaved = saved || hasToken,
            error = err,
        )
    }.stateIn(viewModelScope, SharingStarted.Lazily, AiModelsUiState())

    internal fun onIntent(intent: AiModelsIntent) {
        when (intent) {
            is AiModelsIntent.Download -> viewModelScope.launch {
                runCatching { repository.download(intent.id) }
                    .onFailure { error.value = AiModelsError.Download }
            }

            is AiModelsIntent.Cancel -> repository.cancel(intent.id)

            is AiModelsIntent.Delete -> viewModelScope.launch {
                runCatching { repository.delete(intent.id) }
                    .onFailure { error.value = AiModelsError.Delete }
            }

            is AiModelsIntent.SetActive -> viewModelScope.launch {
                repository.setActive(intent.id)
            }

            is AiModelsIntent.HfTokenChanged -> {
                hfToken.value = intent.text
                tokenSaved.value = false
            }

            AiModelsIntent.SaveToken -> viewModelScope.launch {
                runCatching { repository.setHfToken(hfToken.value) }
                    .onSuccess { tokenSaved.value = true }
                    .onFailure { error.value = AiModelsError.Token }
            }

            AiModelsIntent.ClearError -> error.value = null
        }
    }

    private fun LlmModelState.toRow(): ModelRowUi {
        val current = progress
        return ModelRowUi(
            id = model.id,
            displayNameKey = model.displayNameKey,
            sizeLabel = formatSize(model.sizeBytes),
            status = when {
                current != null -> ModelStatus.Downloading(current)
                active -> ModelStatus.Active
                downloaded -> ModelStatus.Downloaded
                else -> ModelStatus.NotDownloaded
            },
        )
    }

    private fun formatSize(bytes: Long): String {
        val gb = bytes / 1_000_000_000.0
        return if (gb >= 1.0) {
            "${round(gb * 10) / 10} GB"
        } else {
            "${round(bytes / 1_000_000.0).toLong()} MB"
        }
    }
}
