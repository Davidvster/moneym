package com.dv.moneym.feature.overview.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dv.moneym.core.common.AppClock
import com.dv.moneym.core.model.CategoryId
import com.dv.moneym.core.model.CurrencyCode
import com.dv.moneym.core.model.Money
import com.dv.moneym.core.model.Transaction
import com.dv.moneym.core.model.TransactionType
import com.dv.moneym.core.model.YearMonth
import com.dv.moneym.core.model.format
import com.dv.moneym.data.categories.CategoryRepository
import com.dv.moneym.data.transactions.TransactionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.datetime.LocalDate

class OverviewViewModel(
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
    clock: AppClock,
) : ViewModel() {

    private val today = clock.today()
    private val _period = MutableStateFlow<OverviewPeriod>(
        OverviewPeriod.Month(YearMonth(today.year, today.monthNumber))
    )
    private val _periodOffset = MutableStateFlow(0)
    private val _selectedCategoryId = MutableStateFlow<CategoryId?>(null)
    private val _selectedSliceIndex = MutableStateFlow<Int?>(null)

    val state: StateFlow<OverviewUiState> = combine(
        _period,
        _selectedCategoryId,
        transactionRepository.observeAll(),
        categoryRepository.observeAll(),
    ) { period, selectedCatId, allTransactions, categories ->
        val catMap = categories.associateBy { it.id }

        val periodTxns = allTransactions.filter { it.matchesPeriod(period) }

        val filteredTxns = if (selectedCatId != null) {
            periodTxns.filter { it.categoryId == selectedCatId }
        } else {
            periodTxns
        }

        // ── Legacy list-based totals (for existing tests) ────────
        val income = filteredTxns.filter { it.type == TransactionType.INCOME }
            .groupBy { it.amount.currency }
            .map { (cur, txns) -> MoneyTotal(txns.sumOf { it.amount.minorUnits }, cur) }

        val expense = filteredTxns.filter { it.type == TransactionType.EXPENSE }
            .groupBy { it.amount.currency }
            .map { (cur, txns) -> MoneyTotal(txns.sumOf { it.amount.minorUnits }, cur) }

        // ── New Double summaries ──────────────────────────────────
        val incomeDouble = filteredTxns
            .filter { it.type == TransactionType.INCOME }
            .sumOf { it.amount.minorUnits }
            .toDouble() / 100.0

        val expensesDouble = filteredTxns
            .filter { it.type == TransactionType.EXPENSE }
            .sumOf { it.amount.minorUnits }
            .toDouble() / 100.0

        // ── Category breakdown (for donut + legend) ───────────────
        val totalExpenseMinor = periodTxns
            .filter { it.type == TransactionType.EXPENSE }
            .sumOf { it.amount.minorUnits }

        // Legacy breakdown (used internally for chartBars / availableCategories)
        val legacyBreakdown = periodTxns
            .filter { it.type == TransactionType.EXPENSE }
            .groupBy { it.categoryId }
            .map { (catId, txns) ->
                val cat = catMap[catId]
                val amount = txns.sumOf { it.amount.minorUnits }
                val currency = txns.firstOrNull()?.amount?.currency ?: CurrencyCode("EUR")
                CategoryBreakdownItem(
                    id = catId,
                    name = cat?.name ?: "—",
                    colorHex = cat?.colorHex ?: "#8A8A8A",
                    iconKey = cat?.iconKey ?: "dots",
                    expenseMinorUnits = amount,
                    percentage = if (totalExpenseMinor > 0) amount.toFloat() / totalExpenseMinor else 0f,
                    formattedAmount = Money(amount, currency).format(),
                )
            }
            .sortedByDescending { it.expenseMinorUnits }

        // New CategorySpend list
        val newBreakdown = periodTxns
            .filter { it.type == TransactionType.EXPENSE }
            .groupBy { it.categoryId }
            .map { (catId, txns) ->
                val cat = catMap[catId]
                val amountMinor = txns.sumOf { it.amount.minorUnits }
                val colorLong = colorHexToLong(cat?.colorHex ?: "#8A8A8A")
                CategorySpend(
                    categoryName = cat?.name ?: "—",
                    categoryColor = colorLong,
                    categoryIcon = cat?.iconKey ?: "dots",
                    amount = amountMinor.toDouble() / 100.0,
                    percent = if (totalExpenseMinor > 0)
                        ((amountMinor.toDouble() / totalExpenseMinor.toDouble()) * 100).toInt()
                    else 0,
                )
            }
            .sortedByDescending { it.amount }

        val availableCategories = categories.filter { cat ->
            periodTxns.any { it.categoryId == cat.id }
        }

        // ── Mode-specific data ────────────────────────────────────
        val mode = when (period) {
            is OverviewPeriod.Month -> OverviewMode.Month
            is OverviewPeriod.Year -> OverviewMode.Year
        }

        val dailyTotals: List<Double>
        val cumulativeTotals: List<Double>
        val todayIndex: Int
        val categoryDailyTrend: List<CategoryTrend>
        val monthlyTotals: List<Double>
        val categoryMonthlyTrend: List<CategoryTrend>
        val currentMonthIndex: Int

        when (period) {
            is OverviewPeriod.Month -> {
                val month = period.yearMonth
                val days = daysInMonth(month.year, month.monthNumber)
                val isCurrentMonth = month.year == today.year && month.monthNumber == today.monthNumber

                // Daily expense totals (indices 0..days-1)
                val rawDaily = (1..days).map { day ->
                    periodTxns
                        .filter {
                            it.type == TransactionType.EXPENSE &&
                                it.occurredOn.dayOfMonth == day
                        }
                        .sumOf { it.amount.minorUnits }
                        .toDouble() / 100.0
                }
                dailyTotals = rawDaily

                // Cumulative (running sum)
                var running = 0.0
                cumulativeTotals = rawDaily.map { v -> running += v; running }

                // todayIndex: current day - 1 if in current month, else last day - 1
                todayIndex = if (isCurrentMonth) {
                    (today.dayOfMonth - 1).coerceIn(0, days - 1)
                } else {
                    days - 1
                }

                // Category daily trends
                categoryDailyTrend = buildCategoryDailyTrend(
                    periodTxns = periodTxns,
                    catMap = catMap,
                    days = days,
                    month = month,
                )

                // Year-mode fields — empty for month mode
                monthlyTotals = List(12) { 0.0 }
                categoryMonthlyTrend = emptyList()
                currentMonthIndex = -1
            }

            is OverviewPeriod.Year -> {
                // Month totals for the selected year
                monthlyTotals = (1..12).map { m ->
                    allTransactions
                        .filter {
                            it.type == TransactionType.EXPENSE &&
                                it.occurredOn.year == period.year &&
                                it.occurredOn.monthNumber == m
                        }
                        .sumOf { it.amount.minorUnits }
                        .toDouble() / 100.0
                }

                currentMonthIndex = if (period.year == today.year) today.monthNumber - 1 else -1

                categoryMonthlyTrend = buildCategoryMonthlyTrend(
                    allTransactions = allTransactions,
                    catMap = catMap,
                    year = period.year,
                )

                // Month-mode fields — empty for year mode
                dailyTotals = emptyList()
                cumulativeTotals = emptyList()
                todayIndex = 0
                categoryDailyTrend = emptyList()
            }
        }

        OverviewUiState(
            isLoading = false,
            isEmpty = periodTxns.isEmpty(),
            period = period,
            // Legacy
            totalIncome = income,
            totalExpense = expense,
            // New
            income = incomeDouble,
            expenses = expensesDouble,
            categoryBreakdown = newBreakdown,
            dailyTotals = dailyTotals,
            cumulativeTotals = cumulativeTotals,
            todayIndex = todayIndex,
            categoryDailyTrend = categoryDailyTrend,
            monthlyTotals = monthlyTotals,
            categoryMonthlyTrend = categoryMonthlyTrend,
            currentMonthIndex = currentMonthIndex,
            // Legacy chart bars
            chartBars = buildChartBars(allTransactions, period),
            availableCategories = availableCategories,
            selectedCategoryId = selectedCatId,
        )
    }
        .combine(_selectedSliceIndex) { s, slice -> s.copy(selectedSliceIndex = slice) }
        .combine(_periodOffset) { s, offset -> s.copy(periodOffset = offset) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), OverviewUiState())

    fun onIntent(intent: OverviewIntent) {
        when (intent) {
            OverviewIntent.PreviousPeriod -> {
                _periodOffset.value = -1
                _period.update { it.previous() }
            }
            OverviewIntent.NextPeriod -> {
                _periodOffset.value = 1
                _period.update { it.next() }
            }
            OverviewIntent.TogglePeriod -> {
                _periodOffset.value = 0
                _period.update { p ->
                    when (p) {
                        is OverviewPeriod.Month -> OverviewPeriod.Year(p.yearMonth.year)
                        is OverviewPeriod.Year -> OverviewPeriod.Month(YearMonth(p.year, today.monthNumber))
                    }
                }
            }
            is OverviewIntent.CategoryFilterSelected -> {
                _selectedCategoryId.update { id -> if (id == intent.id) null else intent.id }
                _selectedSliceIndex.value = null
            }
            is OverviewIntent.SliceTapped -> {
                _selectedSliceIndex.update { if (it == intent.index) null else intent.index }
            }
        }
    }

    // ── Period helpers ────────────────────────────────────────────

    private fun OverviewPeriod.previous(): OverviewPeriod = when (this) {
        is OverviewPeriod.Month -> OverviewPeriod.Month(yearMonth.previous())
        is OverviewPeriod.Year -> OverviewPeriod.Year(year - 1)
    }

    private fun OverviewPeriod.next(): OverviewPeriod = when (this) {
        is OverviewPeriod.Month -> OverviewPeriod.Month(yearMonth.next())
        is OverviewPeriod.Year -> OverviewPeriod.Year(year + 1)
    }

    private fun Transaction.matchesPeriod(period: OverviewPeriod): Boolean = when (period) {
        is OverviewPeriod.Month ->
            occurredOn.year == period.yearMonth.year &&
                    occurredOn.monthNumber == period.yearMonth.monthNumber
        is OverviewPeriod.Year -> occurredOn.year == period.year
    }

    // ── Legacy chart bars (kept for existing tests) ───────────────

    private fun buildChartBars(allTransactions: List<Transaction>, period: OverviewPeriod): List<BarEntry> =
        when (period) {
            is OverviewPeriod.Month -> {
                val month = period.yearMonth
                val days = daysInMonth(month.year, month.monthNumber)
                (1..days).map { day ->
                    val dayTxns = allTransactions.filter {
                        it.occurredOn.year == month.year &&
                                it.occurredOn.monthNumber == month.monthNumber &&
                                it.occurredOn.dayOfMonth == day
                    }
                    BarEntry(
                        label = day.toString(),
                        expenseMinorUnits = dayTxns.filter { it.type == TransactionType.EXPENSE }
                            .sumOf { it.amount.minorUnits },
                        incomeMinorUnits = dayTxns.filter { it.type == TransactionType.INCOME }
                            .sumOf { it.amount.minorUnits },
                        isHighlighted = day == today.dayOfMonth &&
                                month.year == today.year &&
                                month.monthNumber == today.monthNumber,
                    )
                }
            }
            is OverviewPeriod.Year -> {
                val monthNames = listOf(
                    "Jan", "Feb", "Mar", "Apr", "May", "Jun",
                    "Jul", "Aug", "Sep", "Oct", "Nov", "Dec",
                )
                (1..12).map { month ->
                    val monthTxns = allTransactions.filter {
                        it.occurredOn.year == period.year && it.occurredOn.monthNumber == month
                    }
                    BarEntry(
                        label = monthNames[month - 1],
                        expenseMinorUnits = monthTxns.filter { it.type == TransactionType.EXPENSE }
                            .sumOf { it.amount.minorUnits },
                        incomeMinorUnits = monthTxns.filter { it.type == TransactionType.INCOME }
                            .sumOf { it.amount.minorUnits },
                        isHighlighted = month == today.monthNumber && period.year == today.year,
                    )
                }
            }
        }

    // ── Category trend builders ───────────────────────────────────

    private fun buildCategoryDailyTrend(
        periodTxns: List<Transaction>,
        catMap: Map<CategoryId, com.dv.moneym.core.model.Category>,
        days: Int,
        month: YearMonth,
    ): List<CategoryTrend> {
        val expenseTxns = periodTxns.filter { it.type == TransactionType.EXPENSE }
        return expenseTxns
            .groupBy { it.categoryId }
            .map { (catId, txns) ->
                val cat = catMap[catId]
                val series = (1..days).map { day ->
                    txns.filter { it.occurredOn.dayOfMonth == day }
                        .sumOf { it.amount.minorUnits }
                        .toDouble() / 100.0
                }
                CategoryTrend(
                    categoryName = cat?.name ?: "—",
                    categoryColor = colorHexToLong(cat?.colorHex ?: "#8A8A8A"),
                    categoryIcon = cat?.iconKey ?: "dots",
                    totalAmount = txns.sumOf { it.amount.minorUnits }.toDouble() / 100.0,
                    txCount = txns.size,
                    series = series,
                )
            }
            .sortedByDescending { it.totalAmount }
    }

    private fun buildCategoryMonthlyTrend(
        allTransactions: List<Transaction>,
        catMap: Map<CategoryId, com.dv.moneym.core.model.Category>,
        year: Int,
    ): List<CategoryTrend> {
        val yearExpenses = allTransactions.filter {
            it.type == TransactionType.EXPENSE && it.occurredOn.year == year
        }
        return yearExpenses
            .groupBy { it.categoryId }
            .map { (catId, txns) ->
                val cat = catMap[catId]
                val series = (1..12).map { m ->
                    txns.filter { it.occurredOn.monthNumber == m }
                        .sumOf { it.amount.minorUnits }
                        .toDouble() / 100.0
                }
                CategoryTrend(
                    categoryName = cat?.name ?: "—",
                    categoryColor = colorHexToLong(cat?.colorHex ?: "#8A8A8A"),
                    categoryIcon = cat?.iconKey ?: "dots",
                    totalAmount = txns.sumOf { it.amount.minorUnits }.toDouble() / 100.0,
                    txCount = txns.size,
                    series = series,
                )
            }
            .sortedByDescending { it.totalAmount }
    }
}

// ── Utilities ─────────────────────────────────────────────────────

private fun daysInMonth(year: Int, month: Int): Int {
    val first = LocalDate(year, month, 1)
    val next = if (month == 12) LocalDate(year + 1, 1, 1) else LocalDate(year, month + 1, 1)
    return (next.toEpochDays() - first.toEpochDays()).toInt()
}

/**
 * Converts a CSS hex colour string ("#RRGGBB" or "#AARRGGBB") to an ARGB Long
 * suitable for `androidx.compose.ui.graphics.Color(long)`.
 */
internal fun colorHexToLong(hex: String): Long {
    val stripped = hex.trimStart('#')
    return try {
        when (stripped.length) {
            6 -> ("FF$stripped").toLong(16)
            8 -> stripped.toLong(16)
            else -> 0xFF8A8A8AL
        }
    } catch (_: NumberFormatException) {
        0xFF8A8A8AL
    }
}
