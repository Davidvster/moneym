package com.dv.moneym.feature.onboarding.security

import kotlinx.serialization.Serializable

@Serializable
internal data class OnboardingSecurityUiState(
    val pinEnabled: Boolean = false,
    val biometricAvailable: Boolean = false,
    val biometricEnabled: Boolean = false,
)

internal sealed interface OnboardingSecurityIntent {
    data object SetupPinRequested : OnboardingSecurityIntent
    data class BiometricToggled(val enabled: Boolean) : OnboardingSecurityIntent
    data object Finish : OnboardingSecurityIntent
}

internal sealed interface OnboardingSecurityEffect {
    data object NavigateToPinSetup : OnboardingSecurityEffect
    data object Complete : OnboardingSecurityEffect
}
