package com.dv.moneym.feature.overview.usecase

import com.dv.moneym.core.model.CurrencyCode
import com.dv.moneym.core.model.Icon
import com.dv.moneym.core.model.Money
import com.dv.moneym.core.model.SpendingFilter
import com.dv.moneym.core.model.YearMonth
import com.dv.moneym.data.overview.OverviewAiWidget
import com.dv.moneym.data.overview.OverviewBlockId
import com.dv.moneym.data.overview.OverviewBuiltInBlockIds
import com.dv.moneym.data.overview.OverviewLayoutBlock
import com.dv.moneym.data.overview.OverviewLayoutPrefs
import com.dv.moneym.feature.overview.CategoryTrend
import com.dv.moneym.feature.overview.OverviewPeriod
import com.dv.moneym.feature.overview.page.OverviewPageUiState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.time.Instant

class ResolveOverviewBlocksUseCaseTest {
    private val resolve = ResolveOverviewBlocksUseCase()
    private val now = Instant.parse("2026-05-10T12:00:00Z")

    @Test
    fun defaultMonthOrderMatchesCurrentDashboard() {
        val blocks = resolve(
            period = OverviewPeriod.Month(YearMonth(2026, 5)),
            spendingFilter = SpendingFilter.All,
            layoutPrefs = OverviewLayoutPrefs(),
            pageState = pageState(period = OverviewPeriod.Month(YearMonth(2026, 5)), hasBudget = true),
            aiWidgets = emptyList(),
        )

        assertEquals(
            listOf(
                OverviewBuiltInBlockIds.Totals,
                OverviewBuiltInBlockIds.BudgetProgress,
                OverviewBuiltInBlockIds.Averages,
                OverviewBuiltInBlockIds.CategoryBreakdown,
                OverviewBuiltInBlockIds.CumulativeSpend,
                OverviewBuiltInBlockIds.CategoryTrends,
            ),
            blocks.builtInIds(),
        )
    }

    @Test
    fun defaultYearOrderIncludesChartsForAllFilter() {
        val blocks = resolve(
            period = OverviewPeriod.Year(2026),
            spendingFilter = SpendingFilter.All,
            layoutPrefs = OverviewLayoutPrefs(),
            pageState = pageState(period = OverviewPeriod.Year(2026), hasBudget = true),
            aiWidgets = emptyList(),
        )

        assertEquals(
            listOf(
                OverviewBuiltInBlockIds.Totals,
                OverviewBuiltInBlockIds.BudgetProgress,
                OverviewBuiltInBlockIds.Averages,
                OverviewBuiltInBlockIds.CategoryBreakdown,
                OverviewBuiltInBlockIds.MonthlySpend,
                OverviewBuiltInBlockIds.MonthlyIncome,
                OverviewBuiltInBlockIds.MonthlyNet,
                OverviewBuiltInBlockIds.CategoryTrends,
            ),
            blocks.builtInIds(),
        )
    }

    @Test
    fun defaultDateRangeOrderIncludesTrendsOnlyWhenRangeTrendsExist() {
        val period = OverviewPeriod.DateRange(2026, 5, 1, 2026, 5, 31)

        val withoutTrends = resolve(
            period = period,
            spendingFilter = SpendingFilter.Expenses,
            layoutPrefs = OverviewLayoutPrefs(),
            pageState = pageState(period = period, hasBudget = true),
            aiWidgets = emptyList(),
        )
        val withTrends = resolve(
            period = period,
            spendingFilter = SpendingFilter.Expenses,
            layoutPrefs = OverviewLayoutPrefs(),
            pageState = pageState(period = period, hasBudget = true, expenseTrends = listOf(trend())),
            aiWidgets = emptyList(),
        )

        assertEquals(
            listOf(
                OverviewBuiltInBlockIds.Totals,
                OverviewBuiltInBlockIds.BudgetProgress,
                OverviewBuiltInBlockIds.Averages,
                OverviewBuiltInBlockIds.CategoryBreakdown,
            ),
            withoutTrends.builtInIds(),
        )
        assertEquals(
            listOf(
                OverviewBuiltInBlockIds.Totals,
                OverviewBuiltInBlockIds.BudgetProgress,
                OverviewBuiltInBlockIds.Averages,
                OverviewBuiltInBlockIds.CategoryBreakdown,
                OverviewBuiltInBlockIds.CategoryTrends,
            ),
            withTrends.builtInIds(),
        )
    }

    @Test
    fun hiddenBlocksAreOmitted() {
        val blocks = resolve(
            period = OverviewPeriod.Month(YearMonth(2026, 5)),
            spendingFilter = SpendingFilter.All,
            layoutPrefs = OverviewLayoutPrefs(
                blocks = listOf(
                    OverviewLayoutBlock(OverviewBuiltInBlockIds.CategoryBreakdown, sortOrder = 3, visible = false),
                ),
            ),
            pageState = pageState(period = OverviewPeriod.Month(YearMonth(2026, 5)), hasBudget = true),
            aiWidgets = emptyList(),
        )

        assertEquals(
            listOf(
                OverviewBuiltInBlockIds.Totals,
                OverviewBuiltInBlockIds.BudgetProgress,
                OverviewBuiltInBlockIds.Averages,
                OverviewBuiltInBlockIds.CumulativeSpend,
                OverviewBuiltInBlockIds.CategoryTrends,
            ),
            blocks.builtInIds(),
        )
    }

    @Test
    fun budgetBlockIsOmittedWhenNoBudgetProgressExists() {
        val blocks = resolve(
            period = OverviewPeriod.Month(YearMonth(2026, 5)),
            spendingFilter = SpendingFilter.All,
            layoutPrefs = OverviewLayoutPrefs(),
            pageState = pageState(period = OverviewPeriod.Month(YearMonth(2026, 5))),
            aiWidgets = emptyList(),
        )

        assertEquals(
            listOf(
                OverviewBuiltInBlockIds.Totals,
                OverviewBuiltInBlockIds.Averages,
                OverviewBuiltInBlockIds.CategoryBreakdown,
                OverviewBuiltInBlockIds.CumulativeSpend,
                OverviewBuiltInBlockIds.CategoryTrends,
            ),
            blocks.builtInIds(),
        )
    }

    @Test
    fun yearChartBlocksHonorSpendingFilter() {
        val period = OverviewPeriod.Year(2026)

        val expenseBlocks = resolve(
            period = period,
            spendingFilter = SpendingFilter.Expenses,
            layoutPrefs = OverviewLayoutPrefs(),
            pageState = pageState(period = period),
            aiWidgets = emptyList(),
        )
        val incomeBlocks = resolve(
            period = period,
            spendingFilter = SpendingFilter.Income,
            layoutPrefs = OverviewLayoutPrefs(),
            pageState = pageState(period = period),
            aiWidgets = emptyList(),
        )

        assertEquals(
            listOf(
                OverviewBuiltInBlockIds.Totals,
                OverviewBuiltInBlockIds.Averages,
                OverviewBuiltInBlockIds.CategoryBreakdown,
                OverviewBuiltInBlockIds.MonthlySpend,
                OverviewBuiltInBlockIds.CategoryTrends,
            ),
            expenseBlocks.builtInIds(),
        )
        assertEquals(
            listOf(
                OverviewBuiltInBlockIds.Totals,
                OverviewBuiltInBlockIds.Averages,
                OverviewBuiltInBlockIds.CategoryBreakdown,
                OverviewBuiltInBlockIds.MonthlyIncome,
                OverviewBuiltInBlockIds.CategoryTrends,
            ),
            incomeBlocks.builtInIds(),
        )
    }

    @Test
    fun unknownPersistedBlockIdsAreIgnored() {
        val blocks = resolve(
            period = OverviewPeriod.Month(YearMonth(2026, 5)),
            spendingFilter = SpendingFilter.All,
            layoutPrefs = OverviewLayoutPrefs(
                blocks = listOf(
                    OverviewLayoutBlock(OverviewBlockId("future_block"), sortOrder = 0, visible = true),
                    OverviewLayoutBlock(OverviewBuiltInBlockIds.Averages, sortOrder = 1, visible = true),
                ),
            ),
            pageState = pageState(period = OverviewPeriod.Month(YearMonth(2026, 5))),
            aiWidgets = emptyList(),
        )

        assertEquals(
            listOf(
                OverviewBuiltInBlockIds.Averages,
                OverviewBuiltInBlockIds.Totals,
                OverviewBuiltInBlockIds.CategoryBreakdown,
                OverviewBuiltInBlockIds.CumulativeSpend,
                OverviewBuiltInBlockIds.CategoryTrends,
            ),
            blocks.builtInIds(),
        )
    }

    @Test
    fun enabledAiWidgetsAreIncludedAfterBuiltInsInSavedOrder() {
        val blocks = resolve(
            period = OverviewPeriod.Month(YearMonth(2026, 5)),
            spendingFilter = SpendingFilter.All,
            layoutPrefs = OverviewLayoutPrefs(),
            pageState = pageState(period = OverviewPeriod.Month(YearMonth(2026, 5))),
            aiWidgets = listOf(
                widget(id = 1, sortOrder = 2, enabled = true),
                widget(id = 2, sortOrder = 0, enabled = false),
                widget(id = 3, sortOrder = 1, enabled = true),
            ),
        )

        val aiBlocks = blocks.dropWhile { it is OverviewResolvedBlock.BuiltIn }
        assertEquals(listOf(3L, 1L), aiBlocks.map { assertIs<OverviewResolvedBlock.AiWidget>(it).widget.id })
    }

    private fun pageState(
        period: OverviewPeriod,
        hasBudget: Boolean = false,
        expenseTrends: List<CategoryTrend> = emptyList(),
        incomeTrends: List<CategoryTrend> = emptyList(),
    ) = OverviewPageUiState(
        period = period,
        budgetProgress = if (hasBudget) {
            listOf(
                BudgetProgress(
                    budgetId = 1L,
                    name = "Groceries",
                    amount = Money(10000L, CurrencyCode("EUR")),
                    spent = Money(4000L, CurrencyCode("EUR")),
                    remaining = Money(6000L, CurrencyCode("EUR")),
                    fraction = 0.4f,
                    isOverrun = false,
                    categoryName = "Groceries",
                    categoryColor = 0xFF4CAF50,
                )
            )
        } else {
            emptyList()
        },
        categoryDailyTrend = expenseTrends,
        categoryIncomeDailyTrend = incomeTrends,
    )

    private fun trend() = CategoryTrend(
        categoryName = "Groceries",
        categoryColor = 0xFF4CAF50,
        categoryIcon = Icon.Basket,
        totalAmount = 42.0,
        txCount = 1,
        series = listOf(42.0),
    )

    private fun widget(
        id: Long,
        sortOrder: Int,
        enabled: Boolean,
    ) = OverviewAiWidget(
        id = id,
        title = "Widget $id",
        prompt = "Prompt",
        a2uiJson = "{}",
        enabled = enabled,
        sortOrder = sortOrder,
        createdAt = now,
        updatedAt = now,
    )

    private fun List<OverviewResolvedBlock>.builtInIds(): List<OverviewBlockId> =
        mapNotNull { (it as? OverviewResolvedBlock.BuiltIn)?.blockId }
}
