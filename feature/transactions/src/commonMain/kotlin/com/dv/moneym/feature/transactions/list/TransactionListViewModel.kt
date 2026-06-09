package com.dv.moneym.feature.transactions.list

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.serialization.saved
import androidx.lifecycle.viewModelScope
import com.dv.moneym.core.common.AppClock
import com.dv.moneym.core.common.TransactionSavedSignal
import com.dv.moneym.core.datastore.AppSettingsRepository
import com.dv.moneym.core.model.Category
import com.dv.moneym.core.model.TransactionFilter
import com.dv.moneym.core.model.TransactionType
import com.dv.moneym.core.model.YearMonth
import com.dv.moneym.data.accounts.AccountRepository
import com.dv.moneym.data.categories.CategoryRepository
import com.dv.moneym.data.sync.SyncPuller
import com.dv.moneym.data.sync.SyncStatusProvider
import com.dv.moneym.data.transactions.TransactionRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.number

class TransactionListViewModel(
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
    private val accountRepository: AccountRepository,
    private val appSettingsRepository: AppSettingsRepository,
    private val ephemeralState: TransactionListEphemeralState,
    private val syncStatus: SyncStatusProvider,
    private val syncPuller: SyncPuller? = null,
    private val clock: AppClock,
    private val transactionSavedSignal: TransactionSavedSignal = TransactionSavedSignal(),
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    internal val today
        get() = clock.today()

    private val _currentMonth by savedStateHandle.saved {
        MutableStateFlow(YearMonth(today.year, today.month.number))
    }
    private val _filter by savedStateHandle.saved {
        MutableStateFlow<TransactionFilter>(TransactionFilter.None)
    }
    private val _selectedAccountId by savedStateHandle.saved {
        MutableStateFlow<Long>(-1L)
    }

    private data class UiBooleans(
        val isSearchActive: Boolean = false,
        val showMonthPicker: Boolean = false,
        val showWalletSwitcher: Boolean = false,
        val showCategoryFilter: Boolean = false,
    )

    private val _uiBooleans = MutableStateFlow(UiBooleans())

    private val _showSyncSheet = MutableStateFlow(false)

    private val _earliestMonth: StateFlow<YearMonth?> = transactionRepository
        .getTransactionDates()
        .map { dates -> dates.minOrNull()?.let { YearMonth(it.year, it.month.number) } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private data class BaseState(val month: YearMonth, val categories: List<Category>)

    @OptIn(ExperimentalCoroutinesApi::class)
    internal val state = combine(
        _currentMonth.onStart { init() },
        categoryRepository.observeAll(),
    ) { month, cats -> BaseState(month, cats) }
        .flatMapLatest { base ->
            combine(
                transactionRepository.observeByMonth(base.month.year, base.month.monthNumber),
                _selectedAccountId,
                accountRepository.observeAll(),
            ) { transactions, selectedAccId, accounts ->
                val accountFilteredTxns = if (selectedAccId > 0L) {
                    transactions.filter { it.accountId.value == selectedAccId }
                } else transactions

                val incomeMinor = accountFilteredTxns
                    .filter { it.type == TransactionType.INCOME }
                    .sumOf { it.amount.minorUnits }
                val expenseMinor = accountFilteredTxns
                    .filter { it.type == TransactionType.EXPENSE }
                    .sumOf { it.amount.minorUnits }
                val netMinor = incomeMinor - expenseMinor
                val selectedAccount = if (selectedAccId > 0L) {
                    accounts.find { it.id.value == selectedAccId }
                } else {
                    accounts.firstOrNull { it.isDefault } ?: accounts.firstOrNull()
                }
                val netCurrency = selectedAccount?.currency?.value
                    ?: accountFilteredTxns.firstOrNull()?.amount?.currency?.value
                    ?: "USD"

                TransactionListUiState(
                    currentMonth = base.month,
                    availableCategories = base.categories,
                    netAmount = netMinor,
                    totalIncome = incomeMinor,
                    totalExpenses = expenseMinor,
                    netCurrency = netCurrency,
                    selectedAccount = selectedAccount,
                    availableAccounts = accounts.filter { !it.archived },
                    today = today,
                )
            }
        }
        .combine(_filter) { state, filter -> state.copy(activeFilter = filter) }
        .combine(_earliestMonth) { state, earliestMonth ->
            val anchor = earliestMonth ?: YearMonth(today.year, today.month.number)
            val currentMonth = state.currentMonth ?: YearMonth(today.year, today.month.number)
            val currentPage = yearMonthToPage(currentMonth, anchor)
            val todayPage = yearMonthToPage(YearMonth(today.year, today.month.number), anchor)
            state.copy(
                earliestMonth = earliestMonth,
                currentPage = currentPage,
                pageCount = todayPage + 1 + 120,
                canGoBack = currentPage > 0,
            )
        }
        .combine(ephemeralState.searchQuery) { state, q -> state.copy(searchQuery = q) }
        .combine(ephemeralState.selectedCategoryIds) { state, ids -> state.copy(selectedCategoryIds = ids) }
        .combine(_uiBooleans) { state, ui ->
            state.copy(
                isSearchActive = ui.isSearchActive,
                showMonthPicker = ui.showMonthPicker,
                showWalletSwitcher = ui.showWalletSwitcher,
                showCategoryFilter = ui.showCategoryFilter,
            )
        }
        .combine(syncStatus.isEnabled) { state, enabled -> state.copy(isSyncEnabled = enabled) }
        .combine(syncStatus.isSyncing) { state, syncing -> state.copy(isSyncInProgress = syncing) }
        .combine(syncStatus.pendingDeletionCount) { state, count -> state.copy(pendingDeletionCount = count) }
        .combine(syncStatus.conflict) { state, c -> state.copy(hasSyncConflict = c != null) }
        .combine(syncStatus.lastSyncedMs) { state, ms -> state.copy(lastSyncedMs = ms) }
        .combine(_showSyncSheet) { state, show -> state.copy(showSyncSheet = show) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Lazily,
            initialValue = TransactionListUiState(),
        )

    private fun init() {
        appSettingsRepository.observeLastTransactionFilter()
            .onEach { filter -> _filter.update { filter } }
            .launchIn(viewModelScope)

        appSettingsRepository.observeSelectedAccountId()
            .onEach { id -> _selectedAccountId.value = id }
            .launchIn(viewModelScope)

        transactionSavedSignal.savedDates
            .onEach { date -> _currentMonth.value = YearMonth(date.year, date.month.number) }
            .launchIn(viewModelScope)
    }

    internal fun onIntent(intent: TransactionListIntent) {
        when (intent) {
            TransactionListIntent.PreviousMonth -> {
                val min = _earliestMonth.value
                _currentMonth.update { current ->
                    val prev = current.previous()
                    if (min != null && prev < min) current else prev
                }
            }

            TransactionListIntent.NextMonth -> _currentMonth.update { it.next() }

            is TransactionListIntent.FilterChanged -> {
                _filter.update { intent.filter }
                viewModelScope.launch { appSettingsRepository.setLastTransactionFilter(intent.filter) }
            }

            is TransactionListIntent.SearchQueryChanged -> ephemeralState.searchQuery.value = intent.query

            is TransactionListIntent.MonthSelected -> {
                val min = _earliestMonth.value
                _currentMonth.update {
                    if (min != null && intent.yearMonth < min) min else intent.yearMonth
                }
            }

            is TransactionListIntent.AccountSelected -> {
                val id = intent.accountId?.value ?: -1L
                _selectedAccountId.value = id
                viewModelScope.launch { appSettingsRepository.setSelectedAccountId(id) }
            }

            is TransactionListIntent.CategoryFilterToggled -> {
                ephemeralState.selectedCategoryIds.update { ids ->
                    if (intent.categoryId in ids) ids - intent.categoryId else ids + intent.categoryId
                }
            }

            TransactionListIntent.CategoryFilterCleared -> {
                ephemeralState.selectedCategoryIds.value = emptySet()
            }

            is TransactionListIntent.ToggleSearch -> {
                _uiBooleans.update { it.copy(isSearchActive = intent.active) }
                if (!intent.active) {
                    ephemeralState.searchQuery.value = ""
                }
            }

            is TransactionListIntent.ShowMonthPicker ->
                _uiBooleans.update { it.copy(showMonthPicker = intent.visible) }

            is TransactionListIntent.ShowWalletSwitcher ->
                _uiBooleans.update { it.copy(showWalletSwitcher = intent.visible) }

            is TransactionListIntent.ShowCategoryFilter ->
                _uiBooleans.update { it.copy(showCategoryFilter = intent.visible) }

            is TransactionListIntent.ShowSyncSheet ->
                _showSyncSheet.update { intent.visible }

            TransactionListIntent.SyncNow ->
                viewModelScope.launch { syncPuller?.syncNow() }
        }
    }
}
