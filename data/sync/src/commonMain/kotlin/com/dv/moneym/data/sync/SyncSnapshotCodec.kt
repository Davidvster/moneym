package com.dv.moneym.data.sync

import com.dv.moneym.core.security.BackupCrypto
import com.dv.moneym.core.security.BackupEnvelopeJson
import kotlinx.serialization.json.Json

class SyncSnapshotCodec(
    private val crypto: BackupCrypto,
    private val appVersion: String,
    private val schemaVersion: Int = 1,
    private val nowMs: () -> Long = { kotlin.time.Clock.System.now().toEpochMilliseconds() },
) {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun encode(snapshot: SyncSnapshot): ByteArray =
        json.encodeToString(SyncSnapshot.serializer(), snapshot).encodeToByteArray()

    fun decode(bytes: ByteArray): SyncSnapshot =
        json.decodeFromString(SyncSnapshot.serializer(), bytes.decodeToString())

    suspend fun seal(snapshot: SyncSnapshot, passphrase: CharArray?): ByteArray {
        val plain = encode(snapshot)
        if (passphrase == null) return plain
        val envelope = crypto.encrypt(
            plain = plain,
            passphrase = passphrase,
            schema = schemaVersion,
            appVersion = appVersion,
            createdAt = nowMs(),
        )
        return BackupEnvelopeJson.encodeBytes(envelope)
    }

    suspend fun open(bytes: ByteArray, passphrase: CharArray?): SyncSnapshot {
        if (passphrase == null) return decode(bytes)
        val envelope = BackupEnvelopeJson.decodeBytes(bytes)
        val plain = crypto.decrypt(envelope, passphrase)
        return decode(plain)
    }

    /**
     * A plaintext [SyncSnapshot] and an encrypted envelope both serialize to JSON starting with
     * `{`, so the first-byte trick used for zip-based backups is ambiguous here. Detect an envelope
     * by trying to parse the required `EncryptedBackup` fields — a plaintext snapshot lacks them.
     */
    fun isEncryptedEnvelope(bytes: ByteArray): Boolean =
        runCatching { BackupEnvelopeJson.decodeBytes(bytes) }
            .map { it.cipher.ctB64.isNotBlank() }
            .getOrDefault(false)
}
