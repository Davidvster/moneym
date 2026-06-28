package com.dv.moneym

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import com.dv.moneym.core.designsystem.MM
import com.dv.moneym.feature.onboarding.currency.OnboardingCurrencyViewModel
import com.dv.moneym.feature.onboarding.currency.OnboardingKey
import com.dv.moneym.feature.onboarding.currency.onboardingCurrencyEntry
import com.dv.moneym.feature.onboarding.pin.OnboardingPinSetupKey
import com.dv.moneym.feature.onboarding.restore.OnboardingRestoreKey
import com.dv.moneym.feature.onboarding.restore.onboardingRestoreEntry
import com.dv.moneym.feature.onboarding.security.OnboardingSecurityIntent
import com.dv.moneym.feature.onboarding.security.OnboardingSecurityKey
import com.dv.moneym.feature.onboarding.security.OnboardingSecurityViewModel
import com.dv.moneym.feature.onboarding.security.onboardingSecurityEntry
import com.dv.moneym.feature.onboarding.welcome.OnboardingWelcomeKey
import com.dv.moneym.feature.onboarding.welcome.onboardingWelcomeEntry
import com.dv.moneym.feature.security.setup.PinSetupScreen
import com.dv.moneym.feature.settings.overview.importdata.CsvImportHolder
import com.dv.moneym.feature.settings.overview.importdata.ImportDataKey
import com.dv.moneym.feature.settings.overview.importdata.importDataEntry
import com.dv.moneym.platform.rememberFilePicker
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

@Composable
internal fun OnboardingNav() {
    val backStack = remember { mutableStateListOf<NavKey>(OnboardingWelcomeKey) }
    val securityVm = koinViewModel<OnboardingSecurityViewModel>()
    val currencyVm = koinViewModel<OnboardingCurrencyViewModel>()
    val csvImportHolder = koinInject<CsvImportHolder>()

    val csvFilePicker = rememberFilePicker { content ->
        if (content != null) {
            csvImportHolder.content = content
            backStack.add(ImportDataKey)
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(MM.colors.bg)) {
        NavDisplay(
            backStack = backStack,
            onBack = { backStack.removeLastOrNull() },
            transitionSpec = {
                fadeIn(tween(220)) togetherWith fadeOut(tween(220))
            },
            popTransitionSpec = {
                fadeIn(tween(220)) togetherWith fadeOut(tween(220))
            },
            predictivePopTransitionSpec = { _ ->
                fadeIn(tween(220)) togetherWith fadeOut(tween(220))
            },
            entryProvider = entryProvider {
                onboardingWelcomeEntry(
                    onGetStarted = { backStack.add(OnboardingSecurityKey) },
                )
                onboardingSecurityEntry(
                    viewModel = securityVm,
                    onNavigateToPinSetup = { backStack.add(OnboardingPinSetupKey) },
                    onNavigateToCurrency = { backStack.add(OnboardingKey) },
                )
                onboardingCurrencyEntry(
                    viewModel = currencyVm,
                    onComplete = { },
                    onOpenCsvFilePicker = csvFilePicker,
                    onOpenRestore = { backStack.add(OnboardingRestoreKey) },
                )
                onboardingRestoreEntry(onBack = { backStack.removeLastOrNull() })
                entry<OnboardingPinSetupKey> {
                    PinSetupScreen(
                        onDone = {
                            securityVm.onIntent(OnboardingSecurityIntent.ReturnFromPinSetup)
                            backStack.removeLastOrNull()
                        },
                    )
                }
                importDataEntry(onBack = { backStack.removeLastOrNull() })
            },
        )
    }
}
