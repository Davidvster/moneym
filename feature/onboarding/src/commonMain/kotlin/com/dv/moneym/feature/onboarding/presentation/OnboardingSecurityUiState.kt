package com.dv.moneym.feature.onboarding.presentation

data class OnboardingSecurityUiState(
    val pinEnabled: Boolean = false,
    val biometricAvailable: Boolean = false,
    val biometricEnabled: Boolean = false,
)

sealed interface OnboardingSecurityIntent {
    data object SetupPinRequested : OnboardingSecurityIntent
    data class BiometricToggled(val enabled: Boolean) : OnboardingSecurityIntent
    data object Finish : OnboardingSecurityIntent
}

sealed interface OnboardingSecurityEffect {
    data object NavigateToPinSetup : OnboardingSecurityEffect
    data object Complete : OnboardingSecurityEffect
}
