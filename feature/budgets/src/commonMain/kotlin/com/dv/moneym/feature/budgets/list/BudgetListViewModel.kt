package com.dv.moneym.feature.budgets.list

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.serialization.saved
import androidx.lifecycle.viewModelScope
import com.dv.moneym.core.common.DispatcherProvider
import com.dv.moneym.core.model.AccountId
import com.dv.moneym.core.model.Budget
import com.dv.moneym.core.model.BudgetId
import com.dv.moneym.core.model.Category
import com.dv.moneym.core.model.CategoryId
import com.dv.moneym.data.accounts.AccountRepository
import com.dv.moneym.data.budgets.BudgetRepository
import com.dv.moneym.data.categories.CategoryRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class BudgetListViewModel(
    private val budgetRepository: BudgetRepository,
    private val categoryRepository: CategoryRepository,
    private val accountRepository: AccountRepository,
    private val dispatchers: DispatcherProvider,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val _deleteRequestId by savedStateHandle.saved { MutableStateFlow<BudgetId?>(null) }
    private val _selectedAccountId by savedStateHandle.saved { MutableStateFlow<AccountId?>(null) }

    @OptIn(ExperimentalCoroutinesApi::class)
    private val budgets = _selectedAccountId.flatMapLatest { id ->
        if (id != null) budgetRepository.observeByAccount(id)
        else budgetRepository.observeAll()
    }

    internal val state: StateFlow<BudgetListUiState> = combine(
        budgets,
        categoryRepository.observeAll(),
        accountRepository.observeAll(),
        _selectedAccountId,
        _deleteRequestId,
    ) { budgetList, categories, accounts, selectedId, deleteId ->
        val catMap: Map<CategoryId, Category> = categories.associateBy { it.id }
        val rows = budgetList.map { it.toRow(catMap) }
        val deleted = deleteId?.let { id -> rows.firstOrNull { it.id == id } }
        BudgetListUiState(
            isLoading = false,
            rows = rows,
            accounts = accounts,
            selectedAccountId = selectedId,
            deleteRequestId = deleteId,
            deleteRequestName = deleted?.name,
        )
    }.stateIn(viewModelScope, SharingStarted.Lazily, BudgetListUiState())

    init {
        viewModelScope.launch {
            accountRepository.observeAll().collect { accounts ->
                if (_selectedAccountId.value == null && accounts.isNotEmpty()) {
                    val default = accounts.firstOrNull { it.isDefault } ?: accounts.first()
                    _selectedAccountId.value = default.id
                }
            }
        }
    }

    internal fun onIntent(intent: BudgetListIntent) {
        when (intent) {
            is BudgetListIntent.AccountSelected -> _selectedAccountId.update { intent.id }
            is BudgetListIntent.DeleteRequested -> _deleteRequestId.update { intent.id }
            BudgetListIntent.DismissDelete -> _deleteRequestId.update { null }
            BudgetListIntent.ConfirmDelete -> {
                val id = _deleteRequestId.value ?: return
                _deleteRequestId.update { null }
                viewModelScope.launch {
                    withContext(dispatchers.io) { budgetRepository.delete(id) }
                }
            }
        }
    }

    private fun Budget.toRow(catMap: Map<CategoryId, Category>): BudgetRowVm {
        val scope = categoryId?.let { catMap[it]?.name } ?: ALL_CATEGORIES_SENTINEL
        val r = recurringMonths
        val recurringLabel = when {
            r == null -> null
            r == Budget.UNLIMITED -> RECURRING_UNLIMITED_SENTINEL
            r > 0 -> "$r"
            else -> null
        }
        return BudgetRowVm(
            id = id,
            name = name,
            amount = amount,
            scopeLabel = scope,
            recurringLabel = recurringLabel,
        )
    }

    companion object {
        internal const val ALL_CATEGORIES_SENTINEL = "__all__"
        internal const val RECURRING_UNLIMITED_SENTINEL = "__unlimited__"
    }
}
