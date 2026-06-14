package com.dv.moneym.feature.settings.overview.export

import com.dv.moneym.data.backup.ImportPreview
import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable

@Serializable
data class ExportUiState(
    val isExporting: Boolean = false,
    val isImporting: Boolean = false,
    val importJson: String = "",
    val importPreview: ImportPreview? = null,
    val importError: String? = null,
    val showImportSuccess: Boolean = false,
    val exportFormatCsv: Boolean = false,
    val exportStartDate: LocalDate? = null,
    val exportEndDate: LocalDate? = null,
    val showExportSheet: Boolean = false,
    val showExportDateDialog: Boolean = false,
    val showImportSheet: Boolean = false,
)

sealed interface ExportIntent {
    data class SetExportFormat(val csv: Boolean) : ExportIntent
    data class ShowExportSheet(val visible: Boolean) : ExportIntent
    data class ShowExportDateDialog(val visible: Boolean) : ExportIntent
    data class SetExportDateRange(val start: LocalDate?, val end: LocalDate?) : ExportIntent
    data object ClearExportDateRange : ExportIntent
    data object ExportRequested : ExportIntent
    data class ShowImportSheet(val visible: Boolean) : ExportIntent
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
