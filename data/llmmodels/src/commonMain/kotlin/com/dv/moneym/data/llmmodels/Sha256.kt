package com.dv.moneym.data.llmmodels

import dev.whyoleg.cryptography.CryptographyProvider
import dev.whyoleg.cryptography.algorithms.SHA256

internal expect fun platformCryptographyProvider(): CryptographyProvider

internal suspend fun sha256Hex(bytes: ByteArray): String {
    val digest = platformCryptographyProvider()
        .get(SHA256)
        .hasher()
        .hash(bytes)
    return digest.joinToString("") { byte -> (byte.toInt() and 0xFF).toString(16).padStart(2, '0') }
}
