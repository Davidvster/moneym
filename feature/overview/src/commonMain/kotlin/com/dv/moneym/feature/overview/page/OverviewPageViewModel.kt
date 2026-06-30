package com.dv.moneym.feature.overview.page

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dv.moneym.core.common.AppClock
import com.dv.moneym.core.datastore.AppSettingsRepository
import com.dv.moneym.core.model.SpendingFilter
import com.dv.moneym.core.model.TransactionFilter
import com.dv.moneym.core.model.TransactionType
import com.dv.moneym.core.model.clearCategories
import com.dv.moneym.core.model.selectedType
import com.dv.moneym.core.model.toggleCategory
import com.dv.moneym.data.accounts.AccountRepository
import com.dv.moneym.data.budgets.BudgetRepository
import com.dv.moneym.data.categories.CategoryRepository
import com.dv.moneym.data.overview.OverviewRepository
import com.dv.moneym.data.transactions.TransactionRepository
import com.dv.moneym.feature.overview.OverviewPeriod
import com.dv.moneym.feature.overview.usecase.BuildOverviewPageStateUseCase
import com.dv.moneym.feature.overview.usecase.ResolveOverviewBlocksUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class OverviewPageViewModel(
    private val period: OverviewPeriod,
    transactionRepository: TransactionRepository,
    categoryRepository: CategoryRepository,
    accountRepository: AccountRepository,
    private val appSettingsRepository: AppSettingsRepository,
    budgetRepository: BudgetRepository,
    overviewRepository: OverviewRepository,
    private val buildOverviewPageState: BuildOverviewPageStateUseCase,
    private val resolveOverviewBlocks: ResolveOverviewBlocksUseCase,
    clock: AppClock,
) : ViewModel() {

    private val today = clock.today()

    private val transactionFilter = appSettingsRepository.observeLastTransactionFilter()
        .stateIn(viewModelScope, SharingStarted.Eagerly, TransactionFilter.None)

    private val _selectedSliceIndex = MutableStateFlow<Int?>(null)

    private val pageData = combine(
        transactionRepository.observeAll(),
        categoryRepository.observeAll(),
        combine(
            appSettingsRepository.observeSelectedAccountId(),
            accountRepository.observeAll(),
        ) { id, accs -> id to accs },
        transactionFilter,
        budgetRepository.observeAll(),
    ) { allTransactions, categories, (selectedAccId, _), transactionFilter, budgets ->
        val pageState = buildOverviewPageState(
            period = period,
            today = today,
            allTransactions = allTransactions,
            categories = categories,
            selectedAccountId = selectedAccId,
            transactionFilter = transactionFilter,
            budgets = budgets,
        )
        pageState to transactionFilter.toSpendingFilter()
    }

    internal val state = combine(
        pageData,
        overviewRepository.observeLayoutPrefs(),
        overviewRepository.observeAiWidgets(),
        _selectedSliceIndex,
    ) { (pageState, spendingFilter), layoutPrefs, aiWidgets, slice ->
        val selectedPageState = pageState.copy(selectedSliceIndex = slice)
        selectedPageState.copy(
            blocks = resolveOverviewBlocks(
                period = period,
                spendingFilter = spendingFilter,
                layoutPrefs = layoutPrefs,
                pageState = selectedPageState,
                aiWidgets = aiWidgets,
            )
        )
    }
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
                persistTransactionFilter(
                    intent.id?.let { transactionFilter.value.toggleCategory(it) }
                        ?: transactionFilter.value.clearCategories()
                )
                _selectedSliceIndex.value = null
            }
        }
    }

    private fun persistTransactionFilter(filter: TransactionFilter) {
        viewModelScope.launch { appSettingsRepository.setLastTransactionFilter(filter) }
    }

    private fun TransactionFilter.toSpendingFilter(): SpendingFilter = when (selectedType()) {
        TransactionType.EXPENSE -> SpendingFilter.Expenses
        TransactionType.INCOME -> SpendingFilter.Income
        null -> SpendingFilter.All
    }
}
