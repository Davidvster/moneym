package com.dv.moneym.feature.settings.settings.export

import com.dv.moneym.data.backup.ImportPreview
import kotlinx.serialization.Serializable

@Serializable
data class ExportUiState(
    val isExporting: Boolean = false,
    val isImporting: Boolean = false,
    val importJson: String = "",
    val importPreview: ImportPreview? = null,
    val importError: String? = null,
    val showImportSuccess: Boolean = false,
)

sealed interface ExportIntent {
    data object ExportJsonRequested : ExportIntent
    data object ExportCsvRequested : ExportIntent
    data object ImportRequested : ExportIntent
    data class ImportJsonChanged(val json: String) : ExportIntent
    data object PreviewImportRequested : ExportIntent
    data object ApplyImportRequested : ExportIntent
    data object ClearExport : ExportIntent
    data object ClearImport : ExportIntent
}

sealed interface ExportEffect {
    data class ExportReady(val content: String, val fileName: String, val mimeType: String) : ExportEffect
    data object ImportRequested : ExportEffect
}
