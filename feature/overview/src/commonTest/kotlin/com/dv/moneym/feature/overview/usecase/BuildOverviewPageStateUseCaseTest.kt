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

class BuildOverviewPageStateUseCaseTest {

    private val epoch = Instant.fromEpochMilliseconds(0)
    private val today = LocalDate(2026, 5, 15)

    private val useCase = BuildOverviewPageStateUseCase(
        resolvePeriodRange = ResolvePeriodRangeUseCase(),
        buildCategoryBreakdown = BuildCategoryBreakdownUseCase(),
        buildCategoryTrends = BuildCategoryTrendsUseCase(),
        buildCumulativeSeries = BuildCumulativeSeriesUseCase(),
        buildBudgetProgress = BuildBudgetProgressUseCase(),
    )

    private fun category(id: Long, name: String) = Category(
        id = CategoryId(id),
        name = name,
        iconKey = Icon.Basket.key,
        colorHex = "#112233",
        isUserCreated = true,
        archived = false,
        createdAt = epoch,
        updatedAt = epoch,
    )

    private fun txn(
        id: Long,
        type: TransactionType,
        amountMinor: Long,
        date: LocalDate,
        categoryId: Long = 1,
        accountId: Long = 1,
    ) = Transaction(
        id = TransactionId(id),
        type = type,
        amount = Money(amountMinor, CurrencyCode("EUR")),
        occurredOn = date,
        note = null,
        categoryId = CategoryId(categoryId),
        accountId = AccountId(accountId),
        createdAt = epoch,
        updatedAt = epoch,
    )

    private val cat1 = category(1, "Food")
    private val cat2 = category(2, "Travel")

    @Test
    fun empty_transactions_marks_state_empty() {
        val state = useCase(
            period = OverviewPeriod.Month(YearMonth(2026, 5)),
            today = today,
            allTransactions = emptyList(),
            categories = listOf(cat1),
            selectedAccountId = 0L,
            selectedCategoryId = null,
            budgets = emptyList(),
        )
        assertTrue(state.isEmpty)
        assertFalse(state.isLoading)
        assertEquals(0.0, state.income)
        assertEquals(0.0, state.expenses)
    }

    @Test
    fun month_period_totals_and_breakdowns() {
        val txns = listOf(
            txn(1, TransactionType.INCOME, 200000, LocalDate(2026, 5, 1)),
            txn(2, TransactionType.EXPENSE, 60000, LocalDate(2026, 5, 2), categoryId = 1),
            txn(3, TransactionType.EXPENSE, 40000, LocalDate(2026, 5, 3), categoryId = 2),
            txn(4, TransactionType.EXPENSE, 99999, LocalDate(2026, 4, 1), categoryId = 1),
        )
        val state = useCase(
            period = OverviewPeriod.Month(YearMonth(2026, 5)),
            today = today,
            allTransactions = txns,
            categories = listOf(cat1, cat2),
            selectedAccountId = 0L,
            selectedCategoryId = null,
            budgets = emptyList(),
        )
        assertFalse(state.isEmpty)
        assertEquals(2000.0, state.income)
        assertEquals(1000.0, state.expenses)
        assertEquals(2, state.categoryBreakdown.size)
        assertEquals(1, state.categoryIncomeBreakdown.size)
        assertEquals(31, state.dailyTotals.size)
        assertEquals(-1, state.currentMonthIndex)
    }

    @Test
    fun account_filter_excludes_other_accounts() {
        val txns = listOf(
            txn(1, TransactionType.EXPENSE, 50000, LocalDate(2026, 5, 2), accountId = 1),
            txn(2, TransactionType.EXPENSE, 30000, LocalDate(2026, 5, 2), accountId = 2),
        )
        val state = useCase(
            period = OverviewPeriod.Month(YearMonth(2026, 5)),
            today = today,
            allTransactions = txns,
            categories = listOf(cat1),
            selectedAccountId = 1L,
            selectedCategoryId = null,
            budgets = emptyList(),
        )
        assertEquals(500.0, state.expenses)
    }

    @Test
    fun category_filter_narrows_totals_but_period_emptiness_uses_unfiltered() {
        val txns = listOf(
            txn(1, TransactionType.EXPENSE, 50000, LocalDate(2026, 5, 2), categoryId = 1),
            txn(2, TransactionType.EXPENSE, 30000, LocalDate(2026, 5, 2), categoryId = 2),
        )
        val state = useCase(
            period = OverviewPeriod.Month(YearMonth(2026, 5)),
            today = today,
            allTransactions = txns,
            categories = listOf(cat1, cat2),
            selectedAccountId = 0L,
            selectedCategoryId = CategoryId(1),
            budgets = emptyList(),
        )
        assertEquals(500.0, state.expenses)
        assertFalse(state.isEmpty)
    }

    @Test
    fun year_period_builds_monthly_series() {
        val txns = listOf(
            txn(1, TransactionType.EXPENSE, 10000, LocalDate(2026, 1, 5)),
            txn(2, TransactionType.EXPENSE, 20000, LocalDate(2026, 3, 5)),
        )
        val state = useCase(
            period = OverviewPeriod.Year(2026),
            today = today,
            allTransactions = txns,
            categories = listOf(cat1),
            selectedAccountId = 0L,
            selectedCategoryId = null,
            budgets = emptyList(),
        )
        assertEquals(12, state.monthlyTotals.size)
        assertEquals(100.0, state.monthlyTotals[0])
        assertEquals(200.0, state.monthlyTotals[2])
        assertEquals(4, state.currentMonthIndex)
        assertTrue(state.dailyTotals.isEmpty())
    }

    @Test
    fun date_range_period_builds_trend_only() {
        val txns = listOf(
            txn(1, TransactionType.EXPENSE, 10000, LocalDate(2026, 5, 1)),
            txn(2, TransactionType.EXPENSE, 20000, LocalDate(2026, 5, 3)),
        )
        val state = useCase(
            period = OverviewPeriod.DateRange(2026, 5, 1, 2026, 5, 5),
            today = today,
            allTransactions = txns,
            categories = listOf(cat1),
            selectedAccountId = 0L,
            selectedCategoryId = null,
            budgets = emptyList(),
        )
        assertEquals(300.0, state.expenses)
        assertEquals(1, state.categoryDailyTrend.size)
        assertEquals(-1, state.currentMonthIndex)
    }

    @Test
    fun budget_progress_populated_for_month_period() {
        val budget = Budget(
            id = BudgetId(1),
            name = "Food cap",
            amount = Money(100000, CurrencyCode("EUR")),
            categoryId = CategoryId(1),
            accountId = AccountId(0),
            periodType = BudgetPeriodType.MONTHLY,
            startYearMonth = YearMonth(2026, 5),
            recurringMonths = Budget.UNLIMITED,
            createdAt = epoch,
            updatedAt = epoch,
        )
        val txns = listOf(txn(1, TransactionType.EXPENSE, 40000, LocalDate(2026, 5, 2), categoryId = 1))
        val state = useCase(
            period = OverviewPeriod.Month(YearMonth(2026, 5)),
            today = today,
            allTransactions = txns,
            categories = listOf(cat1),
            selectedAccountId = 0L,
            selectedCategoryId = null,
            budgets = listOf(budget),
        )
        assertEquals(1, state.budgetProgress.size)
        assertEquals(40000L, state.budgetProgress.first().spent.minorUnits)
    }
}
