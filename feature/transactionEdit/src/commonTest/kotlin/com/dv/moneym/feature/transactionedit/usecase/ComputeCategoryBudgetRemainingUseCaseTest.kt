package com.dv.moneym.feature.transactionedit.usecase

import com.dv.moneym.core.model.AccountId
import com.dv.moneym.core.model.Budget
import com.dv.moneym.core.model.BudgetId
import com.dv.moneym.core.model.BudgetPeriodType
import com.dv.moneym.core.model.CategoryId
import com.dv.moneym.core.model.CurrencyCode
import com.dv.moneym.core.model.Money
import com.dv.moneym.core.model.Transaction
import com.dv.moneym.core.model.TransactionId
import com.dv.moneym.core.model.TransactionType
import com.dv.moneym.core.model.YearMonth
import com.dv.moneym.core.testing.FakeBudgetRepository
import com.dv.moneym.core.testing.FakeTransactionRepository
import com.dv.moneym.core.testing.runTestWithDispatchers
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Instant

class ComputeCategoryBudgetRemainingUseCaseTest {

    private val epoch = Instant.fromEpochMilliseconds(0)
    private val budgetRepo = FakeBudgetRepository()
    private val txnRepo = FakeTransactionRepository()
    private val useCase = ComputeCategoryBudgetRemainingUseCase(budgetRepo, txnRepo)

    private fun budget(
        amountMinor: Long,
        categoryId: CategoryId?,
        startYm: YearMonth = YearMonth(2026, 5),
        name: String = "B",
    ) = Budget(
        id = BudgetId(0),
        name = name,
        amount = Money(amountMinor, CurrencyCode("EUR")),
        categoryId = categoryId,
        periodType = BudgetPeriodType.MONTHLY,
        startYearMonth = startYm,
        recurringMonths = Budget.UNLIMITED,
        createdAt = epoch,
        updatedAt = epoch,
    )

    private fun expense(
        id: Long,
        categoryId: CategoryId,
        amountMinor: Long,
        date: LocalDate = LocalDate(2026, 5, 10),
    ) = Transaction(
        id = TransactionId(id),
        type = TransactionType.EXPENSE,
        amount = Money(amountMinor, CurrencyCode("EUR")),
        occurredOn = date,
        note = null,
        categoryId = categoryId,
        accountId = AccountId(1),
        createdAt = epoch,
        updatedAt = epoch,
    )

    @Test
    fun no_budget_returns_null() = runTestWithDispatchers {
        val r = useCase(CategoryId(1), LocalDate(2026, 5, 10))
        assertNull(r)
    }

    @Test
    fun per_category_budget_match() = runTestWithDispatchers {
        budgetRepo.insert(budget(40000L, CategoryId(1), name = "Groceries"))
        txnRepo.addAll(listOf(expense(10, CategoryId(1), 15000L)))
        val r = assertNotNull(useCase(CategoryId(1), LocalDate(2026, 5, 10)))
        assertEquals("Groceries", r.budgetName)
        assertEquals(15000L, r.spent.minorUnits)
        assertEquals(25000L, r.remaining.minorUnits)
        assertFalse(r.isOverrun)
    }

    @Test
    fun all_categories_budget_used_when_no_per_cat() = runTestWithDispatchers {
        budgetRepo.insert(budget(100000L, null, name = "Month cap"))
        txnRepo.addAll(
            listOf(
                expense(10, CategoryId(1), 15000L),
                expense(11, CategoryId(2), 25000L),
            ),
        )
        val r = assertNotNull(useCase(CategoryId(1), LocalDate(2026, 5, 10)))
        assertEquals("Month cap", r.budgetName)
        assertEquals(40000L, r.spent.minorUnits)
    }

    @Test
    fun per_category_wins_over_all_categories() = runTestWithDispatchers {
        budgetRepo.insert(budget(100000L, null, name = "Month cap"))
        budgetRepo.insert(budget(40000L, CategoryId(1), name = "Groceries"))
        txnRepo.addAll(listOf(expense(10, CategoryId(1), 5000L)))
        val r = assertNotNull(useCase(CategoryId(1), LocalDate(2026, 5, 10)))
        assertEquals("Groceries", r.budgetName)
        assertEquals(5000L, r.spent.minorUnits)
    }

    @Test
    fun excludes_specified_transaction() = runTestWithDispatchers {
        budgetRepo.insert(budget(40000L, CategoryId(1)))
        txnRepo.addAll(
            listOf(
                expense(10, CategoryId(1), 15000L),
                expense(11, CategoryId(1), 5000L),
            ),
        )
        val r = assertNotNull(
            useCase(CategoryId(1), LocalDate(2026, 5, 10), TransactionId(10)),
        )
        assertEquals(5000L, r.spent.minorUnits)
    }

    @Test
    fun overrun_marks_negative_remaining() = runTestWithDispatchers {
        budgetRepo.insert(budget(10000L, CategoryId(1)))
        txnRepo.addAll(listOf(expense(10, CategoryId(1), 15000L)))
        val r = assertNotNull(useCase(CategoryId(1), LocalDate(2026, 5, 10)))
        assertTrue(r.isOverrun)
        assertEquals(-5000L, r.remaining.minorUnits)
        assertEquals(1.0f, r.fraction)
    }
}
