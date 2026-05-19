package com.dv.moneym.feature.settings.overview.export

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.serialization.saved
import androidx.lifecycle.viewModelScope
import com.dv.moneym.core.common.DispatcherProvider
import com.dv.moneym.data.backup.BackupExporter
import com.dv.moneym.data.backup.BackupImporter
import com.dv.moneym.core.model.ImportMode
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ExportViewModel(
    private val exporter: BackupExporter,
    private val importer: BackupImporter,
    private val dispatchers: DispatcherProvider,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val _state by savedStateHandle.saved {
        MutableStateFlow(ExportUiState())
    }

    val state: StateFlow<ExportUiState> = _state
        .onStart { }
        .stateIn(viewModelScope, SharingStarted.Lazily, _state.value)

    private val _effects = Channel<ExportEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    fun onIntent(intent: ExportIntent) {
        when (intent) {
            ExportIntent.ExportJsonRequested -> {
                _state.update { it.copy(isExporting = true) }
                viewModelScope.launch {
                    val json = withContext(dispatchers.io) { exporter.exportToJson() }
                    _state.update { it.copy(isExporting = false) }
                    _effects.send(
                        ExportEffect.ExportReady(json, "moneym_backup.json", "application/json")
                    )
                }
            }

            ExportIntent.ExportCsvRequested -> {
                _state.update { it.copy(isExporting = true) }
                viewModelScope.launch {
                    val csv = withContext(dispatchers.io) { exporter.exportToCsv() }
                    _state.update { it.copy(isExporting = false) }
                    _effects.send(ExportEffect.ExportReady(csv, "moneym_export.csv", "text/csv"))
                }
            }

            ExportIntent.ImportRequested -> {
                viewModelScope.launch { _effects.send(ExportEffect.ImportRequested) }
            }

            is ExportIntent.ImportJsonChanged ->
                _state.update {
                    it.copy(
                        importJson = intent.json,
                        importPreview = null,
                        importError = null
                    )
                }

            ExportIntent.PreviewImportRequested -> {
                val json = _state.value.importJson
                if (json.isBlank()) return
                viewModelScope.launch {
                    val preview = withContext(dispatchers.io) { importer.previewFromJson(json) }
                    if (preview.isValid) {
                        _state.update { it.copy(importPreview = preview) }
                    } else {
                        _state.update { it.copy(importError = preview.errorMessage) }
                    }
                }
            }

            ExportIntent.ApplyImportRequested -> {
                val json = _state.value.importJson
                _state.update { it.copy(isImporting = true) }
                viewModelScope.launch {
                    withContext(dispatchers.io) { importer.applyFromJson(json, ImportMode.MERGE) }
                    _state.update {
                        it.copy(
                            isImporting = false,
                            showImportSuccess = true,
                            importJson = "",
                            importPreview = null
                        )
                    }
                }
            }

            ExportIntent.ClearExport -> { /* no-op: export state tracked locally in screen */ }

            ExportIntent.ClearImport -> _state.update {
                it.copy(
                    importJson = "",
                    importPreview = null,
                    importError = null,
                    showImportSuccess = false
                )
            }
        }
    }
}
