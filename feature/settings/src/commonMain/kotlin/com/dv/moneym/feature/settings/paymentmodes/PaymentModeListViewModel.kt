package com.dv.moneym.feature.settings.paymentmodes

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.serialization.saved
import androidx.lifecycle.viewModelScope
import com.dv.moneym.core.model.PaymentModeId
import com.dv.moneym.data.transactions.PaymentModeRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class PaymentModeListViewModel(
    private val paymentModeRepository: PaymentModeRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val _dialogState by savedStateHandle.saved {
        MutableStateFlow<PaymentModeDialogState>(PaymentModeDialogState.None)
    }

    internal val state: StateFlow<PaymentModeListUiState> = combine(
        paymentModeRepository.observeAll(),
        _dialogState,
    ) { modes, dialogState ->
        PaymentModeListUiState(
            modes = modes,
            isLoading = false,
            dialogState = dialogState,
        )
    }.stateIn(viewModelScope, SharingStarted.Lazily, PaymentModeListUiState())

    internal fun showAddDialog() {
        _dialogState.update { PaymentModeDialogState.Add }
    }

    internal fun showRenameDialog(id: PaymentModeId, currentName: String) {
        _dialogState.update { PaymentModeDialogState.Rename(id, currentName) }
    }

    internal fun showDeleteConfirm(id: PaymentModeId, name: String) {
        _dialogState.update { PaymentModeDialogState.DeleteConfirm(id, name) }
    }

    internal fun dismissDialog() {
        _dialogState.update { PaymentModeDialogState.None }
    }

    internal fun createMode(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            paymentModeRepository.create(name.trim())
            _dialogState.update { PaymentModeDialogState.None }
        }
    }

    internal fun renameMode(id: PaymentModeId, name: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            paymentModeRepository.rename(id, name.trim())
            _dialogState.update { PaymentModeDialogState.None }
        }
    }

    internal fun deleteMode(id: PaymentModeId) {
        viewModelScope.launch {
            paymentModeRepository.delete(id)
            _dialogState.update { PaymentModeDialogState.None }
        }
    }
}
