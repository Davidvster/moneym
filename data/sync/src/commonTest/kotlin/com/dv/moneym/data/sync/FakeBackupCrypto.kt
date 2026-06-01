package com.dv.moneym.data.sync

import com.dv.moneym.core.security.BackupCrypto
import com.dv.moneym.core.security.CipherParams
import com.dv.moneym.core.security.EncryptedBackup
import com.dv.moneym.core.security.KdfParams
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

@OptIn(ExperimentalEncodingApi::class)
class FakeBackupCrypto : BackupCrypto {
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
        kdf = KdfParams(iter = 1, saltB64 = ""),
        cipher = CipherParams(ivB64 = "", ctB64 = Base64.encode(plain)),
    )

    override suspend fun decrypt(envelope: EncryptedBackup, passphrase: CharArray): ByteArray =
        Base64.decode(envelope.cipher.ctB64)
}
