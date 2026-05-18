package com.dv.moneym.feature.transactions.list

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.serialization.saved
import androidx.lifecycle.viewModelScope
import com.dv.moneym.core.common.AppClock
import com.dv.moneym.core.datastore.AppSettingsRepository
import com.dv.moneym.core.model.Category
import com.dv.moneym.core.model.Icon
import com.dv.moneym.core.model.Money
import com.dv.moneym.core.model.Transaction
import com.dv.moneym.core.model.TransactionFilter
import com.dv.moneym.core.model.TransactionType
import com.dv.moneym.core.model.TxDisplayPrefs
import com.dv.moneym.core.model.YearMonth
import com.dv.moneym.core.model.format
import com.dv.moneym.data.accounts.AccountRepository
import com.dv.moneym.data.categories.CategoryRepository
import com.dv.moneym.data.transactions.TransactionRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlinx.datetime.number
import kotlin.math.abs

class TransactionListViewModel(
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
    private val accountRepository: AccountRepository,
    private val appSettingsRepository: AppSettingsRepository,
    clock: AppClock,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val today = clock.today()
    private val _currentMonth by savedStateHandle.saved {
        MutableStateFlow(YearMonth(today.year, today.month.number))
    }
    private val _filter by savedStateHandle.saved {
        MutableStateFlow<TransactionFilter>(TransactionFilter.None)
    }
    private val _searchQuery by savedStateHandle.saved {
        MutableStateFlow("")
    }
    private val _selectedAccountId by savedStateHandle.saved {
        MutableStateFlow<Long>(-1L)
    }

    // Combine base state: month + filter + query + cats + prefs
    private data class BaseState(
        val month: YearMonth,
        val filter: TransactionFilter,
        val searchQuery: String,
        val categories: List<Category>,
        val prefs: TxDisplayPrefs,
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    internal val state = combine(
        _currentMonth.onStart { init() },
        _filter,
        _searchQuery,
        categoryRepository.observeAll(),
        appSettingsRepository.observeTxDisplayPrefs(),
    ) { month, filter, query, cats, prefs ->
        BaseState(month, filter, query, cats, prefs)
    }.flatMapLatest { base ->
        val catMap = base.categories.associateBy { it.id }
        val txnFlow = if (base.filter == TransactionFilter.None) {
            transactionRepository.observeByMonth(base.month.year, base.month.monthNumber)
        } else {
            transactionRepository.observeFiltered(base.filter).map { all ->
                all.filter {
                    it.occurredOn.year == base.month.year &&
                            it.occurredOn.monthNumber == base.month.monthNumber
                }
            }
        }
        combine(
            txnFlow,
            _selectedAccountId,
            accountRepository.observeAll(),
        ) { transactions, selectedAccId, accounts ->
            // Filter by selected account (if one is selected and non-negative)
            val accountFilteredTxns = if (selectedAccId > 0L) {
                transactions.filter { it.accountId.value == selectedAccId }
            } else {
                transactions
            }

            val dayGroups = accountFilteredTxns
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
            val filteredGroups = if (base.searchQuery.isBlank()) {
                dayGroups
            } else {
                val q = base.searchQuery.trim().lowercase()
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
            val netMinor = accountFilteredTxns.sumOf { tx ->
                if (tx.type == TransactionType.INCOME) tx.amount.minorUnits
                else -tx.amount.minorUnits
            }
            val netCurrency = accountFilteredTxns.firstOrNull()?.amount?.currency?.value ?: "EUR"

            // Resolve selected account object
            val selectedAccount = if (selectedAccId > 0L) {
                accounts.find { it.id.value == selectedAccId }
            } else {
                accounts.firstOrNull { it.isDefault } ?: accounts.firstOrNull()
            }

            TransactionListUiState(
                isLoading = false,
                currentMonth = base.month,
                dayGroups = filteredGroups,
                activeFilter = base.filter,
                availableCategories = base.categories,
                isEmpty = filteredGroups.isEmpty(),
                monthlySummary = buildSummary(accountFilteredTxns, base.filter),
                netAmount = netMinor,
                netCurrency = netCurrency,
                txDisplayPrefs = base.prefs,
                searchQuery = base.searchQuery,
                selectedAccount = selectedAccount,
                availableAccounts = accounts.filter { !it.archived },
            )
        }
    }.stateIn(
        scope = viewModelScope,
        // Use Eagerly so the flow stays active while the edit modal is shown —
        // this ensures DB deletions/additions are never missed.
        started = SharingStarted.Eagerly,
        initialValue = TransactionListUiState(),
    )

    private suspend fun init() {
        // Restore persisted filter on startup
        appSettingsRepository.observeLastTransactionFilter()
            .onEach { filter ->
                _filter.update { filter }
            }
            .launchIn(viewModelScope)

        // Restore persisted selected account
        appSettingsRepository.observeSelectedAccountId()
            .onEach { id -> _selectedAccountId.value = id }
            .launchIn(viewModelScope)
    }

    internal fun onIntent(intent: TransactionListIntent) {
        when (intent) {
            TransactionListIntent.PreviousMonth -> _currentMonth.update { it.previous() }
            TransactionListIntent.NextMonth -> _currentMonth.update { it.next() }
            is TransactionListIntent.FilterChanged -> {
                _filter.update { intent.filter }
                // Persist the selected filter
                viewModelScope.launch {
                    appSettingsRepository.setLastTransactionFilter(intent.filter)
                }
            }

            is TransactionListIntent.SearchQueryChanged -> _searchQuery.update { intent.query }
            is TransactionListIntent.MonthSelected -> _currentMonth.update { intent.yearMonth }
            is TransactionListIntent.AccountSelected -> {
                val id = intent.accountId?.value ?: -1L
                _selectedAccountId.value = id
                viewModelScope.launch {
                    appSettingsRepository.setSelectedAccountId(id)
                }
            }
        }
    }
}

private fun Transaction.toUiModel(category: Category?) = TransactionUiModel(
    id = id,
    type = type,
    amountFormatted = amount.format(),
    amountMinorUnits = amount.minorUnits,
    currency = amount.currency.value,
    isExpense = type == TransactionType.EXPENSE,
    categoryName = category?.name ?: "—",
    categoryColorHex = category?.colorHex ?: "#8A8A8A",
    categoryIcon = Icon.fromKeyOrDefault(category?.iconKey ?: Icon.Dots.key),
    note = note,
    occurredOn = occurredOn,
)

private fun buildSummary(transactions: List<Transaction>, filter: TransactionFilter): String {
    if (transactions.isEmpty()) return ""
    val currency = transactions.first().amount.currency
    return when (filter) {
        TransactionFilter.None -> {
            val income = transactions.filter { it.type == TransactionType.INCOME }
                .sumOf { it.amount.minorUnits }
            val expense = transactions.filter { it.type == TransactionType.EXPENSE }
                .sumOf { it.amount.minorUnits }
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
            val expense = transactions.filter { it.type == TransactionType.EXPENSE }
                .sumOf { it.amount.minorUnits }
            if (expense > 0) "−${Money(expense, currency).format()}" else ""
        }
    }
}

private fun LocalDate.toDisplayLabel(): String {
    val day = dayOfWeek.name.lowercase().replaceFirstChar { it.uppercase() }
    val month = month.name.lowercase().replaceFirstChar { it.uppercase() }
    return "$day, $month $dayOfMonth"
}
