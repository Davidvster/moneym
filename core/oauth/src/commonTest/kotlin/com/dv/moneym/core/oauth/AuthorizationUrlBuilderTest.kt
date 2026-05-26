package com.dv.moneym.core.oauth

import io.ktor.http.Url
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class AuthorizationUrlBuilderTest {

    @Test
    fun includesAllRequiredParameters() {
        val cfg = GoogleOAuthConfig(
            clientId = "fake-id.apps.googleusercontent.com",
            redirectUri = "com.dv.moneym://oauth",
        )
        val url = Url(AuthorizationUrlBuilder.build(cfg, state = "S1", challenge = "C1"))
        assertEquals("accounts.google.com", url.host)
        assertEquals("/o/oauth2/v2/auth", url.encodedPath)
        val p = url.parameters
        assertEquals("fake-id.apps.googleusercontent.com", p["client_id"])
        assertEquals("com.dv.moneym://oauth", p["redirect_uri"])
        assertEquals("code", p["response_type"])
        assertEquals("https://www.googleapis.com/auth/drive.appdata", p["scope"])
        assertEquals("S1", p["state"])
        assertEquals("C1", p["code_challenge"])
        assertEquals("S256", p["code_challenge_method"])
        assertEquals("offline", p["access_type"])
        assertEquals("consent", p["prompt"])
        assertTrue(p["include_granted_scopes"] == "true")
    }

    @Test
    fun throws_whenClientIdMissing() {
        val cfg = GoogleOAuthConfig(clientId = null, redirectUri = "x")
        assertFailsWith<GoogleAuthError.NotConfigured> {
            AuthorizationUrlBuilder.build(cfg, "s", "c")
        }
    }
}
