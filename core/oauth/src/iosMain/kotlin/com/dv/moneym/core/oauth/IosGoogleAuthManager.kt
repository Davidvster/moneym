package com.dv.moneym.core.oauth

import com.dv.moneym.core.datastore.AppSettings
import com.dv.moneym.core.datastore.PrefKeys
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class IosGoogleAuthManager(
    private val config: GoogleOAuthConfig,
    private val appSettings: AppSettings,
) : GoogleAuthManager {

    private val _state = MutableStateFlow<AuthState>(
        appSettings.getString(PrefKeys.REMOTE_BACKUP_ACCOUNT_EMAIL)
            ?.takeIf { it.isNotBlank() }
            ?.let { AuthState.SignedIn(it) }
            ?: AuthState.SignedOut,
    )

    override val isConfigured: Boolean = config.isConfigured
    override val state: StateFlow<AuthState> = _state.asStateFlow()

    override suspend fun signIn(): Result<AuthState.SignedIn> = runCatching {
        if (!isConfigured) throw GoogleAuthError.NotConfigured()
        val bridge = GoogleSignInBridgeHolder.instance
            ?: throw GoogleAuthError.Platform("GoogleSignIn bridge not registered")
        val email = suspendCancellableCoroutine { cont ->
            bridge.signIn(config.scopes) { email, error ->
                if (error != null) cont.resumeWithException(GoogleAuthError.Platform(error))
                else cont.resume(email)
            }
        }
        appSettings.putString(PrefKeys.REMOTE_BACKUP_ACCOUNT_EMAIL, email ?: "")
        val signedIn = AuthState.SignedIn(email)
        _state.value = signedIn
        signedIn
    }

    override suspend fun signOut() {
        GoogleSignInBridgeHolder.instance?.signOut()
        appSettings.remove(PrefKeys.REMOTE_BACKUP_ACCOUNT_EMAIL)
        _state.value = AuthState.SignedOut
    }

    override suspend fun accessToken(): String? {
        if (!isConfigured) return null
        val bridge = GoogleSignInBridgeHolder.instance ?: return null
        return suspendCancellableCoroutine { cont ->
            bridge.currentAccessToken { token, error ->
                if (error != null) cont.resume(null) else cont.resume(token)
            }
        }
    }
}
