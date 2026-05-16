package com.dv.moneym.feature.settings.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dv.moneym.core.common.DispatcherProvider
import com.dv.moneym.core.common.LocaleController
import com.dv.moneym.core.datastore.AppSettings
import com.dv.moneym.core.datastore.AppSettingsRepository
import com.dv.moneym.core.model.ThemeMode
import com.dv.moneym.core.model.TxDisplayPrefs
import com.dv.moneym.core.security.BiometricAuthenticator
import com.dv.moneym.core.security.PinManager
import com.dv.moneym.core.security.SecurityPrefs
import com.dv.moneym.data.backup.BackupExporter
import com.dv.moneym.data.backup.BackupImporter
import com.dv.moneym.data.backup.ImportMode
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SettingsViewModel(
    private val settings: AppSettings,
    private val appSettingsRepository: AppSettingsRepository,
    private val pinManager: PinManager,
    private val biometricAuth: BiometricAuthenticator,
    private val exporter: BackupExporter,
    private val importer: BackupImporter,
    private val dispatchers: DispatcherProvider,
    private val localeController: LocaleController,
) : ViewModel() {

    private val _state = MutableStateFlow(SettingsUiState())
    val state: StateFlow<SettingsUiState> = _state.asStateFlow()

    private val _effects = Channel<SettingsEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    init {
        // Load security prefs from legacy AppSettings (unchanged)
        _state.update {
            it.copy(
                pinEnabled = settings.getBoolean(SecurityPrefs.PIN_ENABLED),
                biometricEnabled = settings.getBoolean(SecurityPrefs.BIOMETRIC_ENABLED),
                biometricAvailable = biometricAuth.isAvailable,
                backgroundLockSeconds = settings.getInt(SecurityPrefs.BACKGROUND_LOCK_SECONDS, SecurityPrefs.DEFAULT_LOCK_SECONDS),
                language = localeController.getCurrentLanguageTag(),
            )
        }

        // Observe repository-backed prefs
        combine(
            appSettingsRepository.observeThemeMode(),
            appSettingsRepository.observeTxDisplayPrefs(),
            appSettingsRepository.observeDefaultCurrency(),
            appSettingsRepository.observeLanguage(),
        ) { themeMode, txPrefs, currency, language ->
            _state.update {
                it.copy(
                    themeMode = themeMode,
                    txDisplayPrefs = txPrefs,
                    defaultCurrency = currency,
                    language = language,
                )
            }
        }.launchIn(viewModelScope)
    }

    fun onIntent(intent: SettingsIntent) {
        when (intent) {
            is SettingsIntent.PinToggled -> {
                if (intent.enable) {
                    viewModelScope.launch { _effects.send(SettingsEffect.NavigateToPinSetup) }
                } else {
                    viewModelScope.launch {
                        withContext(dispatchers.io) { pinManager.clearPin() }
                        _state.update { it.copy(pinEnabled = false, biometricEnabled = false) }
                    }
                }
            }
            is SettingsIntent.BiometricToggled -> {
                settings.putBoolean(SecurityPrefs.BIOMETRIC_ENABLED, intent.enable)
                _state.update { it.copy(biometricEnabled = intent.enable) }
            }
            is SettingsIntent.LockTimeoutChanged -> {
                settings.putInt(SecurityPrefs.BACKGROUND_LOCK_SECONDS, intent.seconds)
                _state.update { it.copy(backgroundLockSeconds = intent.seconds) }
            }
            SettingsIntent.ChangePinRequested ->
                viewModelScope.launch { _effects.send(SettingsEffect.NavigateToPinSetup) }

            is SettingsIntent.ThemeModeChanged ->
                viewModelScope.launch { appSettingsRepository.setThemeMode(intent.mode) }

            SettingsIntent.ExportJsonRequested -> {
                _state.update { it.copy(isExporting = true) }
                viewModelScope.launch {
                    val json = withContext(dispatchers.io) { exporter.exportToJson() }
                    _state.update { it.copy(isExporting = false, exportedJson = json) }
                }
            }
            SettingsIntent.ExportCsvRequested -> {
                _state.update { it.copy(isExporting = true) }
                viewModelScope.launch {
                    val csv = withContext(dispatchers.io) { exporter.exportToCsv() }
                    _state.update { it.copy(isExporting = false, exportedJson = csv) }
                }
            }
            is SettingsIntent.ImportJsonChanged ->
                _state.update { it.copy(importJson = intent.json, importPreview = null, importError = null) }
            SettingsIntent.PreviewImportRequested -> {
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
            SettingsIntent.ApplyImportRequested -> {
                val json = _state.value.importJson
                _state.update { it.copy(isImporting = true) }
                viewModelScope.launch {
                    withContext(dispatchers.io) { importer.applyFromJson(json, ImportMode.MERGE) }
                    _state.update {
                        it.copy(isImporting = false, showImportSuccess = true, importJson = "", importPreview = null)
                    }
                }
            }
            SettingsIntent.ClearExport -> _state.update { it.copy(exportedJson = null) }
            SettingsIntent.ClearImport -> _state.update {
                it.copy(importJson = "", importPreview = null, importError = null, showImportSuccess = false)
            }
        }
    }

    fun setDefaultCurrency(code: String) {
        viewModelScope.launch { appSettingsRepository.setDefaultCurrency(code) }
    }

    fun setLanguage(tag: String) {
        viewModelScope.launch {
            appSettingsRepository.setLanguage(tag)
            localeController.applyLocale(tag)
        }
    }

    fun setTxDisplayPrefs(prefs: TxDisplayPrefs) {
        viewModelScope.launch { appSettingsRepository.setTxDisplayPrefs(prefs) }
    }

    fun refreshPinState() {
        _state.update { it.copy(pinEnabled = settings.getBoolean(SecurityPrefs.PIN_ENABLED)) }
    }
}
