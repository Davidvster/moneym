package com.dv.moneym.feature.onboarding.currency

import kotlinx.serialization.Serializable

@Serializable
internal data class OnboardingCurrencyUiState(
    val selectedCurrency: String = "",
    val searchQuery: String = "",
)

internal sealed interface OnboardingCurrencyIntent {
    data class CurrencySelected(val code: String) : OnboardingCurrencyIntent
    data class SearchQueryChanged(val query: String) : OnboardingCurrencyIntent
    data object Continue : OnboardingCurrencyIntent
}

internal sealed interface OnboardingCurrencyEffect {
    data object NavigateToSecurity : OnboardingCurrencyEffect
}
