package com.dv.moneym.feature.settings.wallet

internal data class EditWalletCurrencyUiState(
    val currentCurrency: String,
    val selectedCurrency: String? = null,
    val conversionRate: String = "1",
    val searchQuery: String = "",
    val showConfirmSheet: Boolean = false,
    val isConverting: Boolean = false,
)

internal sealed interface EditWalletCurrencyIntent {
    data class CurrencySelected(val code: String) : EditWalletCurrencyIntent
    data class RateChanged(val text: String) : EditWalletCurrencyIntent
    data class SearchQueryChanged(val query: String) : EditWalletCurrencyIntent
    data object ConfirmRequested : EditWalletCurrencyIntent
    data object ConfirmConversion : EditWalletCurrencyIntent
    data object CancelConversion : EditWalletCurrencyIntent
}

internal sealed interface EditWalletCurrencyEffect {
    data object Done : EditWalletCurrencyEffect
}
