package com.dv.moneym.core.oauth

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class OAuthTokenClientTest {

    private val config = GoogleOAuthConfig(
        clientId = "abc.apps.googleusercontent.com",
        redirectUri = "com.dv.moneym://oauth",
    )

    @Test
    fun exchange_returnsStoredTokens() = runTest {
        var capturedBody: String? = null
        val http = HttpClient(MockEngine { req ->
            capturedBody = req.body.toByteArray().decodeToString()
            respond(
                content = """{"access_token":"at","refresh_token":"rt","expires_in":3600,"token_type":"Bearer"}""",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        })
        val client = OAuthTokenClient(http, nowMs = { 1_000L })
        val tokens = client.exchangeAuthCode(config, code = "AUTHCODE", codeVerifier = "VERIFIER")
        assertEquals("at", tokens.accessToken)
        assertEquals("rt", tokens.refreshToken)
        assertEquals(1_000L + (3600 - 60) * 1000L, tokens.expiresAtMs)
        val body = capturedBody!!
        assertEquals(true, body.contains("grant_type=authorization_code"))
        assertEquals(true, body.contains("code=AUTHCODE"))
        assertEquals(true, body.contains("code_verifier=VERIFIER"))
    }

    @Test
    fun exchange_throwsWhenNoRefreshToken() = runTest {
        val http = HttpClient(MockEngine {
            respond(
                """{"access_token":"at","expires_in":3600}""",
                HttpStatusCode.OK,
                headersOf(HttpHeaders.ContentType, "application/json"),
            )
        })
        val client = OAuthTokenClient(http)
        assertFailsWith<GoogleAuthError.NoRefreshToken> {
            client.exchangeAuthCode(config, "c", "v")
        }
    }

    @Test
    fun exchange_throwsOnHttpError() = runTest {
        val http = HttpClient(MockEngine {
            respond(
                """{"error":"invalid_grant","error_description":"bad code"}""",
                HttpStatusCode.BadRequest,
                headersOf(HttpHeaders.ContentType, "application/json"),
            )
        })
        val client = OAuthTokenClient(http)
        val err = assertFailsWith<GoogleAuthError.TokenExchange> {
            client.exchangeAuthCode(config, "c", "v")
        }
        assertEquals("bad code", err.description)
    }

    @Test
    fun refresh_updatesAccessTokenAndExpiry() = runTest {
        val http = HttpClient(MockEngine {
            respond(
                """{"access_token":"new","expires_in":1800}""",
                HttpStatusCode.OK,
                headersOf(HttpHeaders.ContentType, "application/json"),
            )
        })
        val client = OAuthTokenClient(http, nowMs = { 5_000L })
        val prev = StoredTokens(refreshToken = "rt", accessToken = "old", expiresAtMs = 0L, email = "u@x")
        val updated = client.refresh(config, "rt", prev)
        assertEquals("new", updated.accessToken)
        assertEquals("rt", updated.refreshToken)
        assertEquals("u@x", updated.email)
        assertEquals(5_000L + (1800 - 60) * 1000L, updated.expiresAtMs)
    }
}
