package com.dv.moneym.feature.onboarding.restore

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.serialization.saved
import androidx.lifecycle.viewModelScope
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

    private var pendingRestoreBytes: ByteArray? = null

    fun onIntent(intent: OnboardingRestoreIntent) {
        when (intent) {
            is OnboardingRestoreIntent.LocalFileSelected -> handleLocalFileSelected(intent.bytes)
            is OnboardingRestoreIntent.LocalRestoreConfirmed -> handleLocalRestoreConfirmed(intent.passphrase)
            OnboardingRestoreIntent.LocalRestoreDismissed -> handleLocalRestoreDismissed()
            OnboardingRestoreIntent.ConnectGoogleTapped -> handleConnectGoogle()
            OnboardingRestoreIntent.RemoteRestoreTapped -> handleRemoteRestoreTapped()
            is OnboardingRestoreIntent.RemoteRestoreConfirmed -> handleRemoteRestoreConfirmed(intent.passphrase)
            OnboardingRestoreIntent.RemoteRestoreDismissed -> _base.update {
                it.copy(showRemoteRestoreDialog = false, remotePreview = null, remoteError = null)
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
        _base.update { it.copy(isLoading = true, showLocalRestoreDialog = false, localError = null) }
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
                _base.update {
                    it.copy(isLoading = false, showLocalRestoreDialog = true, localError = e.message)
                }
            } catch (e: Exception) {
                _base.update {
                    it.copy(isLoading = false, showLocalRestoreDialog = true, localError = e.message)
                }
            }
        }
    }

    private fun handleLocalRestoreDismissed() {
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
                    _base.update { it.copy(remoteError = t.message ?: "Sign-in failed") }
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
                    _base.update {
                        it.copy(
                            showRemoteRestoreDialog = false,
                            remotePreviewLoading = false,
                            remoteError = t.message ?: "Failed to fetch backup info",
                        )
                    }
                }
        }
    }

    private fun handleRemoteRestoreConfirmed(passphrase: CharArray) {
        val manager = remoteBackupManager ?: return
        _base.update { it.copy(showRemoteRestoreDialog = false, isLoading = true) }
        viewModelScope.launch {
            appSettings.putBoolean(PrefKeys.ONBOARDING_COMPLETED, true)
            manager.restoreLatest(passphrase).onFailure { t ->
                _base.update { it.copy(isLoading = false, remoteError = t.message ?: "Restore failed") }
            }
            passphrase.fill(' ')
        }
    }
}
