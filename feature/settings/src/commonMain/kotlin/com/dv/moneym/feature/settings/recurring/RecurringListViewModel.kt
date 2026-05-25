package com.dv.moneym.feature.settings.recurring

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dv.moneym.core.model.Category
import com.dv.moneym.core.model.CategoryId
import com.dv.moneym.core.model.RecurringTransaction
import com.dv.moneym.data.categories.CategoryRepository
import com.dv.moneym.data.transactions.RecurringTransactionRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

internal data class RecurringListUiState(
    val isLoading: Boolean = true,
    val rules: List<RecurringTransaction> = emptyList(),
    val categories: Map<CategoryId, Category> = emptyMap(),
)

class RecurringListViewModel(
    recurringRepo: RecurringTransactionRepository,
    categoryRepository: CategoryRepository,
) : ViewModel() {

    internal val state: StateFlow<RecurringListUiState> = combine(
        recurringRepo.observeAll(),
        categoryRepository.observeAll(),
    ) { rules, cats ->
        RecurringListUiState(
            isLoading = false,
            rules = rules,
            categories = cats.associateBy { it.id },
        )
    }.stateIn(viewModelScope, SharingStarted.Lazily, RecurringListUiState())
}
