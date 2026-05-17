package com.dv.moneym.feature.settings.presentation

import com.dv.moneym.core.model.ThemeMode
import com.dv.moneym.core.model.TxDisplayPrefs
import com.dv.moneym.data.backup.ImportPreview
import kotlinx.serialization.Serializable

@Serializable
data class SettingsUiState(
    val pinEnabled: Boolean = false,
    val biometricEnabled: Boolean = false,
    val biometricAvailable: Boolean = false,
    val backgroundLockSeconds: Int = 30,
    // appearance
    val themeMode: ThemeMode = ThemeMode.Auto,
    val txDisplayPrefs: TxDisplayPrefs = TxDisplayPrefs(),
    // currency
    val defaultCurrency: String = "EUR",
    // language
    val language: String = "",
    // backup
    val exportedJson: String? = null,
    val importJson: String = "",
    val importPreview: ImportPreview? = null,
    val isExporting: Boolean = false,
    val isImporting: Boolean = false,
    val importError: String? = null,
    val showImportSuccess: Boolean = false,
)

sealed interface SettingsIntent {
    data class PinToggled(val enable: Boolean) : SettingsIntent
    data class BiometricToggled(val enable: Boolean) : SettingsIntent
    data class LockTimeoutChanged(val seconds: Int) : SettingsIntent
    data object ChangePinRequested : SettingsIntent
    // appearance
    data class ThemeModeChanged(val mode: ThemeMode) : SettingsIntent
    // backup
    data object ExportJsonRequested : SettingsIntent
    data object ExportCsvRequested : SettingsIntent
    data object ImportRequested : SettingsIntent
    data class ImportJsonChanged(val json: String) : SettingsIntent
    data object PreviewImportRequested : SettingsIntent
    data object ApplyImportRequested : SettingsIntent
    data object ClearExport : SettingsIntent
    data object ClearImport : SettingsIntent
}

sealed interface SettingsEffect {
    data object NavigateToPinSetup : SettingsEffect
    data class ExportReady(val content: String, val fileName: String, val mimeType: String) : SettingsEffect
    data object ImportRequested : SettingsEffect
}
