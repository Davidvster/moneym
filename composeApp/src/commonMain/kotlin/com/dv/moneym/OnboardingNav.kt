package com.dv.moneym

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import com.dv.moneym.feature.onboarding.presentation.OnboardingViewModel
import com.dv.moneym.feature.onboarding.ui.OnboardingKey
import com.dv.moneym.feature.onboarding.ui.OnboardingPinSetupKey
import com.dv.moneym.feature.onboarding.ui.onboardingEntry
import com.dv.moneym.feature.security.ui.PinSetupScreen
import org.koin.compose.viewmodel.koinViewModel

@Composable
internal fun OnboardingNav() {
    val backStack = remember { mutableStateListOf<NavKey>(OnboardingKey) }
    val onboardingVm = koinViewModel<OnboardingViewModel>()

    NavDisplay(
        backStack = backStack,
        onBack = { backStack.removeLastOrNull() },
        transitionSpec = {
            when (targetState.key) {
                is OnboardingPinSetupKey ->
                    (slideInVertically(tween(300)) { it } + fadeIn(tween(300))) togetherWith fadeOut(tween(150))
                else -> fadeIn(tween(220)) togetherWith fadeOut(tween(220))
            }
        },
        popTransitionSpec = {
            when (initialState.key) {
                is OnboardingPinSetupKey ->
                    fadeIn(tween(220)) togetherWith (slideOutVertically(tween(300)) { it } + fadeOut(tween(200)))
                else -> fadeIn(tween(220)) togetherWith fadeOut(tween(220))
            }
        },
        entryProvider = entryProvider {
            onboardingEntry(
                viewModel = onboardingVm,
                onNavigateToPinSetup = { backStack.add(OnboardingPinSetupKey) },
                onComplete = { },
            )
            entry<OnboardingPinSetupKey> {
                PinSetupScreen(
                    onDone = {
                        onboardingVm.onReturnFromPinSetup()
                        backStack.removeLastOrNull()
                    },
                )
            }
        },
    )
}
