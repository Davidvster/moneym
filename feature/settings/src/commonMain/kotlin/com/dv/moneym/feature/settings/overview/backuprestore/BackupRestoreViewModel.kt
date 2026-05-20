package com.dv.moneym.feature.settings.overview.backuprestore

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dv.moneym.core.datastore.AppSettings
import com.dv.moneym.core.datastore.PrefKeys
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

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
    data class RestoreFileSelected(val content: ByteArray) : BackupRestoreIntent
    data object RestoreConfirmed : BackupRestoreIntent
    data object RestoreDismissed : BackupRestoreIntent
    data class AutoBackupToggled(val enabled: Boolean) : BackupRestoreIntent
    data class BackupSaveCompleted(val success: Boolean) : BackupRestoreIntent
}

sealed interface BackupRestoreEffect {
    data object DoExport : BackupRestoreEffect
    data class DoRestore(val content: ByteArray) : BackupRestoreEffect
    data class RestoreError(val message: String) : BackupRestoreEffect
}

class BackupRestoreViewModel(
    private val appSettings: AppSettings,
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

    private var pendingRestoreContent: ByteArray? = null

    fun onIntent(intent: BackupRestoreIntent) {
        when (intent) {
            BackupRestoreIntent.BackupTapped -> {
                _base.update { it.copy(isLoading = true, showBackupSuccess = false) }
                viewModelScope.launch {
                    _effects.send(BackupRestoreEffect.DoExport)
                }
            }

            is BackupRestoreIntent.RestoreFileSelected -> {
                pendingRestoreContent = intent.content
                _base.update { it.copy(showRestoreWarning = true) }
            }

            BackupRestoreIntent.RestoreConfirmed -> {
                val content = pendingRestoreContent ?: return
                pendingRestoreContent = null
                _base.update { it.copy(isLoading = true, showRestoreWarning = false) }
                viewModelScope.launch {
                    _effects.send(BackupRestoreEffect.DoRestore(content))
                }
            }

            BackupRestoreIntent.RestoreDismissed -> {
                pendingRestoreContent = null
                _base.update { it.copy(showRestoreWarning = false) }
            }

            is BackupRestoreIntent.AutoBackupToggled -> {
                appSettings.putBoolean(PrefKeys.AUTO_BACKUP_ENABLED, intent.enabled)
                _base.update { it.copy(autoBackupEnabled = intent.enabled) }
            }

            is BackupRestoreIntent.BackupSaveCompleted -> {
                _base.update { it.copy(isLoading = false, showBackupSuccess = intent.success) }
            }
        }
    }
}
