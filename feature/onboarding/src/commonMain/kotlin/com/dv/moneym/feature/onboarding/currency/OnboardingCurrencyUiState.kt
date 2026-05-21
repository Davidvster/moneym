package com.dv.moneym.feature.onboarding.currency

import kotlinx.serialization.Serializable

@Serializable
internal data class OnboardingCurrencyUiState(
    val selectedCurrency: String = "",
    val searchQuery: String = "",
    val showRestoreWarning: Boolean = false,
)

internal sealed interface OnboardingCurrencyIntent {
    data class CurrencySelected(val code: String) : OnboardingCurrencyIntent
    data class SearchQueryChanged(val query: String) : OnboardingCurrencyIntent
    data object Continue : OnboardingCurrencyIntent
    data class RestoreFileSelected(val content: ByteArray) : OnboardingCurrencyIntent
    data object RestoreConfirmed : OnboardingCurrencyIntent
    data object RestoreDismissed : OnboardingCurrencyIntent
    data object ImportCsvTapped : OnboardingCurrencyIntent
}

internal sealed interface OnboardingCurrencyEffect {
    data object NavigateToSecurity : OnboardingCurrencyEffect
    data object OpenCsvFilePicker : OnboardingCurrencyEffect
}
