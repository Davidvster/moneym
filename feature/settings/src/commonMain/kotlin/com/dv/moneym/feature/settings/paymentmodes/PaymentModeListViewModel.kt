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

    internal fun onIntent(intent: PaymentModeListIntent) {
        when (intent) {
            PaymentModeListIntent.ShowAdd -> showAddDialog()
            is PaymentModeListIntent.ShowRename -> showRenameDialog(intent.id, intent.currentName)
            is PaymentModeListIntent.ShowDelete -> showDeleteConfirm(intent.id, intent.name)
            PaymentModeListIntent.Dismiss -> dismissDialog()
            is PaymentModeListIntent.Create -> createMode(intent.name)
            is PaymentModeListIntent.Rename -> renameMode(intent.id, intent.name)
            is PaymentModeListIntent.Delete -> deleteMode(intent.id)
        }
    }

    private fun showAddDialog() {
        _dialogState.update { PaymentModeDialogState.Add }
    }

    private fun showRenameDialog(id: PaymentModeId, currentName: String) {
        _dialogState.update { PaymentModeDialogState.Rename(id, currentName) }
    }

    private fun showDeleteConfirm(id: PaymentModeId, name: String) {
        _dialogState.update { PaymentModeDialogState.DeleteConfirm(id, name) }
    }

    private fun dismissDialog() {
        _dialogState.update { PaymentModeDialogState.None }
    }

    private fun createMode(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            paymentModeRepository.create(name.trim())
            _dialogState.update { PaymentModeDialogState.None }
        }
    }

    private fun renameMode(id: PaymentModeId, name: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            paymentModeRepository.rename(id, name.trim())
            _dialogState.update { PaymentModeDialogState.None }
        }
    }

    private fun deleteMode(id: PaymentModeId) {
        viewModelScope.launch {
            paymentModeRepository.delete(id)
            _dialogState.update { PaymentModeDialogState.None }
        }
    }
}
