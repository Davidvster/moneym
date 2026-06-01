package com.dv.moneym

import com.dv.moneym.data.backup.BackupCodec
import com.dv.moneym.data.backup.DbBackupManager
import com.dv.moneym.core.common.DispatcherProvider
import com.dv.moneym.core.datastore.AppSettings
import com.dv.moneym.core.datastore.PrefKeys
import com.dv.moneym.data.accounts.AccountRepository
import com.dv.moneym.data.categories.CategoryRepository
import com.dv.moneym.data.remotebackup.RemoteBackupManager
import com.dv.moneym.data.remotebackup.SessionPassphrase
import com.dv.moneym.data.sync.SyncEngine
import com.dv.moneym.data.transactions.TransactionRepository
import com.dv.moneym.platform.FilePlatform
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.time.Clock

class AutoBackupManager(
    private val categoryRepository: CategoryRepository,
    private val accountRepository: AccountRepository,
    private val transactionRepository: TransactionRepository,
    private val dbBackupManager: DbBackupManager,
    private val filePlatform: FilePlatform,
    private val appSettings: AppSettings,
    private val dispatchers: DispatcherProvider,
    private val backupCodec: BackupCodec,
    private val sessionPassphrase: SessionPassphrase,
    private val remoteBackupManager: RemoteBackupManager? = null,
    private val syncEngine: SyncEngine? = null,
) {
    private var job: Job? = null

    @OptIn(FlowPreview::class)
    fun start(scope: CoroutineScope) {
        job?.cancel()
        job = scope.launch {
            combine(
                categoryRepository.observeAll(),
                accountRepository.observeAll(),
                transactionRepository.observeAll(),
            ) { _, _, _ -> Unit }
                .drop(1)
                .debounce(3_000)
                .collect {
                    val encrypt = appSettings.getBoolean(PrefKeys.LOCAL_BACKUP_ENCRYPT, defaultValue = false) &&
                        sessionPassphrase.isSet.value
                    val bytes = withContext(dispatchers.io) {
                        backupCodec.seal(
                            plain = dbBackupManager.export(),
                            passphrase = if (encrypt) sessionPassphrase.get() else null,
                            schema = BackupCodec.CURRENT_SCHEMA,
                            createdAt = Clock.System.now().toEpochMilliseconds(),
                        )
                    }
                    val name = if (encrypt) "moneym-backup.bin" else "moneym-backup.zip"
                    val dirUri = appSettings.getString(PrefKeys.AUTO_BACKUP_DIR_URI)
                    val path = if (dirUri != null && dirUri != "default") {
                        filePlatform.saveFileToDirBinary(dirUri, name, bytes)
                    } else {
                        filePlatform.saveFileLocallyBinary(name, bytes)
                    }
                    if (path != null) recordBackup(path)
                    appSettings.putLong(
                        PrefKeys.LAST_LOCAL_MUTATION_MS,
                        Clock.System.now().toEpochMilliseconds(),
                    )
                    remoteBackupManager?.enqueueUpload()
                    syncEngine?.enqueuePush()
                }
        }
        remoteBackupManager?.start(scope)
        syncEngine?.start(scope)
    }

    fun stop() {
        job?.cancel()
        job = null
        remoteBackupManager?.stop()
        syncEngine?.stop()
    }

    fun recordBackup(path: String) {
        appSettings.putString(PrefKeys.LAST_BACKUP_TIME_MS, Clock.System.now().toEpochMilliseconds().toString())
        appSettings.putString(PrefKeys.LAST_BACKUP_PATH, path)
    }
}
