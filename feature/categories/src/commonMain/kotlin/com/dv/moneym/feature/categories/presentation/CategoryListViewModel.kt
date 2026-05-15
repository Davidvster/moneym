package com.dv.moneym.feature.categories.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dv.moneym.core.common.DispatcherProvider
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

class CategoryListViewModel(
    private val categoryRepository: CategoryRepository,
    private val archiveCategory: ArchiveCategoryUseCase,
    private val dispatchers: DispatcherProvider,
) : ViewModel() {

    private val _showArchived = MutableStateFlow(false)

    val state: StateFlow<CategoryListUiState> = combine(
        categoryRepository.observeAll(),
        _showArchived,
    ) { categories, showArchived ->
        CategoryListUiState(
            isLoading = false,
            active = categories.filter { !it.archived },
            archived = categories.filter { it.archived },
            showArchived = showArchived,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CategoryListUiState())

    private val _effects = Channel<CategoryListEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    fun onIntent(intent: CategoryListIntent) {
        when (intent) {
            CategoryListIntent.ToggleShowArchived -> _showArchived.update { !it }
            is CategoryListIntent.ArchiveRequested -> {
                viewModelScope.launch {
                    withContext(dispatchers.io) { archiveCategory(intent.id) }
                }
            }
            is CategoryListIntent.UnarchiveRequested -> {
                viewModelScope.launch {
                    val cat = withContext(dispatchers.io) { categoryRepository.getById(intent.id) } ?: return@launch
                    withContext(dispatchers.io) { categoryRepository.update(cat.copy(archived = false)) }
                }
            }
        }
    }

    fun navigateToEdit(id: com.dv.moneym.core.model.CategoryId? = null) {
        viewModelScope.launch { _effects.send(CategoryListEffect.NavigateToEdit(id)) }
    }
}
