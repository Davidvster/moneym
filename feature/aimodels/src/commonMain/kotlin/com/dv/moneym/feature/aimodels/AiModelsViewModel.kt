package com.dv.moneym.feature.aimodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dv.moneym.data.llmmodels.DownloadProgress
import com.dv.moneym.data.llmmodels.LlmModelRepository
import com.dv.moneym.data.llmmodels.LlmModelState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.math.round
import kotlin.math.roundToInt

class AiModelsViewModel(
    private val repository: LlmModelRepository,
) : ViewModel() {

    private val error = MutableStateFlow<AiModelsError?>(null)
    private val pendingDeleteId = MutableStateFlow<String?>(null)

    internal val state: StateFlow<AiModelsUiState> = combine(
        repository.observeModels(),
        pendingDeleteId,
        error,
    ) { models, pendingDelete, err ->
        AiModelsUiState(
            models = models.map { it.toRow() },
            pendingDeleteId = pendingDelete,
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

            is AiModelsIntent.Delete -> pendingDeleteId.value = intent.id

            AiModelsIntent.DeleteCancelled -> pendingDeleteId.value = null

            AiModelsIntent.DeleteConfirmed -> {
                val id = pendingDeleteId.value ?: return
                pendingDeleteId.value = null
                viewModelScope.launch {
                    runCatching { repository.delete(id) }
                        .onFailure { error.value = AiModelsError.Delete }
                }
            }

            is AiModelsIntent.SetActive -> viewModelScope.launch {
                repository.setActive(intent.id)
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
                current != null -> ModelStatus.Downloading(
                    progress = current.fraction,
                    percentText = "${(current.fraction * 100).roundToInt()}%",
                    sizeText = "${formatBytes(current.bytesRead)} / " +
                        if (current.totalBytes > 0L) formatBytes(current.totalBytes) else "?",
                    speedText = current.bytesPerSecond?.let { "${formatBytes(it)}/s" } ?: "",
                    etaSeconds = etaSeconds(current),
                )
                active -> ModelStatus.Active
                downloaded -> ModelStatus.Downloaded
                else -> ModelStatus.NotDownloaded
            },
        )
    }

    private fun etaSeconds(p: DownloadProgress): Long? {
        val speed = p.bytesPerSecond ?: return null
        if (speed <= 0L || p.totalBytes <= 0L) return null
        val remaining = (p.totalBytes - p.bytesRead).coerceAtLeast(0L)
        return remaining / speed
    }

    private fun formatSize(bytes: Long): String {
        val gb = bytes / 1_000_000_000.0
        return if (gb >= 1.0) {
            "${round(gb * 10) / 10} GB"
        } else {
            "${round(bytes / 1_000_000.0).toLong()} MB"
        }
    }

    private fun formatBytes(bytes: Long): String = when {
        bytes >= 1_000_000_000L -> "${round(bytes / 1_000_000_000.0 * 10) / 10} GB"
        bytes >= 1_000_000L -> "${round(bytes / 1_000_000.0 * 10) / 10} MB"
        else -> "${round(bytes / 1_000.0).toLong()} KB"
    }
}
