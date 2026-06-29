package com.dv.moneym.feature.categories.list

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.serialization.saved
import androidx.lifecycle.viewModelScope
import com.dv.moneym.core.common.DispatcherProvider
import com.dv.moneym.core.model.Category
import com.dv.moneym.core.model.CategoryId
import com.dv.moneym.core.model.Icon
import com.dv.moneym.core.model.TransactionFilter
import com.dv.moneym.core.model.TransactionType
import com.dv.moneym.data.categories.CategoryRepository
import com.dv.moneym.data.transactions.TransactionRepository
import com.dv.moneym.feature.categories.domain.DeleteCategoryUseCase
import com.dv.moneym.feature.categories.domain.DeleteStrategy
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
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
    private val transactionRepository: TransactionRepository,
    private val deleteCategory: DeleteCategoryUseCase,
    private val dispatchers: DispatcherProvider,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _showArchived by savedStateHandle.saved { MutableStateFlow(false) }
    private val _activeTab by savedStateHandle.saved { MutableStateFlow(CategoryTab.Expense) }

    private data class Transient(
        val showCategoryEditSheet: Boolean = false,
        val editingCategory: Category? = null,
        val editingName: String = "",
        val editingIcon: Icon = Icon.Basket,
        val editingColorHex: String = DEFAULT_COLOR_HEX,
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

    private val _transient = MutableStateFlow(Transient())

    internal val state: StateFlow<CategoryListUiState> = combine(
        categoryRepository.observeAll(),
        transactionRepository.observeAll(),
        _showArchived,
        _activeTab,
        _transient,
    ) { categories, transactions, showArchived, tab, t ->
        val tabType =
            if (tab == CategoryTab.Expense) TransactionType.EXPENSE else TransactionType.INCOME
        val active = categories.filter { !it.archived && it.type == tabType }
        val archived = categories.filter { it.archived && it.type == tabType }
        val transactionCountsByCategoryId = transactions
            .groupingBy { it.categoryId }
            .eachCount()

        CategoryListUiState(
            isLoading = false,
            active = active,
            archived = archived,
            transactionCountsByCategoryId = transactionCountsByCategoryId,
            showArchived = showArchived,
            activeTab = tab,
            orderedCategories = active,
            showCategoryEditSheet = t.showCategoryEditSheet,
            editingCategory = t.editingCategory,
            editingName = t.editingName,
            editingIcon = t.editingIcon,
            editingColorHex = t.editingColorHex,
            nameError = t.nameError,
            showDeleteConfirm = t.showDeleteConfirm,
            showColorPicker = t.showColorPicker,
            isSaving = t.isSaving,
            deleteOptionsFor = t.deleteOptionsFor,
            deleteTxCount = t.deleteTxCount,
            showMigratePicker = t.showMigratePicker,
            migrateTargets = t.migrateTargets,
            typeConfirmFor = t.typeConfirmFor,
            typeConfirmInput = t.typeConfirmInput,
        )
    }.stateIn(viewModelScope, SharingStarted.Lazily, CategoryListUiState())

    private val _effects = Channel<CategoryListEffect>(Channel.BUFFERED)
    internal val effects = _effects.receiveAsFlow()

    internal fun onIntent(intent: CategoryListIntent) {
        when (intent) {
            CategoryListIntent.ToggleShowArchived -> _showArchived.update { !it }
            is CategoryListIntent.ArchiveRequested -> archive(intent.id)
            is CategoryListIntent.UnarchiveRequested -> unarchive(intent.id)
            is CategoryListIntent.SetTab -> _activeTab.update { intent.tab }
            is CategoryListIntent.Reorder -> reorder(intent.orderedIds)
            is CategoryListIntent.CreateCategory ->
                createCategory(intent.name, intent.icon, intent.colorHex)

            is CategoryListIntent.UpdateCategory ->
                updateCategory(intent.id, intent.name, intent.icon, intent.colorHex)

            is CategoryListIntent.DeleteCategory -> requestDelete(intent.id)

            is CategoryListIntent.ShowCategoryEditSheet -> {
                if (intent.visible) {
                    _transient.update { it.copy(showCategoryEditSheet = true) }
                } else {
                    closeSheet()
                }
            }

            is CategoryListIntent.StartEditCategory -> _transient.update {
                it.copy(
                    showCategoryEditSheet = true,
                    editingCategory = intent.category,
                    editingName = intent.category.name,
                    editingIcon = Icon.fromKey(intent.category.iconKey) ?: Icon.Basket,
                    editingColorHex = intent.category.colorHex,
                    nameError = null,
                )
            }

            is CategoryListIntent.EditingNameChanged -> _transient.update {
                it.copy(editingName = intent.name, nameError = null)
            }

            is CategoryListIntent.EditingIconChanged -> _transient.update {
                it.copy(editingIcon = intent.icon)
            }

            is CategoryListIntent.EditingColorChanged -> _transient.update {
                it.copy(editingColorHex = intent.colorHex)
            }

            is CategoryListIntent.ShowDeleteConfirm ->
                _transient.update { it.copy(showDeleteConfirm = intent.visible) }

            is CategoryListIntent.ShowColorPicker ->
                _transient.update { it.copy(showColorPicker = intent.visible) }

            CategoryListIntent.ConfirmSimpleDelete -> confirmSimpleDelete()
            is CategoryListIntent.OpenDeleteOptions -> openDeleteOptions(intent.category)
            CategoryListIntent.DismissDeleteOptions ->
                _transient.update { it.copy(deleteOptionsFor = null, deleteTxCount = 0) }

            CategoryListIntent.DeleteArchive -> deleteArchive()
            CategoryListIntent.OpenMigratePicker -> openMigratePicker()
            CategoryListIntent.DismissMigratePicker ->
                _transient.update { it.copy(showMigratePicker = false) }

            is CategoryListIntent.MigrateTo -> migrateTo(intent.target)
            CategoryListIntent.OpenDeleteAllConfirm -> openDeleteAllConfirm()
            is CategoryListIntent.TypeConfirmChanged ->
                _transient.update { it.copy(typeConfirmInput = intent.text) }

            CategoryListIntent.DismissDeleteAllConfirm ->
                _transient.update { it.copy(typeConfirmFor = null, typeConfirmInput = "") }

            CategoryListIntent.ConfirmDeleteAll -> confirmDeleteAll()
        }
    }

    private fun reorder(orderedIds: List<CategoryId>) {
        if (orderedIds.isEmpty()) return
        _transient.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            withContext(dispatchers.io) { categoryRepository.reorder(orderedIds) }
            _transient.update { it.copy(isSaving = false) }
        }
    }

    private fun archive(id: CategoryId) {
        viewModelScope.launch {
            val cat = withContext(dispatchers.io) { categoryRepository.getById(id) } ?: return@launch
            withContext(dispatchers.io) { categoryRepository.update(cat.copy(archived = true)) }
        }
    }

    private fun unarchive(id: CategoryId) {
        _transient.update { it.copy(deleteOptionsFor = null) }
        viewModelScope.launch {
            val cat = withContext(dispatchers.io) { categoryRepository.getById(id) } ?: return@launch
            withContext(dispatchers.io) { categoryRepository.update(cat.copy(archived = false)) }
        }
    }

    private fun requestDelete(id: CategoryId) {
        viewModelScope.launch {
            val category = withContext(dispatchers.io) { categoryRepository.getById(id) } ?: return@launch
            val count = withContext(dispatchers.io) {
                transactionRepository.observeFiltered(TransactionFilter.ByCategory(id)).first().size
            }
            if (count == 0) {
                _transient.update { it.copy(showDeleteConfirm = true) }
            } else {
                _transient.update {
                    it.copy(
                        showCategoryEditSheet = false,
                        showDeleteConfirm = false,
                        deleteOptionsFor = category,
                        deleteTxCount = count,
                    )
                }
            }
        }
    }

    private fun confirmSimpleDelete() {
        val id = _transient.value.editingCategory?.id ?: return
        closeSheet()
        viewModelScope.launch {
            withContext(dispatchers.io) { categoryRepository.delete(id) }
        }
    }

    private fun openDeleteOptions(category: Category) {
        _transient.update { it.copy(deleteOptionsFor = category, deleteTxCount = 0) }
        viewModelScope.launch {
            val count = withContext(dispatchers.io) {
                transactionRepository.observeFiltered(TransactionFilter.ByCategory(category.id)).first().size
            }
            _transient.update { it.copy(deleteTxCount = count) }
        }
    }

    private fun deleteArchive() {
        val id = _transient.value.deleteOptionsFor?.id ?: return
        closeDeleteFlow()
        runOnIo { deleteCategory(id, DeleteStrategy.Archive) }
    }

    private fun openMigratePicker() {
        val target = _transient.value.deleteOptionsFor ?: return
        val targets = state.value.active.filter { it.id != target.id && it.type == target.type }
        _transient.update { it.copy(showMigratePicker = true, migrateTargets = targets) }
    }

    private fun migrateTo(targetId: CategoryId) {
        val id = _transient.value.deleteOptionsFor?.id ?: return
        closeDeleteFlow()
        runOnIo { deleteCategory(id, DeleteStrategy.Migrate(targetId)) }
    }

    private fun openDeleteAllConfirm() {
        val category = _transient.value.deleteOptionsFor ?: return
        _transient.update { it.copy(typeConfirmFor = category, typeConfirmInput = "") }
    }

    private fun confirmDeleteAll() {
        val category = _transient.value.typeConfirmFor ?: return
        if (!_transient.value.typeConfirmInput.trim().equals(category.name, ignoreCase = true)) return
        closeDeleteFlow()
        runOnIo { deleteCategory(category.id, DeleteStrategy.DeleteWithTransactions) }
    }

    private fun runOnIo(block: suspend () -> Unit) {
        _transient.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            withContext(dispatchers.io) { block() }
            _transient.update { it.copy(isSaving = false) }
        }
    }

    private fun closeDeleteFlow() {
        _transient.update {
            it.copy(
                deleteOptionsFor = null,
                deleteTxCount = 0,
                showMigratePicker = false,
                migrateTargets = emptyList(),
                typeConfirmFor = null,
                typeConfirmInput = "",
            )
        }
    }

    private fun setNameError(res: StringResource) {
        viewModelScope.launch {
            val msg = getString(res)
            _transient.update { it.copy(nameError = msg) }
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

    private fun closeSheet() {
        _transient.update {
            it.copy(
                showCategoryEditSheet = false,
                editingCategory = null,
                editingName = "",
                editingIcon = Icon.Basket,
                editingColorHex = DEFAULT_COLOR_HEX,
                nameError = null,
                showDeleteConfirm = false,
                showColorPicker = false,
            )
        }
    }

    private companion object {
        private const val DEFAULT_COLOR_HEX = "#4A8E5C"
    }
}
