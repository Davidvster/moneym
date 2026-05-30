package com.dv.moneym.core.oauth

data class GoogleOAuthConfig(
    val clientId: String?,
    val serverClientId: String? = null,
    val scopes: List<String> = listOf("https://www.googleapis.com/auth/drive.appdata"),
) {
    val isConfigured: Boolean get() = !clientId.isNullOrBlank() || !serverClientId.isNullOrBlank()
}
