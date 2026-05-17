package com.dv.moneym.feature.categories.list

import com.dv.moneym.core.model.Category
import com.dv.moneym.core.model.CategoryId
import kotlinx.serialization.Serializable

@Serializable
internal enum class CategoryTab { Expense, Income }

@Serializable
internal data class CategoryListUiState(
    val isLoading: Boolean = true,
    val active: List<Category> = emptyList(),
    val archived: List<Category> = emptyList(),
    val showArchived: Boolean = false,
    val activeTab: CategoryTab = CategoryTab.Expense,
    val orderedCategories: List<Category> = emptyList(),
)

internal sealed interface CategoryListIntent {
    data object ToggleShowArchived : CategoryListIntent
    data class ArchiveRequested(val id: CategoryId) : CategoryListIntent
    data class UnarchiveRequested(val id: CategoryId) : CategoryListIntent
}

internal sealed interface CategoryListEffect
