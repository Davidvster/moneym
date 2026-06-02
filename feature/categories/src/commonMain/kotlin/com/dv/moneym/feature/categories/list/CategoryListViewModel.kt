package com.dv.moneym.feature.categories.list

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.serialization.saved
import androidx.lifecycle.viewModelScope
import com.dv.moneym.core.common.DispatcherProvider
import com.dv.moneym.core.model.Category
import com.dv.moneym.core.model.CategoryId
import com.dv.moneym.core.model.Icon
import com.dv.moneym.core.model.TransactionType
import com.dv.moneym.data.categories.CategoryRepository
import com.dv.moneym.feature.categories.domain.ArchiveCategoryUseCase
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import moneym.feature.categories.generated.resources.Res
import moneym.feature.categories.generated.resources.categories_name_blank
import moneym.feature.categories.generated.resources.categories_name_duplicate
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString
import kotlin.time.Clock

class CategoryListViewModel(
    private val categoryRepository: CategoryRepository,
    private val archiveCategory: ArchiveCategoryUseCase,
    private val dispatchers: DispatcherProvider,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _showArchived by savedStateHandle.saved { MutableStateFlow(false) }
    private val _activeTab by savedStateHandle.saved { MutableStateFlow(CategoryTab.Expense) }

    // local ordering: maps category id to its display index (for in-memory reorder)
    // TODO order should be persisted to disk - the category in the database should have an order index
    private val _manualOrder by savedStateHandle.saved { MutableStateFlow<List<Long>>(emptyList()) }

    private data class EditState(
        val showCategoryEditSheet: Boolean = false,
        val editingCategory: Category? = null,
        val editingName: String = "",
        val editingIcon: Icon = Icon.Basket,
        val editingColorHex: String = DEFAULT_COLOR_HEX,
        val nameError: String? = null,
        val showDeleteConfirm: Boolean = false,
        val showColorPicker: Boolean = false,
    )

    private val _editState = MutableStateFlow(EditState())

    internal val state: StateFlow<CategoryListUiState> = combine(
        categoryRepository.observeAll(),
        _showArchived,
        _activeTab,
        _manualOrder,
        _editState,
    ) { categories, showArchived, tab, manualOrder, edit ->
        val tabType =
            if (tab == CategoryTab.Expense) TransactionType.EXPENSE else TransactionType.INCOME
        val active = categories.filter { !it.archived && it.type == tabType }
        val archived = categories.filter { it.archived && it.type == tabType }

        // Apply manual ordering: categories not yet in manualOrder go to the end
        val orderedActive = if (manualOrder.isEmpty()) {
            active
        } else {
            val orderMap = manualOrder.mapIndexed { i, id -> id to i }.toMap()
            active.sortedWith(Comparator { a, b ->
                val ia = orderMap[a.id.value] ?: Int.MAX_VALUE
                val ib = orderMap[b.id.value] ?: Int.MAX_VALUE
                ia.compareTo(ib)
            })
        }

        CategoryListUiState(
            isLoading = false,
            active = orderedActive,
            archived = archived,
            showArchived = showArchived,
            activeTab = tab,
            orderedCategories = orderedActive,
            showCategoryEditSheet = edit.showCategoryEditSheet,
            editingCategory = edit.editingCategory,
            editingName = edit.editingName,
            editingIcon = edit.editingIcon,
            editingColorHex = edit.editingColorHex,
            nameError = edit.nameError,
            showDeleteConfirm = edit.showDeleteConfirm,
            showColorPicker = edit.showColorPicker,
        )
    }.stateIn(viewModelScope, SharingStarted.Lazily, CategoryListUiState())

    private val _effects = Channel<CategoryListEffect>(Channel.BUFFERED)
    internal val effects = _effects.receiveAsFlow()

    internal fun onIntent(intent: CategoryListIntent) {
        when (intent) {
            CategoryListIntent.ToggleShowArchived -> _showArchived.update { !it }
            is CategoryListIntent.ArchiveRequested -> {
                viewModelScope.launch {
                    withContext(dispatchers.io) { archiveCategory(intent.id) }
                }
            }

            is CategoryListIntent.UnarchiveRequested -> {
                viewModelScope.launch {
                    val cat = withContext(dispatchers.io) { categoryRepository.getById(intent.id) }
                        ?: return@launch
                    withContext(dispatchers.io) { categoryRepository.update(cat.copy(archived = false)) }
                }
            }

            is CategoryListIntent.SetTab -> setTab(intent.tab)
            is CategoryListIntent.Reorder -> reorder(intent.fromIndex, intent.toIndex)
            is CategoryListIntent.CreateCategory ->
                createCategory(intent.name, intent.icon, intent.colorHex)

            is CategoryListIntent.UpdateCategory ->
                updateCategory(intent.id, intent.name, intent.icon, intent.colorHex)

            is CategoryListIntent.DeleteCategory -> deleteCategory(intent.id)

            is CategoryListIntent.ShowCategoryEditSheet -> {
                if (intent.visible) {
                    _editState.update {
                        EditState(showCategoryEditSheet = true)
                    }
                } else {
                    _editState.update { EditState() }
                }
            }

            is CategoryListIntent.StartEditCategory -> _editState.update {
                EditState(
                    showCategoryEditSheet = true,
                    editingCategory = intent.category,
                    editingName = intent.category.name,
                    editingIcon = Icon.fromKey(intent.category.iconKey) ?: Icon.Basket,
                    editingColorHex = intent.category.colorHex,
                )
            }

            is CategoryListIntent.EditingNameChanged -> _editState.update {
                it.copy(editingName = intent.name, nameError = null)
            }

            is CategoryListIntent.EditingIconChanged -> _editState.update {
                it.copy(editingIcon = intent.icon)
            }

            is CategoryListIntent.EditingColorChanged -> _editState.update {
                it.copy(editingColorHex = intent.colorHex)
            }

            is CategoryListIntent.ShowDeleteConfirm ->
                _editState.update { it.copy(showDeleteConfirm = intent.visible) }

            is CategoryListIntent.ShowColorPicker ->
                _editState.update { it.copy(showColorPicker = intent.visible) }
        }
    }

    private fun setTab(tab: CategoryTab) {
        _activeTab.update { tab }
    }

    private fun reorder(fromIndex: Int, toIndex: Int) {
        val current = state.value.orderedCategories
        if (fromIndex < 0 || toIndex < 0 || fromIndex >= current.size || toIndex >= current.size) return
        val mutable = current.toMutableList()
        val moved = mutable.removeAt(fromIndex)
        mutable.add(toIndex, moved)
        _manualOrder.update { mutable.map { it.id.value } }
    }

    private fun setNameError(res: StringResource) {
        viewModelScope.launch {
            val msg = getString(res)
            _editState.update { it.copy(nameError = msg) }
        }
    }

    private fun createCategory(name: String, icon: Icon, colorHex: String) {
        val trimmed = name.trim()
        if (trimmed.isBlank()) {
            setNameError(Res.string.categories_name_blank)
            return
        }
        val tabType =
            if (_activeTab.value == CategoryTab.Expense) TransactionType.EXPENSE else TransactionType.INCOME
        val isDuplicate = state.value.active.any { it.name.equals(trimmed, ignoreCase = true) }
        if (isDuplicate) {
            setNameError(Res.string.categories_name_duplicate)
            return
        }
        closeSheet()
        viewModelScope.launch {
            withContext(dispatchers.io) {
                val now = Clock.System.now()
                val category = Category(
                    id = CategoryId(0),
                    name = trimmed,
                    iconKey = icon.key,
                    colorHex = colorHex,
                    isUserCreated = true,
                    archived = false,
                    createdAt = now,
                    updatedAt = now,
                    type = tabType,
                )
                categoryRepository.insert(category)
            }
        }
    }

    private fun updateCategory(id: CategoryId, name: String, icon: Icon, colorHex: String) {
        val trimmed = name.trim()
        if (trimmed.isBlank()) {
            setNameError(Res.string.categories_name_blank)
            return
        }
        val isDuplicate = state.value.active.any {
            it.id != id && it.name.equals(trimmed, ignoreCase = true)
        }
        if (isDuplicate) {
            setNameError(Res.string.categories_name_duplicate)
            return
        }
        closeSheet()
        viewModelScope.launch {
            val existing =
                withContext(dispatchers.io) { categoryRepository.getById(id) } ?: return@launch
            withContext(dispatchers.io) {
                val now = Clock.System.now()
                categoryRepository.update(
                    existing.copy(
                        name = trimmed,
                        iconKey = icon.key,
                        colorHex = colorHex,
                        updatedAt = now,
                    )
                )
            }
        }
    }

    private fun deleteCategory(id: CategoryId) {
        closeSheet()
        viewModelScope.launch {
            withContext(dispatchers.io) { archiveCategory(id) }
        }
    }

    private fun closeSheet() {
        _editState.update { EditState() }
    }

    private companion object {
        private const val DEFAULT_COLOR_HEX = "#4A8E5C"
    }
}
