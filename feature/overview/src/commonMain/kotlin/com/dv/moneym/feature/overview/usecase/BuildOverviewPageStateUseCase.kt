package com.dv.moneym.feature.overview.usecase

import com.dv.moneym.core.model.Budget
import com.dv.moneym.core.model.Category
import com.dv.moneym.core.model.CategoryId
import com.dv.moneym.core.model.Transaction
import com.dv.moneym.core.model.TransactionType
import com.dv.moneym.feature.overview.CategoryTrend
import com.dv.moneym.feature.overview.OverviewPeriod
import com.dv.moneym.feature.overview.page.OverviewPageUiState
import kotlinx.datetime.LocalDate
import kotlinx.datetime.number

class BuildOverviewPageStateUseCase(
    private val resolvePeriodRange: ResolvePeriodRangeUseCase,
    private val buildCategoryBreakdown: BuildCategoryBreakdownUseCase,
    private val buildCategoryTrends: BuildCategoryTrendsUseCase,
    private val buildCumulativeSeries: BuildCumulativeSeriesUseCase,
    private val buildBudgetProgress: BuildBudgetProgressUseCase,
) {

    internal operator fun invoke(
        period: OverviewPeriod,
        today: LocalDate,
        allTransactions: List<Transaction>,
        categories: List<Category>,
        selectedAccountId: Long,
        selectedCategoryId: CategoryId?,
        budgets: List<Budget>,
    ): OverviewPageUiState {
        val catMap = categories.associateBy { it.id }
        val accountFilteredTransactions = if (selectedAccountId > 0L) {
            allTransactions.filter { it.accountId.value == selectedAccountId }
        } else allTransactions
        val periodTxns = resolvePeriodRange.filterByPeriod(accountFilteredTransactions, period)
        val filteredTxns = if (selectedCategoryId != null) {
            periodTxns.filter { it.categoryId == selectedCategoryId }
        } else periodTxns

        val incomeDouble = filteredTxns
            .filter { it.type == TransactionType.INCOME }
            .sumOf { it.amount.minorUnits }
            .toDouble() / 100.0
        val expensesDouble = filteredTxns
            .filter { it.type == TransactionType.EXPENSE }
            .sumOf { it.amount.minorUnits }
            .toDouble() / 100.0

        val range = resolvePeriodRange(period, today)
        val isMonthMode = period is OverviewPeriod.Month
        val seriesBundle = buildPeriodSeries(
            period = period,
            today = today,
            range = range,
            periodTxns = periodTxns,
            accountFilteredTransactions = accountFilteredTransactions,
            catMap = catMap,
            expensesDouble = expensesDouble,
        )

        val expenseBreakdown = buildCategoryBreakdown(
            periodTxns = periodTxns,
            catMap = catMap,
            type = TransactionType.EXPENSE,
            elapsedDays = range.daysInRange,
            elapsedMonths = range.monthsInRange,
            isMonthMode = isMonthMode,
        )
        val incomeBreakdown = buildCategoryBreakdown(
            periodTxns = periodTxns,
            catMap = catMap,
            type = TransactionType.INCOME,
            elapsedDays = range.daysInRange,
            elapsedMonths = range.monthsInRange,
            isMonthMode = isMonthMode,
        )

        val budgetProgress = buildBudgetProgress(
            budgets = budgets,
            periodTxns = periodTxns,
            period = period,
            catMap = catMap,
        )

        return OverviewPageUiState(
            isLoading = false,
            isEmpty = periodTxns.isEmpty(),
            period = period,
            income = incomeDouble,
            expenses = expensesDouble,
            categoryBreakdown = expenseBreakdown,
            categoryIncomeBreakdown = incomeBreakdown,
            dailyTotals = seriesBundle.dailyTotals,
            cumulativeTotals = seriesBundle.cumulativeTotals,
            todayIndex = seriesBundle.todayIndex,
            categoryDailyTrend = seriesBundle.categoryDailyTrend,
            monthlyTotals = seriesBundle.monthlyTotals,
            categoryMonthlyTrend = seriesBundle.categoryMonthlyTrend,
            currentMonthIndex = seriesBundle.currentMonthIndex,
            avgDailyExpense = seriesBundle.avgDailyExpense,
            avgMonthlyExpense = seriesBundle.avgMonthlyExpense,
            avgDailyExpenseYear = seriesBundle.avgDailyExpenseYear,
            budgetProgress = budgetProgress,
        )
    }

    private data class PeriodSeriesBundle(
        val dailyTotals: List<Double>,
        val cumulativeTotals: List<Double>,
        val todayIndex: Int,
        val categoryDailyTrend: List<CategoryTrend>,
        val monthlyTotals: List<Double>,
        val categoryMonthlyTrend: List<CategoryTrend>,
        val currentMonthIndex: Int,
        val avgDailyExpense: Double,
        val avgMonthlyExpense: Double,
        val avgDailyExpenseYear: Double,
    )

    private fun buildPeriodSeries(
        period: OverviewPeriod,
        today: LocalDate,
        range: PeriodRange,
        periodTxns: List<Transaction>,
        accountFilteredTransactions: List<Transaction>,
        catMap: Map<CategoryId, Category>,
        expensesDouble: Double,
    ): PeriodSeriesBundle = when (period) {
        is OverviewPeriod.Month -> {
            val month = period.yearMonth
            val days = (range.endDate?.day) ?: 0
            val cumulative = buildCumulativeSeries(periodTxns, month.year, month.monthNumber, today)
            val categoryDailyTrend = buildCategoryTrends.daily(
                periodTxns = periodTxns,
                catMap = catMap,
                days = days,
                elapsedDays = range.daysInRange,
            )
            PeriodSeriesBundle(
                dailyTotals = cumulative.dailyTotals,
                cumulativeTotals = cumulative.cumulativeTotals,
                todayIndex = cumulative.todayIndex,
                categoryDailyTrend = categoryDailyTrend,
                monthlyTotals = List(12) { 0.0 },
                categoryMonthlyTrend = emptyList(),
                currentMonthIndex = -1,
                avgDailyExpense = if (range.daysInRange > 0) expensesDouble / range.daysInRange else 0.0,
                avgMonthlyExpense = 0.0,
                avgDailyExpenseYear = 0.0,
            )
        }

        is OverviewPeriod.Year -> {
            val monthlyTotals =
                buildCumulativeSeries.monthlyTotals(accountFilteredTransactions, period.year)
            val currentMonthIndex = if (period.year == today.year) today.month.number - 1 else -1
            val categoryMonthlyTrend = buildCategoryTrends.monthly(
                allTransactions = accountFilteredTransactions,
                catMap = catMap,
                year = period.year,
                elapsedMonths = range.monthsInRange,
                elapsedDays = range.daysInRange,
            )
            PeriodSeriesBundle(
                dailyTotals = emptyList(),
                cumulativeTotals = emptyList(),
                todayIndex = 0,
                categoryDailyTrend = emptyList(),
                monthlyTotals = monthlyTotals,
                categoryMonthlyTrend = categoryMonthlyTrend,
                currentMonthIndex = currentMonthIndex,
                avgDailyExpense = 0.0,
                avgMonthlyExpense = if (range.monthsInRange > 0) expensesDouble / range.monthsInRange else 0.0,
                avgDailyExpenseYear = if (range.daysInRange > 0) expensesDouble / range.daysInRange else 0.0,
            )
        }

        is OverviewPeriod.DateRange -> {
            val startDate = range.startDate!!
            val endDate = range.endDate!!
            val categoryDailyTrend = buildCategoryTrends.range(
                periodTxns = periodTxns,
                catMap = catMap,
                startDate = startDate,
                endDate = endDate,
            )
            PeriodSeriesBundle(
                dailyTotals = emptyList(),
                cumulativeTotals = emptyList(),
                todayIndex = 0,
                categoryDailyTrend = categoryDailyTrend,
                monthlyTotals = List(12) { 0.0 },
                categoryMonthlyTrend = emptyList(),
                currentMonthIndex = -1,
                avgDailyExpense = if (range.daysInRange > 0) expensesDouble / range.daysInRange else 0.0,
                avgMonthlyExpense = 0.0,
                avgDailyExpenseYear = 0.0,
            )
        }
    }
}
