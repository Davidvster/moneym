package com.dv.moneym.feature.categories.edit

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.serialization.saved
import androidx.lifecycle.viewModelScope
import com.dv.moneym.core.common.AppClock
import com.dv.moneym.core.common.DispatcherProvider
import com.dv.moneym.core.model.Category
import com.dv.moneym.core.model.Icon
import com.dv.moneym.core.model.CategoryId
import com.dv.moneym.data.categories.CategoryRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.time.Instant

class CategoryEditViewModel(
    private val editingId: CategoryId?,
    private val repository: CategoryRepository,
    private val dispatchers: DispatcherProvider,
    private val clock: AppClock,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _state by savedStateHandle.saved {
        MutableStateFlow(
            CategoryEditUiState(
                isLoading = editingId != null,
                isEditMode = editingId != null,
            )
        )
    }

    internal val state: StateFlow<CategoryEditUiState> = _state
        .onStart { init() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, _state.value)

    private val _effects = Channel<CategoryEditEffect>(Channel.BUFFERED)
    internal val effects = _effects.receiveAsFlow()

    private suspend fun init() {
        if (editingId != null) {
            val cat = withContext(dispatchers.io) { repository.getById(editingId) }
            if (cat != null) {
                _state.update {
                    it.copy(
                        isLoading = false,
                        name = cat.name,
                        selectedIcon = Icon.fromKeyOrDefault(cat.iconKey),
                        selectedColorHex = cat.colorHex,
                    )
                }
            }
        }
    }

    internal fun onIntent(intent: CategoryEditIntent) {
        when (intent) {
            is CategoryEditIntent.NameChanged -> _state.update {
                it.copy(
                    name = intent.name,
                    nameError = false
                )
            }

            is CategoryEditIntent.IconSelected -> _state.update { it.copy(selectedIcon = intent.icon) }
            is CategoryEditIntent.ColorSelected -> _state.update { it.copy(selectedColorHex = intent.hex) }
            CategoryEditIntent.SaveRequested -> save()
        }
    }

    private fun save() {
        val s = _state.value
        if (s.name.isBlank()) {
            _state.update { it.copy(nameError = true) }; return
        }
        _state.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            val now = clock.now()
            val epoch = Instant.fromEpochMilliseconds(0)
            val category = Category(
                id = editingId ?: CategoryId(0),
                name = s.name.trim(),
                iconKey = s.selectedIcon.key,
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
