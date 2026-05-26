package com.dv.moneym.core.oauth

data class GoogleOAuthConfig(
    val clientId: String?,
    val redirectUri: String,
    val scopes: List<String> = listOf("https://www.googleapis.com/auth/drive.appdata"),
) {
    val isConfigured: Boolean get() = !clientId.isNullOrBlank()

    companion object {
        const val DEFAULT_REDIRECT = "com.dv.moneym://oauth"
    }
}
