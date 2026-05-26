package com.dv.moneym.core.oauth

import com.dv.moneym.core.common.DispatcherProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

class DefaultGoogleAuthManager(
    private val config: GoogleOAuthConfig,
    private val tokenClient: OAuthTokenClient,
    private val launcher: AuthorizationLauncher,
    private val tokenStore: SecureTokenStore,
    private val dispatchers: DispatcherProvider,
    private val nowMs: () -> Long = { kotlin.time.Clock.System.now().toEpochMilliseconds() },
) : GoogleAuthManager {

    private val _state = MutableStateFlow<AuthState>(AuthState.SignedOut)
    private val refreshLock = Mutex()

    override val isConfigured: Boolean = config.isConfigured
    override val state: StateFlow<AuthState> = _state.asStateFlow()

    suspend fun initialize() = withContext(dispatchers.io) {
        if (!isConfigured) return@withContext
        val stored = tokenStore.read() ?: return@withContext
        _state.value = AuthState.SignedIn(stored.email)
    }

    override suspend fun signIn(): Result<AuthState.SignedIn> = runCatching {
        if (!isConfigured) throw GoogleAuthError.NotConfigured()
        val pkce = Pkce.generate()
        val state = Pkce.randomState()
        val url = AuthorizationUrlBuilder.build(config, state, pkce.challenge)
        val request = AuthorizationRequest(url = url, state = state, verifier = pkce.verifier)
        val response = launcher.launch(request, config.redirectUri)
        if (response.state != state) throw GoogleAuthError.StateMismatch()
        val tokens = tokenClient.exchangeAuthCode(config, response.code, pkce.verifier)
        tokenStore.write(tokens)
        val signedIn = AuthState.SignedIn(tokens.email)
        _state.value = signedIn
        signedIn
    }

    override suspend fun signOut() = withContext(dispatchers.io) {
        val stored = tokenStore.read()
        stored?.refreshToken?.let { tokenClient.revoke(it) }
        tokenStore.clear()
        _state.value = AuthState.SignedOut
    }

    override suspend fun accessToken(): String? = withContext(dispatchers.io) {
        if (!isConfigured) return@withContext null
        val current = tokenStore.read() ?: return@withContext null
        val now = nowMs()
        if (current.accessToken != null && current.expiresAtMs > now + REFRESH_THRESHOLD_MS) {
            return@withContext current.accessToken
        }
        refreshLock.withLock {
            val latest = tokenStore.read() ?: return@withLock null
            if (latest.accessToken != null && latest.expiresAtMs > now + REFRESH_THRESHOLD_MS) {
                return@withLock latest.accessToken
            }
            val refreshed = runCatching {
                tokenClient.refresh(config, latest.refreshToken, latest)
            }.getOrElse {
                _state.value = AuthState.SignedOut
                tokenStore.clear()
                throw it
            }
            tokenStore.write(refreshed)
            _state.value = AuthState.SignedIn(refreshed.email)
            refreshed.accessToken
        }
    }

    companion object {
        private const val REFRESH_THRESHOLD_MS = 30_000L
    }
}
