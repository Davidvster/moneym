package com.dv.moneym.feature.overview.usecase

import com.dv.moneym.core.model.Budget
import com.dv.moneym.core.model.Category
import com.dv.moneym.core.model.CategoryId
import com.dv.moneym.core.model.Transaction
import com.dv.moneym.core.model.TransactionFilter
import com.dv.moneym.core.model.TransactionType
import com.dv.moneym.core.model.matches
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
        transactionFilter: TransactionFilter,
        budgets: List<Budget>,
    ): OverviewPageUiState {
        val catMap = categories.associateBy { it.id }
        val accountFilteredTransactions = if (selectedAccountId > 0L) {
            allTransactions.filter { it.accountId.value == selectedAccountId }
        } else allTransactions
        val filterableTransactions = accountFilteredTransactions.filter { transactionFilter.matches(it) }
        val periodTxns = resolvePeriodRange.filterByPeriod(filterableTransactions, period)

        val incomeDouble = periodTxns
            .filter { it.type == TransactionType.INCOME }
            .sumOf { it.amount.minorUnits }
            .toDouble() / 100.0
        val expensesDouble = periodTxns
            .filter { it.type == TransactionType.EXPENSE }
            .sumOf { it.amount.minorUnits }
            .toDouble() / 100.0
        val netDouble = incomeDouble - expensesDouble

        val range = resolvePeriodRange(period, today)
        val isMonthMode = period is OverviewPeriod.Month
        val seriesBundle = buildPeriodSeries(
            period = period,
            today = today,
            range = range,
            periodTxns = periodTxns,
            accountFilteredTransactions = filterableTransactions,
            catMap = catMap,
            expensesDouble = expensesDouble,
            incomeDouble = incomeDouble,
            netDouble = netDouble,
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
            categoryIncomeDailyTrend = seriesBundle.categoryIncomeDailyTrend,
            monthlyTotals = seriesBundle.monthlyTotals,
            monthlyIncomeTotals = seriesBundle.monthlyIncomeTotals,
            monthlyNetTotals = seriesBundle.monthlyNetTotals,
            categoryMonthlyTrend = seriesBundle.categoryMonthlyTrend,
            categoryIncomeMonthlyTrend = seriesBundle.categoryIncomeMonthlyTrend,
            currentMonthIndex = seriesBundle.currentMonthIndex,
            avgDailyExpense = seriesBundle.avgDailyExpense,
            avgMonthlyExpense = seriesBundle.avgMonthlyExpense,
            avgDailyExpenseYear = seriesBundle.avgDailyExpenseYear,
            avgDailyIncome = seriesBundle.avgDailyIncome,
            avgMonthlyIncome = seriesBundle.avgMonthlyIncome,
            avgDailyIncomeYear = seriesBundle.avgDailyIncomeYear,
            avgDailyNet = seriesBundle.avgDailyNet,
            avgMonthlyNet = seriesBundle.avgMonthlyNet,
            avgDailyNetYear = seriesBundle.avgDailyNetYear,
            budgetProgress = budgetProgress,
        )
    }

    private data class PeriodSeriesBundle(
        val dailyTotals: List<Double>,
        val cumulativeTotals: List<Double>,
        val todayIndex: Int,
        val categoryDailyTrend: List<CategoryTrend>,
        val categoryIncomeDailyTrend: List<CategoryTrend>,
        val monthlyTotals: List<Double>,
        val monthlyIncomeTotals: List<Double>,
        val monthlyNetTotals: List<Double>,
        val categoryMonthlyTrend: List<CategoryTrend>,
        val categoryIncomeMonthlyTrend: List<CategoryTrend>,
        val currentMonthIndex: Int,
        val avgDailyExpense: Double,
        val avgMonthlyExpense: Double,
        val avgDailyExpenseYear: Double,
        val avgDailyIncome: Double,
        val avgMonthlyIncome: Double,
        val avgDailyIncomeYear: Double,
        val avgDailyNet: Double,
        val avgMonthlyNet: Double,
        val avgDailyNetYear: Double,
    )

    private fun buildPeriodSeries(
        period: OverviewPeriod,
        today: LocalDate,
        range: PeriodRange,
        periodTxns: List<Transaction>,
        accountFilteredTransactions: List<Transaction>,
        catMap: Map<CategoryId, Category>,
        expensesDouble: Double,
        incomeDouble: Double,
        netDouble: Double,
    ): PeriodSeriesBundle = when (period) {
        is OverviewPeriod.Month -> {
            val month = period.yearMonth
            val days = (range.endDate?.day) ?: 0
            val cumulative = buildCumulativeSeries(periodTxns, month.year, month.monthNumber, today)
            val categoryDailyTrend = buildCategoryTrends.daily(
                periodTxns = periodTxns,
                catMap = catMap,
                types = setOf(TransactionType.EXPENSE),
                days = days,
                elapsedDays = range.daysInRange,
            )
            val categoryIncomeDailyTrend = buildCategoryTrends.daily(
                periodTxns = periodTxns,
                catMap = catMap,
                types = setOf(TransactionType.INCOME),
                days = days,
                elapsedDays = range.daysInRange,
            )
            val daysDiv = range.daysInRange.coerceAtLeast(1)
            PeriodSeriesBundle(
                dailyTotals = cumulative.dailyTotals,
                cumulativeTotals = cumulative.cumulativeTotals,
                todayIndex = cumulative.todayIndex,
                categoryDailyTrend = categoryDailyTrend,
                categoryIncomeDailyTrend = categoryIncomeDailyTrend,
                monthlyTotals = List(12) { 0.0 },
                monthlyIncomeTotals = List(12) { 0.0 },
                monthlyNetTotals = List(12) { 0.0 },
                categoryMonthlyTrend = emptyList(),
                categoryIncomeMonthlyTrend = emptyList(),
                currentMonthIndex = -1,
                avgDailyExpense = expensesDouble / daysDiv,
                avgMonthlyExpense = 0.0,
                avgDailyExpenseYear = 0.0,
                avgDailyIncome = incomeDouble / daysDiv,
                avgMonthlyIncome = 0.0,
                avgDailyIncomeYear = 0.0,
                avgDailyNet = netDouble / daysDiv,
                avgMonthlyNet = 0.0,
                avgDailyNetYear = 0.0,
            )
        }

        is OverviewPeriod.Year -> {
            val monthlyTotals =
                buildCumulativeSeries.monthlyTotals(accountFilteredTransactions, period.year)
            val monthlyIncomeTotals = buildCumulativeSeries.monthlyTotals(
                accountFilteredTransactions, period.year, TransactionType.INCOME,
            )
            val monthlyNetTotals =
                buildCumulativeSeries.monthlyNetTotals(accountFilteredTransactions, period.year)
            val currentMonthIndex = if (period.year == today.year) today.month.number - 1 else -1
            val categoryMonthlyTrend = buildCategoryTrends.monthly(
                allTransactions = accountFilteredTransactions,
                catMap = catMap,
                types = setOf(TransactionType.EXPENSE),
                year = period.year,
                elapsedMonths = range.monthsInRange,
                elapsedDays = range.daysInRange,
            )
            val categoryIncomeMonthlyTrend = buildCategoryTrends.monthly(
                allTransactions = accountFilteredTransactions,
                catMap = catMap,
                types = setOf(TransactionType.INCOME),
                year = period.year,
                elapsedMonths = range.monthsInRange,
                elapsedDays = range.daysInRange,
            )
            val daysDiv = range.daysInRange.coerceAtLeast(1)
            val monthsDiv = range.monthsInRange.coerceAtLeast(1)
            PeriodSeriesBundle(
                dailyTotals = emptyList(),
                cumulativeTotals = emptyList(),
                todayIndex = 0,
                categoryDailyTrend = emptyList(),
                categoryIncomeDailyTrend = emptyList(),
                monthlyTotals = monthlyTotals,
                monthlyIncomeTotals = monthlyIncomeTotals,
                monthlyNetTotals = monthlyNetTotals,
                categoryMonthlyTrend = categoryMonthlyTrend,
                categoryIncomeMonthlyTrend = categoryIncomeMonthlyTrend,
                currentMonthIndex = currentMonthIndex,
                avgDailyExpense = 0.0,
                avgMonthlyExpense = expensesDouble / monthsDiv,
                avgDailyExpenseYear = expensesDouble / daysDiv,
                avgDailyIncome = 0.0,
                avgMonthlyIncome = incomeDouble / monthsDiv,
                avgDailyIncomeYear = incomeDouble / daysDiv,
                avgDailyNet = 0.0,
                avgMonthlyNet = netDouble / monthsDiv,
                avgDailyNetYear = netDouble / daysDiv,
            )
        }

        is OverviewPeriod.DateRange -> {
            val startDate = range.startDate!!
            val endDate = range.endDate!!
            val categoryDailyTrend = buildCategoryTrends.range(
                periodTxns = periodTxns,
                catMap = catMap,
                types = setOf(TransactionType.EXPENSE),
                startDate = startDate,
                endDate = endDate,
            )
            val categoryIncomeDailyTrend = buildCategoryTrends.range(
                periodTxns = periodTxns,
                catMap = catMap,
                types = setOf(TransactionType.INCOME),
                startDate = startDate,
                endDate = endDate,
            )
            val daysDiv = range.daysInRange.coerceAtLeast(1)
            PeriodSeriesBundle(
                dailyTotals = emptyList(),
                cumulativeTotals = emptyList(),
                todayIndex = 0,
                categoryDailyTrend = categoryDailyTrend,
                categoryIncomeDailyTrend = categoryIncomeDailyTrend,
                monthlyTotals = List(12) { 0.0 },
                monthlyIncomeTotals = List(12) { 0.0 },
                monthlyNetTotals = List(12) { 0.0 },
                categoryMonthlyTrend = emptyList(),
                categoryIncomeMonthlyTrend = emptyList(),
                currentMonthIndex = -1,
                avgDailyExpense = expensesDouble / daysDiv,
                avgMonthlyExpense = 0.0,
                avgDailyExpenseYear = 0.0,
                avgDailyIncome = incomeDouble / daysDiv,
                avgMonthlyIncome = 0.0,
                avgDailyIncomeYear = 0.0,
                avgDailyNet = netDouble / daysDiv,
                avgMonthlyNet = 0.0,
                avgDailyNetYear = 0.0,
            )
        }
    }
}
