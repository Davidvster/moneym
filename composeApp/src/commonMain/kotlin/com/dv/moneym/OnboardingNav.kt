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
import com.dv.moneym.backup.DbBackupManager
import com.dv.moneym.core.datastore.AppSettings
import com.dv.moneym.core.datastore.PrefKeys
import com.dv.moneym.feature.onboarding.currency.OnboardingCurrencyViewModel
import com.dv.moneym.feature.onboarding.currency.OnboardingKey
import com.dv.moneym.feature.onboarding.currency.onboardingCurrencyEntry
import com.dv.moneym.feature.onboarding.pin.OnboardingPinSetupKey
import com.dv.moneym.feature.onboarding.security.OnboardingSecurityKey
import com.dv.moneym.feature.onboarding.security.OnboardingSecurityViewModel
import com.dv.moneym.feature.onboarding.security.onboardingSecurityEntry
import com.dv.moneym.feature.security.setup.PinSetupScreen
import com.dv.moneym.feature.settings.overview.importdata.CsvImportHolder
import com.dv.moneym.feature.settings.overview.importdata.ImportDataKey
import com.dv.moneym.feature.settings.overview.importdata.importDataEntry
import com.dv.moneym.platform.rememberBinaryFilePicker
import com.dv.moneym.platform.rememberFilePicker
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

@Composable
internal fun OnboardingNav() {
    val backStack = remember { mutableStateListOf<NavKey>(OnboardingKey) }
    val securityVm = koinViewModel<OnboardingSecurityViewModel>()
    val currencyVm = koinViewModel<OnboardingCurrencyViewModel>()
    val csvImportHolder = koinInject<CsvImportHolder>()
    val dbBackupManager = koinInject<DbBackupManager>()
    val appSettings = koinInject<AppSettings>()

    val restoreFilePicker = rememberBinaryFilePicker { bytes ->
        if (bytes != null) currencyVm.onRestoreFileSelected(bytes)
    }

    val csvFilePicker = rememberFilePicker { content ->
        if (content != null) {
            csvImportHolder.content = content
            backStack.add(ImportDataKey)
        }
    }

    NavDisplay(
        backStack = backStack,
        onBack = { backStack.removeLastOrNull() },
        transitionSpec = {
            when (targetState.key) {
                is OnboardingPinSetupKey ->
                    (slideInVertically(tween(300)) { it } + fadeIn(tween(300))) togetherWith fadeOut(
                        tween(150)
                    )

                else -> fadeIn(tween(220)) togetherWith fadeOut(tween(220))
            }
        },
        popTransitionSpec = {
            when (initialState.key) {
                is OnboardingPinSetupKey ->
                    fadeIn(tween(220)) togetherWith (slideOutVertically(tween(300)) { it } + fadeOut(
                        tween(200)
                    ))

                else -> fadeIn(tween(220)) togetherWith fadeOut(tween(220))
            }
        },
        entryProvider = entryProvider {
            onboardingCurrencyEntry(
                viewModel = currencyVm,
                onNavigateToSecurity = { backStack.add(OnboardingSecurityKey) },
                onOpenRestoreFilePicker = restoreFilePicker,
                onOpenCsvFilePicker = csvFilePicker,
                onRestoreReady = { bytes ->
                    // Set pref BEFORE process exit so it survives restart
                    appSettings.putBoolean(PrefKeys.ONBOARDING_COMPLETED, true)
                    dbBackupManager.restore(bytes)
                },
            )
            onboardingSecurityEntry(
                viewModel = securityVm,
                onNavigateToPinSetup = { backStack.add(OnboardingPinSetupKey) },
                onComplete = { },
            )
            entry<OnboardingPinSetupKey> {
                PinSetupScreen(
                    onDone = {
                        securityVm.onReturnFromPinSetup()
                        backStack.removeLastOrNull()
                    },
                )
            }
            importDataEntry(onBack = { backStack.removeLastOrNull() })
        },
    )
}
