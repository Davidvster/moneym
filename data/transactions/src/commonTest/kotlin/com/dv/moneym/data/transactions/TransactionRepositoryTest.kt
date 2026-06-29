package com.dv.moneym.data.transactions

import app.cash.turbine.test
import com.dv.moneym.core.model.AccountId
import com.dv.moneym.core.model.CategoryId
import com.dv.moneym.core.model.CurrencyCode
import com.dv.moneym.core.model.Money
import com.dv.moneym.core.model.PaymentModeId
import com.dv.moneym.core.model.Transaction
import com.dv.moneym.core.model.TransactionFilter
import com.dv.moneym.core.model.TransactionType
import com.dv.moneym.core.model.UNSAVED_TRANSACTION_ID
import com.dv.moneym.core.testing.FakeTransactionRepository
import com.dv.moneym.core.testing.runTestWithDispatchers
import kotlinx.datetime.LocalDate
import kotlin.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TransactionRepositoryTest {

    private val repo = FakeTransactionRepository()
    private val eur = CurrencyCode("EUR")
    private val catId = CategoryId(1)
    private val accId = AccountId(1)
    private val epoch = Instant.fromEpochMilliseconds(0)

    private fun txn(
        amount: Long = 100,
        date: LocalDate = LocalDate(2026, 5, 1),
        paymentModeId: PaymentModeId? = null,
    ) = Transaction(
        id = UNSAVED_TRANSACTION_ID,
        type = TransactionType.EXPENSE,
        amount = Money(amount, eur),
        occurredOn = date,
        note = null,
        categoryId = catId,
        accountId = accId,
        createdAt = epoch,
        updatedAt = epoch,
        paymentModeId = paymentModeId,
    )

    @Test
    fun upsertedTransactionAppearsInObserveAll() = runTestWithDispatchers {
        val id = repo.upsert(txn())

        repo.observeAll().test {
            val list = awaitItem()
            assertEquals(1, list.size)
            assertEquals(id, list.first().id)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun deletedTransactionRemovedFromObserveAll() = runTestWithDispatchers {
        val id = repo.upsert(txn())
        repo.delete(id)

        repo.observeAll().test {
            assertTrue(awaitItem().isEmpty())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun bulkDeletedTransactionsRemovedFromObserveAll() = runTestWithDispatchers {
        val first = repo.upsert(txn())
        val second = repo.upsert(txn())
        val third = repo.upsert(txn())

        repo.delete(setOf(first, third))

        repo.observeAll().test {
            assertEquals(listOf(second), awaitItem().map { it.id })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun bulkCategoryUpdateChangesCategoryAndType() = runTestWithDispatchers {
        val first = repo.upsert(txn())
        val second = repo.upsert(txn())
        repo.upsert(txn())

        repo.updateCategory(setOf(first, second), CategoryId(9), TransactionType.INCOME)

        val changed = repo.transactions.filter { it.id in setOf(first, second) }
        assertEquals(setOf(CategoryId(9)), changed.map { it.categoryId }.toSet())
        assertEquals(setOf(TransactionType.INCOME), changed.map { it.type }.toSet())
    }

    @Test
    fun bulkAccountUpdateMovesSelectedTransactionsAndConvertsWhenRateProvided() = runTestWithDispatchers {
        val first = repo.upsert(txn(amount = 100))
        val second = repo.upsert(txn(amount = 250))
        val untouched = repo.upsert(txn(amount = 500))

        repo.updateAccount(setOf(first, second), AccountId(7), CurrencyCode("USD"), rate = 1.5)

        val changed = repo.transactions.filter { it.id in setOf(first, second) }.sortedBy { it.id.value }
        assertEquals(listOf(Money(150, CurrencyCode("USD")), Money(375, CurrencyCode("USD"))), changed.map { it.amount })
        assertEquals(setOf(AccountId(7)), changed.map { it.accountId }.toSet())
        assertEquals(Money(500, eur), repo.getById(untouched)!!.amount)
    }

    @Test
    fun bulkPaymentModeUpdateChangesSelectedTransactions() = runTestWithDispatchers {
        val first = repo.upsert(txn(paymentModeId = PaymentModeId(1)))
        val second = repo.upsert(txn(paymentModeId = PaymentModeId(1)))
        val untouched = repo.upsert(txn(paymentModeId = PaymentModeId(1)))

        repo.updatePaymentMode(setOf(first, second), PaymentModeId(9))

        assertEquals(setOf(PaymentModeId(9)), repo.transactions.filter { it.id in setOf(first, second) }.map { it.paymentModeId }.toSet())
        assertEquals(PaymentModeId(1), repo.getById(untouched)!!.paymentModeId)

        repo.updatePaymentMode(setOf(first), null)
        assertEquals(null, repo.getById(first)!!.paymentModeId)
    }

    @Test
    fun filterByCategoryReturnsOnlyMatchingTransactions() = runTestWithDispatchers {
        repo.upsert(txn())
        repo.upsert(txn().copy(categoryId = CategoryId(99)))

        repo.observeFiltered(TransactionFilter.ByCategory(catId)).test {
            val list = awaitItem()
            assertEquals(1, list.size)
            assertEquals(catId, list.first().categoryId)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
