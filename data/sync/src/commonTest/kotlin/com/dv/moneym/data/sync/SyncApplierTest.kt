package com.dv.moneym.data.sync

import com.dv.moneym.core.testing.FakeAccountRepository
import com.dv.moneym.core.testing.FakeBudgetRepository
import com.dv.moneym.core.testing.FakeCategoryRepository
import com.dv.moneym.core.testing.FakeRecurringTransactionRepository
import com.dv.moneym.core.testing.FakeTransactionRepository
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class SyncApplierTest {

    private val accounts = FakeAccountRepository()
    private val categories = FakeCategoryRepository()
    private val paymentModes = FakePaymentModeRepository()
    private val transactions = FakeTransactionRepository()
    private val recurring = FakeRecurringTransactionRepository()
    private val budgets = FakeBudgetRepository()

    private val applier = SyncApplier(
        accountRepository = accounts,
        categoryRepository = categories,
        paymentModeRepository = paymentModes,
        transactionRepository = transactions,
        recurringTransactionRepository = recurring,
        budgetRepository = budgets,
    )

    private fun account(syncId: String) = SyncAccount(
        syncId = syncId, name = "Acc $syncId", type = "CASH", currency = "EUR",
        isDefault = false, archived = false, createdAt = 0L, updatedAt = 1L,
    )

    private fun category(syncId: String) = SyncCategory(
        syncId = syncId, name = "Cat $syncId", iconKey = "icon", colorHex = "#FFFFFF",
        isUserCreated = false, archived = false, categoryType = "EXPENSE", createdAt = 0L, updatedAt = 1L,
    )

    private fun transaction(syncId: String, categorySyncId: String, accountSyncId: String, updatedAt: Long = 1L) =
        SyncTransaction(
            syncId = syncId, type = "EXPENSE", amountMinor = 1000L, currency = "EUR",
            occurredOn = "2026-01-01", categorySyncId = categorySyncId, accountSyncId = accountSyncId,
            createdAt = 0L, updatedAt = updatedAt,
        )

    @Test
    fun resolvesTransactionForeignKeysToAppliedParentPks() = runTest {
        val snapshot = SyncSnapshot(
            generatedAtMs = 1L,
            originDeviceId = "d",
            accounts = listOf(account("acc-1")),
            categories = listOf(category("cat-1")),
            transactions = listOf(transaction("tx-1", "cat-1", "acc-1")),
        )

        applier.apply(snapshot)

        val accId = accounts.accounts.single().id.value
        val catId = categories.categories.single().id.value
        val tx = transactions.transactions.single()
        assertEquals(accId, tx.accountId.value)
        assertEquals(catId, tx.categoryId.value)
    }

    @Test
    fun preservesIncomingSyncIdAfterApply() = runTest {
        val snapshot = SyncSnapshot(
            generatedAtMs = 1L,
            originDeviceId = "d",
            accounts = listOf(account("acc-keep")),
        )

        applier.apply(snapshot)

        assertEquals("acc-keep", accounts.exportForSync().single().syncId)
    }

    @Test
    fun editBySyncIdUpdatesExistingRowWithoutDuplicate() = runTest {
        applier.apply(
            SyncSnapshot(generatedAtMs = 1L, originDeviceId = "d", accounts = listOf(account("acc-1")))
        )
        val firstId = accounts.accounts.single().id.value

        applier.apply(
            SyncSnapshot(
                generatedAtMs = 2L,
                originDeviceId = "d",
                accounts = listOf(account("acc-1").copy(name = "Renamed", updatedAt = 99L)),
            )
        )

        assertEquals(1, accounts.accounts.size)
        assertEquals(firstId, accounts.accounts.single().id.value)
        assertEquals("Renamed", accounts.accounts.single().name)
    }

    @Test
    fun transactionWithUnresolvedRequiredFkIsSkipped() = runTest {
        val snapshot = SyncSnapshot(
            generatedAtMs = 1L,
            originDeviceId = "d",
            accounts = listOf(account("acc-1")),
            transactions = listOf(transaction("tx-1", categorySyncId = "missing-cat", accountSyncId = "acc-1")),
        )

        applier.apply(snapshot)

        assertEquals(0, transactions.transactions.size)
    }

    @Test
    fun transactionFkResolvesAgainstPreexistingLocalParents() = runTest {
        applier.apply(
            SyncSnapshot(
                generatedAtMs = 1L, originDeviceId = "d",
                accounts = listOf(account("acc-1")), categories = listOf(category("cat-1")),
            )
        )

        applier.apply(
            SyncSnapshot(
                generatedAtMs = 2L, originDeviceId = "d",
                transactions = listOf(transaction("tx-1", "cat-1", "acc-1")),
            )
        )

        val tx = transactions.transactions.single()
        assertNotNull(accounts.accounts.find { it.id == tx.accountId })
        assertNotNull(categories.categories.find { it.id == tx.categoryId })
    }
}
