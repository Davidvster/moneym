package com.dv.moneym.data.sync

import com.dv.moneym.core.model.CurrencyCode
import com.dv.moneym.core.model.Money
import com.dv.moneym.core.model.format

/**
 * Pure last-write-wins reconciliation by syncId. No repository / IO dependencies.
 *
 * Truth table per remote row (vs local row of the same syncId):
 *  - remote not in local, !remote.deleted  -> toApply (add)
 *  - remote not in local,  remote.deleted   -> no-op (already absent)
 *  - both present, remote.deleted && !local.deleted -> PendingDeletion
 *  - both present, remote.deleted &&  local.deleted -> no-op
 *  - both present, !remote.deleted, remote.updatedAt > local.updatedAt -> toApply (edit)
 *  - both present, !remote.deleted, remote.updatedAt <= local.updatedAt -> no-op (tie keeps local)
 *  - local-only (syncId absent in remote) -> no-op here
 */
class SyncReconciler {

    fun reconcile(local: SyncSnapshot, remote: SyncSnapshot): ReconcileResult {
        val pendingDeletions = mutableListOf<PendingDeletion>()

        val accounts = reconcileList(
            local = local.accounts,
            remote = remote.accounts,
            type = SyncEntityType.ACCOUNT,
            syncId = { it.syncId },
            deleted = { it.deleted },
            updatedAt = { it.updatedAt },
            label = { it.name },
            pendingDeletions = pendingDeletions,
        )
        val categories = reconcileList(
            local = local.categories,
            remote = remote.categories,
            type = SyncEntityType.CATEGORY,
            syncId = { it.syncId },
            deleted = { it.deleted },
            updatedAt = { it.updatedAt },
            label = { it.name },
            pendingDeletions = pendingDeletions,
        )
        val paymentModes = reconcileList(
            local = local.paymentModes,
            remote = remote.paymentModes,
            type = SyncEntityType.PAYMENT_MODE,
            syncId = { it.syncId },
            deleted = { it.deleted },
            updatedAt = { it.updatedAt },
            label = { it.name },
            pendingDeletions = pendingDeletions,
        )
        val recurring = reconcileList(
            local = local.recurring,
            remote = remote.recurring,
            type = SyncEntityType.RECURRING,
            syncId = { it.syncId },
            deleted = { it.deleted },
            updatedAt = { it.updatedAt },
            label = { it.note ?: "recurring" },
            pendingDeletions = pendingDeletions,
        )
        val budgets = reconcileList(
            local = local.budgets,
            remote = remote.budgets,
            type = SyncEntityType.BUDGET,
            syncId = { it.syncId },
            deleted = { it.deleted },
            updatedAt = { it.updatedAt },
            label = { it.name },
            pendingDeletions = pendingDeletions,
        )
        val transactions = reconcileList(
            local = local.transactions,
            remote = remote.transactions,
            type = SyncEntityType.TRANSACTION,
            syncId = { it.syncId },
            deleted = { it.deleted },
            updatedAt = { it.updatedAt },
            label = {
                "${
                    Money(
                        it.amountMinor,
                        CurrencyCode(it.currency)
                    ).format(useSymbol = true)
                } · ${it.occurredOn}"
            },
            pendingDeletions = pendingDeletions,
        )

        val toApply = SyncSnapshot(
            formatVersion = remote.formatVersion,
            generatedAtMs = remote.generatedAtMs,
            originDeviceId = remote.originDeviceId,
            accounts = accounts,
            categories = categories,
            paymentModes = paymentModes,
            recurring = recurring,
            budgets = budgets,
            transactions = transactions,
        )
        return ReconcileResult(toApply = toApply, pendingDeletions = pendingDeletions)
    }

    private inline fun <T> reconcileList(
        local: List<T>,
        remote: List<T>,
        type: SyncEntityType,
        syncId: (T) -> String,
        deleted: (T) -> Boolean,
        updatedAt: (T) -> Long,
        label: (T) -> String,
        pendingDeletions: MutableList<PendingDeletion>,
    ): List<T> {
        val localBySyncId = local.associateBy(syncId)
        val toApply = mutableListOf<T>()
        for (remoteItem in remote) {
            val localItem = localBySyncId[syncId(remoteItem)]
            if (localItem == null) {
                if (!deleted(remoteItem)) toApply.add(remoteItem)
                continue
            }
            if (deleted(remoteItem)) {
                if (!deleted(localItem)) {
                    pendingDeletions.add(
                        PendingDeletion(
                            entityType = type,
                            syncId = syncId(remoteItem),
                            label = label(localItem),
                            remoteUpdatedAt = updatedAt(remoteItem),
                        )
                    )
                }
                continue
            }
            if (updatedAt(remoteItem) > updatedAt(localItem)) toApply.add(remoteItem)
        }
        return toApply
    }
}
