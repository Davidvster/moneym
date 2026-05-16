package com.dv.moneym.feature.transactions.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dv.moneym.core.common.AppClock
import com.dv.moneym.core.datastore.AppSettingsRepository
import com.dv.moneym.core.model.Category
import com.dv.moneym.core.model.CategoryId
import com.dv.moneym.core.model.Transaction
import com.dv.moneym.core.model.Money
import com.dv.moneym.core.model.TransactionFilter
import com.dv.moneym.core.model.TxDisplayPrefs
import kotlin.math.abs
import com.dv.moneym.core.model.TransactionType
import com.dv.moneym.core.model.YearMonth
import com.dv.moneym.core.model.format
import com.dv.moneym.data.categories.CategoryRepository
import com.dv.moneym.data.transactions.TransactionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.datetime.LocalDate

class TransactionListViewModel(
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
    private val appSettingsRepository: AppSettingsRepository,
    clock: AppClock,
) : ViewModel() {

    private val today = clock.today()
    private val _currentMonth = MutableStateFlow(YearMonth(today.year, today.monthNumber))
    private val _filter = MutableStateFlow<TransactionFilter>(TransactionFilter.None)
    private val _searchQuery = MutableStateFlow("")

    val state = combine(
        _currentMonth,
        _filter,
        _searchQuery,
        categoryRepository.observeAll(),
        appSettingsRepository.observeTxDisplayPrefs(),
    ) { month, filter, query, cats, prefs ->
        Quint(month, filter, query, cats, prefs)
    }.flatMapLatest { (month, filter, searchQuery, categories, prefs) ->
        val catMap = categories.associateBy { it.id }
        val txnFlow = if (filter == TransactionFilter.None) {
            transactionRepository.observeByMonth(month.year, month.monthNumber)
        } else {
            transactionRepository.observeFiltered(filter).map { all ->
                all.filter { it.occurredOn.year == month.year && it.occurredOn.monthNumber == month.monthNumber }
            }
        }
        txnFlow.map { transactions ->
            val dayGroups = transactions
                .groupBy { it.occurredOn }
                .map { (date, txns) ->
                    DayGroup(
                        date = date,
                        label = date.toDisplayLabel(),
                        transactions = txns.map { it.toUiModel(catMap[it.categoryId]) },
                    )
                }
                .sortedByDescending { it.date }

            // Apply search filter
            val filteredGroups = if (searchQuery.isBlank()) {
                dayGroups
            } else {
                val q = searchQuery.trim().lowercase()
                dayGroups.mapNotNull { group ->
                    val matchingTxns = group.transactions.filter { tx ->
                        tx.categoryName.lowercase().contains(q) ||
                            (tx.note?.lowercase()?.contains(q) == true)
                    }
                    if (matchingTxns.isEmpty()) null
                    else group.copy(transactions = matchingTxns)
                }
            }

            // Net amount: income positive, expenses negative (in minor units)
            val netMinor = transactions.sumOf { tx ->
                if (tx.type == TransactionType.INCOME) tx.amount.minorUnits
                else -tx.amount.minorUnits
            }
            val netCurrency = transactions.firstOrNull()?.amount?.currency?.value ?: "EUR"

            TransactionListUiState(
                isLoading = false,
                currentMonth = month,
                dayGroups = filteredGroups,
                activeFilter = filter,
                availableCategories = categories,
                isEmpty = filteredGroups.isEmpty(),
                monthlySummary = buildSummary(transactions, filter),
                netAmount = netMinor,
                netCurrency = netCurrency,
                txDisplayPrefs = prefs,
                searchQuery = searchQuery,
            )
        }
    }.stateIn(
        scope = viewModelScope,
        // Use Eagerly so the flow stays active while the edit modal is shown —
        // this ensures DB deletions/additions are never missed.
        started = SharingStarted.Eagerly,
        initialValue = TransactionListUiState(),
    )

    fun onIntent(intent: TransactionListIntent) {
        when (intent) {
            TransactionListIntent.PreviousMonth -> _currentMonth.update { it.previous() }
            TransactionListIntent.NextMonth -> _currentMonth.update { it.next() }
            is TransactionListIntent.FilterChanged -> _filter.update { intent.filter }
            is TransactionListIntent.SearchQueryChanged -> _searchQuery.update { intent.query }
            is TransactionListIntent.MonthSelected -> _currentMonth.update { intent.yearMonth }
        }
    }
}

private data class Quint<A, B, C, D, E>(val a: A, val b: B, val c: C, val d: D, val e: E)

private operator fun <A, B, C, D, E> Quint<A, B, C, D, E>.component1() = a
private operator fun <A, B, C, D, E> Quint<A, B, C, D, E>.component2() = b
private operator fun <A, B, C, D, E> Quint<A, B, C, D, E>.component3() = c
private operator fun <A, B, C, D, E> Quint<A, B, C, D, E>.component4() = d
private operator fun <A, B, C, D, E> Quint<A, B, C, D, E>.component5() = e

private fun Transaction.toUiModel(category: Category?) = TransactionUiModel(
    id = id,
    type = type,
    amountFormatted = amount.format(),
    amountMinorUnits = amount.minorUnits,
    currency = amount.currency.value,
    isExpense = type == TransactionType.EXPENSE,
    categoryName = category?.name ?: "—",
    categoryColorHex = category?.colorHex ?: "#8A8A8A",
    categoryIconKey = category?.iconKey ?: "dots",
    note = note,
    occurredOn = occurredOn,
)

private fun buildSummary(transactions: List<Transaction>, filter: TransactionFilter): String {
    if (transactions.isEmpty()) return ""
    val currency = transactions.first().amount.currency
    return when (filter) {
        TransactionFilter.None -> {
            val income = transactions.filter { it.type == TransactionType.INCOME }.sumOf { it.amount.minorUnits }
            val expense = transactions.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount.minorUnits }
            val net = income - expense
            val prefix = if (net >= 0) "+" else "−"
            "$prefix${Money(abs(net), currency).format()}"
        }
        is TransactionFilter.ByType -> {
            val total = transactions.sumOf { it.amount.minorUnits }
            val prefix = if (filter.type == TransactionType.INCOME) "+" else "−"
            "$prefix${Money(total, currency).format()}"
        }
        is TransactionFilter.ByCategory, is TransactionFilter.ByCategoryAndType -> {
            val expense = transactions.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount.minorUnits }
            if (expense > 0) "−${Money(expense, currency).format()}" else ""
        }
    }
}

private fun LocalDate.toDisplayLabel(): String {
    val day = dayOfWeek.name.lowercase().replaceFirstChar { it.uppercase() }
    val month = month.name.lowercase().replaceFirstChar { it.uppercase() }
    return "$day, $month $dayOfMonth"
}
