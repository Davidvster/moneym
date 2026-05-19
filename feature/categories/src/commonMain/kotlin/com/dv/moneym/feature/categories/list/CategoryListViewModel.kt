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

    internal val state: StateFlow<CategoryListUiState> = combine(
        categoryRepository.observeAll(),
        _showArchived,
        _activeTab,
        _manualOrder,
    ) { categories, showArchived, tab, manualOrder ->
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
        }
    }

    // TODO use onIntent to communicate with the viewmodel instead of public functions
    internal fun setTab(tab: CategoryTab) {
        _activeTab.update { tab }
    }

    fun reorder(fromIndex: Int, toIndex: Int) {
        val current = state.value.orderedCategories
        if (fromIndex < 0 || toIndex < 0 || fromIndex >= current.size || toIndex >= current.size) return
        val mutable = current.toMutableList()
        val moved = mutable.removeAt(fromIndex)
        mutable.add(toIndex, moved)
        _manualOrder.update { mutable.map { it.id.value } }
    }

    fun createCategory(name: String, icon: Icon, colorHex: String): Boolean {
        val trimmed = name.trim()
        if (trimmed.isBlank()) return false
        val tabType =
            if (_activeTab.value == CategoryTab.Expense) TransactionType.EXPENSE else TransactionType.INCOME
        val isDuplicate = state.value.active.any { it.name.equals(trimmed, ignoreCase = true) }
        if (isDuplicate) return false
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
        return true
    }

    fun updateCategory(id: CategoryId, name: String, icon: Icon, colorHex: String): Boolean {
        val trimmed = name.trim()
        if (trimmed.isBlank()) return false
        val isDuplicate = state.value.active.any {
            it.id != id && it.name.equals(trimmed, ignoreCase = true)
        }
        if (isDuplicate) return false
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
        return true
    }

    fun deleteCategory(id: CategoryId) {
        viewModelScope.launch {
            withContext(dispatchers.io) { archiveCategory(id) }
        }
    }
}
