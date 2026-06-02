package com.dv.moneym.data.transactions.internal

import app.cash.turbine.test
import com.dv.moneym.core.model.AccountId
import com.dv.moneym.core.model.CategoryId
import com.dv.moneym.core.model.CurrencyCode
import com.dv.moneym.core.model.Money
import com.dv.moneym.core.model.RecurringTransactionId
import com.dv.moneym.core.model.Transaction
import com.dv.moneym.core.model.TransactionFilter
import com.dv.moneym.core.model.TransactionId
import com.dv.moneym.core.model.TransactionType
import com.dv.moneym.core.model.UNSAVED_TRANSACTION_ID
import com.dv.moneym.core.testing.runTestWithDispatchers
import com.dv.moneym.data.transactions.TransactionSyncRow
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Instant

class TransactionRepositoryImplTest {

    private val ds = FakeTransactionLocalDataSource()
    private val repo = TransactionRepositoryImpl(ds)
    private val eur = CurrencyCode("EUR")
    private val epoch = Instant.fromEpochMilliseconds(0)

    private fun txn(
        id: Long = UNSAVED_TRANSACTION_ID.value,
        amount: Long = 100,
        date: LocalDate = LocalDate(2026, 5, 1),
        type: TransactionType = TransactionType.EXPENSE,
        categoryId: Long = 1,
        accountId: Long = 1,
        recurringId: Long? = null,
    ) = Transaction(
        id = TransactionId(id),
        type = type,
        amount = Money(amount, eur),
        occurredOn = date,
        note = null,
        categoryId = CategoryId(categoryId),
        accountId = AccountId(accountId),
        createdAt = epoch,
        updatedAt = epoch,
        recurringId = recurringId?.let(::RecurringTransactionId),
    )

    @Test
    fun upsertInsertsWhenUnsavedAndReturnsNewId() = runTestWithDispatchers {
        val id = repo.upsert(txn())

        assertEquals(1L, id.value)
        val stored = repo.getById(id)
        assertNotNull(stored)
        assertEquals(Money(100, eur), stored.amount)
        assertEquals(LocalDate(2026, 5, 1), stored.occurredOn)
    }

    @Test
    fun upsertUpdatesWhenIdPresent() = runTestWithDispatchers {
        val id = repo.upsert(txn())
        val updatedId = repo.upsert(txn(id = id.value, amount = 999))

        assertEquals(id, updatedId)
        assertEquals(Money(999, eur), repo.getById(id)!!.amount)
        assertEquals(1, ds.rows.value.size)
    }

    @Test
    fun observeAllReflectsInsertsSortedByDate() = runTestWithDispatchers {
        repo.observeAll().test {
            assertTrue(awaitItem().isEmpty())

            repo.upsert(txn(date = LocalDate(2026, 1, 1)))
            assertEquals(1, awaitItem().size)

            repo.upsert(txn(date = LocalDate(2026, 6, 1)))
            val list = awaitItem()
            assertEquals(
                listOf(LocalDate(2026, 6, 1), LocalDate(2026, 1, 1)),
                list.map { it.occurredOn },
            )
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun observeByMonthFiltersByYearMonth() = runTestWithDispatchers {
        repo.upsert(txn(date = LocalDate(2026, 5, 15)))
        repo.upsert(txn(date = LocalDate(2026, 6, 15)))

        repo.observeByMonth(2026, 5).test {
            val list = awaitItem()
            assertEquals(1, list.size)
            assertEquals(LocalDate(2026, 5, 15), list.first().occurredOn)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun observeFilteredByCategory() = runTestWithDispatchers {
        repo.upsert(txn(categoryId = 1))
        repo.upsert(txn(categoryId = 2))

        repo.observeFiltered(TransactionFilter.ByCategory(CategoryId(1))).test {
            val list = awaitItem()
            assertEquals(1, list.size)
            assertEquals(CategoryId(1), list.first().categoryId)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun observeFilteredByType() = runTestWithDispatchers {
        repo.upsert(txn(type = TransactionType.EXPENSE))
        repo.upsert(txn(type = TransactionType.INCOME))

        repo.observeFiltered(TransactionFilter.ByType(TransactionType.INCOME)).test {
            val list = awaitItem()
            assertEquals(1, list.size)
            assertEquals(TransactionType.INCOME, list.first().type)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun observeFilteredByCategoryAndType() = runTestWithDispatchers {
        repo.upsert(txn(categoryId = 1, type = TransactionType.INCOME))
        repo.upsert(txn(categoryId = 1, type = TransactionType.EXPENSE))
        repo.upsert(txn(categoryId = 2, type = TransactionType.INCOME))

        repo.observeFiltered(
            TransactionFilter.ByCategoryAndType(CategoryId(1), TransactionType.INCOME),
        ).test {
            val list = awaitItem()
            assertEquals(1, list.size)
            assertEquals(CategoryId(1), list.first().categoryId)
            assertEquals(TransactionType.INCOME, list.first().type)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun observeFilteredNoneDelegatesToObserveAll() = runTestWithDispatchers {
        repo.upsert(txn())
        repo.observeFiltered(TransactionFilter.None).test {
            assertEquals(1, awaitItem().size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun getByIdMiss() = runTestWithDispatchers {
        assertNull(repo.getById(TransactionId(404)))
    }

    @Test
    fun deleteTombstonesRow() = runTestWithDispatchers {
        val id = repo.upsert(txn())
        repo.delete(id)

        assertTrue(ds.rows.value.first { it.id == id.value }.deleted)
        repo.observeAll().test {
            assertTrue(awaitItem().isEmpty())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun deleteByAccountIdTombstonesAllForAccount() = runTestWithDispatchers {
        repo.upsert(txn(accountId = 1))
        repo.upsert(txn(accountId = 1))
        repo.upsert(txn(accountId = 2))

        repo.deleteByAccountId(AccountId(1))

        assertEquals(2, ds.rows.value.count { it.accountId == 1L && it.deleted })
        assertEquals(0, ds.rows.value.count { it.accountId == 2L && it.deleted })
    }

    @Test
    fun deleteAllClearsEverything() = runTestWithDispatchers {
        repo.upsert(txn())
        repo.deleteAll()
        assertTrue(ds.rows.value.isEmpty())
    }

    @Test
    fun convertCurrencyForAccountScalesAmountAndSetsCurrency() = runTestWithDispatchers {
        val id = repo.upsert(txn(amount = 100, accountId = 1))
        repo.upsert(txn(amount = 200, accountId = 2))

        repo.convertCurrencyForAccount(AccountId(1), CurrencyCode("USD"), 1.5)

        val converted = repo.getById(id)!!
        assertEquals(Money(150, CurrencyCode("USD")), converted.amount)
        assertEquals(eur, ds.rows.value.first { it.accountId == 2L }.let { CurrencyCode(it.currency) })
    }

    @Test
    fun earliestAndLatestTransactionDates() = runTestWithDispatchers {
        assertNull(repo.getEarliestTransactionDate())
        assertNull(repo.getLatestTransactionDate())

        repo.upsert(txn(date = LocalDate(2026, 3, 1)))
        repo.upsert(txn(date = LocalDate(2026, 8, 1)))

        assertEquals(LocalDate(2026, 3, 1), repo.getEarliestTransactionDate())
        assertEquals(LocalDate(2026, 8, 1), repo.getLatestTransactionDate())
    }

    @Test
    fun getTransactionDatesReturnsDistinctParsedDates() = runTestWithDispatchers {
        repo.upsert(txn(date = LocalDate(2026, 3, 1)))
        repo.upsert(txn(date = LocalDate(2026, 3, 1)))
        repo.upsert(txn(date = LocalDate(2026, 4, 2)))

        repo.getTransactionDates().test {
            val dates = awaitItem()
            assertEquals(setOf(LocalDate(2026, 3, 1), LocalDate(2026, 4, 2)), dates)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun countByRecurringId() = runTestWithDispatchers {
        repo.upsert(txn(recurringId = 7))
        repo.upsert(txn(recurringId = 7))
        repo.upsert(txn(recurringId = 9))

        assertEquals(2, repo.countByRecurringId(RecurringTransactionId(7)))
        assertEquals(0, repo.countByRecurringId(RecurringTransactionId(404)))
    }

    @Test
    fun exportForSyncMapsEntitiesToSyncRows() = runTestWithDispatchers {
        val id = repo.upsert(txn(amount = 321))

        val rows = repo.exportForSync()
        assertEquals(1, rows.size)
        val row = rows.first()
        assertEquals(id.value, row.id)
        assertEquals(321, row.amountMinor)
        assertEquals("2026-05-01", row.occurredOn)
        assertNotNull(row.syncId)
        assertFalse(row.deleted)
    }

    @Test
    fun upsertFromSyncInsertsThenUpdatesBySyncId() = runTestWithDispatchers {
        val syncId = "sync-1"
        val row = TransactionSyncRow(
            id = 0,
            syncId = syncId,
            type = "EXPENSE",
            amountMinor = 100,
            currency = "EUR",
            occurredOn = "2026-05-01",
            note = null,
            categoryId = 1,
            accountId = 1,
            paymentModeId = null,
            recurringId = null,
            deleted = false,
            createdAt = 10,
            updatedAt = 20,
        )

        val insertedId = repo.upsertFromSync(row)
        assertEquals(1, ds.rows.value.size)

        val updatedId = repo.upsertFromSync(row.copy(amountMinor = 555, updatedAt = 30))
        assertEquals(insertedId, updatedId)
        assertEquals(1, ds.rows.value.size)
        assertEquals(555, ds.rows.value.first().amountMinor)
    }

    @Test
    fun markDeletedAndReviveBySyncIdRoundTrip() = runTestWithDispatchers {
        val id = repo.upsert(txn())
        val syncId = ds.rows.value.first { it.id == id.value }.syncId!!

        repo.markDeletedBySyncId(syncId, 100)
        assertTrue(ds.rows.value.first { it.id == id.value }.deleted)

        repo.reviveBySyncId(syncId, 200)
        assertEquals(200, ds.rows.value.first { it.id == id.value }.updatedAt)
    }
}
