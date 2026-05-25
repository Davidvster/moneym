package com.dv.moneym.feature.overview.usecase

import com.dv.moneym.core.model.AccountId
import com.dv.moneym.core.model.Budget
import com.dv.moneym.core.model.BudgetId
import com.dv.moneym.core.model.BudgetPeriodType
import com.dv.moneym.core.model.Category
import com.dv.moneym.core.model.CategoryId
import com.dv.moneym.core.model.CurrencyCode
import com.dv.moneym.core.model.Icon
import com.dv.moneym.core.model.Money
import com.dv.moneym.core.model.Transaction
import com.dv.moneym.core.model.TransactionId
import com.dv.moneym.core.model.TransactionType
import com.dv.moneym.core.model.YearMonth
import com.dv.moneym.feature.overview.OverviewPeriod
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Instant

class BuildBudgetProgressUseCaseTest {

    private val epoch = Instant.fromEpochMilliseconds(0)
    private val useCase = BuildBudgetProgressUseCase()

    private fun budget(
        id: Long,
        amountMinor: Long = 40000L,
        categoryId: CategoryId? = null,
        startYm: YearMonth = YearMonth(2026, 5),
        recurringMonths: Int? = Budget.UNLIMITED,
    ) = Budget(
        id = BudgetId(id),
        name = "B$id",
        amount = Money(amountMinor, CurrencyCode("EUR")),
        categoryId = categoryId,
        periodType = BudgetPeriodType.MONTHLY,
        startYearMonth = startYm,
        recurringMonths = recurringMonths,
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
        accountId = AccountId(1),
        categoryId = categoryId,
        type = TransactionType.EXPENSE,
        amount = Money(amountMinor, CurrencyCode("EUR")),
        occurredOn = date,
        note = null,
        paymentModeId = null,
        createdAt = epoch,
        updatedAt = epoch,
    )

    private val cat1 = Category(
        id = CategoryId(1),
        name = "Groceries",
        iconKey = Icon.Basket.key,
        colorHex = "#4A8E5C",
        isUserCreated = true,
        archived = false,
        createdAt = epoch,
        updatedAt = epoch,
    )
    private val cat2 = Category(
        id = CategoryId(2),
        name = "Rent",
        iconKey = Icon.Home.key,
        colorHex = "#C2566B",
        isUserCreated = true,
        archived = false,
        createdAt = epoch,
        updatedAt = epoch,
    )
    private val catMap = mapOf(cat1.id to cat1, cat2.id to cat2)

    private val periodMay = OverviewPeriod.Month(YearMonth(2026, 5))

    @Test
    fun per_category_budget_counts_only_matching_txns() {
        val budgets = listOf(budget(1, amountMinor = 40000L, categoryId = cat1.id))
        val txns = listOf(
            expense(10, cat1.id, 15000L),
            expense(11, cat2.id, 99999L),
        )
        val result = useCase(budgets, txns, periodMay, catMap)
        assertEquals(1, result.size)
        val p = result.first()
        assertEquals(15000L, p.spent.minorUnits)
        assertEquals(25000L, p.remaining.minorUnits)
        assertEquals(0.375f, p.fraction)
        assertFalse(p.isOverrun)
        assertEquals("Groceries", p.categoryName)
    }

    @Test
    fun all_categories_budget_sums_all_expenses() {
        val budgets = listOf(budget(1, amountMinor = 100000L, categoryId = null))
        val txns = listOf(
            expense(10, cat1.id, 15000L),
            expense(11, cat2.id, 25000L),
        )
        val result = useCase(budgets, txns, periodMay, catMap)
        assertEquals(1, result.size)
        assertEquals(40000L, result.first().spent.minorUnits)
        assertEquals(60000L, result.first().remaining.minorUnits)
        assertEquals(null, result.first().categoryName)
    }

    @Test
    fun overrun_marks_negative_remaining_and_fraction_clamped() {
        val budgets = listOf(budget(1, amountMinor = 10000L, categoryId = cat1.id))
        val txns = listOf(expense(10, cat1.id, 15000L))
        val result = useCase(budgets, txns, periodMay, catMap)
        val p = result.first()
        assertTrue(p.isOverrun)
        assertEquals(-5000L, p.remaining.minorUnits)
        assertEquals(1.0f, p.fraction)
    }

    @Test
    fun inactive_budget_excluded() {
        val budgets = listOf(
            budget(1, categoryId = cat1.id, startYm = YearMonth(2026, 6), recurringMonths = null),
        )
        val result = useCase(budgets, emptyList(), periodMay, catMap)
        assertTrue(result.isEmpty())
    }

    @Test
    fun n_months_window_includes_then_excludes() {
        val b = budget(
            id = 1,
            categoryId = cat1.id,
            startYm = YearMonth(2026, 5),
            recurringMonths = 2,
        )
        val activeIn5 = useCase(listOf(b), emptyList(), OverviewPeriod.Month(YearMonth(2026, 5)), catMap)
        val activeIn6 = useCase(listOf(b), emptyList(), OverviewPeriod.Month(YearMonth(2026, 6)), catMap)
        val activeIn7 = useCase(listOf(b), emptyList(), OverviewPeriod.Month(YearMonth(2026, 7)), catMap)
        assertEquals(1, activeIn5.size)
        assertEquals(1, activeIn6.size)
        assertTrue(activeIn7.isEmpty())
    }

    @Test
    fun year_period_returns_empty_in_v1() {
        val budgets = listOf(budget(1, categoryId = cat1.id))
        val result = useCase(budgets, emptyList(), OverviewPeriod.Year(2026), catMap)
        assertTrue(result.isEmpty())
    }

    @Test
    fun date_range_period_returns_empty_in_v1() {
        val budgets = listOf(budget(1, categoryId = cat1.id))
        val result = useCase(
            budgets,
            emptyList(),
            OverviewPeriod.DateRange(2026, 5, 1, 2026, 5, 31),
            catMap,
        )
        assertTrue(result.isEmpty())
    }

    @Test
    fun sorted_worst_first() {
        val budgets = listOf(
            budget(1, amountMinor = 10000L, categoryId = cat1.id),
            budget(2, amountMinor = 10000L, categoryId = cat2.id),
        )
        val txns = listOf(
            expense(10, cat1.id, 2000L),
            expense(11, cat2.id, 8000L),
        )
        val result = useCase(budgets, txns, periodMay, catMap)
        assertEquals(2L, result.first().budgetId)
        assertEquals(1L, result.last().budgetId)
    }
}
