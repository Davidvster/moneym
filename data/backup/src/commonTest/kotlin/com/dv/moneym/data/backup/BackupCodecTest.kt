package com.dv.moneym.data.backup

import com.dv.moneym.core.security.BackupCrypto
import com.dv.moneym.core.security.BackupCryptoError
import com.dv.moneym.core.security.CipherParams
import com.dv.moneym.core.security.EncryptedBackup
import com.dv.moneym.core.security.KdfParams
import com.dv.moneym.core.testing.runTestWithDispatchers
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

private class FakeBackupCrypto : BackupCrypto {
    override suspend fun encrypt(
        plain: ByteArray,
        passphrase: CharArray,
        schema: Int,
        appVersion: String,
        createdAt: Long,
    ): EncryptedBackup = EncryptedBackup(
        schema = schema,
        createdAt = createdAt,
        appVersion = appVersion,
        kdf = KdfParams(iter = 1, saltB64 = passphrase.concatToString()),
        cipher = CipherParams(ivB64 = "", ctB64 = plain.joinToString(",") { it.toString() }),
    )

    override suspend fun decrypt(envelope: EncryptedBackup, passphrase: CharArray): ByteArray {
        if (envelope.kdf.saltB64 != passphrase.concatToString()) throw BackupCryptoError.WrongPassphrase()
        val ct = envelope.cipher.ctB64
        return if (ct.isEmpty()) ByteArray(0) else ct.split(",").map { it.toByte() }.toByteArray()
    }
}

class BackupCodecTest {

    private val codec = BackupCodec(FakeBackupCrypto(), appVersion = "test")

    @Test
    fun seal_without_passphrase_returns_plaintext() = runTestWithDispatchers {
        val plain = byteArrayOf(1, 2, 3)
        val out = codec.seal(plain, passphrase = null, schema = 1, createdAt = 0L)
        assertContentEquals(plain, out)
        assertFalse(codec.isEncrypted(out))
    }

    @Test
    fun seal_then_open_round_trips() = runTestWithDispatchers {
        val plain = "hello world".encodeToByteArray()
        val sealed = codec.seal(plain, "secret".toCharArray(), schema = 1, createdAt = 123L)
        assertTrue(codec.isEncrypted(sealed))
        val opened = codec.open(sealed, "secret".toCharArray())
        assertContentEquals(plain, opened)
    }

    @Test
    fun open_with_wrong_passphrase_throws() = runTestWithDispatchers {
        val sealed = codec.seal("hi".encodeToByteArray(), "right".toCharArray(), schema = 1, createdAt = 0L)
        assertFailsWith<BackupCryptoError.WrongPassphrase> {
            codec.open(sealed, "wrong".toCharArray())
        }
    }
}
