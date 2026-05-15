package com.dv.moneym.feature.settings.presentation

import com.dv.moneym.data.backup.ImportPreview

data class SettingsUiState(
    val pinEnabled: Boolean = false,
    val biometricEnabled: Boolean = false,
    val biometricAvailable: Boolean = false,
    val backgroundLockSeconds: Int = 30,
    // currency
    val defaultCurrency: String = "EUR",
    val showCurrencyPicker: Boolean = false,
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
    // currency
    data object CurrencyChangeRequested : SettingsIntent
    data class CurrencySelected(val code: String) : SettingsIntent
    data object CurrencyPickerDismissed : SettingsIntent
    // backup
    data object ExportJsonRequested : SettingsIntent
    data object ExportCsvRequested : SettingsIntent
    data class ImportJsonChanged(val json: String) : SettingsIntent
    data object PreviewImportRequested : SettingsIntent
    data object ApplyImportRequested : SettingsIntent
    data object ClearExport : SettingsIntent
    data object ClearImport : SettingsIntent
}

sealed interface SettingsEffect {
    data object NavigateToPinSetup : SettingsEffect
}
