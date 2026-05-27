package com.dv.moneym.feature.settings.wallet

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.serialization.saved
import androidx.lifecycle.viewModelScope
import com.dv.moneym.core.common.DispatcherProvider
import com.dv.moneym.core.model.AccountId
import com.dv.moneym.core.model.CurrencyCode
import com.dv.moneym.data.accounts.AccountRepository
import com.dv.moneym.data.transactions.TransactionRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class EditWalletCurrencyViewModel(
    private val accountId: Long,
    private val currentCurrency: String,
    private val accountRepository: AccountRepository,
    private val transactionRepository: TransactionRepository,
    private val dispatchers: DispatcherProvider,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val _state by savedStateHandle.saved { MutableStateFlow(EditWalletCurrencyUiState(currentCurrency = currentCurrency)) }
    internal val state: StateFlow<EditWalletCurrencyUiState> = _state.asStateFlow()

    private val _effects = Channel<EditWalletCurrencyEffect>(Channel.BUFFERED)
    internal val effects = _effects.receiveAsFlow()

    internal fun onIntent(intent: EditWalletCurrencyIntent) {
        when (intent) {
            is EditWalletCurrencyIntent.CurrencySelected -> {
                if (intent.code == currentCurrency) return
                _state.update { it.copy(selectedCurrency = intent.code, showConfirmSheet = true) }
            }

            is EditWalletCurrencyIntent.RateChanged -> {
                val filtered = intent.text.filter { it.isDigit() || it == '.' }
                _state.update { it.copy(conversionRate = filtered) }
            }

            is EditWalletCurrencyIntent.SearchQueryChanged ->
                _state.update { it.copy(searchQuery = intent.query) }

            EditWalletCurrencyIntent.ConfirmRequested ->
                _state.update { it.copy(showConfirmSheet = true) }

            EditWalletCurrencyIntent.ConfirmConversion -> convert()

            EditWalletCurrencyIntent.CancelConversion ->
                _state.update { it.copy(showConfirmSheet = false, selectedCurrency = null) }
        }
    }

    private fun convert() {
        val s = _state.value
        val newCurrency = s.selectedCurrency ?: return
        val rate = s.conversionRate.toDoubleOrNull()?.takeIf { it > 0 } ?: return

        _state.update { it.copy(isConverting = true) }
        viewModelScope.launch {
            withContext(dispatchers.io) {
                transactionRepository.convertCurrencyForAccount(
                    accountId = AccountId(accountId),
                    newCurrency = CurrencyCode(newCurrency),
                    rate = rate,
                )
                val account = accountRepository.observeAll().first()
                    .firstOrNull { it.id.value == accountId } ?: return@withContext
                accountRepository.update(account.copy(currency = CurrencyCode(newCurrency)))
            }
            _effects.send(EditWalletCurrencyEffect.Done)
        }
    }
}
