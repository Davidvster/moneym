package com.dv.moneym.feature.onboarding.presentation

data class OnboardingUiState(
    val step: OnboardingStep = OnboardingStep.CURRENCY,
    val selectedCurrency: String = "EUR",
    val pinEnabled: Boolean = false,
    val biometricAvailable: Boolean = false,
    val biometricEnabled: Boolean = false,
)

enum class OnboardingStep { CURRENCY, SECURITY }

sealed interface OnboardingIntent {
    data class CurrencySelected(val code: String) : OnboardingIntent
    data object ContinueToSecurity : OnboardingIntent
    data object SetupPinRequested : OnboardingIntent
    data object SkipSecurity : OnboardingIntent
    data object Finish : OnboardingIntent
    data class BiometricToggled(val enabled: Boolean) : OnboardingIntent
}

sealed interface OnboardingEffect {
    data object NavigateToPinSetup : OnboardingEffect
    data object Complete : OnboardingEffect
}
