package com.dv.moneym.core.security

import com.dv.moneym.core.common.DispatcherProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals

class BackupCryptoRoundTripTest {

    private val dispatchers = object : DispatcherProvider {
        override val main = Dispatchers.Default
        override val default = Dispatchers.Default
        override val io = Dispatchers.Default
    }

    private val crypto = DefaultBackupCrypto(
        dispatchers = dispatchers,
        pbkdf2Iterations = 1_000,
    )

    @Test
    fun encrypt_then_decrypt_returnsOriginal() = runTest {
        val plain = "hello backup world".repeat(50).encodeToByteArray()
        val pass = "correct horse battery staple".toCharArray()
        val env = crypto.encrypt(
            plain = plain,
            passphrase = pass,
            schema = 1,
            appVersion = "0.0.1-test",
            createdAt = 1L,
        )
        val decrypted = crypto.decrypt(env, "correct horse battery staple".toCharArray())
        assertContentEquals(plain, decrypted)
    }

    @Test
    fun ciphertext_differs_from_plaintext() = runTest {
        val plain = "secrets".encodeToByteArray()
        val env = crypto.encrypt(plain, "pw".toCharArray(), 1, "x", 0L)
        val ct = env.cipher.ctB64.fromBase64()
        assertNotEquals(plain.toList(), ct.toList())
    }

    @Test
    fun wrong_passphrase_throws() = runTest {
        val env = crypto.encrypt("payload".encodeToByteArray(), "right".toCharArray(), 1, "x", 0L)
        assertFailsWith<BackupCryptoError.WrongPassphrase> {
            crypto.decrypt(env, "wrong".toCharArray())
        }
    }
}
