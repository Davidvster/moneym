package com.dv.moneym.feature.onboarding.presentation

data class OnboardingCurrencyUiState(
    val selectedCurrency: String = "EUR",
    val searchQuery: String = "",
)

sealed interface OnboardingCurrencyIntent {
    data class CurrencySelected(val code: String) : OnboardingCurrencyIntent
    data class SearchQueryChanged(val query: String) : OnboardingCurrencyIntent
    data object Continue : OnboardingCurrencyIntent
}

sealed interface OnboardingCurrencyEffect {
    data object NavigateToSecurity : OnboardingCurrencyEffect
}

val commonCurrencies = listOf(
    "EUR" to "Euro",
    "USD" to "US Dollar",
    "GBP" to "British Pound",
    "CHF" to "Swiss Franc",
    "JPY" to "Japanese Yen",
    "CAD" to "Canadian Dollar",
    "AUD" to "Australian Dollar",
    "SEK" to "Swedish Krona",
    "NOK" to "Norwegian Krone",
    "DKK" to "Danish Krone",
    "PLN" to "Polish Złoty",
    "CZK" to "Czech Koruna",
    "BRL" to "Brazilian Real",
    "MXN" to "Mexican Peso",
    "CNY" to "Chinese Yuan",
    "INR" to "Indian Rupee",
)
