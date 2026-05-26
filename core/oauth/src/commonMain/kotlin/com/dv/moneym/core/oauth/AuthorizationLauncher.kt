package com.dv.moneym.core.oauth

import io.ktor.http.URLBuilder
import io.ktor.http.URLProtocol

data class AuthorizationRequest(
    val url: String,
    val state: String,
    val verifier: String,
)

data class AuthorizationResponse(
    val code: String,
    val state: String,
)

interface AuthorizationLauncher {
    suspend fun launch(request: AuthorizationRequest, redirectUri: String): AuthorizationResponse
}

object AuthorizationUrlBuilder {
    fun build(config: GoogleOAuthConfig, state: String, challenge: String): String {
        val clientId = config.clientId ?: throw GoogleAuthError.NotConfigured()
        return URLBuilder(OAuthEndpoints.GOOGLE_AUTHORIZATION).apply {
            protocol = URLProtocol.HTTPS
            parameters.append("client_id", clientId)
            parameters.append("redirect_uri", config.redirectUri)
            parameters.append("response_type", "code")
            parameters.append("scope", config.scopes.joinToString(" "))
            parameters.append("state", state)
            parameters.append("code_challenge", challenge)
            parameters.append("code_challenge_method", "S256")
            parameters.append("access_type", "offline")
            parameters.append("prompt", "consent")
            parameters.append("include_granted_scopes", "true")
        }.buildString()
    }
}
