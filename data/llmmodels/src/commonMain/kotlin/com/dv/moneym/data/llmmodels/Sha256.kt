package com.dv.moneym.data.llmmodels

import dev.whyoleg.cryptography.CryptographyProvider
import dev.whyoleg.cryptography.algorithms.SHA256
import dev.whyoleg.cryptography.operations.HashFunction

internal expect fun platformCryptographyProvider(): CryptographyProvider

internal suspend fun sha256Hex(bytes: ByteArray): String =
    platformCryptographyProvider().get(SHA256).hasher().hash(bytes).toSha256Hex()

/** Incremental SHA-256 — feed chunks via [HashFunction.update], then read [HashFunction.hashToByteArray].
 *  Lets multi-GB model files be hashed without loading them fully into memory. */
internal fun sha256HashFunction(): HashFunction =
    platformCryptographyProvider().get(SHA256).hasher().createHashFunction()

internal fun ByteArray.toSha256Hex(): String =
    joinToString("") { byte -> (byte.toInt() and 0xFF).toString(16).padStart(2, '0') }
