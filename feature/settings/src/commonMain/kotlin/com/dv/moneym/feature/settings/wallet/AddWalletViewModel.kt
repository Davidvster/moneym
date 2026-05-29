package com.dv.moneym.feature.settings.wallet

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.serialization.saved
import androidx.lifecycle.viewModelScope
import com.dv.moneym.core.model.Account
import com.dv.moneym.core.model.AccountId
import com.dv.moneym.core.model.AccountType
import com.dv.moneym.core.model.CommonCurrencies
import com.dv.moneym.core.model.CurrencyCode
import com.dv.moneym.core.model.CurrencyInfo
import com.dv.moneym.core.model.PopularCurrencyCodes
import com.dv.moneym.data.accounts.AccountRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.time.Clock

private val popularCurrencies = CommonCurrencies.filter { it.code in PopularCurrencyCodes }

class AddWalletViewModel(
    private val accountRepository: AccountRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val _name by savedStateHandle.saved { MutableStateFlow("") }
    private val _selectedCurrency by savedStateHandle.saved { MutableStateFlow("") }
    private val _colorHex by savedStateHandle.saved { MutableStateFlow<String?>(null) }
    private val _searchQuery = MutableStateFlow("")

    val name: StateFlow<String> = _name.asStateFlow()
    val selectedCurrency: StateFlow<String> = _selectedCurrency.asStateFlow()
    val colorHex: StateFlow<String?> = _colorHex.asStateFlow()
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val filteredCurrencies: StateFlow<List<CurrencyInfo>> = _searchQuery
        .map { query ->
            if (query.isBlank()) {
                CommonCurrencies
            } else {
                val q = query.trim().lowercase()
                CommonCurrencies.filter { c ->
                    c.code.lowercase().contains(q) || c.name.lowercase().contains(q)
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.Lazily, CommonCurrencies)

    val popularFilteredCurrencies: StateFlow<List<CurrencyInfo>> = filteredCurrencies
        .map { filtered -> popularCurrencies.filter { p -> filtered.any { it.code == p.code } } }
        .stateIn(viewModelScope, SharingStarted.Lazily, popularCurrencies)

    fun setName(value: String) {
        _name.value = value
    }

    fun setCurrency(code: String) {
        _selectedCurrency.value = code
    }

    fun setColor(hex: String?) {
        _colorHex.value = hex
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
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
                    colorHex = _colorHex.value,
                )
            )
            _name.value = ""
            _selectedCurrency.value = ""
            _colorHex.value = null
            _searchQuery.value = ""
        }
    }
}
