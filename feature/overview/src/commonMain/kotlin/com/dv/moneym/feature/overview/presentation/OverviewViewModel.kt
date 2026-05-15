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

        val income = filteredTxns.filter { it.type == TransactionType.INCOME }
            .groupBy { it.amount.currency }
            .map { (cur, txns) -> MoneyTotal(txns.sumOf { it.amount.minorUnits }, cur) }

        val expense = filteredTxns.filter { it.type == TransactionType.EXPENSE }
            .groupBy { it.amount.currency }
            .map { (cur, txns) -> MoneyTotal(txns.sumOf { it.amount.minorUnits }, cur) }

        val totalExpense = periodTxns.filter { it.type == TransactionType.EXPENSE }
            .sumOf { it.amount.minorUnits }

        val breakdown = periodTxns
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
                    percentage = if (totalExpense > 0) amount.toFloat() / totalExpense else 0f,
                    formattedAmount = Money(amount, currency).format(),
                )
            }
            .sortedByDescending { it.expenseMinorUnits }

        val availableCategories = categories.filter { cat ->
            periodTxns.any { it.categoryId == cat.id }
        }

        OverviewUiState(
            isLoading = false,
            period = period,
            totalIncome = income,
            totalExpense = expense,
            categoryBreakdown = breakdown,
            chartBars = buildChartBars(allTransactions, period),
            availableCategories = availableCategories,
            selectedCategoryId = selectedCatId,
            isEmpty = periodTxns.isEmpty(),
        )
    }
        .combine(_selectedSliceIndex) { s, slice -> s.copy(selectedSliceIndex = slice) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), OverviewUiState())

    fun onIntent(intent: OverviewIntent) {
        when (intent) {
            OverviewIntent.PreviousPeriod -> _period.update { it.previous() }
            OverviewIntent.NextPeriod -> _period.update { it.next() }
            OverviewIntent.TogglePeriod -> _period.update { p ->
                when (p) {
                    is OverviewPeriod.Month -> OverviewPeriod.Year(p.yearMonth.year)
                    is OverviewPeriod.Year -> OverviewPeriod.Month(YearMonth(p.year, today.monthNumber))
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
}

private fun daysInMonth(year: Int, month: Int): Int {
    val first = LocalDate(year, month, 1)
    val next = if (month == 12) LocalDate(year + 1, 1, 1) else LocalDate(year, month + 1, 1)
    return (next.toEpochDays() - first.toEpochDays()).toInt()
}
