package com.dv.moneym

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.dv.moneym.core.datastore.AppSettings
import com.dv.moneym.core.datastore.PrefKeys
import com.dv.moneym.core.designsystem.MoneyMTheme
import com.dv.moneym.di.appModules
import com.dv.moneym.feature.security.ui.PinUnlockScreen
import org.koin.compose.KoinApplication
import org.koin.compose.koinInject
import org.koin.core.module.Module

// ── Entry point ───────────────────────────────────────────────────────────────

@Composable
fun App(platformModules: List<Module> = emptyList()) {
    KoinApplication(application = { modules(appModules + platformModules) }) {
        AppContent()
    }
}

@Composable
private fun AppContent() {
    val initializer = koinInject<AppInitializer>()
    val lockController = koinInject<AppLockController>()
    val appSettings = koinInject<AppSettings>()

    LaunchedEffect(Unit) { initializer.initialize() }

    val lifecycleOwner = LocalLifecycleOwner.current
    val lifecycleObserver = remember { AppLifecycleObserver(lockController) }
    DisposableEffect(lifecycleOwner) {
        lifecycleOwner.lifecycle.addObserver(lifecycleObserver)
        onDispose { lifecycleOwner.lifecycle.removeObserver(lifecycleObserver) }
    }

    val isLocked by lockController.isLocked.collectAsState()
    val onboardingDone by appSettings.observeBoolean(PrefKeys.ONBOARDING_COMPLETED)
        .collectAsState(initial = appSettings.getBoolean(PrefKeys.ONBOARDING_COMPLETED))

    LaunchedEffect(onboardingDone) {
        if (onboardingDone) lockController.init()
    }

    MoneyMTheme {
        when {
            isLocked -> PinUnlockScreen(onUnlocked = { lockController.unlock() })
            !onboardingDone -> OnboardingNav()
            else -> MainNav(lockController = lockController)
        }
    }
}

