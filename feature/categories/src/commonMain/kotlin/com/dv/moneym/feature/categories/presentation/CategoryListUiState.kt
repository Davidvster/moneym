package com.dv.moneym.feature.categories.presentation

import com.dv.moneym.core.model.Category
import com.dv.moneym.core.model.CategoryId

enum class CategoryTab { Expense, Income }

data class CategoryListUiState(
    val isLoading: Boolean = true,
    val active: List<Category> = emptyList(),
    val archived: List<Category> = emptyList(),
    val showArchived: Boolean = false,
    val activeTab: CategoryTab = CategoryTab.Expense,
    val orderedCategories: List<Category> = emptyList(),
)

sealed interface CategoryListIntent {
    data object ToggleShowArchived : CategoryListIntent
    data class ArchiveRequested(val id: CategoryId) : CategoryListIntent
    data class UnarchiveRequested(val id: CategoryId) : CategoryListIntent
}

sealed interface CategoryListEffect {
    data class NavigateToEdit(val id: CategoryId?) : CategoryListEffect
}
