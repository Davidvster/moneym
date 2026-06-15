package com.dv.moneym.core.security

import kotlinx.serialization.Serializable

@Serializable
data class EncryptedBackup(
    val version: Int = ENVELOPE_VERSION,
    val schema: Int,
    val createdAt: Long,
    val appVersion: String,
    val kdf: KdfParams,
    val cipher: CipherParams,
) {
    companion object {
        const val ENVELOPE_VERSION = 1
    }
}

@Serializable
data class KdfParams(
    val name: String = "PBKDF2-HMAC-SHA256",
    val iter: Int,
    val saltB64: String,
)

@Serializable
data class CipherParams(
    val name: String = "AES-256-GCM",
    val ivB64: String,
    val ctB64: String,
    val tagBits: Int = 128,
)

sealed class BackupCryptoError(message: String, cause: Throwable? = null) :
    RuntimeException(message, cause) {
    class WrongPassphrase(cause: Throwable? = null) :
        BackupCryptoError("Wrong passphrase or corrupted backup", cause)
    class UnsupportedEnvelope(val got: Int, val max: Int) :
        BackupCryptoError("Unsupported envelope version $got (max $max)")
    class PlatformFailure(cause: Throwable) :
        BackupCryptoError("Crypto platform failure: ${cause.message}", cause)
}

interface BackupCrypto {
    suspend fun encrypt(
        plain: ByteArray,
        passphrase: CharArray,
        schema: Int,
        appVersion: String,
        createdAt: Long,
    ): EncryptedBackup

    suspend fun decrypt(envelope: EncryptedBackup, passphrase: CharArray): ByteArray
}

object BackupCryptoConstants {
    const val PBKDF2_ITERATIONS = 600_000
    const val KEY_BITS = 256
    const val SALT_BYTES = 16
    const val IV_BYTES = 12
    const val TAG_BITS = 128
}
