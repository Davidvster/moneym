package com.dv.moneym.feature.onboarding.ui

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.dv.moneym.feature.onboarding.presentation.OnboardingSecurityViewModel
import com.dv.moneym.feature.onboarding.ui.components.OnboardingCurrencyScreen
import com.dv.moneym.feature.onboarding.ui.components.OnboardingSecurityScreen
import kotlinx.serialization.Serializable

@Serializable data object OnboardingKey : NavKey
@Serializable data object OnboardingSecurityKey : NavKey
@Serializable data object OnboardingPinSetupKey : NavKey

fun EntryProviderScope<NavKey>.onboardingCurrencyEntry(
    onNavigateToSecurity: () -> Unit,
) = entry<OnboardingKey> {
    OnboardingCurrencyScreen(onNavigateToSecurity = onNavigateToSecurity)
}

fun EntryProviderScope<NavKey>.onboardingSecurityEntry(
    onNavigateToPinSetup: () -> Unit,
    onComplete: () -> Unit,
    viewModel: OnboardingSecurityViewModel,
) = entry<OnboardingSecurityKey> {
    OnboardingSecurityScreen(
        viewModel = viewModel,
        onNavigateToPinSetup = onNavigateToPinSetup,
        onComplete = onComplete,
    )
}
