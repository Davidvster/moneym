package com.dv.moneym.core.oauth

import dev.whyoleg.cryptography.BinarySize.Companion.bytes
import dev.whyoleg.cryptography.CryptographyProvider
import dev.whyoleg.cryptography.algorithms.SHA256
import dev.whyoleg.cryptography.random.CryptographyRandom
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

data class PkcePair(val verifier: String, val challenge: String)

object Pkce {
    private const val VERIFIER_BYTES = 64

    @OptIn(ExperimentalEncodingApi::class)
    suspend fun generate(provider: CryptographyProvider = CryptographyProvider.Default): PkcePair {
        val raw = CryptographyRandom.Default.nextBytes(VERIFIER_BYTES)
        val verifier = Base64.UrlSafe.encode(raw).trimEnd('=')
        val hasher = provider.get(SHA256).hasher()
        val digest = hasher.hash(verifier.encodeToByteArray())
        val challenge = Base64.UrlSafe.encode(digest).trimEnd('=')
        return PkcePair(verifier, challenge)
    }

    @OptIn(ExperimentalEncodingApi::class)
    fun randomState(bytes: Int = 16): String {
        val raw = CryptographyRandom.Default.nextBytes(bytes)
        return Base64.UrlSafe.encode(raw).trimEnd('=')
    }
}
