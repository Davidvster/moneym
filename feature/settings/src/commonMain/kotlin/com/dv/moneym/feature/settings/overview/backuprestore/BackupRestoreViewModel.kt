package com.dv.moneym.feature.settings.overview.backuprestore

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dv.moneym.core.common.DispatcherProvider
import com.dv.moneym.core.datastore.AppSettings
import com.dv.moneym.core.datastore.PrefKeys
import com.dv.moneym.data.backup.DbBackupManager
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.time.Clock

data class BackupRestoreUiState(
    val isLoading: Boolean = false,
    val showRestoreWarning: Boolean = false,
    val autoBackupEnabled: Boolean = false,
    val showBackupSuccess: Boolean = false,
    val lastBackupTimeMs: Long = 0L,
    val lastBackupPath: String? = null,
)

sealed interface BackupRestoreIntent {
    data object BackupTapped : BackupRestoreIntent
    data class RestoreFileSelected(val bytes: ByteArray) : BackupRestoreIntent
    data object RestoreConfirmed : BackupRestoreIntent
    data object RestoreDismissed : BackupRestoreIntent
    data class AutoBackupToggled(val enabled: Boolean) : BackupRestoreIntent
    data class BackupSaveCompleted(val path: String?) : BackupRestoreIntent
    data class AutoBackupLocationSelected(val uri: String?) : BackupRestoreIntent
}

sealed interface BackupRestoreEffect {
    data class LaunchFileSaver(val bytes: ByteArray, val fileName: String) : BackupRestoreEffect
    data object LaunchRestorePicker : BackupRestoreEffect
    data class RestoreError(val message: String) : BackupRestoreEffect
    data object LaunchFolderPicker : BackupRestoreEffect
}

class BackupRestoreViewModel(
    private val dbBackupManager: DbBackupManager,
    private val appSettings: AppSettings,
    private val dispatchers: DispatcherProvider,
    @Suppress("UNUSED_PARAMETER") savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val _base = MutableStateFlow(
        BackupRestoreUiState(
            autoBackupEnabled = appSettings.getBoolean(PrefKeys.AUTO_BACKUP_ENABLED),
            lastBackupTimeMs = appSettings.getString(PrefKeys.LAST_BACKUP_TIME_MS)?.toLongOrNull() ?: 0L,
            lastBackupPath = appSettings.getString(PrefKeys.LAST_BACKUP_PATH),
        )
    )

    val state: StateFlow<BackupRestoreUiState> = combine(
        _base,
        appSettings.observeString(PrefKeys.LAST_BACKUP_TIME_MS),
        appSettings.observeString(PrefKeys.LAST_BACKUP_PATH),
    ) { base, timeStr, path ->
        base.copy(
            lastBackupTimeMs = timeStr?.toLongOrNull() ?: base.lastBackupTimeMs,
            lastBackupPath = path ?: base.lastBackupPath,
        )
    }.stateIn(viewModelScope, SharingStarted.Lazily, _base.value)

    private val _effects = Channel<BackupRestoreEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    private var pendingRestoreBytes: ByteArray? = null

    fun onIntent(intent: BackupRestoreIntent) {
        when (intent) {
            BackupRestoreIntent.BackupTapped -> {
                _base.update { it.copy(isLoading = true, showBackupSuccess = false) }
                viewModelScope.launch {
                    val bytes = withContext(dispatchers.io) { dbBackupManager.export() }
                    _effects.send(BackupRestoreEffect.LaunchFileSaver(bytes, "moneym-backup.zip"))
                }
            }

            is BackupRestoreIntent.RestoreFileSelected -> {
                pendingRestoreBytes = intent.bytes
                _base.update { it.copy(showRestoreWarning = true) }
            }

            BackupRestoreIntent.RestoreConfirmed -> {
                val bytes = pendingRestoreBytes ?: return
                pendingRestoreBytes = null
                _base.update { it.copy(isLoading = true, showRestoreWarning = false) }
                viewModelScope.launch {
                    try {
                        withContext(dispatchers.io) { dbBackupManager.restore(bytes) }
                        // restore() calls terminateApp() — code below won't execute
                    } catch (e: Exception) {
                        _base.update { it.copy(isLoading = false) }
                        _effects.send(BackupRestoreEffect.RestoreError(e.message ?: "Restore failed"))
                    }
                }
            }

            BackupRestoreIntent.RestoreDismissed -> {
                pendingRestoreBytes = null
                _base.update { it.copy(showRestoreWarning = false) }
            }

            is BackupRestoreIntent.AutoBackupToggled -> {
                if (intent.enabled) {
                    val hasDirUri = appSettings.getString(PrefKeys.AUTO_BACKUP_DIR_URI) != null
                    if (hasDirUri) {
                        appSettings.putBoolean(PrefKeys.AUTO_BACKUP_ENABLED, true)
                        _base.update { it.copy(autoBackupEnabled = true) }
                    } else {
                        viewModelScope.launch { _effects.send(BackupRestoreEffect.LaunchFolderPicker) }
                    }
                } else {
                    appSettings.putBoolean(PrefKeys.AUTO_BACKUP_ENABLED, false)
                    _base.update { it.copy(autoBackupEnabled = false) }
                }
            }

            is BackupRestoreIntent.BackupSaveCompleted -> {
                val path = intent.path
                if (path != null) {
                    appSettings.putString(PrefKeys.LAST_BACKUP_PATH, path)
                    appSettings.putString(
                        PrefKeys.LAST_BACKUP_TIME_MS,
                        Clock.System.now().toEpochMilliseconds().toString(),
                    )
                    _base.update { it.copy(isLoading = false, showBackupSuccess = true) }
                } else {
                    _base.update { it.copy(isLoading = false) }
                }
            }

            is BackupRestoreIntent.AutoBackupLocationSelected -> {
                val uri = intent.uri
                if (uri != null) {
                    appSettings.putString(PrefKeys.AUTO_BACKUP_DIR_URI, uri)
                    appSettings.putBoolean(PrefKeys.AUTO_BACKUP_ENABLED, true)
                    _base.update { it.copy(autoBackupEnabled = true) }
                }
            }
        }
    }
}
