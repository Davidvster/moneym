package com.dv.moneym.core.oauth

import kotlinx.coroutines.flow.StateFlow

sealed interface AuthState {
    data object SignedOut : AuthState
    data class SignedIn(val email: String?) : AuthState
}

interface GoogleAuthManager {
    val isConfigured: Boolean
    val state: StateFlow<AuthState>

    suspend fun signIn(): Result<AuthState.SignedIn>
    suspend fun signOut()
    suspend fun accessToken(): String?
}

sealed class GoogleAuthError(message: String, cause: Throwable? = null) :
    RuntimeException(message, cause) {
    class NotConfigured : GoogleAuthError("Google OAuth client ID is not configured")
    class UserCancelled : GoogleAuthError("Sign-in was cancelled")
    class StateMismatch : GoogleAuthError("OAuth state mismatch — possible CSRF attempt")
    class NoRefreshToken : GoogleAuthError("Google did not return a refresh token; revoke app access and retry")
    class TokenExchange(val description: String, cause: Throwable? = null) :
        GoogleAuthError("Token exchange failed: $description", cause)
    class Platform(reason: String) : GoogleAuthError("Platform auth failure: $reason")
}
