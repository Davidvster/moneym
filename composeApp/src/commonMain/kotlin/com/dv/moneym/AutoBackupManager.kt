package com.dv.moneym

import com.dv.moneym.data.backup.DbBackupManager
import com.dv.moneym.core.common.DispatcherProvider
import com.dv.moneym.core.datastore.AppSettings
import com.dv.moneym.core.datastore.PrefKeys
import com.dv.moneym.data.accounts.AccountRepository
import com.dv.moneym.data.categories.CategoryRepository
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
                    val bytes = withContext(dispatchers.io) { dbBackupManager.export() }
                    val path = filePlatform.saveFileLocallyBinary("moneym-backup.zip", bytes)
                    if (path != null) recordBackup(path)
                }
        }
    }

    fun stop() {
        job?.cancel()
        job = null
    }

    fun recordBackup(path: String) {
        appSettings.putString(PrefKeys.LAST_BACKUP_TIME_MS, Clock.System.now().toEpochMilliseconds().toString())
        appSettings.putString(PrefKeys.LAST_BACKUP_PATH, path)
    }
}
