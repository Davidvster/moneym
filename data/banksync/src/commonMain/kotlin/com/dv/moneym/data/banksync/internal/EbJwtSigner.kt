package com.dv.moneym.data.banksync.internal

import com.dv.moneym.data.banksync.EbError
import dev.whyoleg.cryptography.CryptographyProvider
import dev.whyoleg.cryptography.algorithms.RSA
import dev.whyoleg.cryptography.algorithms.SHA256
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.time.Instant
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject

@OptIn(ExperimentalEncodingApi::class)
internal class EbJwtSigner(
    provider: CryptographyProvider = platformCryptographyProvider(),
) {
    private val pkcs1 = provider.get(RSA.PKCS1)
    private val base64Url = Base64.UrlSafe.withPadding(Base64.PaddingOption.ABSENT)

    suspend fun sign(applicationId: String, privateKeyPem: String, now: Instant): String {
        val pem = privateKeyPem.trim()
        if (pem.contains("BEGIN RSA PRIVATE KEY")) {
            throw EbError.InvalidPrivateKey(
                "PKCS#1 key detected. Convert it to PKCS#8 first: " +
                    "openssl pkcs8 -topk8 -nocrypt -in key.pem -out key-pkcs8.pem"
            )
        }
        if (!pem.contains("BEGIN PRIVATE KEY")) {
            throw EbError.InvalidPrivateKey("Expected a PEM-encoded PKCS#8 private key (-----BEGIN PRIVATE KEY-----).")
        }
        val privateKey = try {
            pkcs1.privateKeyDecoder(SHA256)
                .decodeFromByteArray(RSA.PrivateKey.Format.PEM, pem.encodeToByteArray())
        } catch (t: Throwable) {
            throw EbError.InvalidPrivateKey("Could not parse the private key: ${t.message}", t)
        }

        val header = buildJsonObject {
            put("typ", JsonPrimitive("JWT"))
            put("alg", JsonPrimitive("RS256"))
            put("kid", JsonPrimitive(applicationId))
        }
        val iat = now.epochSeconds
        val payload = buildJsonObject {
            put("iss", JsonPrimitive("enablebanking.com"))
            put("aud", JsonPrimitive("api.enablebanking.com"))
            put("iat", JsonPrimitive(iat))
            put("exp", JsonPrimitive(iat + 3600))
        }
        val signingInput =
            base64Url.encode(Json.encodeToString(header).encodeToByteArray()) +
                "." +
                base64Url.encode(Json.encodeToString(payload).encodeToByteArray())
        val signature = privateKey.signatureGenerator().generateSignature(signingInput.encodeToByteArray())
        return signingInput + "." + base64Url.encode(signature)
    }
}
