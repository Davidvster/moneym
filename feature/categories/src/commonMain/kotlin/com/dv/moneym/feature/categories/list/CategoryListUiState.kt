package com.dv.moneym.feature.categories.list

import com.dv.moneym.core.model.Category
import com.dv.moneym.core.model.CategoryId
import com.dv.moneym.core.model.Icon
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
    data class SetTab(val tab: CategoryTab) : CategoryListIntent
    data class Reorder(val fromIndex: Int, val toIndex: Int) : CategoryListIntent
    data class CreateCategory(
        val name: String,
        val icon: Icon,
        val colorHex: String,
    ) : CategoryListIntent

    data class UpdateCategory(
        val id: CategoryId,
        val name: String,
        val icon: Icon,
        val colorHex: String,
    ) : CategoryListIntent

    data class DeleteCategory(val id: CategoryId) : CategoryListIntent
}

internal sealed interface CategoryListEffect
