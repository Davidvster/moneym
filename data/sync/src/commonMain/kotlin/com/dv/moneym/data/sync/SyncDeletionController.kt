package com.dv.moneym.data.sync

import kotlinx.coroutines.flow.Flow

/**
 * Minimal surface the deletion-confirmation UI depends on. [SyncEngine] implements it. Keeping
 * this an interface lets the ViewModel be tested without constructing the full engine graph.
 */
interface SyncDeletionController {
    val pendingDeletions: Flow<List<PendingDeletion>>
    suspend fun resolveDeletions(confirmedSyncIds: Set<String>): Result<Unit>
}
