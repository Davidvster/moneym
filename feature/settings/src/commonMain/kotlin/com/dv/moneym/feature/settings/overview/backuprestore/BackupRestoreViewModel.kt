package com.dv.moneym.feature.settings.overview.backuprestore

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.serialization.saved
import androidx.lifecycle.viewModelScope
import com.dv.moneym.core.common.AppLogger
import com.dv.moneym.core.common.DispatcherProvider
import com.dv.moneym.core.datastore.AppSettings
import com.dv.moneym.core.datastore.PrefKeys
import com.dv.moneym.core.oauth.AuthState
import com.dv.moneym.core.oauth.GoogleAuthManager
import com.dv.moneym.core.security.BackupCryptoError
import com.dv.moneym.data.backup.BackupCodec
import com.dv.moneym.data.backup.DbBackupManager
import com.dv.moneym.data.remotebackup.RemoteBackupManager
import com.dv.moneym.data.remotebackup.RemoteBackupMetadata
import com.dv.moneym.data.remotebackup.RemoteBackupRuntimeState
import com.dv.moneym.data.remotebackup.SessionPassphrase
import com.dv.moneym.data.remotebackup.SyncPassphraseStore
import com.dv.moneym.platform.FilePlatform
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
import kotlinx.serialization.Serializable
import moneym.feature.settings.generated.resources.Res
import moneym.feature.settings.generated.resources.settings_remote_error_backup_failed
import moneym.feature.settings.generated.resources.settings_remote_error_fetch_info
import moneym.feature.settings.generated.resources.settings_remote_error_restore_failed
import moneym.feature.settings.generated.resources.settings_remote_error_sign_in_failed
import moneym.feature.settings.generated.resources.settings_remote_restore_no_backup
import moneym.feature.settings.generated.resources.settings_remote_restore_try_again
import moneym.feature.settings.generated.resources.settings_remote_password_too_short
import org.jetbrains.compose.resources.getString
import kotlin.time.Clock

@Serializable
data class BackupRestoreUiState(
    val isLoading: Boolean = false,
    val isLocalLoading: Boolean = false,
    val showRestoreWarning: Boolean = false,
    val restoreNeedsPassphrase: Boolean = false,
    val restoreInProgress: Boolean = false,
    val restoreError: String? = null,
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
    val showDeleteRemoteDialog: Boolean = false,
    val remoteRestorePreview: RemoteBackupMetadata? = null,
    val remoteRestoreEncrypted: Boolean = false,
    val remoteRestoreErrorDialog: String? = null,
    val lastLocalMutationMs: Long = 0L,
    val remoteRuntime: RemoteBackupRuntimeState = RemoteBackupRuntimeState.Idle,
    val remoteRestoreInProgress: Boolean = false,
    val remoteRestoreError: String? = null,
)

enum class PendingBackup { RemoteAuto, RemoteNow, LocalAuto, LocalNow }

sealed interface BackupRestoreIntent {
    data object BackupTapped : BackupRestoreIntent
    data class RestoreFileSelected(val bytes: ByteArray) : BackupRestoreIntent
    data class RestoreConfirmed(val passphrase: CharArray? = null) : BackupRestoreIntent
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
    data class PasswordSubmitted(val value: CharArray, val encrypt: Boolean) : BackupRestoreIntent
    data object RemoteBackupNowTapped : BackupRestoreIntent
    data object RemoteRestoreTapped : BackupRestoreIntent
    data class RemoteRestoreConfirmed(val passphrase: CharArray) : BackupRestoreIntent
    data object RemoteRestoreDismissed : BackupRestoreIntent
    data object RemoteRestoreErrorDismissed : BackupRestoreIntent
    data object DeleteRemoteDataTapped : BackupRestoreIntent
    data object DeleteRemoteDataConfirmed : BackupRestoreIntent
    data object DeleteRemoteDataDismissed : BackupRestoreIntent
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
    private val backupCodec: BackupCodec,
    private val appSettings: AppSettings,
    private val dispatchers: DispatcherProvider,
    private val googleAuthManager: GoogleAuthManager? = null,
    private val remoteBackupManager: RemoteBackupManager? = null,
    private val sessionPassphrase: SessionPassphrase? = null,
    private val syncPassphraseStore: SyncPassphraseStore? = null,
    private val filePlatform: FilePlatform,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val _base by savedStateHandle.saved {
        MutableStateFlow(
            BackupRestoreUiState(
                autoBackupEnabled = appSettings.getBoolean(PrefKeys.AUTO_BACKUP_ENABLED),
                lastBackupTimeMs = appSettings.getString(PrefKeys.LAST_BACKUP_TIME_MS)
                    ?.toLongOrNull() ?: 0L,
                lastBackupPath = appSettings.getString(PrefKeys.LAST_BACKUP_PATH),
                remoteAvailable = googleAuthManager?.isConfigured == true,
                remoteAutoEnabled = appSettings.getBoolean(PrefKeys.AUTO_REMOTE_BACKUP_ENABLED),
                lastRemoteBackupMs = appSettings.getLong(PrefKeys.LAST_REMOTE_BACKUP_TIME_MS),
                remoteAccountEmail = appSettings.getString(PrefKeys.REMOTE_BACKUP_ACCOUNT_EMAIL),
                remotePassphraseSet = sessionPassphrase?.isSet?.value == true,
                lastLocalMutationMs = appSettings.getLong(PrefKeys.LAST_LOCAL_MUTATION_MS),
            )
        )
    }

    private val logger = AppLogger.tag("BackupRestore")

    private val authFlow = googleAuthManager?.state ?: flowOf(AuthState.SignedOut)
    private val runtimeFlow = remoteBackupManager?.runtime ?: flowOf(RemoteBackupRuntimeState.Idle)
    private val passphraseFlow = sessionPassphrase?.isSet ?: flowOf(false)

    val state: StateFlow<BackupRestoreUiState> = combine(
        _base,
        appSettings.observeString(PrefKeys.LAST_BACKUP_TIME_MS),
        appSettings.observeString(PrefKeys.LAST_BACKUP_PATH),
        authFlow,
        combine(runtimeFlow, passphraseFlow) { rt, ps ->
            Triple(rt, ps, appSettings.getLong(PrefKeys.LAST_REMOTE_BACKUP_TIME_MS))
        },
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
            lastRemoteBackupMs = rtPs.third.takeIf { it > 0L } ?: base.lastRemoteBackupMs,
        )
    }.stateIn(viewModelScope, SharingStarted.Lazily, _base.value)

    private val _effects = Channel<BackupRestoreEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    private var pendingRestoreBytes: ByteArray? = null
    private var pendingBackup: PendingBackup? = null

    fun onIntent(intent: BackupRestoreIntent) {
        when (intent) {
            BackupRestoreIntent.BackupTapped -> handleBackupTapped()
            is BackupRestoreIntent.RestoreFileSelected -> handleRestoreFileSelected(intent.bytes)
            is BackupRestoreIntent.RestoreConfirmed -> handleRestoreConfirmed(intent.passphrase)
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
            BackupRestoreIntent.PassphrasePromptDismissed -> {
                pendingBackup = null
                _base.update { it.copy(showPassphraseDialog = false, passphraseError = null) }
            }
            is BackupRestoreIntent.PasswordSubmitted -> handlePasswordSubmitted(intent.value, intent.encrypt)
            BackupRestoreIntent.RemoteBackupNowTapped -> handleRemoteBackupNow()
            BackupRestoreIntent.RemoteRestoreTapped -> handleRemoteRestoreTapped()
            is BackupRestoreIntent.RemoteRestoreConfirmed -> handleRemoteRestoreConfirmed(intent.passphrase)
            BackupRestoreIntent.RemoteRestoreDismissed -> if (!_base.value.remoteRestoreInProgress) {
                _base.update {
                    it.copy(
                        showRemoteRestoreDialog = false,
                        remoteRestorePreview = null,
                        remoteRestoreEncrypted = false,
                        remoteRestoreError = null,
                        remoteRestoreInProgress = false,
                    )
                }
            }
            BackupRestoreIntent.RemoteRestoreErrorDismissed -> _base.update {
                it.copy(remoteRestoreErrorDialog = null)
            }
            BackupRestoreIntent.DeleteRemoteDataTapped -> _base.update { it.copy(showDeleteRemoteDialog = true) }
            BackupRestoreIntent.DeleteRemoteDataConfirmed -> handleDeleteRemoteData()
            BackupRestoreIntent.DeleteRemoteDataDismissed -> _base.update { it.copy(showDeleteRemoteDialog = false) }
        }
    }

    private fun handleBackupTapped() {
        pendingBackup = PendingBackup.LocalNow
        _base.update { it.copy(showPassphraseDialog = true, passphraseError = null) }
    }

    private fun handleRestoreFileSelected(bytes: ByteArray) {
        pendingRestoreBytes = bytes
        _base.update {
            it.copy(
                showRestoreWarning = true,
                restoreNeedsPassphrase = backupCodec.isEncrypted(bytes),
                restoreError = null,
            )
        }
    }

    private fun handleRestoreConfirmed(passphrase: CharArray?) {
        val bytes = pendingRestoreBytes ?: return
        val needsPassphrase = backupCodec.isEncrypted(bytes)
        if (needsPassphrase && (passphrase == null || passphrase.isEmpty())) return
        _base.update { it.copy(restoreInProgress = true, showRestoreWarning = true, restoreError = null) }
        viewModelScope.launch {
            try {
                val plain = withContext(dispatchers.io) {
                    if (needsPassphrase) backupCodec.open(bytes, passphrase!!) else bytes
                }
                passphrase?.fill(' ')
                pendingRestoreBytes = null
                withContext(dispatchers.io) { dbBackupManager.restore(plain) }
            } catch (e: BackupCryptoError) {
                _base.update {
                    it.copy(restoreInProgress = false, showRestoreWarning = true, restoreError = e.message)
                }
            } catch (e: Exception) {
                logger.e(e) { "Local restore failed" }
                _base.update { it.copy(restoreInProgress = false) }
                _effects.send(BackupRestoreEffect.RestoreError(e.message ?: getString(Res.string.settings_remote_error_restore_failed)))
            }
        }
    }

    private fun handleRestoreDismissed() {
        if (_base.value.restoreInProgress) return
        pendingRestoreBytes = null
        _base.update { it.copy(showRestoreWarning = false, restoreNeedsPassphrase = false, restoreError = null) }
    }

    private fun handleAutoBackupToggled(enabled: Boolean) {
        if (enabled) {
            val hasDirUri = appSettings.getString(PrefKeys.AUTO_BACKUP_DIR_URI) != null
            if (hasDirUri) {
                pendingBackup = PendingBackup.LocalAuto
                _base.update { it.copy(showPassphraseDialog = true, passphraseError = null) }
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
            _base.update { it.copy(isLocalLoading = false, showBackupSuccess = true) }
        } else {
            _base.update { it.copy(isLocalLoading = false) }
        }
    }

    private fun handleAutoBackupLocationSelected(uri: String?) {
        if (uri != null) {
            appSettings.putString(PrefKeys.AUTO_BACKUP_DIR_URI, uri)
            pendingBackup = PendingBackup.LocalAuto
            _base.update { it.copy(showPassphraseDialog = true, passphraseError = null) }
        }
    }

    private fun runLocalBackupToFile() {
        _base.update { it.copy(isLocalLoading = true, showBackupSuccess = false) }
        viewModelScope.launch {
            val bytes = withContext(dispatchers.io) {
                backupCodec.seal(
                    plain = dbBackupManager.export(),
                    passphrase = localPassphraseOrNull(),
                    schema = BackupCodec.CURRENT_SCHEMA,
                    createdAt = Clock.System.now().toEpochMilliseconds(),
                )
            }
            _effects.send(BackupRestoreEffect.LaunchFileSaver(bytes, localBackupFileName()))
        }
    }

    private fun runImmediateLocalBackup() {
        _base.update { it.copy(isLocalLoading = true, showBackupSuccess = false) }
        viewModelScope.launch {
            val path = withContext(dispatchers.io) {
                val bytes = backupCodec.seal(
                    plain = dbBackupManager.export(),
                    passphrase = localPassphraseOrNull(),
                    schema = BackupCodec.CURRENT_SCHEMA,
                    createdAt = Clock.System.now().toEpochMilliseconds(),
                )
                val name = localBackupFileName()
                val dirUri = appSettings.getString(PrefKeys.AUTO_BACKUP_DIR_URI)
                if (dirUri != null && dirUri != "default") {
                    filePlatform.saveFileToDirBinary(dirUri, name, bytes)
                } else {
                    filePlatform.saveFileLocallyBinary(name, bytes)
                }
            }
            handleBackupSaveCompleted(path)
        }
    }

    private fun localEncryptEnabled(): Boolean =
        appSettings.getBoolean(PrefKeys.LOCAL_BACKUP_ENCRYPT, defaultValue = false)

    private fun localBackupFileName(): String =
        if (localEncryptEnabled()) "moneym-backup.bin" else "moneym-backup.zip"

    private fun localPassphraseOrNull(): CharArray? =
        if (localEncryptEnabled() && sessionPassphrase?.isSet?.value == true) sessionPassphrase.get() else null

    private fun handleConnectGoogle() {
        val manager = googleAuthManager ?: return
        viewModelScope.launch {
            val result = manager.signIn()
            result.onSuccess { signedIn ->
                signedIn.email?.let { appSettings.putString(PrefKeys.REMOTE_BACKUP_ACCOUNT_EMAIL, it) }
                _effects.send(BackupRestoreEffect.RemoteSignedIn)
            }.onFailure { t ->
                logger.e(t) { "Google sign-in failed" }
                _effects.send(BackupRestoreEffect.RemoteError(t.message ?: getString(Res.string.settings_remote_error_sign_in_failed)))
            }
        }
    }

    private fun handleDisconnectGoogle() {
        val manager = googleAuthManager ?: return
        _base.update { it.copy(showDisconnectDialog = false, isLoading = true) }
        viewModelScope.launch {
            try {
                manager.signOut()
                appSettings.putBoolean(PrefKeys.AUTO_REMOTE_BACKUP_ENABLED, false)
                appSettings.remove(PrefKeys.REMOTE_BACKUP_ACCOUNT_EMAIL)
                sessionPassphrase?.clear()
                syncPassphraseStore?.clear()
                _base.update { it.copy(remoteAutoEnabled = false, remotePassphraseSet = false) }
            } catch (t: Throwable) {
                logger.e(t) { "Google disconnect failed" }
            } finally {
                _base.update { it.copy(isLoading = false) }
            }
        }
    }

    private fun handleRemoteAutoToggled(enabled: Boolean) {
        if (enabled) {
            pendingBackup = PendingBackup.RemoteAuto
            _base.update { it.copy(showPassphraseDialog = true, passphraseError = null) }
        } else {
            appSettings.putBoolean(PrefKeys.AUTO_REMOTE_BACKUP_ENABLED, false)
            _base.update { it.copy(remoteAutoEnabled = false) }
        }
    }

    private fun handlePasswordSubmitted(value: CharArray, encrypt: Boolean) {
        val action = pendingBackup
        val isRemote = action == PendingBackup.RemoteAuto || action == PendingBackup.RemoteNow
        val encryptPref = if (isRemote) PrefKeys.REMOTE_BACKUP_ENCRYPT else PrefKeys.LOCAL_BACKUP_ENCRYPT
        if (encrypt) {
            if (value.size < MIN_PASSPHRASE_LENGTH) {
                viewModelScope.launch {
                    val msg = getString(Res.string.settings_remote_password_too_short, MIN_PASSPHRASE_LENGTH)
                    _base.update { it.copy(passphraseError = msg) }
                }
                return
            }
            sessionPassphrase?.set(value)
            val persistCopy = value.copyOf()
            viewModelScope.launch { syncPassphraseStore?.persist(persistCopy); persistCopy.fill(' ') }
            appSettings.putBoolean(encryptPref, true)
        } else {
            appSettings.putBoolean(encryptPref, false)
            viewModelScope.launch { syncPassphraseStore?.clear() }
        }
        value.fill(' ')
        pendingBackup = null
        _base.update {
            it.copy(
                showPassphraseDialog = false,
                passphraseError = null,
                remotePassphraseSet = if (isRemote) encrypt else it.remotePassphraseSet,
            )
        }
        when (action) {
            PendingBackup.RemoteAuto -> {
                appSettings.putBoolean(PrefKeys.AUTO_REMOTE_BACKUP_ENABLED, true)
                _base.update { it.copy(remoteAutoEnabled = true) }
                flushRemoteNow()
            }
            PendingBackup.RemoteNow -> flushRemoteNow()
            PendingBackup.LocalAuto -> {
                appSettings.putBoolean(PrefKeys.AUTO_BACKUP_ENABLED, true)
                _base.update { it.copy(autoBackupEnabled = true) }
                runImmediateLocalBackup()
            }
            PendingBackup.LocalNow -> runLocalBackupToFile()
            null -> Unit
        }
    }

    private fun handleRemoteBackupNow() {
        pendingBackup = PendingBackup.RemoteNow
        _base.update { it.copy(showPassphraseDialog = true, passphraseError = null) }
    }

    private fun flushRemoteNow() {
        val manager = remoteBackupManager ?: return
        viewModelScope.launch {
            manager.flushNow().onFailure { t ->
                logger.e(t) { "Remote backup-now failed" }
                _effects.send(BackupRestoreEffect.RemoteError(t.message ?: getString(Res.string.settings_remote_error_backup_failed)))
            }
        }
    }

    private fun handleRemoteRestoreTapped() {
        val manager = remoteBackupManager ?: return
        _base.update {
            it.copy(
                isLoading = true,
                showRemoteRestoreDialog = false,
                remoteRestorePreview = null,
                remoteRestoreError = null,
                remoteRestoreErrorDialog = null,
            )
        }
        viewModelScope.launch {
            manager.peekLatestMetadata()
                .onSuccess { meta ->
                    if (meta == null) {
                        _base.update {
                            it.copy(
                                isLoading = false,
                                remoteRestoreErrorDialog = getString(Res.string.settings_remote_restore_no_backup),
                            )
                        }
                    } else {
                        _base.update {
                            it.copy(
                                isLoading = false,
                                remoteRestorePreview = meta,
                                remoteRestoreEncrypted = meta.encrypted,
                                showRemoteRestoreDialog = true,
                            )
                        }
                    }
                }
                .onFailure { t ->
                    logger.e(t) { "Remote restore metadata peek failed" }
                    val reason = t.message ?: getString(Res.string.settings_remote_error_fetch_info)
                    val msg = "$reason ${getString(Res.string.settings_remote_restore_try_again)}"
                    _base.update {
                        it.copy(
                            isLoading = false,
                            remoteRestoreErrorDialog = msg,
                        )
                    }
                }
        }
    }

    private fun handleRemoteRestoreConfirmed(passphrase: CharArray) {
        val manager = remoteBackupManager ?: return
        _base.update { it.copy(remoteRestoreInProgress = true, remoteRestoreError = null) }
        viewModelScope.launch {
            manager.restoreLatest(passphrase)
                .onFailure { t ->
                    logger.e(t) { "Remote restore failed" }
                    val msg = t.message ?: getString(Res.string.settings_remote_error_restore_failed)
                    _base.update {
                        it.copy(
                            remoteRestoreInProgress = false,
                            remoteRestoreError = msg,
                        )
                    }
                }
                .onSuccess { _base.update { it.copy(remoteRestoreInProgress = false) } }
            passphrase.fill(' ')
        }
    }

    private fun handleDeleteRemoteData() {
        val manager = remoteBackupManager ?: return
        _base.update { it.copy(showDeleteRemoteDialog = false, isLoading = true) }
        viewModelScope.launch {
            manager.deleteAllRemoteData()
                .onFailure { logger.e(it) { "Delete remote data failed" } }
            appSettings.putBoolean(PrefKeys.AUTO_REMOTE_BACKUP_ENABLED, false)
            appSettings.putBoolean(PrefKeys.CROSS_DEVICE_SYNC_ENABLED, false)
            appSettings.putLong(PrefKeys.LAST_REMOTE_BACKUP_TIME_MS, 0L)
            _base.update {
                it.copy(
                    isLoading = false,
                    remoteAutoEnabled = false,
                    lastRemoteBackupMs = 0L,
                )
            }
        }
    }

    companion object {
        const val MIN_PASSPHRASE_LENGTH = 4
    }
}
