package com.dv.moneym.data.budgets

import app.cash.turbine.test
import com.dv.moneym.core.model.Budget
import com.dv.moneym.core.model.BudgetId
import com.dv.moneym.core.model.BudgetPeriodType
import com.dv.moneym.core.model.CategoryId
import com.dv.moneym.core.model.CurrencyCode
import com.dv.moneym.core.model.Money
import com.dv.moneym.core.model.YearMonth
import com.dv.moneym.core.testing.FakeBudgetRepository
import com.dv.moneym.core.testing.runTestWithDispatchers
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.time.Instant

class FakeBudgetRepositoryTest {

    private fun sample(name: String = "Groceries"): Budget = Budget(
        id = BudgetId(0),
        name = name,
        amount = Money(40000L, CurrencyCode("EUR")),
        categoryId = CategoryId(1),
        periodType = BudgetPeriodType.MONTHLY,
        startYearMonth = YearMonth(2026, 5),
        recurringMonths = Budget.UNLIMITED,
        createdAt = Instant.fromEpochMilliseconds(0),
        updatedAt = Instant.fromEpochMilliseconds(0),
    )

    @Test fun insert_then_observe_emits_inserted_budget() = runTestWithDispatchers {
        val repo = FakeBudgetRepository()
        repo.observeAll().test {
            assertEquals(emptyList(), awaitItem())
            val id = repo.insert(sample())
            val list = awaitItem()
            assertEquals(1, list.size)
            assertEquals(id, list.first().id)
            assertEquals("Groceries", list.first().name)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test fun update_replaces_existing() = runTestWithDispatchers {
        val repo = FakeBudgetRepository()
        val id = repo.insert(sample())
        val current = assertNotNull(repo.getById(id))
        repo.update(current.copy(name = "Food"))
        assertEquals("Food", repo.getById(id)?.name)
    }

    @Test fun delete_removes() = runTestWithDispatchers {
        val repo = FakeBudgetRepository()
        val id = repo.insert(sample())
        repo.delete(id)
        assertNull(repo.getById(id))
    }
}
