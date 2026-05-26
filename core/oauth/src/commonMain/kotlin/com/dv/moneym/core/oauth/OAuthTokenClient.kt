package com.dv.moneym.core.oauth

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.forms.FormDataContent
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.http.Parameters
import io.ktor.http.parameters
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
internal data class GoogleTokenResponse(
    @SerialName("access_token") val accessToken: String? = null,
    @SerialName("refresh_token") val refreshToken: String? = null,
    @SerialName("expires_in") val expiresIn: Long? = null,
    @SerialName("token_type") val tokenType: String? = null,
    @SerialName("id_token") val idToken: String? = null,
    @SerialName("scope") val scope: String? = null,
    @SerialName("error") val error: String? = null,
    @SerialName("error_description") val errorDescription: String? = null,
)

class OAuthTokenClient(
    private val httpClient: HttpClient,
    private val json: Json = Json { ignoreUnknownKeys = true; isLenient = true },
    private val nowMs: () -> Long = { kotlin.time.Clock.System.now().toEpochMilliseconds() },
) {

    suspend fun exchangeAuthCode(
        config: GoogleOAuthConfig,
        code: String,
        codeVerifier: String,
    ): StoredTokens {
        val clientId = config.clientId ?: throw GoogleAuthError.NotConfigured()
        val params = parameters {
            append("client_id", clientId)
            append("grant_type", "authorization_code")
            append("code", code)
            append("redirect_uri", config.redirectUri)
            append("code_verifier", codeVerifier)
        }
        val resp = postForm(params)
        val email = resp.idToken?.let(::extractEmailFromIdToken)
        val refresh = resp.refreshToken ?: throw GoogleAuthError.NoRefreshToken()
        return StoredTokens(
            refreshToken = refresh,
            accessToken = resp.accessToken,
            expiresAtMs = computeExpiry(resp.expiresIn),
            email = email,
        )
    }

    suspend fun refresh(config: GoogleOAuthConfig, refreshToken: String, previous: StoredTokens): StoredTokens {
        val clientId = config.clientId ?: throw GoogleAuthError.NotConfigured()
        val params = parameters {
            append("client_id", clientId)
            append("grant_type", "refresh_token")
            append("refresh_token", refreshToken)
        }
        val resp = postForm(params)
        return previous.copy(
            accessToken = resp.accessToken ?: previous.accessToken,
            expiresAtMs = computeExpiry(resp.expiresIn),
        )
    }

    suspend fun revoke(token: String) {
        runCatching {
            httpClient.post(OAuthEndpoints.GOOGLE_REVOCATION) {
                setBody(FormDataContent(parameters { append("token", token) }))
            }
        }
    }

    private suspend fun postForm(params: Parameters): GoogleTokenResponse {
        val response = httpClient.post(OAuthEndpoints.GOOGLE_TOKEN) {
            setBody(FormDataContent(params))
        }
        val text = response.bodyAsText()
        val parsed = json.decodeFromString(GoogleTokenResponse.serializer(), text)
        if (response.status != HttpStatusCode.OK || parsed.error != null) {
            throw GoogleAuthError.TokenExchange(parsed.errorDescription ?: parsed.error ?: text.take(500))
        }
        return parsed
    }

    private fun computeExpiry(expiresIn: Long?): Long {
        val seconds = expiresIn ?: 0L
        return nowMs() + (seconds - SAFETY_SECONDS).coerceAtLeast(0) * 1_000L
    }

    private fun extractEmailFromIdToken(idToken: String): String? {
        val parts = idToken.split('.')
        if (parts.size < 2) return null
        return runCatching {
            val payload = base64UrlDecode(parts[1]).decodeToString()
            EMAIL_REGEX.find(payload)?.groupValues?.get(1)
        }.getOrNull()
    }

    companion object {
        private const val SAFETY_SECONDS = 60L
        private val EMAIL_REGEX = Regex("\"email\"\\s*:\\s*\"([^\"]+)\"")
    }
}

@OptIn(kotlin.io.encoding.ExperimentalEncodingApi::class)
private fun base64UrlDecode(input: String): ByteArray {
    val padded = input + "=".repeat((4 - input.length % 4) % 4)
    return kotlin.io.encoding.Base64.UrlSafe.decode(padded)
}
