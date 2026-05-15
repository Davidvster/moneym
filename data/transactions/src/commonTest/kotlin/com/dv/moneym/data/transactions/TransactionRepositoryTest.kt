package com.dv.moneym.data.transactions

import app.cash.turbine.test
import com.dv.moneym.core.model.AccountId
import com.dv.moneym.core.model.CategoryId
import com.dv.moneym.core.model.CurrencyCode
import com.dv.moneym.core.model.Money
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

    private fun txn(amount: Long = 100, date: LocalDate = LocalDate(2026, 5, 1)) = Transaction(
        id = UNSAVED_TRANSACTION_ID,
        type = TransactionType.EXPENSE,
        amount = Money(amount, eur),
        occurredOn = date,
        note = null,
        categoryId = catId,
        accountId = accId,
        createdAt = epoch,
        updatedAt = epoch,
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
