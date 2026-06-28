package com.dv.moneym.data.banksync

import kotlinx.coroutines.flow.Flow

/**
 * Minimal bank-sync status surface for UI (e.g. the transaction-list sync sheet).
 * [BankSyncEngine] implements it. Keeping this an interface lets the ViewModel be tested without
 * constructing the full engine graph.
 */
interface BankSyncStatusProvider {
    val isEnabled: Flow<Boolean>
    val isSyncing: Flow<Boolean>
    val failure: Flow<BankSyncFailure?>
    val pendingCount: Flow<Int>
    val lastSyncedMs: Flow<Long>
    suspend fun requestSync()
}
