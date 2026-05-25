package com.dv.moneym.feature.budgets.list

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dv.moneym.core.common.DispatcherProvider
import com.dv.moneym.core.model.Budget
import com.dv.moneym.core.model.BudgetId
import com.dv.moneym.core.model.Category
import com.dv.moneym.core.model.CategoryId
import com.dv.moneym.data.budgets.BudgetRepository
import com.dv.moneym.data.categories.CategoryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class BudgetListViewModel(
    private val budgetRepository: BudgetRepository,
    private val categoryRepository: CategoryRepository,
    private val dispatchers: DispatcherProvider,
    @Suppress("UNUSED_PARAMETER") savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val _deleteRequestId = MutableStateFlow<BudgetId?>(null)

    internal val state: StateFlow<BudgetListUiState> = combine(
        budgetRepository.observeAll(),
        categoryRepository.observeAll(),
        _deleteRequestId,
    ) { budgets, categories, deleteId ->
        val catMap: Map<CategoryId, Category> = categories.associateBy { it.id }
        val rows = budgets.map { it.toRow(catMap) }
        val deleted = deleteId?.let { id -> rows.firstOrNull { it.id == id } }
        BudgetListUiState(
            isLoading = false,
            rows = rows,
            deleteRequestId = deleteId,
            deleteRequestName = deleted?.name,
        )
    }.stateIn(viewModelScope, SharingStarted.Lazily, BudgetListUiState())

    internal fun onIntent(intent: BudgetListIntent) {
        when (intent) {
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
