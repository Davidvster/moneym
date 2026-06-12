package com.dv.moneym

import com.dv.moneym.core.common.AppLogger
import com.dv.moneym.data.accounts.SeedAccountsUseCase
import com.dv.moneym.data.banksync.BankSyncEngine
import com.dv.moneym.data.categories.SeedCategoriesUseCase
import com.dv.moneym.data.remotebackup.SessionPassphrase
import com.dv.moneym.data.remotebackup.SyncPassphraseStore
import com.dv.moneym.data.sync.SyncEngine
import com.dv.moneym.data.transactions.MaterializeRecurringTransactionsUseCase
import com.dv.moneym.data.transactions.SeedPaymentModesUseCase

private val logger = AppLogger.tag("App")

class AppInitializer(
    private val seedCategories: SeedCategoriesUseCase,
    private val seedAccounts: SeedAccountsUseCase,
    private val lockController: AppLockController,
    private val seedPaymentModes: SeedPaymentModesUseCase,
    private val materializeRecurringTransactions: MaterializeRecurringTransactionsUseCase,
    private val syncEngine: SyncEngine,
    private val sessionPassphrase: SessionPassphrase,
    private val syncPassphraseStore: SyncPassphraseStore,
    private val bankSyncEngine: BankSyncEngine,
) {
    suspend fun initialize() {
        seedCategories()
        seedAccounts()
        seedPaymentModes()
        materializeRecurringTransactions()
        lockController.init()
        syncPassphraseStore.hydrate(sessionPassphrase)
        syncEngine.pullNow()
        runCatching { bankSyncEngine.autoSyncIfDue() }
            .onFailure { logger.w(it) { "Bank auto-sync failed" } }
        logger.d { "App bootstrap complete" }
    }
}
