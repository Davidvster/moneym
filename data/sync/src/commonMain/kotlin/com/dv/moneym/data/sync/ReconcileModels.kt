package com.dv.moneym.data.sync

data class PendingDeletion(
    val entityType: SyncEntityType,
    val syncId: String,
    val label: String,
    val remoteUpdatedAt: Long,
)

data class ReconcileResult(
    val toApply: SyncSnapshot,
    val pendingDeletions: List<PendingDeletion>,
)
