package com.dv.moneym.feature.overview.page

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dv.moneym.core.common.AppClock
import com.dv.moneym.core.datastore.AppSettingsRepository
import com.dv.moneym.core.model.CategoryId
import com.dv.moneym.data.accounts.AccountRepository
import com.dv.moneym.data.budgets.BudgetRepository
import com.dv.moneym.data.categories.CategoryRepository
import com.dv.moneym.data.transactions.TransactionRepository
import com.dv.moneym.feature.overview.OverviewPeriod
import com.dv.moneym.feature.overview.usecase.BuildOverviewPageStateUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

class OverviewPageViewModel(
    private val period: OverviewPeriod,
    transactionRepository: TransactionRepository,
    categoryRepository: CategoryRepository,
    accountRepository: AccountRepository,
    appSettingsRepository: AppSettingsRepository,
    budgetRepository: BudgetRepository,
    private val buildOverviewPageState: BuildOverviewPageStateUseCase,
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
    ) { allTransactions, categories, (selectedAccId, _), selectedCatId, budgets ->
        buildOverviewPageState(
            period = period,
            today = today,
            allTransactions = allTransactions,
            categories = categories,
            selectedAccountId = selectedAccId,
            selectedCategoryId = selectedCatId,
            budgets = budgets,
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
}
