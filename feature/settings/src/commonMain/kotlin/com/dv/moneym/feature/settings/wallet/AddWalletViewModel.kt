package com.dv.moneym.feature.settings.wallet

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.serialization.saved
import androidx.lifecycle.viewModelScope
import com.dv.moneym.core.model.Account
import com.dv.moneym.core.model.AccountId
import com.dv.moneym.core.model.AccountType
import com.dv.moneym.core.model.CurrencyCode
import com.dv.moneym.data.accounts.AccountRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.time.Clock

class AddWalletViewModel(
    private val accountRepository: AccountRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val _name by savedStateHandle.saved { MutableStateFlow("") }
    private val _selectedCurrency by savedStateHandle.saved { MutableStateFlow("") }

    val name: StateFlow<String> = _name.asStateFlow()
    val selectedCurrency: StateFlow<String> = _selectedCurrency.asStateFlow()

    fun setName(value: String) {
        _name.value = value
    }

    fun setCurrency(code: String) {
        _selectedCurrency.value = code
    }

    fun addWallet(name: String, currency: String) {
        viewModelScope.launch {
            val accounts = accountRepository.observeAll().stateIn(viewModelScope).value
            accountRepository.insert(
                Account(
                    id = AccountId(0),
                    name = name,
                    type = AccountType.CASH,
                    currency = CurrencyCode(currency),
                    isDefault = accounts.isEmpty(),
                    archived = false,
                    createdAt = Clock.System.now(),
                    updatedAt = Clock.System.now(),
                )
            )
            _name.value = ""
            _selectedCurrency.value = ""
        }
    }
}
