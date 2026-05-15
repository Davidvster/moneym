package com.dv.moneym.feature.categories.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dv.moneym.core.common.AppClock
import com.dv.moneym.core.common.DispatcherProvider
import com.dv.moneym.core.model.Category
import com.dv.moneym.core.model.CategoryId
import com.dv.moneym.data.categories.CategoryRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.time.Instant

class CategoryEditViewModel(
    private val editingId: CategoryId?,
    private val repository: CategoryRepository,
    private val dispatchers: DispatcherProvider,
    private val clock: AppClock,
) : ViewModel() {

    private val _state = MutableStateFlow(CategoryEditUiState(
        isLoading = editingId != null,
        isEditMode = editingId != null,
    ))
    val state: StateFlow<CategoryEditUiState> = _state.asStateFlow()

    private val _effects = Channel<CategoryEditEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    init {
        if (editingId != null) {
            viewModelScope.launch {
                val cat = withContext(dispatchers.io) { repository.getById(editingId) }
                if (cat != null) {
                    _state.update { it.copy(
                        isLoading = false,
                        name = cat.name,
                        selectedIconKey = cat.iconKey,
                        selectedColorHex = cat.colorHex,
                    )}
                }
            }
        }
    }

    fun onIntent(intent: CategoryEditIntent) {
        when (intent) {
            is CategoryEditIntent.NameChanged -> _state.update { it.copy(name = intent.name, nameError = false) }
            is CategoryEditIntent.IconSelected -> _state.update { it.copy(selectedIconKey = intent.key) }
            is CategoryEditIntent.ColorSelected -> _state.update { it.copy(selectedColorHex = intent.hex) }
            CategoryEditIntent.SaveRequested -> save()
        }
    }

    private fun save() {
        val s = _state.value
        if (s.name.isBlank()) { _state.update { it.copy(nameError = true) }; return }
        _state.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            val now = clock.now()
            val epoch = Instant.fromEpochMilliseconds(0)
            val category = Category(
                id = editingId ?: CategoryId(0),
                name = s.name.trim(),
                iconKey = s.selectedIconKey,
                colorHex = s.selectedColorHex,
                isUserCreated = true,
                archived = false,
                createdAt = if (editingId == null) now else epoch,
                updatedAt = now,
            )
            withContext(dispatchers.io) {
                if (editingId == null) repository.insert(category)
                else repository.update(category)
            }
            _effects.send(CategoryEditEffect.Saved)
        }
    }
}
