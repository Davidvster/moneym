package com.dv.moneym.data.sync

import kotlinx.coroutines.flow.Flow

/**
 * Minimal sync-status surface for UI (e.g. the transaction-list banner). [SyncEngine] implements
 * it. Keeping this an interface lets the ViewModel be tested without constructing the full engine
 * graph.
 */
interface SyncStatusProvider {
    val isSyncing: Flow<Boolean>
    val pendingDeletionCount: Flow<Int>
    val conflict: Flow<SyncConflict?>
    val lastSyncedMs: Flow<Long>
}
