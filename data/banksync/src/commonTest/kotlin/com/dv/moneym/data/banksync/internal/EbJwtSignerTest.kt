package com.dv.moneym.data.banksync.internal

import com.dv.moneym.data.banksync.EbError
import dev.whyoleg.cryptography.algorithms.RSA
import dev.whyoleg.cryptography.algorithms.SHA256
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlin.time.Instant
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

@OptIn(ExperimentalEncodingApi::class)
class EbJwtSignerTest {

    private val base64Url = Base64.UrlSafe.withPadding(Base64.PaddingOption.ABSENT)

    @Test
    fun signsVerifiableRs256Jwt() = runTest {
        val provider = platformCryptographyProvider()
        val pkcs1 = provider.get(RSA.PKCS1)
        val keyPair = pkcs1.keyPairGenerator(digest = SHA256).generateKey()
        val pem = keyPair.privateKey.encodeToByteArray(RSA.PrivateKey.Format.PEM).decodeToString()

        val signer = EbJwtSigner(provider)
        val jwt = signer.sign("app-123", pem, Instant.fromEpochSeconds(1_000_000))

        val parts = jwt.split(".")
        assertEquals(3, parts.size)

        val header = Json.parseToJsonElement(base64Url.decode(parts[0]).decodeToString()).jsonObject
        assertEquals("RS256", header["alg"]?.jsonPrimitive?.content)
        assertEquals("app-123", header["kid"]?.jsonPrimitive?.content)

        val payload = Json.parseToJsonElement(base64Url.decode(parts[1]).decodeToString()).jsonObject
        assertEquals("enablebanking.com", payload["iss"]?.jsonPrimitive?.content)
        assertEquals("api.enablebanking.com", payload["aud"]?.jsonPrimitive?.content)
        assertEquals(1_000_000L + 3600L, payload["exp"]?.jsonPrimitive?.content?.toLong())

        val verified = keyPair.publicKey.signatureVerifier().tryVerifySignature(
            data = "${parts[0]}.${parts[1]}".encodeToByteArray(),
            signature = base64Url.decode(parts[2]),
        )
        assertTrue(verified)
    }

    @Test
    fun rejectsPkcs1PemWithConversionHint() = runTest {
        val signer = EbJwtSigner(platformCryptographyProvider())
        val error = assertFailsWith<EbError.InvalidPrivateKey> {
            signer.sign(
                "app-123",
                "-----BEGIN RSA PRIVATE KEY-----\nabc\n-----END RSA PRIVATE KEY-----",
                Instant.fromEpochSeconds(0),
            )
        }
        assertTrue(error.message!!.contains("pkcs8"))
    }

    @Test
    fun rejectsGarbageInput() = runTest {
        val signer = EbJwtSigner(platformCryptographyProvider())
        assertFailsWith<EbError.InvalidPrivateKey> {
            signer.sign("app-123", "not a key at all", Instant.fromEpochSeconds(0))
        }
    }
}
