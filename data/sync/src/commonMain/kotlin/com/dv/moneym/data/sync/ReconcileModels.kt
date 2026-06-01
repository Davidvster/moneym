package com.dv.moneym.data.sync

import kotlinx.serialization.Serializable

@Serializable
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
