package com.dv.moneym.data.backup

import com.dv.moneym.core.security.BackupCrypto
import com.dv.moneym.core.security.BackupEnvelopeJson

/**
 * Wraps/unwraps backup payloads with the shared [BackupCrypto] envelope. Used by both local
 * (file / auto) and remote backups so encrypted archives have one on-disk format. An encrypted
 * archive is a JSON [com.dv.moneym.core.security.EncryptedBackup] envelope (first byte `{`);
 * a plaintext archive is the raw zip from [DbBackupManager.export].
 */
class BackupCodec(
    private val crypto: BackupCrypto,
    private val appVersion: String,
) {
    fun isEncrypted(bytes: ByteArray): Boolean =
        bytes.isNotEmpty() && bytes[0] == '{'.code.toByte()

    suspend fun seal(
        plain: ByteArray,
        passphrase: CharArray?,
        schema: Int,
        createdAt: Long,
    ): ByteArray =
        if (passphrase == null) {
            plain
        } else {
            BackupEnvelopeJson.encodeBytes(
                crypto.encrypt(
                    plain = plain,
                    passphrase = passphrase,
                    schema = schema,
                    appVersion = appVersion,
                    createdAt = createdAt,
                ),
            )
        }

    suspend fun open(bytes: ByteArray, passphrase: CharArray): ByteArray =
        crypto.decrypt(BackupEnvelopeJson.decodeBytes(bytes), passphrase)

    companion object {
        const val CURRENT_SCHEMA = 1
    }
}
