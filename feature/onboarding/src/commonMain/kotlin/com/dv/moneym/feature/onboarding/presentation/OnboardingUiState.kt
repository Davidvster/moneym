package com.dv.moneym.feature.onboarding.presentation

data class OnboardingUiState(
    val step: OnboardingStep = OnboardingStep.CURRENCY,
    val selectedCurrency: String = "EUR",
    val pinEnabled: Boolean = false,
)

enum class OnboardingStep { CURRENCY, SECURITY }

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

sealed interface OnboardingIntent {
    data class CurrencySelected(val code: String) : OnboardingIntent
    data object ContinueToSecurity : OnboardingIntent
    data object SetupPinRequested : OnboardingIntent
    data object SkipSecurity : OnboardingIntent
    data object Finish : OnboardingIntent
}

sealed interface OnboardingEffect {
    data object NavigateToPinSetup : OnboardingEffect
    data object Complete : OnboardingEffect
}
