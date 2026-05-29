package com.dv.moneym.feature.settings.wallet

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.serialization.saved
import androidx.lifecycle.viewModelScope
import com.dv.moneym.core.model.AccountId
import com.dv.moneym.data.accounts.AccountRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class EditWalletViewModel(
    private val accountId: Long,
    private val accountRepository: AccountRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val _state by savedStateHandle.saved { MutableStateFlow(EditWalletUiState()) }
    internal val state: StateFlow<EditWalletUiState> = _state.asStateFlow()

    private val _effects = Channel<EditWalletEffect>(Channel.BUFFERED)
    internal val effects = _effects.receiveAsFlow()

    init {
        if (!_state.value.loaded) {
            viewModelScope.launch {
                val account = accountRepository.getById(AccountId(accountId)) ?: return@launch
                _state.update {
                    it.copy(name = account.name, colorHex = account.colorHex, loaded = true)
                }
            }
        }
    }

    internal fun onIntent(intent: EditWalletIntent) {
        when (intent) {
            is EditWalletIntent.NameChanged ->
                _state.update { it.copy(name = intent.value) }

            is EditWalletIntent.ColorChanged ->
                _state.update { it.copy(colorHex = intent.hex) }

            EditWalletIntent.Save -> save()
        }
    }

    private fun save() {
        val s = _state.value
        if (s.name.isBlank()) return
        viewModelScope.launch {
            val account = accountRepository.getById(AccountId(accountId)) ?: return@launch
            accountRepository.update(account.copy(name = s.name.trim(), colorHex = s.colorHex))
            _effects.send(EditWalletEffect.Done)
        }
    }
}
