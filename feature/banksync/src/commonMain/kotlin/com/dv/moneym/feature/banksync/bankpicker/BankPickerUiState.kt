package com.dv.moneym.feature.banksync.bankpicker

import com.dv.moneym.core.common.countryDisplayName
import com.dv.moneym.feature.banksync.countries.EnableBankingCountries
import kotlinx.serialization.Serializable

@Serializable
data class BankRow(
    val name: String,
    val country: String,
)

@Serializable
data class CountryRow(
    val code: String,
    val displayName: String,
)

@Serializable
data class BankPickerUiState(
    val selectedCountry: String? = null,
    val countrySearch: String = "",
    val isLoadingBanks: Boolean = false,
    val banks: List<BankRow> = emptyList(),
    val bankSearch: String = "",
    val authUrlToOpen: String? = null,
    val awaitingAuth: Boolean = false,
    val connectingBankName: String? = null,
    val redirectDraft: String = "",
    val redirectInvalid: Boolean = false,
    val isCompletingConnection: Boolean = false,
    val connectError: String? = null,
) {
    val countries: List<CountryRow>
        get() = EnableBankingCountries.codes
            .map { CountryRow(code = it, displayName = countryDisplayName(it)) }
            .sortedBy { it.displayName }

    val filteredCountries: List<CountryRow>
        get() {
            val query = countrySearch.trim()
            if (query.isEmpty()) return countries
            return countries.filter {
                it.code.contains(query, ignoreCase = true) ||
                    it.displayName.contains(query, ignoreCase = true)
            }
        }

    val filteredBanks: List<BankRow>
        get() = if (bankSearch.isBlank()) banks
        else banks.filter { it.name.contains(bankSearch, ignoreCase = true) }
}

sealed interface BankPickerIntent {
    data class CountrySearchChanged(val value: String) : BankPickerIntent
    data class CountrySelected(val code: String) : BankPickerIntent
    data object ChangeCountry : BankPickerIntent
    data class BankSearchChanged(val value: String) : BankPickerIntent
    data class ConnectBank(val name: String, val country: String) : BankPickerIntent
    data object AuthUrlOpened : BankPickerIntent
    data class RedirectChanged(val value: String) : BankPickerIntent
    data object SubmitRedirect : BankPickerIntent
    data object CancelAuth : BankPickerIntent
}
