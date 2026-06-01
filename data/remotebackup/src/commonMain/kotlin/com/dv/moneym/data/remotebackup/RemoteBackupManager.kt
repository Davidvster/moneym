package com.dv.moneym.data.remotebackup

import co.touchlab.kermit.Logger
import com.dv.moneym.core.common.DispatcherProvider
import com.dv.moneym.core.datastore.AppSettings
import com.dv.moneym.core.datastore.PrefKeys
import com.dv.moneym.core.security.BackupCrypto
import com.dv.moneym.core.security.BackupCryptoError
import com.dv.moneym.core.security.BackupEnvelopeJson
import com.dv.moneym.core.security.EncryptedBackup
import com.dv.moneym.data.backup.DbBackupManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

class RemoteBackupManager(
    private val dbBackupManager: DbBackupManager,
    private val crypto: BackupCrypto,
    private val provider: RemoteBackupProvider,
    private val appSettings: AppSettings,
    private val sessionPassphrase: SessionPassphrase,
    private val dispatchers: DispatcherProvider,
    private val appVersion: String,
    private val schemaVersion: Int = CURRENT_SCHEMA,
    private val nowMs: () -> Long = { kotlin.time.Clock.System.now().toEpochMilliseconds() },
    private val debounceMs: Long = DEFAULT_DEBOUNCE_MS,
    private val retentionCount: Int = DEFAULT_RETENTION_COUNT,
) {

    private val logger = Logger.withTag("RemoteBackup")
    private val pulse = MutableSharedFlow<Unit>(extraBufferCapacity = 64)
    private val uploadLock = Mutex()
    private var job: Job? = null

    private val _runtime = MutableStateFlow<RemoteBackupRuntimeState>(RemoteBackupRuntimeState.Idle)
    val runtime: StateFlow<RemoteBackupRuntimeState> = _runtime.asStateFlow()

    @OptIn(FlowPreview::class)
    fun start(scope: CoroutineScope) {
        job?.cancel()
        job = scope.launch(dispatchers.io) {
            pulse.debounce(debounceMs).collect {
                runUpload()
            }
        }
    }

    fun stop() {
        job?.cancel()
        job = null
    }

    fun enqueueUpload() {
        if (!isEnabled()) return
        if (encryptEnabled() && !sessionPassphrase.isSet.value) return
        pulse.tryEmit(Unit)
    }

    suspend fun flushNow(): Result<Unit> = runCatching {
        if (encryptEnabled() && !sessionPassphrase.isSet.value) return@runCatching
        runUpload()
    }

    suspend fun peekLatestMetadata(): Result<RemoteBackupMetadata?> = runCatching {
        val ref = provider.latest() ?: return@runCatching null
        val bytes = provider.download(ref)
        if (isEncryptedEnvelope(bytes)) {
            val envelope: EncryptedBackup = BackupEnvelopeJson.decodeBytes(bytes)
            RemoteBackupMetadata(
                createdAtMs = envelope.createdAt,
                appVersion = envelope.appVersion,
                schema = envelope.schema,
                envelopeVersion = envelope.version,
                sizeBytes = bytes.size.toLong(),
                fileName = ref.name,
            )
        } else {
            RemoteBackupMetadata(
                createdAtMs = ref.modifiedAtMs,
                appVersion = "—",
                schema = CURRENT_SCHEMA,
                envelopeVersion = EncryptedBackup.ENVELOPE_VERSION,
                sizeBytes = bytes.size.toLong(),
                fileName = ref.name,
            )
        }
    }

    suspend fun restoreLatest(passphrase: CharArray): Result<Unit> = runCatching {
        uploadLock.withLock {
            try {
                _runtime.value = RemoteBackupRuntimeState.Downloading
                val ref = provider.latest() ?: throw RemoteBackupError.NotFound()
                val bytes = provider.download(ref)
                val plain = if (isEncryptedEnvelope(bytes)) {
                    _runtime.value = RemoteBackupRuntimeState.Decrypting
                    val envelope = BackupEnvelopeJson.decodeBytes(bytes)
                    crypto.decrypt(envelope, passphrase)
                } else {
                    bytes
                }
                _runtime.value = RemoteBackupRuntimeState.Restoring
                withContext(dispatchers.io) { dbBackupManager.restore(plain) }
                _runtime.value = RemoteBackupRuntimeState.Idle
            } catch (t: Throwable) {
                _runtime.value = RemoteBackupRuntimeState.Error(humanMessage(t))
                logger.e(t) { "Remote restore failed" }
                throw t
            }
        }
    }

    private suspend fun runUpload() {
        if (!uploadLock.tryLock()) {
            pulse.tryEmit(Unit)
            return
        }
        try {
            val createdAt = nowMs()
            val plain = withContext(dispatchers.io) { dbBackupManager.export() }
            val encrypt = encryptEnabled()
            val bytes: ByteArray
            val name: String
            if (encrypt) {
                val passphrase = sessionPassphrase.get() ?: return
                _runtime.value = RemoteBackupRuntimeState.Encrypting
                val envelope = crypto.encrypt(
                    plain = plain,
                    passphrase = passphrase,
                    schema = schemaVersion,
                    appVersion = appVersion,
                    createdAt = createdAt,
                )
                passphrase.fill(' ')
                bytes = BackupEnvelopeJson.encodeBytes(envelope)
                name = timestampedName(createdAt)
            } else {
                bytes = plain
                name = plaintextName(createdAt)
            }
            val remaining = runCatching { provider.remainingQuotaBytes() }.getOrNull()
            if (remaining != null && remaining < bytes.size) {
                _runtime.value = RemoteBackupRuntimeState.QuotaWarning(
                    remainingBytes = remaining,
                    requiredBytes = bytes.size.toLong(),
                )
                logger.w { "Remote backup aborted: insufficient Drive quota ($remaining < ${bytes.size})" }
                return
            }
            _runtime.value = RemoteBackupRuntimeState.Uploading
            val ref = provider.upload(
                bytes = bytes,
                name = name,
                properties = mapOf(
                    "schema" to schemaVersion.toString(),
                    "appVersion" to appVersion,
                    "createdAt" to createdAt.toString(),
                    "encrypted" to encrypt.toString(),
                ),
            )
            appSettings.putLong(PrefKeys.LAST_REMOTE_BACKUP_TIME_MS, ref.modifiedAtMs.takeIf { it > 0 } ?: createdAt)
            appSettings.putString(PrefKeys.REMOTE_BACKUP_PROVIDER_ID, provider.id)
            pruneOldBackups()
            _runtime.value = RemoteBackupRuntimeState.Idle
            logger.i { "Remote backup uploaded id=${ref.id} size=${bytes.size}" }
        } catch (t: BackupCryptoError) {
            _runtime.value = RemoteBackupRuntimeState.Error(humanMessage(t))
            logger.e(t) { "Remote backup encrypt failed" }
        } catch (t: RemoteBackupError) {
            _runtime.value = RemoteBackupRuntimeState.Error(humanMessage(t))
            logger.e(t) { "Remote backup upload failed" }
        } catch (t: Throwable) {
            _runtime.value = RemoteBackupRuntimeState.Error(humanMessage(t))
            logger.e(t) { "Remote backup unexpected failure" }
        } finally {
            if (uploadLock.isLocked) uploadLock.unlock()
        }
    }

    private fun timestampedName(createdAt: Long): String = "moneym-backup-$createdAt.bin"

    private fun plaintextName(createdAt: Long): String = "moneym-backup-$createdAt.zip"

    private fun isEncryptedEnvelope(bytes: ByteArray): Boolean =
        bytes.isNotEmpty() && bytes[0] == '{'.code.toByte()

    private suspend fun pruneOldBackups() {
        if (retentionCount <= 0) return
        runCatching {
            val all = provider.list(limit = retentionCount * 4)
            val moneymBackups = all.filter { it.name.startsWith("moneym-backup") }
            val excess = moneymBackups.drop(retentionCount)
            excess.forEach { stale ->
                runCatching { provider.delete(stale) }
                    .onFailure { logger.w(it) { "Failed to prune stale backup ${stale.id}" } }
            }
        }.onFailure { logger.w(it) { "Backup retention pruning failed" } }
    }

    private fun isEnabled(): Boolean =
        appSettings.getBoolean(PrefKeys.AUTO_REMOTE_BACKUP_ENABLED, defaultValue = false)

    private fun encryptEnabled(): Boolean =
        appSettings.getBoolean(PrefKeys.REMOTE_BACKUP_ENCRYPT, defaultValue = true)

    private fun humanMessage(t: Throwable): String = when (t) {
        is BackupCryptoError.WrongPassphrase -> "Wrong passphrase"
        is BackupCryptoError.UnsupportedEnvelope -> "Backup format is too new for this app version"
        is BackupCryptoError.PlatformFailure -> "Encryption failed: ${t.message}"
        is RemoteBackupError.NotAuthenticated -> "Google Drive access denied. Please disconnect and reconnect your account."
        is RemoteBackupError.NotFound -> "No remote backup found"
        is RemoteBackupError.Http -> "Server error: HTTP ${t.status}"
        is RemoteBackupError.Network -> "Network error"
        else -> t.message ?: "Unknown error"
    }

    companion object {
        const val BACKUP_FILE_NAME = "moneym-backup.bin"
        const val CURRENT_SCHEMA = 1
        const val DEFAULT_DEBOUNCE_MS = 5_000L
        const val DEFAULT_RETENTION_COUNT = 5
    }
}
