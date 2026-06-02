package com.dv.moneym.feature.onboarding.restore

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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import moneym.feature.onboarding.generated.resources.Res
import moneym.feature.onboarding.generated.resources.onboarding_restore_error_fetch_info
import moneym.feature.onboarding.generated.resources.onboarding_restore_error_restore_failed
import moneym.feature.onboarding.generated.resources.onboarding_restore_error_sign_in_failed
import org.jetbrains.compose.resources.getString

@Serializable
data class OnboardingRestoreUiState(
    val remoteAvailable: Boolean = false,
    val remoteSignedIn: Boolean = false,
    val remoteAccountEmail: String? = null,
    val showLocalRestoreDialog: Boolean = false,
    val localNeedsPassphrase: Boolean = false,
    val localError: String? = null,
    val showRemoteRestoreDialog: Boolean = false,
    val remotePreview: RemoteBackupMetadata? = null,
    val remotePreviewLoading: Boolean = false,
    val remoteError: String? = null,
    val restoreRunning: Boolean = false,
    val isLoading: Boolean = false,
)

sealed interface OnboardingRestoreIntent {
    data class LocalFileSelected(val bytes: ByteArray) : OnboardingRestoreIntent
    data class LocalRestoreConfirmed(val passphrase: CharArray? = null) : OnboardingRestoreIntent
    data object LocalRestoreDismissed : OnboardingRestoreIntent
    data object ConnectGoogleTapped : OnboardingRestoreIntent
    data object RemoteRestoreTapped : OnboardingRestoreIntent
    data class RemoteRestoreConfirmed(val passphrase: CharArray) : OnboardingRestoreIntent
    data object RemoteRestoreDismissed : OnboardingRestoreIntent
}

class OnboardingRestoreViewModel(
    private val dbBackupManager: DbBackupManager,
    private val backupCodec: BackupCodec,
    private val appSettings: AppSettings,
    private val dispatchers: DispatcherProvider,
    private val googleAuthManager: GoogleAuthManager? = null,
    private val remoteBackupManager: RemoteBackupManager? = null,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val _base by savedStateHandle.saved {
        MutableStateFlow(
            OnboardingRestoreUiState(
                remoteAvailable = googleAuthManager?.isConfigured == true,
                remoteAccountEmail = appSettings.getString(PrefKeys.REMOTE_BACKUP_ACCOUNT_EMAIL),
            )
        )
    }

    private val authFlow = googleAuthManager?.state ?: flowOf(AuthState.SignedOut)

    val state: StateFlow<OnboardingRestoreUiState> = combine(_base, authFlow) { base, auth ->
        val signedIn = auth is AuthState.SignedIn
        val email = (auth as? AuthState.SignedIn)?.email
        base.copy(
            remoteSignedIn = signedIn,
            remoteAccountEmail = email ?: base.remoteAccountEmail,
        )
    }.stateIn(viewModelScope, SharingStarted.Lazily, _base.value)

    private val logger = AppLogger.tag("OnboardingRestore")

    private var pendingRestoreBytes: ByteArray? = null

    fun onIntent(intent: OnboardingRestoreIntent) {
        when (intent) {
            is OnboardingRestoreIntent.LocalFileSelected -> handleLocalFileSelected(intent.bytes)
            is OnboardingRestoreIntent.LocalRestoreConfirmed -> handleLocalRestoreConfirmed(intent.passphrase)
            OnboardingRestoreIntent.LocalRestoreDismissed -> handleLocalRestoreDismissed()
            OnboardingRestoreIntent.ConnectGoogleTapped -> handleConnectGoogle()
            OnboardingRestoreIntent.RemoteRestoreTapped -> handleRemoteRestoreTapped()
            is OnboardingRestoreIntent.RemoteRestoreConfirmed -> handleRemoteRestoreConfirmed(intent.passphrase)
            OnboardingRestoreIntent.RemoteRestoreDismissed -> if (!_base.value.restoreRunning) {
                _base.update {
                    it.copy(showRemoteRestoreDialog = false, remotePreview = null, remoteError = null)
                }
            }
        }
    }

    private fun handleLocalFileSelected(bytes: ByteArray) {
        pendingRestoreBytes = bytes
        _base.update {
            it.copy(
                showLocalRestoreDialog = true,
                localNeedsPassphrase = backupCodec.isEncrypted(bytes),
                localError = null,
            )
        }
    }

    private fun handleLocalRestoreConfirmed(passphrase: CharArray?) {
        val bytes = pendingRestoreBytes ?: return
        val needsPassphrase = backupCodec.isEncrypted(bytes)
        if (needsPassphrase && (passphrase == null || passphrase.isEmpty())) return
        _base.update { it.copy(restoreRunning = true, showLocalRestoreDialog = true, localError = null) }
        viewModelScope.launch {
            try {
                val plain = withContext(dispatchers.io) {
                    if (needsPassphrase) backupCodec.open(bytes, passphrase!!) else bytes
                }
                passphrase?.fill(' ')
                pendingRestoreBytes = null
                appSettings.putBoolean(PrefKeys.ONBOARDING_COMPLETED, true)
                withContext(dispatchers.io) { dbBackupManager.restore(plain) }
            } catch (e: BackupCryptoError) {
                logger.e(e) { "Local restore crypto error" }
                _base.update {
                    it.copy(restoreRunning = false, showLocalRestoreDialog = true, localError = e.message)
                }
            } catch (e: Exception) {
                logger.e(e) { "Local restore failed" }
                _base.update {
                    it.copy(restoreRunning = false, showLocalRestoreDialog = true, localError = e.message)
                }
            }
        }
    }

    private fun handleLocalRestoreDismissed() {
        if (_base.value.restoreRunning) return
        pendingRestoreBytes = null
        _base.update {
            it.copy(showLocalRestoreDialog = false, localNeedsPassphrase = false, localError = null)
        }
    }

    private fun handleConnectGoogle() {
        val manager = googleAuthManager ?: return
        viewModelScope.launch {
            manager.signIn()
                .onSuccess { signedIn ->
                    signedIn.email?.let {
                        appSettings.putString(PrefKeys.REMOTE_BACKUP_ACCOUNT_EMAIL, it)
                    }
                }
                .onFailure { t ->
                    logger.e(t) { "Google sign-in failed" }
                    val msg = t.message ?: getString(Res.string.onboarding_restore_error_sign_in_failed)
                    _base.update { it.copy(remoteError = msg) }
                }
        }
    }

    private fun handleRemoteRestoreTapped() {
        val manager = remoteBackupManager ?: return
        _base.update {
            it.copy(
                showRemoteRestoreDialog = true,
                remotePreviewLoading = true,
                remotePreview = null,
                remoteError = null,
            )
        }
        viewModelScope.launch {
            manager.peekLatestMetadata()
                .onSuccess { meta ->
                    _base.update { it.copy(remotePreview = meta, remotePreviewLoading = false) }
                }
                .onFailure { t ->
                    logger.e(t) { "Remote restore metadata peek failed" }
                    val msg = t.message ?: getString(Res.string.onboarding_restore_error_fetch_info)
                    _base.update {
                        it.copy(
                            showRemoteRestoreDialog = true,
                            remotePreviewLoading = false,
                            remotePreview = null,
                            remoteError = msg,
                        )
                    }
                }
        }
    }

    private fun handleRemoteRestoreConfirmed(passphrase: CharArray) {
        val manager = remoteBackupManager ?: return
        _base.update { it.copy(restoreRunning = true, remoteError = null) }
        viewModelScope.launch {
            appSettings.putBoolean(PrefKeys.ONBOARDING_COMPLETED, true)
            manager.restoreLatest(passphrase).onFailure { t ->
                logger.e(t) { "Remote restore failed" }
                val msg = t.message ?: getString(Res.string.onboarding_restore_error_restore_failed)
                _base.update { it.copy(restoreRunning = false, remoteError = msg) }
            }
            passphrase.fill(' ')
        }
    }
}
