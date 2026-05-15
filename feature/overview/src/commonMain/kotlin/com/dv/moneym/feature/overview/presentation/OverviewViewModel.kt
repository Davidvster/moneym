package com.dv.moneym.feature.overview.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dv.moneym.core.common.AppClock
import com.dv.moneym.core.model.CurrencyCode
import com.dv.moneym.core.model.Money
import com.dv.moneym.core.model.TransactionType
import com.dv.moneym.core.model.YearMonth
import com.dv.moneym.core.model.format
import com.dv.moneym.data.categories.CategoryRepository
import com.dv.moneym.data.transactions.TransactionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

class OverviewViewModel(
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
    clock: AppClock,
) : ViewModel() {

    private val today = clock.today()
    private val _currentMonth = MutableStateFlow(YearMonth(today.year, today.monthNumber))

    val state: StateFlow<OverviewUiState> = combine(
        _currentMonth,
        transactionRepository.observeAll(),
        categoryRepository.observeAll(),
    ) { month, allTransactions, categories ->
        val catMap = categories.associateBy { it.id }

        // Filter current month transactions
        val monthTxns = allTransactions.filter {
            it.occurredOn.year == month.year && it.occurredOn.monthNumber == month.monthNumber
        }

        // Income / expense totals grouped by currency
        val income = monthTxns.filter { it.type == TransactionType.INCOME }
            .groupBy { it.amount.currency }
            .map { (cur, txns) -> MoneyTotal(txns.sumOf { it.amount.minorUnits }, cur) }

        val expense = monthTxns.filter { it.type == TransactionType.EXPENSE }
            .groupBy { it.amount.currency }
            .map { (cur, txns) -> MoneyTotal(txns.sumOf { it.amount.minorUnits }, cur) }

        // Category breakdown (expenses only)
        val totalExpense = monthTxns.filter { it.type == TransactionType.EXPENSE }
            .sumOf { it.amount.minorUnits }

        val breakdown = monthTxns
            .filter { it.type == TransactionType.EXPENSE }
            .groupBy { it.categoryId }
            .map { (catId, txns) ->
                val cat = catMap[catId]
                val amount = txns.sumOf { it.amount.minorUnits }
                val currency = txns.firstOrNull()?.amount?.currency ?: CurrencyCode("EUR")
                CategoryBreakdownItem(
                    name = cat?.name ?: "—",
                    colorHex = cat?.colorHex ?: "#8A8A8A",
                    iconKey = cat?.iconKey ?: "dots",
                    expenseMinorUnits = amount,
                    percentage = if (totalExpense > 0) amount.toFloat() / totalExpense else 0f,
                    formattedAmount = Money(amount, currency).format(),
                )
            }
            .sortedByDescending { it.expenseMinorUnits }

        // 6-month trend
        val trend = buildTrend(allTransactions, month)

        OverviewUiState(
            isLoading = false,
            currentMonth = month,
            totalIncome = income,
            totalExpense = expense,
            categoryBreakdown = breakdown,
            trendMonths = trend,
            isEmpty = monthTxns.isEmpty(),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), OverviewUiState())

    fun onIntent(intent: OverviewIntent) {
        when (intent) {
            OverviewIntent.PreviousMonth -> _currentMonth.update { it.previous() }
            OverviewIntent.NextMonth -> _currentMonth.update { it.next() }
        }
    }

    private fun buildTrend(
        allTransactions: List<com.dv.moneym.core.model.Transaction>,
        current: YearMonth,
    ): List<MonthTrend> {
        val months = (5 downTo 0).map { offset ->
            var m = current
            repeat(offset) { m = m.previous() }
            m
        }
        val monthNames = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun",
            "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
        return months.map { month ->
            val txns = allTransactions.filter {
                it.occurredOn.year == month.year && it.occurredOn.monthNumber == month.monthNumber
            }
            MonthTrend(
                label = monthNames[month.monthNumber - 1],
                incomeMinorUnits = txns.filter { it.type == TransactionType.INCOME }
                    .sumOf { it.amount.minorUnits },
                expenseMinorUnits = txns.filter { it.type == TransactionType.EXPENSE }
                    .sumOf { it.amount.minorUnits },
                isCurrentMonth = month == current,
            )
        }
    }
}
