package com.dv.moneym.feature.overview

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dv.moneym.core.common.AppClock
import com.dv.moneym.core.datastore.AppSettingsRepository
import com.dv.moneym.core.model.Category
import com.dv.moneym.core.model.CategoryId
import com.dv.moneym.core.model.Icon
import com.dv.moneym.core.model.Transaction
import com.dv.moneym.core.model.TransactionType
import com.dv.moneym.data.accounts.AccountRepository
import com.dv.moneym.data.categories.CategoryRepository
import com.dv.moneym.data.transactions.TransactionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.datetime.LocalDate

internal class OverviewPageViewModel(
    private val period: OverviewPeriod,
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
    private val accountRepository: AccountRepository,
    private val appSettingsRepository: AppSettingsRepository,
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
    ) { allTransactions, categories, (selectedAccId, accounts), selectedCatId ->
        val catMap = categories.associateBy { it.id }

        val accountFilteredTransactions = if (selectedAccId > 0L) {
            allTransactions.filter { it.accountId.value == selectedAccId }
        } else allTransactions

        val periodTxns = accountFilteredTransactions.filter { it.matchesPeriod(period) }

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

        val totalExpenseMinor = periodTxns
            .filter { it.type == TransactionType.EXPENSE }
            .sumOf { it.amount.minorUnits }

        val prelimBreakdown = periodTxns
            .filter { it.type == TransactionType.EXPENSE }
            .groupBy { it.categoryId }
            .map { (catId, txns) ->
                val cat = catMap[catId]
                val amountMinor = txns.sumOf { it.amount.minorUnits }
                CategorySpend(
                    categoryName = cat?.name ?: "—",
                    categoryColor = colorHexToLong(cat?.colorHex ?: "#8A8A8A"),
                    categoryIcon = Icon.fromKeyOrDefault(cat?.iconKey ?: Icon.Dots.key),
                    amount = amountMinor.toDouble() / 100.0,
                    percent = if (totalExpenseMinor > 0)
                        ((amountMinor.toDouble() / totalExpenseMinor.toDouble()) * 100).toInt()
                    else 0,
                )
            }
            .sortedByDescending { it.amount }

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
        var elapsedDaysForCat = 1
        var elapsedMonthsForCat = 1

        when (period) {
            is OverviewPeriod.Month -> {
                val month = period.yearMonth
                val days = daysInMonth(month.year, month.monthNumber)
                val isCurrentMonth =
                    month.year == today.year && month.monthNumber == today.monthNumber

                val rawDaily = (1..days).map { day ->
                    periodTxns
                        .filter { it.type == TransactionType.EXPENSE && it.occurredOn.dayOfMonth == day }
                        .sumOf { it.amount.minorUnits }
                        .toDouble() / 100.0
                }
                dailyTotals = rawDaily

                var running = 0.0
                cumulativeTotals = rawDaily.map { v -> running += v; running }

                todayIndex = if (isCurrentMonth) {
                    (today.dayOfMonth - 1).coerceIn(0, days - 1)
                } else {
                    days - 1
                }

                val elapsed = if (isCurrentMonth) today.dayOfMonth else days
                elapsedDaysForCat = elapsed.coerceAtLeast(1)
                elapsedMonthsForCat = 1

                categoryDailyTrend = buildCategoryDailyTrend(
                    periodTxns = periodTxns,
                    catMap = catMap,
                    days = days,
                    elapsedDays = elapsedDaysForCat,
                )

                monthlyTotals = List(12) { 0.0 }
                categoryMonthlyTrend = emptyList()
                currentMonthIndex = -1

                avgDailyExpense = if (elapsed > 0) expensesDouble / elapsed else 0.0
                avgMonthlyExpense = 0.0
                avgDailyExpenseYear = 0.0
            }

            is OverviewPeriod.Year -> {
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

                categoryMonthlyTrend = buildCategoryMonthlyTrend(
                    allTransactions = accountFilteredTransactions,
                    catMap = catMap,
                    year = period.year,
                    elapsedMonths = elapsedMonthsForCat,
                    elapsedDays = elapsedDaysForCat,
                )

                dailyTotals = emptyList()
                cumulativeTotals = emptyList()
                todayIndex = 0
                categoryDailyTrend = emptyList()

                avgDailyExpense = 0.0
                avgMonthlyExpense = if (elapsedMonths > 0) expensesDouble / elapsedMonths else 0.0
                avgDailyExpenseYear =
                    if (elapsedDaysInYear > 0) expensesDouble / elapsedDaysInYear else 0.0
            }

            is OverviewPeriod.DateRange -> {
                val startDate = LocalDate(period.startYear, period.startMonth, period.startDay)
                val endDate = LocalDate(period.endYear, period.endMonth, period.endDay)
                val rangeDays =
                    ((endDate.toEpochDays() - startDate.toEpochDays()).toInt() + 1).coerceAtLeast(1)

                categoryDailyTrend = buildCategoryRangeTrend(
                    periodTxns = periodTxns,
                    catMap = catMap,
                    startDate = startDate,
                    endDate = endDate,
                )

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

        val isMonthMode = period is OverviewPeriod.Month
        val newBreakdown = prelimBreakdown.map { cs ->
            cs.copy(
                avgPerDay = cs.amount / elapsedDaysForCat,
                avgPerMonth = if (isMonthMode) 0.0 else cs.amount / elapsedMonthsForCat,
            )
        }

        val totalIncomeMinor = periodTxns
            .filter { it.type == TransactionType.INCOME }
            .sumOf { it.amount.minorUnits }
        val incomeBreakdown = periodTxns
            .filter { it.type == TransactionType.INCOME }
            .groupBy { it.categoryId }
            .map { (catId, txns) ->
                val cat = catMap[catId]
                val amountMinor = txns.sumOf { it.amount.minorUnits }
                CategorySpend(
                    categoryName = cat?.name ?: "—",
                    categoryColor = colorHexToLong(cat?.colorHex ?: "#4A7A56"),
                    categoryIcon = Icon.fromKeyOrDefault(cat?.iconKey ?: Icon.Dots.key),
                    amount = amountMinor.toDouble() / 100.0,
                    percent = if (totalIncomeMinor > 0)
                        ((amountMinor.toDouble() / totalIncomeMinor.toDouble()) * 100).toInt()
                    else 0,
                    avgPerDay = amountMinor.toDouble() / 100.0 / elapsedDaysForCat,
                    avgPerMonth = if (isMonthMode) 0.0
                    else amountMinor.toDouble() / 100.0 / elapsedMonthsForCat,
                )
            }
            .sortedByDescending { it.amount }

        OverviewPageUiState(
            isLoading = false,
            isEmpty = periodTxns.isEmpty(),
            period = period,
            income = incomeDouble,
            expenses = expensesDouble,
            categoryBreakdown = newBreakdown,
            categoryIncomeBreakdown = incomeBreakdown,
            dailyTotals = dailyTotals,
            cumulativeTotals = cumulativeTotals,
            todayIndex = todayIndex,
            categoryDailyTrend = categoryDailyTrend,
            monthlyTotals = monthlyTotals,
            categoryMonthlyTrend = categoryMonthlyTrend,
            currentMonthIndex = currentMonthIndex,
            avgDailyExpense = avgDailyExpense,
            avgMonthlyExpense = avgMonthlyExpense,
            avgDailyExpenseYear = avgDailyExpenseYear,
        )
    }
        .combine(_selectedSliceIndex) { s, slice -> s.copy(selectedSliceIndex = slice) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Lazily,
            initialValue = OverviewPageUiState(period = period),
        )

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

    private fun Transaction.matchesPeriod(p: OverviewPeriod): Boolean = when (p) {
        is OverviewPeriod.Month ->
            occurredOn.year == p.yearMonth.year && occurredOn.monthNumber == p.yearMonth.monthNumber

        is OverviewPeriod.Year -> occurredOn.year == p.year
        is OverviewPeriod.DateRange -> {
            val start = LocalDate(p.startYear, p.startMonth, p.startDay)
            val end = LocalDate(p.endYear, p.endMonth, p.endDay)
            occurredOn >= start && occurredOn <= end
        }
    }

    private fun buildCategoryDailyTrend(
        periodTxns: List<Transaction>,
        catMap: Map<CategoryId, Category>,
        days: Int,
        elapsedDays: Int = 1,
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
                val totalAmount = txns.sumOf { it.amount.minorUnits }.toDouble() / 100.0
                CategoryTrend(
                    categoryName = cat?.name ?: "—",
                    categoryColor = colorHexToLong(cat?.colorHex ?: "#8A8A8A"),
                    categoryIcon = Icon.fromKeyOrDefault(cat?.iconKey ?: Icon.Dots.key),
                    totalAmount = totalAmount,
                    txCount = txns.size,
                    series = series,
                    avgPerDay = totalAmount / elapsedDays,
                )
            }
            .sortedByDescending { it.totalAmount }
    }

    private fun buildCategoryMonthlyTrend(
        allTransactions: List<Transaction>,
        catMap: Map<CategoryId, Category>,
        year: Int,
        elapsedMonths: Int = 1,
        elapsedDays: Int = 1,
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
                val totalAmount = txns.sumOf { it.amount.minorUnits }.toDouble() / 100.0
                CategoryTrend(
                    categoryName = cat?.name ?: "—",
                    categoryColor = colorHexToLong(cat?.colorHex ?: "#8A8A8A"),
                    categoryIcon = Icon.fromKeyOrDefault(cat?.iconKey ?: Icon.Dots.key),
                    totalAmount = totalAmount,
                    txCount = txns.size,
                    series = series,
                    avgPerDay = totalAmount / elapsedDays,
                    avgPerMonth = totalAmount / elapsedMonths,
                )
            }
            .sortedByDescending { it.totalAmount }
    }

    private fun buildCategoryRangeTrend(
        periodTxns: List<Transaction>,
        catMap: Map<CategoryId, Category>,
        startDate: LocalDate,
        endDate: LocalDate,
    ): List<CategoryTrend> {
        val expenseTxns = periodTxns.filter { it.type == TransactionType.EXPENSE }
        val totalDays =
            ((endDate.toEpochDays() - startDate.toEpochDays()).toInt() + 1).coerceAtLeast(1)
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
                    categoryIcon = Icon.fromKeyOrDefault(cat?.iconKey ?: Icon.Dots.key),
                    totalAmount = txns.sumOf { it.amount.minorUnits }.toDouble() / 100.0,
                    txCount = txns.size,
                    series = series,
                )
            }
            .sortedByDescending { it.totalAmount }
    }
}

private fun daysInMonth(year: Int, month: Int): Int {
    val first = LocalDate(year, month, 1)
    val next = if (month == 12) LocalDate(year + 1, 1, 1) else LocalDate(year, month + 1, 1)
    return (next.toEpochDays() - first.toEpochDays()).toInt()
}

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
