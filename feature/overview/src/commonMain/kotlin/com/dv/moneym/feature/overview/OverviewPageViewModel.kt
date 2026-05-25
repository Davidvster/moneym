package com.dv.moneym.feature.overview

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dv.moneym.core.common.AppClock
import com.dv.moneym.core.datastore.AppSettingsRepository
import com.dv.moneym.core.model.Category
import com.dv.moneym.core.model.CategoryId
import com.dv.moneym.core.model.Transaction
import com.dv.moneym.core.model.TransactionType
import com.dv.moneym.data.accounts.AccountRepository
import com.dv.moneym.data.budgets.BudgetRepository
import com.dv.moneym.data.categories.CategoryRepository
import com.dv.moneym.data.transactions.TransactionRepository
import com.dv.moneym.feature.overview.usecase.BuildCategoryBreakdownUseCase
import com.dv.moneym.feature.overview.usecase.BuildCategoryTrendsUseCase
import com.dv.moneym.feature.overview.usecase.BuildCumulativeSeriesUseCase
import com.dv.moneym.feature.overview.usecase.PeriodRange
import com.dv.moneym.feature.overview.usecase.ResolvePeriodRangeUseCase
import com.dv.moneym.feature.overview.usecase.BuildBudgetProgressUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

class OverviewPageViewModel(
    private val period: OverviewPeriod,
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
    private val accountRepository: AccountRepository,
    private val appSettingsRepository: AppSettingsRepository,
    private val budgetRepository: BudgetRepository,
    private val buildBudgetProgress: BuildBudgetProgressUseCase,
    private val resolvePeriodRange: ResolvePeriodRangeUseCase,
    private val buildCategoryBreakdown: BuildCategoryBreakdownUseCase,
    private val buildCategoryTrends: BuildCategoryTrendsUseCase,
    private val buildCumulativeSeries: BuildCumulativeSeriesUseCase,
    clock: AppClock,
) : ViewModel() {

    private val today = clock.today()

    private val _selectedCategoryId = MutableStateFlow<CategoryId?>(null)
    private val _selectedSliceIndex = MutableStateFlow<Int?>(null)

    internal val state = combine(
        transactionRepository.observeAll(),
        categoryRepository.observeAll(),
        combine(
            appSettingsRepository.observeSelectedAccountId(),
            accountRepository.observeAll(),
        ) { id, accs -> id to accs },
        _selectedCategoryId,
        budgetRepository.observeAll(),
    ) { allTransactions, categories, (selectedAccId, accounts), selectedCatId, budgets ->
        val catMap = categories.associateBy { it.id }
        val accountFilteredTransactions = if (selectedAccId > 0L) {
            allTransactions.filter { it.accountId.value == selectedAccId }
        } else allTransactions
        val periodTxns = resolvePeriodRange.filterByPeriod(accountFilteredTransactions, period)
        val filteredTxns = if (selectedCatId != null) {
            periodTxns.filter { it.categoryId == selectedCatId }
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
            range = range,
            periodTxns = periodTxns,
            accountFilteredTransactions = accountFilteredTransactions,
            catMap = catMap,
            expensesDouble = expensesDouble,
        )

        val newBreakdown = buildCategoryBreakdown(
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

        OverviewPageUiState(
            isLoading = false,
            isEmpty = periodTxns.isEmpty(),
            period = period,
            income = incomeDouble,
            expenses = expensesDouble,
            categoryBreakdown = newBreakdown,
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
        .combine(_selectedSliceIndex) { s, slice -> s.copy(selectedSliceIndex = slice) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Lazily,
            initialValue = OverviewPageUiState(period = period),
        )

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
        range: PeriodRange,
        periodTxns: List<Transaction>,
        accountFilteredTransactions: List<Transaction>,
        catMap: Map<CategoryId, Category>,
        expensesDouble: Double,
    ): PeriodSeriesBundle = when (period) {
        is OverviewPeriod.Month -> {
            val month = period.yearMonth
            val days = (range.endDate?.dayOfMonth) ?: 0
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
            val monthlyTotals = buildCumulativeSeries.monthlyTotals(accountFilteredTransactions, period.year)
            val currentMonthIndex = if (period.year == today.year) today.monthNumber - 1 else -1
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

    internal fun onIntent(intent: OverviewPageIntent) {
        when (intent) {
            is OverviewPageIntent.SliceTapped -> {
                _selectedSliceIndex.update { if (it == intent.index) null else intent.index }
            }

            is OverviewPageIntent.CategoryFilterSelected -> {
                _selectedCategoryId.update { id -> if (id == intent.id) null else intent.id }
                _selectedSliceIndex.value = null
            }
        }
    }
}
