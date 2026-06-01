package com.dv.moneym.data.sync

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SyncReconcilerTest {

    private val reconciler = SyncReconciler()

    private fun snapshot(
        accounts: List<SyncAccount> = emptyList(),
        categories: List<SyncCategory> = emptyList(),
        transactions: List<SyncTransaction> = emptyList(),
        device: String = "remote-device",
    ) = SyncSnapshot(
        generatedAtMs = 5L,
        originDeviceId = device,
        accounts = accounts,
        categories = categories,
        transactions = transactions,
    )

    private fun account(syncId: String, updatedAt: Long, deleted: Boolean = false, name: String = "Acc") = SyncAccount(
        syncId = syncId,
        name = name,
        type = "CASH",
        currency = "EUR",
        isDefault = false,
        archived = false,
        deleted = deleted,
        createdAt = 0L,
        updatedAt = updatedAt,
    )

    private fun transaction(syncId: String, updatedAt: Long, deleted: Boolean = false) = SyncTransaction(
        syncId = syncId,
        type = "EXPENSE",
        amountMinor = 1000L,
        currency = "EUR",
        occurredOn = "2026-01-01",
        categorySyncId = "cat-1",
        accountSyncId = "acc-1",
        deleted = deleted,
        createdAt = 0L,
        updatedAt = updatedAt,
    )

    @Test
    fun remoteAddIsIncludedInToApply() {
        val result = reconciler.reconcile(
            local = snapshot(),
            remote = snapshot(accounts = listOf(account("a1", updatedAt = 10L))),
        )

        assertEquals(listOf("a1"), result.toApply.accounts.map { it.syncId })
        assertTrue(result.pendingDeletions.isEmpty())
    }

    @Test
    fun remoteEditNewerIsIncludedInToApply() {
        val result = reconciler.reconcile(
            local = snapshot(accounts = listOf(account("a1", updatedAt = 10L, name = "Old"))),
            remote = snapshot(accounts = listOf(account("a1", updatedAt = 20L, name = "New"))),
        )

        assertEquals(1, result.toApply.accounts.size)
        assertEquals("New", result.toApply.accounts.single().name)
    }

    @Test
    fun remoteEditOlderIsNoOp() {
        val result = reconciler.reconcile(
            local = snapshot(accounts = listOf(account("a1", updatedAt = 20L))),
            remote = snapshot(accounts = listOf(account("a1", updatedAt = 10L))),
        )

        assertTrue(result.toApply.accounts.isEmpty())
        assertTrue(result.pendingDeletions.isEmpty())
    }

    @Test
    fun equalUpdatedAtKeepsLocalNoOp() {
        val result = reconciler.reconcile(
            local = snapshot(accounts = listOf(account("a1", updatedAt = 15L, name = "Local"))),
            remote = snapshot(accounts = listOf(account("a1", updatedAt = 15L, name = "Remote"))),
        )

        assertTrue(result.toApply.accounts.isEmpty())
    }

    @Test
    fun remoteTombstoneVsLiveLocalProducesPendingDeletionWithTypeAndLabel() {
        val result = reconciler.reconcile(
            local = snapshot(accounts = listOf(account("a1", updatedAt = 10L, name = "Wallet"))),
            remote = snapshot(accounts = listOf(account("a1", updatedAt = 20L, deleted = true))),
        )

        assertTrue(result.toApply.accounts.isEmpty())
        val pd = result.pendingDeletions.single()
        assertEquals(SyncEntityType.ACCOUNT, pd.entityType)
        assertEquals("a1", pd.syncId)
        assertEquals("Wallet", pd.label)
        assertEquals(20L, pd.remoteUpdatedAt)
    }

    @Test
    fun bothDeletedIsNoOp() {
        val result = reconciler.reconcile(
            local = snapshot(accounts = listOf(account("a1", updatedAt = 10L, deleted = true))),
            remote = snapshot(accounts = listOf(account("a1", updatedAt = 20L, deleted = true))),
        )

        assertTrue(result.toApply.accounts.isEmpty())
        assertTrue(result.pendingDeletions.isEmpty())
    }

    @Test
    fun remoteTombstoneVsAbsentLocalIsNoOp() {
        val result = reconciler.reconcile(
            local = snapshot(),
            remote = snapshot(accounts = listOf(account("a1", updatedAt = 20L, deleted = true))),
        )

        assertTrue(result.toApply.accounts.isEmpty())
        assertTrue(result.pendingDeletions.isEmpty())
    }

    @Test
    fun localOnlyRowIsNotInToApply() {
        val result = reconciler.reconcile(
            local = snapshot(accounts = listOf(account("local-only", updatedAt = 10L))),
            remote = snapshot(),
        )

        assertTrue(result.toApply.accounts.isEmpty())
        assertTrue(result.pendingDeletions.isEmpty())
    }

    @Test
    fun transactionLabelUsesAmountCurrencyAndDate() {
        val result = reconciler.reconcile(
            local = snapshot(transactions = listOf(transaction("t1", updatedAt = 10L))),
            remote = snapshot(transactions = listOf(transaction("t1", updatedAt = 20L, deleted = true))),
        )

        assertEquals("1000 EUR · 2026-01-01", result.pendingDeletions.single().label)
    }

    @Test
    fun toApplyCopiesGeneratedAtAndOriginFromRemote() {
        val result = reconciler.reconcile(
            local = snapshot(),
            remote = snapshot(accounts = listOf(account("a1", updatedAt = 10L)), device = "remote-xyz"),
        )

        assertEquals(5L, result.toApply.generatedAtMs)
        assertEquals("remote-xyz", result.toApply.originDeviceId)
    }
}
