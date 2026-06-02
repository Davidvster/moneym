package com.dv.moneym.data.budgets.internal

import app.cash.turbine.test
import com.dv.moneym.core.model.AccountId
import com.dv.moneym.core.model.Budget
import com.dv.moneym.core.model.BudgetId
import com.dv.moneym.core.model.BudgetPeriodType
import com.dv.moneym.core.model.CategoryId
import com.dv.moneym.core.model.CurrencyCode
import com.dv.moneym.core.model.Money
import com.dv.moneym.core.model.YearMonth
import com.dv.moneym.core.testing.runTestWithDispatchers
import com.dv.moneym.data.budgets.BudgetSyncRow
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Instant

class BudgetRepositoryImplTest {

    private val ds = FakeBudgetLocalDataSource()
    private val repo = BudgetRepositoryImpl(ds)
    private val epoch = Instant.fromEpochMilliseconds(0)

    private fun budget(
        id: Long = 0,
        name: String = "Groceries",
        accountId: Long = 1,
        categoryId: Long? = 5,
    ) = Budget(
        id = BudgetId(id),
        name = name,
        amount = Money(50000, CurrencyCode("EUR")),
        categoryId = categoryId?.let(::CategoryId),
        accountId = AccountId(accountId),
        periodType = BudgetPeriodType.MONTHLY,
        startYearMonth = YearMonth(2026, 3),
        recurringMonths = Budget.UNLIMITED,
        createdAt = epoch,
        updatedAt = epoch,
    )

    @Test
    fun insertReturnsTypedIdAndPersistsMappedFields() = runTestWithDispatchers {
        val id = repo.insert(budget(name = "Fuel", categoryId = 7))

        assertEquals(1L, id.value)
        val stored = repo.getById(id)
        assertNotNull(stored)
        assertEquals("Fuel", stored.name)
        assertEquals(Money(50000, CurrencyCode("EUR")), stored.amount)
        assertEquals(CategoryId(7), stored.categoryId)
        assertEquals(YearMonth(2026, 3), stored.startYearMonth)
        assertEquals(Budget.UNLIMITED, stored.recurringMonths)
    }

    @Test
    fun insertWithNullCategoryMapsBack() = runTestWithDispatchers {
        val id = repo.insert(budget(categoryId = null))
        assertNull(repo.getById(id)!!.categoryId)
    }

    @Test
    fun observeAllReturnsMappedDomainAndReflectsInserts() = runTestWithDispatchers {
        repo.observeAll().test {
            assertTrue(awaitItem().isEmpty())

            repo.insert(budget(name = "B1"))
            assertEquals(listOf("B1"), awaitItem().map { it.name })

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun observeByAccountFiltersToAccountOrGlobal() = runTestWithDispatchers {
        repo.insert(budget(name = "AcctOne", accountId = 1))
        repo.insert(budget(name = "AcctTwo", accountId = 2))
        repo.insert(budget(name = "Global", accountId = 0))

        repo.observeByAccount(AccountId(1)).test {
            val names = awaitItem().map { it.name }.toSet()
            assertEquals(setOf("AcctOne", "Global"), names)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun getByIdMiss() = runTestWithDispatchers {
        assertNull(repo.getById(BudgetId(404)))
    }

    @Test
    fun updatePropagatesFields() = runTestWithDispatchers {
        val id = repo.insert(budget(name = "Old"))
        repo.update(
            budget(id = id.value, name = "New").copy(
                amount = Money(999, CurrencyCode("USD")),
            )
        )

        val stored = repo.getById(id)
        assertNotNull(stored)
        assertEquals("New", stored.name)
        assertEquals(Money(999, CurrencyCode("USD")), stored.amount)
    }

    @Test
    fun deleteTombstonesRow() = runTestWithDispatchers {
        val id = repo.insert(budget())
        repo.delete(id)

        assertTrue(ds.rows.value.first { it.id == id.value }.deleted)
        repo.observeAll().test {
            assertTrue(awaitItem().isEmpty())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun exportForSyncMapsEntitiesToSyncRows() = runTestWithDispatchers {
        val id = repo.insert(budget(name = "Exp"))

        val rows = repo.exportForSync()
        assertEquals(1, rows.size)
        val row = rows.first()
        assertEquals(id.value, row.id)
        assertEquals("Exp", row.name)
        assertEquals("2026-03", row.startYearMonth)
        assertNotNull(row.syncId)
        assertFalse(row.deleted)
    }

    @Test
    fun upsertFromSyncInsertsThenUpdatesBySyncId() = runTestWithDispatchers {
        val syncId = "sync-1"
        val row = BudgetSyncRow(
            id = 0,
            syncId = syncId,
            name = "Synced",
            amountMinor = 1000,
            currency = "EUR",
            categoryId = null,
            accountId = 3,
            periodType = "MONTHLY",
            startYearMonth = "2026-01",
            recurringMonths = null,
            deleted = false,
            createdAt = 10,
            updatedAt = 20,
        )

        val insertedId = repo.upsertFromSync(row)
        assertEquals(1, ds.rows.value.size)

        val updatedId = repo.upsertFromSync(row.copy(name = "Renamed", updatedAt = 30))
        assertEquals(insertedId, updatedId)
        assertEquals(1, ds.rows.value.size)
        assertEquals("Renamed", ds.rows.value.first().name)
    }

    @Test
    fun markDeletedAndReviveBySyncIdRoundTrip() = runTestWithDispatchers {
        val id = repo.insert(budget())
        val syncId = ds.rows.value.first { it.id == id.value }.syncId!!

        repo.markDeletedBySyncId(syncId, 100)
        assertTrue(ds.rows.value.first { it.id == id.value }.deleted)

        repo.reviveBySyncId(syncId, 200)
        assertEquals(200, ds.rows.value.first { it.id == id.value }.updatedAt)
    }
}
