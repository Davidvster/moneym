package com.dv.moneym.core.security

import com.dv.moneym.core.common.DispatcherProvider
import dev.whyoleg.cryptography.BinarySize.Companion.bits
import dev.whyoleg.cryptography.BinarySize.Companion.bytes
import dev.whyoleg.cryptography.CryptographyProvider
import dev.whyoleg.cryptography.DelicateCryptographyApi
import dev.whyoleg.cryptography.algorithms.AES
import dev.whyoleg.cryptography.algorithms.PBKDF2
import dev.whyoleg.cryptography.algorithms.SHA256
import dev.whyoleg.cryptography.random.CryptographyRandom
import kotlinx.coroutines.withContext
import kotlinx.io.bytestring.ByteString

@OptIn(DelicateCryptographyApi::class)
class DefaultBackupCrypto(
    private val dispatchers: DispatcherProvider,
    provider: CryptographyProvider = platformCryptographyProvider(),
    internal val pbkdf2Iterations: Int = BackupCryptoConstants.PBKDF2_ITERATIONS,
) : BackupCrypto {

    private val aesGcm = provider.get(AES.GCM)
    private val pbkdf2 = provider.get(PBKDF2)

    override suspend fun encrypt(
        plain: ByteArray,
        passphrase: CharArray,
        schema: Int,
        appVersion: String,
        createdAt: Long,
    ): EncryptedBackup = withContext(dispatchers.default) {
        val salt = CryptographyRandom.Default.nextBytes(BackupCryptoConstants.SALT_BYTES)
        val iv = CryptographyRandom.Default.nextBytes(BackupCryptoConstants.IV_BYTES)
        val keyBytes = deriveKey(passphrase, salt, pbkdf2Iterations)
        val ciphertext = try {
            val key = aesGcm.keyDecoder().decodeFromByteArray(AES.Key.Format.RAW, keyBytes)
            key.cipher(tagSize = BackupCryptoConstants.TAG_BITS.bits)
                .encryptWithIv(iv, plain)
        } catch (t: Throwable) {
            throw BackupCryptoError.PlatformFailure(t.message ?: "encrypt failed")
        } finally {
            keyBytes.fill(0)
        }
        EncryptedBackup(
            schema = schema,
            createdAt = createdAt,
            appVersion = appVersion,
            kdf = KdfParams(
                iter = pbkdf2Iterations,
                saltB64 = salt.toBase64(),
            ),
            cipher = CipherParams(
                ivB64 = iv.toBase64(),
                ctB64 = ciphertext.toBase64(),
                tagBits = BackupCryptoConstants.TAG_BITS,
            ),
        )
    }

    override suspend fun decrypt(envelope: EncryptedBackup, passphrase: CharArray): ByteArray =
        withContext(dispatchers.default) {
            if (envelope.version > EncryptedBackup.ENVELOPE_VERSION) {
                throw BackupCryptoError.UnsupportedEnvelope(
                    envelope.version,
                    EncryptedBackup.ENVELOPE_VERSION,
                )
            }
            val salt = envelope.kdf.saltB64.fromBase64()
            val iv = envelope.cipher.ivB64.fromBase64()
            val ct = envelope.cipher.ctB64.fromBase64()
            val keyBytes = deriveKey(passphrase, salt, envelope.kdf.iter)
            try {
                val key = aesGcm.keyDecoder().decodeFromByteArray(AES.Key.Format.RAW, keyBytes)
                key.cipher(tagSize = envelope.cipher.tagBits.bits)
                    .decryptWithIv(iv, ct)
            } catch (t: Throwable) {
                throw BackupCryptoError.WrongPassphrase()
            } finally {
                keyBytes.fill(0)
            }
        }

    private suspend fun deriveKey(passphrase: CharArray, salt: ByteArray, iterations: Int): ByteArray {
        val passBytes = passphrase.concatToString().encodeToByteArray()
        return try {
            pbkdf2.secretDerivation(
                digest = SHA256,
                iterations = iterations,
                outputSize = BackupCryptoConstants.KEY_BITS.bits,
                salt = ByteString(salt),
            ).deriveSecretToByteArray(passBytes)
        } finally {
            passBytes.fill(0)
        }
    }
}
