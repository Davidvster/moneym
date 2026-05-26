package com.dv.moneym.feature.settings.overview.backuprestore

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dv.moneym.core.common.DispatcherProvider
import com.dv.moneym.core.datastore.AppSettings
import com.dv.moneym.core.datastore.PrefKeys
import com.dv.moneym.core.oauth.AuthState
import com.dv.moneym.core.oauth.GoogleAuthManager
import com.dv.moneym.data.backup.DbBackupManager
import com.dv.moneym.data.remotebackup.RemoteBackupManager
import com.dv.moneym.data.remotebackup.RemoteBackupMetadata
import com.dv.moneym.data.remotebackup.RemoteBackupRuntimeState
import com.dv.moneym.data.remotebackup.SessionPassphrase
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
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
    // Remote backup
    val remoteAvailable: Boolean = false,
    val remoteSignedIn: Boolean = false,
    val remoteAccountEmail: String? = null,
    val remoteAutoEnabled: Boolean = false,
    val lastRemoteBackupMs: Long = 0L,
    val remotePassphraseSet: Boolean = false,
    val showPassphraseDialog: Boolean = false,
    val passphraseError: String? = null,
    val showRemoteRestoreDialog: Boolean = false,
    val showDisconnectDialog: Boolean = false,
    val remoteRestorePreview: RemoteBackupMetadata? = null,
    val remoteRestorePreviewLoading: Boolean = false,
    val lastLocalMutationMs: Long = 0L,
    val remoteRuntime: RemoteBackupRuntimeState = RemoteBackupRuntimeState.Idle,
)

sealed interface BackupRestoreIntent {
    data object BackupTapped : BackupRestoreIntent
    data class RestoreFileSelected(val bytes: ByteArray) : BackupRestoreIntent
    data object RestoreConfirmed : BackupRestoreIntent
    data object RestoreDismissed : BackupRestoreIntent
    data class AutoBackupToggled(val enabled: Boolean) : BackupRestoreIntent
    data class BackupSaveCompleted(val path: String?) : BackupRestoreIntent
    data class AutoBackupLocationSelected(val uri: String?) : BackupRestoreIntent
    // Remote
    data object ConnectGoogleTapped : BackupRestoreIntent
    data object DisconnectGoogleTapped : BackupRestoreIntent
    data object DisconnectGoogleConfirmed : BackupRestoreIntent
    data object DisconnectGoogleDismissed : BackupRestoreIntent
    data class RemoteAutoBackupToggled(val enabled: Boolean) : BackupRestoreIntent
    data object PassphrasePromptOpened : BackupRestoreIntent
    data object PassphrasePromptDismissed : BackupRestoreIntent
    data class PassphraseSubmitted(val value: CharArray) : BackupRestoreIntent
    data object RemoteBackupNowTapped : BackupRestoreIntent
    data object RemoteRestoreTapped : BackupRestoreIntent
    data class RemoteRestoreConfirmed(val passphrase: CharArray) : BackupRestoreIntent
    data object RemoteRestoreDismissed : BackupRestoreIntent
}

sealed interface BackupRestoreEffect {
    data class LaunchFileSaver(val bytes: ByteArray, val fileName: String) : BackupRestoreEffect
    data object LaunchRestorePicker : BackupRestoreEffect
    data class RestoreError(val message: String) : BackupRestoreEffect
    data object LaunchFolderPicker : BackupRestoreEffect
    data class RemoteError(val message: String) : BackupRestoreEffect
    data object RemoteSignedIn : BackupRestoreEffect
}

class BackupRestoreViewModel(
    private val dbBackupManager: DbBackupManager,
    private val appSettings: AppSettings,
    private val dispatchers: DispatcherProvider,
    private val googleAuthManager: GoogleAuthManager? = null,
    private val remoteBackupManager: RemoteBackupManager? = null,
    private val sessionPassphrase: SessionPassphrase? = null,
    @Suppress("UNUSED_PARAMETER") savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val _base = MutableStateFlow(
        BackupRestoreUiState(
            autoBackupEnabled = appSettings.getBoolean(PrefKeys.AUTO_BACKUP_ENABLED),
            lastBackupTimeMs = appSettings.getString(PrefKeys.LAST_BACKUP_TIME_MS)?.toLongOrNull() ?: 0L,
            lastBackupPath = appSettings.getString(PrefKeys.LAST_BACKUP_PATH),
            remoteAvailable = googleAuthManager?.isConfigured == true,
            remoteAutoEnabled = appSettings.getBoolean(PrefKeys.AUTO_REMOTE_BACKUP_ENABLED),
            lastRemoteBackupMs = appSettings.getLong(PrefKeys.LAST_REMOTE_BACKUP_TIME_MS),
            remoteAccountEmail = appSettings.getString(PrefKeys.REMOTE_BACKUP_ACCOUNT_EMAIL),
            remotePassphraseSet = sessionPassphrase?.isSet?.value == true,
            lastLocalMutationMs = appSettings.getLong(PrefKeys.LAST_LOCAL_MUTATION_MS),
        )
    )

    private val authFlow = googleAuthManager?.state ?: flowOf(AuthState.SignedOut)
    private val runtimeFlow = remoteBackupManager?.runtime ?: flowOf(RemoteBackupRuntimeState.Idle)
    private val passphraseFlow = sessionPassphrase?.isSet ?: flowOf(false)

    val state: StateFlow<BackupRestoreUiState> = combine(
        _base,
        appSettings.observeString(PrefKeys.LAST_BACKUP_TIME_MS),
        appSettings.observeString(PrefKeys.LAST_BACKUP_PATH),
        authFlow,
        combine(runtimeFlow, passphraseFlow) { rt, ps -> rt to ps },
    ) { base, timeStr, path, auth, rtPs ->
        val signedIn = auth is AuthState.SignedIn
        val email = (auth as? AuthState.SignedIn)?.email
        base.copy(
            lastBackupTimeMs = timeStr?.toLongOrNull() ?: base.lastBackupTimeMs,
            lastBackupPath = path ?: base.lastBackupPath,
            remoteSignedIn = signedIn,
            remoteAccountEmail = email ?: base.remoteAccountEmail,
            remoteRuntime = rtPs.first,
            remotePassphraseSet = rtPs.second,
            lastLocalMutationMs = appSettings.getLong(PrefKeys.LAST_LOCAL_MUTATION_MS),
        )
    }.stateIn(viewModelScope, SharingStarted.Lazily, _base.value)

    private val _effects = Channel<BackupRestoreEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    private var pendingRestoreBytes: ByteArray? = null

    fun onIntent(intent: BackupRestoreIntent) {
        when (intent) {
            BackupRestoreIntent.BackupTapped -> handleBackupTapped()
            is BackupRestoreIntent.RestoreFileSelected -> handleRestoreFileSelected(intent.bytes)
            BackupRestoreIntent.RestoreConfirmed -> handleRestoreConfirmed()
            BackupRestoreIntent.RestoreDismissed -> handleRestoreDismissed()
            is BackupRestoreIntent.AutoBackupToggled -> handleAutoBackupToggled(intent.enabled)
            is BackupRestoreIntent.BackupSaveCompleted -> handleBackupSaveCompleted(intent.path)
            is BackupRestoreIntent.AutoBackupLocationSelected -> handleAutoBackupLocationSelected(intent.uri)
            BackupRestoreIntent.ConnectGoogleTapped -> handleConnectGoogle()
            BackupRestoreIntent.DisconnectGoogleTapped -> _base.update { it.copy(showDisconnectDialog = true) }
            BackupRestoreIntent.DisconnectGoogleConfirmed -> handleDisconnectGoogle()
            BackupRestoreIntent.DisconnectGoogleDismissed -> _base.update { it.copy(showDisconnectDialog = false) }
            is BackupRestoreIntent.RemoteAutoBackupToggled -> handleRemoteAutoToggled(intent.enabled)
            BackupRestoreIntent.PassphrasePromptOpened -> _base.update { it.copy(showPassphraseDialog = true, passphraseError = null) }
            BackupRestoreIntent.PassphrasePromptDismissed -> _base.update { it.copy(showPassphraseDialog = false, passphraseError = null) }
            is BackupRestoreIntent.PassphraseSubmitted -> handlePassphraseSubmitted(intent.value)
            BackupRestoreIntent.RemoteBackupNowTapped -> handleRemoteBackupNow()
            BackupRestoreIntent.RemoteRestoreTapped -> handleRemoteRestoreTapped()
            is BackupRestoreIntent.RemoteRestoreConfirmed -> handleRemoteRestoreConfirmed(intent.passphrase)
            BackupRestoreIntent.RemoteRestoreDismissed -> _base.update {
                it.copy(showRemoteRestoreDialog = false, remoteRestorePreview = null)
            }
        }
    }

    private fun handleBackupTapped() {
        _base.update { it.copy(isLoading = true, showBackupSuccess = false) }
        viewModelScope.launch {
            val bytes = withContext(dispatchers.io) { dbBackupManager.export() }
            _effects.send(BackupRestoreEffect.LaunchFileSaver(bytes, "moneym-backup.zip"))
        }
    }

    private fun handleRestoreFileSelected(bytes: ByteArray) {
        pendingRestoreBytes = bytes
        _base.update { it.copy(showRestoreWarning = true) }
    }

    private fun handleRestoreConfirmed() {
        val bytes = pendingRestoreBytes ?: return
        pendingRestoreBytes = null
        _base.update { it.copy(isLoading = true, showRestoreWarning = false) }
        viewModelScope.launch {
            try {
                withContext(dispatchers.io) { dbBackupManager.restore(bytes) }
            } catch (e: Exception) {
                _base.update { it.copy(isLoading = false) }
                _effects.send(BackupRestoreEffect.RestoreError(e.message ?: "Restore failed"))
            }
        }
    }

    private fun handleRestoreDismissed() {
        pendingRestoreBytes = null
        _base.update { it.copy(showRestoreWarning = false) }
    }

    private fun handleAutoBackupToggled(enabled: Boolean) {
        if (enabled) {
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

    private fun handleBackupSaveCompleted(path: String?) {
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

    private fun handleAutoBackupLocationSelected(uri: String?) {
        if (uri != null) {
            appSettings.putString(PrefKeys.AUTO_BACKUP_DIR_URI, uri)
            appSettings.putBoolean(PrefKeys.AUTO_BACKUP_ENABLED, true)
            _base.update { it.copy(autoBackupEnabled = true) }
        }
    }

    private fun handleConnectGoogle() {
        val manager = googleAuthManager ?: return
        viewModelScope.launch {
            val result = manager.signIn()
            result.onSuccess { signedIn ->
                signedIn.email?.let { appSettings.putString(PrefKeys.REMOTE_BACKUP_ACCOUNT_EMAIL, it) }
                _effects.send(BackupRestoreEffect.RemoteSignedIn)
            }.onFailure { t ->
                _effects.send(BackupRestoreEffect.RemoteError(t.message ?: "Sign-in failed"))
            }
        }
    }

    private fun handleDisconnectGoogle() {
        val manager = googleAuthManager ?: return
        _base.update { it.copy(showDisconnectDialog = false) }
        viewModelScope.launch {
            manager.signOut()
            appSettings.putBoolean(PrefKeys.AUTO_REMOTE_BACKUP_ENABLED, false)
            appSettings.remove(PrefKeys.REMOTE_BACKUP_ACCOUNT_EMAIL)
            sessionPassphrase?.clear()
            _base.update { it.copy(remoteAutoEnabled = false, remotePassphraseSet = false) }
        }
    }

    private fun handleRemoteAutoToggled(enabled: Boolean) {
        if (enabled) {
            if (sessionPassphrase?.isSet?.value != true) {
                _base.update { it.copy(showPassphraseDialog = true, passphraseError = null) }
                return
            }
            appSettings.putBoolean(PrefKeys.AUTO_REMOTE_BACKUP_ENABLED, true)
            _base.update { it.copy(remoteAutoEnabled = true) }
            remoteBackupManager?.enqueueUpload()
        } else {
            appSettings.putBoolean(PrefKeys.AUTO_REMOTE_BACKUP_ENABLED, false)
            _base.update { it.copy(remoteAutoEnabled = false) }
        }
    }

    private fun handlePassphraseSubmitted(value: CharArray) {
        if (value.size < MIN_PASSPHRASE_LENGTH) {
            _base.update { it.copy(passphraseError = "Passphrase must be at least $MIN_PASSPHRASE_LENGTH characters") }
            return
        }
        sessionPassphrase?.set(value)
        appSettings.putBoolean(PrefKeys.AUTO_REMOTE_BACKUP_ENABLED, true)
        _base.update {
            it.copy(
                showPassphraseDialog = false,
                passphraseError = null,
                remoteAutoEnabled = true,
                remotePassphraseSet = true,
            )
        }
        remoteBackupManager?.enqueueUpload()
    }

    private fun handleRemoteBackupNow() {
        val manager = remoteBackupManager ?: return
        if (sessionPassphrase?.isSet?.value != true) {
            _base.update { it.copy(showPassphraseDialog = true, passphraseError = null) }
            return
        }
        viewModelScope.launch {
            manager.flushNow().onFailure { t ->
                _effects.send(BackupRestoreEffect.RemoteError(t.message ?: "Backup failed"))
            }
        }
    }

    private fun handleRemoteRestoreTapped() {
        val manager = remoteBackupManager ?: return
        _base.update {
            it.copy(
                showRemoteRestoreDialog = true,
                remoteRestorePreviewLoading = true,
                remoteRestorePreview = null,
            )
        }
        viewModelScope.launch {
            manager.peekLatestMetadata()
                .onSuccess { meta ->
                    _base.update {
                        it.copy(
                            remoteRestorePreview = meta,
                            remoteRestorePreviewLoading = false,
                        )
                    }
                }
                .onFailure { t ->
                    _base.update {
                        it.copy(
                            showRemoteRestoreDialog = false,
                            remoteRestorePreviewLoading = false,
                        )
                    }
                    _effects.send(BackupRestoreEffect.RemoteError(t.message ?: "Failed to fetch backup info"))
                }
        }
    }

    private fun handleRemoteRestoreConfirmed(passphrase: CharArray) {
        val manager = remoteBackupManager ?: return
        _base.update { it.copy(showRemoteRestoreDialog = false, isLoading = true) }
        viewModelScope.launch {
            manager.restoreLatest(passphrase).onFailure { t ->
                _base.update { it.copy(isLoading = false) }
                _effects.send(BackupRestoreEffect.RemoteError(t.message ?: "Restore failed"))
            }
            passphrase.fill(' ')
        }
    }

    companion object {
        const val MIN_PASSPHRASE_LENGTH = 8
    }
}
