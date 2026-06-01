package com.dv.moneym.data.sync

import com.dv.moneym.core.datastore.PrefKeys
import com.dv.moneym.core.model.Account
import com.dv.moneym.core.model.AccountId
import com.dv.moneym.core.model.AccountType
import com.dv.moneym.core.model.Category
import com.dv.moneym.core.model.CategoryId
import com.dv.moneym.core.model.CurrencyCode
import com.dv.moneym.core.model.Money
import com.dv.moneym.core.model.PaymentMode
import com.dv.moneym.core.model.PaymentModeId
import com.dv.moneym.core.model.Transaction
import com.dv.moneym.core.model.TransactionId
import com.dv.moneym.core.model.TransactionType
import com.dv.moneym.core.testing.FakeAccountRepository
import com.dv.moneym.core.testing.FakeBudgetRepository
import com.dv.moneym.core.testing.FakeCategoryRepository
import com.dv.moneym.core.testing.FakeRecurringTransactionRepository
import com.dv.moneym.core.testing.FakeTransactionRepository
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Instant

class SyncExporterTest {

    private val epoch = Instant.fromEpochMilliseconds(0)
    private val eur = CurrencyCode("EUR")

    private fun account(id: Long) = Account(
        id = AccountId(id),
        name = "Acc $id",
        type = AccountType.CASH,
        currency = eur,
        isDefault = id == 10L,
        archived = false,
        createdAt = epoch,
        updatedAt = epoch,
    )

    private fun category(id: Long) = Category(
        id = CategoryId(id),
        name = "Cat $id",
        iconKey = "icon",
        colorHex = "#FFFFFF",
        isUserCreated = false,
        archived = false,
        createdAt = epoch,
        updatedAt = epoch,
        type = TransactionType.EXPENSE,
    )

    private fun paymentMode(id: Long) = PaymentMode(
        id = PaymentModeId(id),
        name = "PM $id",
        createdAt = epoch,
        updatedAt = epoch,
    )

    private fun transaction(
        id: Long,
        categoryId: Long,
        accountId: Long,
        paymentModeId: Long? = null,
    ) = Transaction(
        id = TransactionId(id),
        type = TransactionType.EXPENSE,
        amount = Money(1000L, eur),
        occurredOn = LocalDate(2026, 1, 1),
        note = "note",
        categoryId = CategoryId(categoryId),
        accountId = AccountId(accountId),
        createdAt = epoch,
        updatedAt = epoch,
        paymentModeId = paymentModeId?.let { PaymentModeId(it) },
    )

    private fun exporter(
        accounts: FakeAccountRepository = FakeAccountRepository(),
        categories: FakeCategoryRepository = FakeCategoryRepository(),
        paymentModes: FakePaymentModeRepository = FakePaymentModeRepository(),
        transactions: FakeTransactionRepository = FakeTransactionRepository(),
        recurring: FakeRecurringTransactionRepository = FakeRecurringTransactionRepository(),
        budgets: FakeBudgetRepository = FakeBudgetRepository(),
        deviceId: String = "device-123",
    ): SyncExporter {
        val settings = InMemoryAppSettings().apply { putString(PrefKeys.DEVICE_ID, deviceId) }
        return SyncExporter(
            accountRepository = accounts,
            categoryRepository = categories,
            paymentModeRepository = paymentModes,
            transactionRepository = transactions,
            recurringTransactionRepository = recurring,
            budgetRepository = budgets,
            deviceIdentity = DeviceIdentity(settings),
            nowMs = { 999L },
        )
    }

    @Test
    fun resolvesTransactionForeignKeysToSyncIds() = runTest {
        val accounts = FakeAccountRepository().apply { addAll(listOf(account(10))) }
        val categories = FakeCategoryRepository().apply { addAll(listOf(category(20))) }
        val paymentModes = FakePaymentModeRepository().apply { addAll(listOf(paymentMode(30))) }
        val transactions = FakeTransactionRepository().apply {
            addAll(listOf(transaction(id = 1, categoryId = 20, accountId = 10, paymentModeId = 30)))
        }

        val snapshot = exporter(accounts, categories, paymentModes, transactions).export()

        assertEquals(1, snapshot.transactions.size)
        val tx = snapshot.transactions.single()
        assertEquals("sync-cat-20", tx.categorySyncId)
        assertEquals("sync-acc-10", tx.accountSyncId)
        assertEquals("sync-pm-30", tx.paymentModeSyncId)
        assertNull(tx.recurringSyncId)
    }

    @Test
    fun skipsTransactionWithUnresolvableCategoryWithoutCrashing() = runTest {
        val accounts = FakeAccountRepository().apply { addAll(listOf(account(10))) }
        val categories = FakeCategoryRepository().apply { addAll(listOf(category(20))) }
        val transactions = FakeTransactionRepository().apply {
            addAll(
                listOf(
                    transaction(id = 1, categoryId = 20, accountId = 10),
                    transaction(id = 2, categoryId = 999, accountId = 10),
                )
            )
        }

        val snapshot = exporter(accounts, categories, transactions = transactions).export()

        assertEquals(1, snapshot.transactions.size)
        assertEquals("sync-tx-1", snapshot.transactions.single().syncId)
    }

    @Test
    fun populatesOriginDeviceIdFromDeviceIdentity() = runTest {
        val snapshot = exporter(deviceId = "device-xyz").export()

        assertEquals("device-xyz", snapshot.originDeviceId)
        assertEquals(1, snapshot.formatVersion)
        assertEquals(999L, snapshot.generatedAtMs)
    }

    @Test
    fun exportsAllLiveRowsAcrossTables() = runTest {
        val accounts = FakeAccountRepository().apply { addAll(listOf(account(10), account(11))) }
        val categories = FakeCategoryRepository().apply { addAll(listOf(category(20))) }
        val transactions = FakeTransactionRepository().apply {
            addAll(listOf(transaction(id = 1, categoryId = 20, accountId = 10)))
        }

        val snapshot = exporter(accounts, categories, transactions = transactions).export()

        assertEquals(2, snapshot.accounts.size)
        assertEquals(1, snapshot.categories.size)
        assertEquals(1, snapshot.transactions.size)
        assertTrue(snapshot.accounts.all { !it.deleted })
    }
}
