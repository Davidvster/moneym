package com.dv.moneym.feature.overview.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dv.moneym.core.common.AppClock
import com.dv.moneym.core.datastore.AppSettingsRepository
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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate

class OverviewViewModel(
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
    private val appSettingsRepository: AppSettingsRepository,
    clock: AppClock,
) : ViewModel() {

    private val today = clock.today()
    private val _period = MutableStateFlow<OverviewPeriod>(
        OverviewPeriod.Month(YearMonth(today.year, today.monthNumber))
    )
    private val _periodOffset = MutableStateFlow(0)
    private val _selectedCategoryId = MutableStateFlow<CategoryId?>(null)
    private val _selectedSliceIndex = MutableStateFlow<Int?>(null)
    private val _selectedAccountId = MutableStateFlow<Long>(-1L)

    init {
        // Restore persisted overview period mode on startup
        viewModelScope.launch {
            val saved = appSettingsRepository.observeLastOverviewPeriod().first()
            if (saved == "year") {
                _period.value = OverviewPeriod.Year(today.year)
            }
            // if "month" or anything else, default is already Month (set above)
        }

        // Observe selected account (wallet filter)
        viewModelScope.launch {
            appSettingsRepository.observeSelectedAccountId().collect { id ->
                _selectedAccountId.value = id
            }
        }
    }

    val state: StateFlow<OverviewUiState> = combine(
        _period,
        _selectedCategoryId,
        transactionRepository.observeAll(),
        categoryRepository.observeAll(),
        _selectedAccountId,
    ) { period, selectedCatId, allTransactions, categories, selectedAccId ->
        val catMap = categories.associateBy { it.id }

        // Filter by selected account
        val accountFilteredTransactions = if (selectedAccId > 0L) {
            allTransactions.filter { it.accountId.value == selectedAccId }
        } else {
            allTransactions
        }

        val periodTxns = accountFilteredTransactions.filter { it.matchesPeriod(period) }

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

        // Preliminary CategorySpend list (without averages — added after period block)
        val prelimBreakdown = periodTxns
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
        val dailyTotals: List<Double>
        val cumulativeTotals: List<Double>
        val todayIndex: Int
        val categoryDailyTrend: List<CategoryTrend>
        val monthlyTotals: List<Double>
        val categoryMonthlyTrend: List<CategoryTrend>
        val currentMonthIndex: Int
        val avgDailyExpense: Double
        val avgMonthlyExpense: Double
        val avgDailyExpenseYear: Double
        // For per-category averages
        var elapsedDaysForCat = 1
        var elapsedMonthsForCat = 1

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

                // Average stats for month mode
                val elapsed = if (isCurrentMonth) today.dayOfMonth else days
                elapsedDaysForCat = elapsed.coerceAtLeast(1)
                elapsedMonthsForCat = 1
                avgDailyExpense = if (elapsed > 0) expensesDouble / elapsed else 0.0
                avgMonthlyExpense = 0.0
                avgDailyExpenseYear = 0.0
            }

            is OverviewPeriod.Year -> {
                // Month totals for the selected year
                monthlyTotals = (1..12).map { m ->
                    accountFilteredTransactions
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
                    allTransactions = accountFilteredTransactions,
                    catMap = catMap,
                    year = period.year,
                )

                // Month-mode fields — empty for year mode
                dailyTotals = emptyList()
                cumulativeTotals = emptyList()
                todayIndex = 0
                categoryDailyTrend = emptyList()

                // Average stats for year mode
                val isCurrentYear = period.year == today.year
                val elapsedMonths = if (isCurrentYear) today.monthNumber else 12
                val elapsedDaysInYear = if (isCurrentYear) {
                    val jan1 = LocalDate(period.year, 1, 1)
                    (today.toEpochDays() - jan1.toEpochDays()).toInt() + 1
                } else {
                    val jan1 = LocalDate(period.year, 1, 1)
                    val jan1Next = LocalDate(period.year + 1, 1, 1)
                    (jan1Next.toEpochDays() - jan1.toEpochDays()).toInt()
                }
                elapsedMonthsForCat = elapsedMonths.coerceAtLeast(1)
                elapsedDaysForCat = elapsedDaysInYear.coerceAtLeast(1)
                avgDailyExpense = 0.0
                avgMonthlyExpense = if (elapsedMonths > 0) expensesDouble / elapsedMonths else 0.0
                avgDailyExpenseYear = if (elapsedDaysInYear > 0) expensesDouble / elapsedDaysInYear else 0.0
            }

            is OverviewPeriod.DateRange -> {
                val startDate = LocalDate(period.startYear, period.startMonth, period.startDay)
                val endDate = LocalDate(period.endYear, period.endMonth, period.endDay)
                val rangeDays = ((endDate.toEpochDays() - startDate.toEpochDays()).toInt() + 1).coerceAtLeast(1)

                // Build daily series for the range
                categoryDailyTrend = buildCategoryRangeTrend(
                    periodTxns = periodTxns,
                    catMap = catMap,
                    startDate = startDate,
                    endDate = endDate,
                )

                // Not applicable fields for range mode
                dailyTotals = emptyList()
                cumulativeTotals = emptyList()
                todayIndex = 0
                monthlyTotals = List(12) { 0.0 }
                categoryMonthlyTrend = emptyList()
                currentMonthIndex = -1

                elapsedDaysForCat = rangeDays
                elapsedMonthsForCat = ((rangeDays + 15) / 30).coerceAtLeast(1)

                avgDailyExpense = if (rangeDays > 0) expensesDouble / rangeDays else 0.0
                avgMonthlyExpense = 0.0
                avgDailyExpenseYear = 0.0
            }
        }

        // Enrich CategorySpend with per-category averages
        val isMonthMode = period is OverviewPeriod.Month
        val newBreakdown = prelimBreakdown.map { cs ->
            cs.copy(
                avgPerDay = cs.amount / elapsedDaysForCat,
                avgPerMonth = if (isMonthMode) 0.0 else cs.amount / elapsedMonthsForCat,
            )
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
            // Average stats
            avgDailyExpense = avgDailyExpense,
            avgMonthlyExpense = avgMonthlyExpense,
            avgDailyExpenseYear = avgDailyExpenseYear,
            // Legacy chart bars
            chartBars = buildChartBars(accountFilteredTransactions, period),
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
                    val newPeriod = when (p) {
                        is OverviewPeriod.Month -> OverviewPeriod.Year(p.yearMonth.year)
                        is OverviewPeriod.Year -> OverviewPeriod.Month(YearMonth(p.year, today.monthNumber))
                        is OverviewPeriod.DateRange -> OverviewPeriod.Month(YearMonth(today.year, today.monthNumber))
                    }
                    persistPeriod(newPeriod)
                    newPeriod
                }
            }
            is OverviewIntent.CategoryFilterSelected -> {
                _selectedCategoryId.update { id -> if (id == intent.id) null else intent.id }
                _selectedSliceIndex.value = null
            }
            is OverviewIntent.SliceTapped -> {
                _selectedSliceIndex.update { if (it == intent.index) null else intent.index }
            }
            is OverviewIntent.PeriodSelected -> {
                _periodOffset.value = 0
                _period.value = intent.period
                persistPeriod(intent.period)
            }
            is OverviewIntent.DateRangeSelected -> {
                _periodOffset.value = 0
                val newPeriod = OverviewPeriod.DateRange(
                    startYear = intent.startYear,
                    startMonth = intent.startMonth,
                    startDay = intent.startDay,
                    endYear = intent.endYear,
                    endMonth = intent.endMonth,
                    endDay = intent.endDay,
                )
                _period.value = newPeriod
                persistPeriod(newPeriod)
            }
        }
    }

    private fun persistPeriod(period: OverviewPeriod) {
        val encoded = when (period) {
            is OverviewPeriod.Month -> "month"
            is OverviewPeriod.Year -> "year"
            is OverviewPeriod.DateRange -> "range"
        }
        viewModelScope.launch {
            appSettingsRepository.setLastOverviewPeriod(encoded)
        }
    }

    // ── Period helpers ────────────────────────────────────────────

    private fun OverviewPeriod.previous(): OverviewPeriod = when (this) {
        is OverviewPeriod.Month -> OverviewPeriod.Month(yearMonth.previous())
        is OverviewPeriod.Year -> OverviewPeriod.Year(year - 1)
        is OverviewPeriod.DateRange -> this  // no previous/next for date range
    }

    private fun OverviewPeriod.next(): OverviewPeriod = when (this) {
        is OverviewPeriod.Month -> OverviewPeriod.Month(yearMonth.next())
        is OverviewPeriod.Year -> OverviewPeriod.Year(year + 1)
        is OverviewPeriod.DateRange -> this  // no previous/next for date range
    }

    private fun Transaction.matchesPeriod(period: OverviewPeriod): Boolean = when (period) {
        is OverviewPeriod.Month ->
            occurredOn.year == period.yearMonth.year &&
                    occurredOn.monthNumber == period.yearMonth.monthNumber
        is OverviewPeriod.Year -> occurredOn.year == period.year
        is OverviewPeriod.DateRange -> {
            val start = LocalDate(period.startYear, period.startMonth, period.startDay)
            val end = LocalDate(period.endYear, period.endMonth, period.endDay)
            occurredOn >= start && occurredOn <= end
        }
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
            is OverviewPeriod.DateRange -> emptyList()
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

    private fun buildCategoryRangeTrend(
        periodTxns: List<Transaction>,
        catMap: Map<CategoryId, com.dv.moneym.core.model.Category>,
        startDate: LocalDate,
        endDate: LocalDate,
    ): List<CategoryTrend> {
        val expenseTxns = periodTxns.filter { it.type == TransactionType.EXPENSE }
        val totalDays = ((endDate.toEpochDays() - startDate.toEpochDays()).toInt() + 1).coerceAtLeast(1)
        val useDayBuckets = totalDays <= 31

        return expenseTxns
            .groupBy { it.categoryId }
            .map { (catId, txns) ->
                val cat = catMap[catId]
                val series = if (useDayBuckets) {
                    (0 until totalDays).map { offset ->
                        val date = LocalDate.fromEpochDays(startDate.toEpochDays() + offset)
                        txns.filter { it.occurredOn == date }
                            .sumOf { it.amount.minorUnits }
                            .toDouble() / 100.0
                    }
                } else {
                    // Group into months
                    val startEpochMonth = startDate.year * 12 + (startDate.monthNumber - 1)
                    val endEpochMonth = endDate.year * 12 + (endDate.monthNumber - 1)
                    (startEpochMonth..endEpochMonth).map { epochMonth ->
                        val y = epochMonth / 12
                        val m = epochMonth % 12 + 1
                        txns.filter { it.occurredOn.year == y && it.occurredOn.monthNumber == m }
                            .sumOf { it.amount.minorUnits }
                            .toDouble() / 100.0
                    }
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
