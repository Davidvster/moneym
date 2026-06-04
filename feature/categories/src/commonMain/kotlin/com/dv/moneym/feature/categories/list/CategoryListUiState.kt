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
    val showCategoryEditSheet: Boolean = false,
    val editingCategory: Category? = null,
    val editingName: String = "",
    val editingIcon: Icon = Icon.Basket,
    val editingColorHex: String = "#4A8E5C",
    val nameError: String? = null,
    val showDeleteConfirm: Boolean = false,
    val showColorPicker: Boolean = false,
    val isSaving: Boolean = false,
    val deleteOptionsFor: Category? = null,
    val deleteTxCount: Int = 0,
    val showMigratePicker: Boolean = false,
    val migrateTargets: List<Category> = emptyList(),
    val typeConfirmFor: Category? = null,
    val typeConfirmInput: String = "",
)

internal sealed interface CategoryListIntent {
    data object ToggleShowArchived : CategoryListIntent
    data class ArchiveRequested(val id: CategoryId) : CategoryListIntent
    data class UnarchiveRequested(val id: CategoryId) : CategoryListIntent
    data class SetTab(val tab: CategoryTab) : CategoryListIntent
    data class Reorder(val orderedIds: List<CategoryId>) : CategoryListIntent
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
    data class ShowCategoryEditSheet(val visible: Boolean) : CategoryListIntent
    data class StartEditCategory(val category: Category) : CategoryListIntent
    data class EditingNameChanged(val name: String) : CategoryListIntent
    data class EditingIconChanged(val icon: Icon) : CategoryListIntent
    data class EditingColorChanged(val colorHex: String) : CategoryListIntent
    data class ShowDeleteConfirm(val visible: Boolean) : CategoryListIntent
    data class ShowColorPicker(val visible: Boolean) : CategoryListIntent

    // Delete-with-options flow
    data object ConfirmSimpleDelete : CategoryListIntent
    data class OpenDeleteOptions(val category: Category) : CategoryListIntent
    data object DismissDeleteOptions : CategoryListIntent
    data object DeleteArchive : CategoryListIntent
    data object OpenMigratePicker : CategoryListIntent
    data object DismissMigratePicker : CategoryListIntent
    data class MigrateTo(val target: CategoryId) : CategoryListIntent
    data object OpenDeleteAllConfirm : CategoryListIntent
    data class TypeConfirmChanged(val text: String) : CategoryListIntent
    data object DismissDeleteAllConfirm : CategoryListIntent
    data object ConfirmDeleteAll : CategoryListIntent
}

internal sealed interface CategoryListEffect
