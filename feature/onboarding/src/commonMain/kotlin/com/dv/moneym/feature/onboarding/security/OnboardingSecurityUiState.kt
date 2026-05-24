package com.dv.moneym.feature.onboarding.security

import kotlinx.serialization.Serializable

@Serializable
internal data class OnboardingSecurityUiState(
    val pinEnabled: Boolean = false,
    val biometricAvailable: Boolean = false,
    val biometricEnabled: Boolean = false,
)

sealed interface OnboardingSecurityIntent {
    data object SetupPinRequested : OnboardingSecurityIntent
    data class BiometricToggled(val enabled: Boolean) : OnboardingSecurityIntent
    data object Finish : OnboardingSecurityIntent
    data object ReturnFromPinSetup : OnboardingSecurityIntent
}

internal sealed interface OnboardingSecurityEffect {
    data object NavigateToPinSetup : OnboardingSecurityEffect
    data object NavigateToCurrency : OnboardingSecurityEffect
}
