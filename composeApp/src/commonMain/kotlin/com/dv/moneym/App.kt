package com.dv.moneym

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dv.moneym.core.datastore.AppSettings
import com.dv.moneym.core.datastore.AppSettingsRepository
import com.dv.moneym.core.datastore.PrefKeys
import com.dv.moneym.core.designsystem.MM
import com.dv.moneym.core.designsystem.MoneyMTheme
import com.dv.moneym.core.model.ThemeMode
import com.dv.moneym.core.ui.LocalUseCurrencySymbol
import com.dv.moneym.di.appModules
import com.dv.moneym.feature.security.unlock.PinUnlockScreen
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
    val appSettingsRepo = koinInject<AppSettingsRepository>()
    val autoBackupManager = koinInject<AutoBackupManager>()

    LaunchedEffect(Unit) { initializer.initialize() }

    val lifecycleOwner = LocalLifecycleOwner.current
    val lifecycleObserver = remember { AppLifecycleObserver(lockController) }
    DisposableEffect(lifecycleOwner) {
        lifecycleOwner.lifecycle.addObserver(lifecycleObserver)
        onDispose { lifecycleOwner.lifecycle.removeObserver(lifecycleObserver) }
    }

    val isLocked by lockController.isLocked.collectAsStateWithLifecycle()
    val onboardingDone by appSettings.observeBoolean(PrefKeys.ONBOARDING_COMPLETED)
        .collectAsStateWithLifecycle(initialValue = appSettings.getBoolean(PrefKeys.ONBOARDING_COMPLETED))

    LaunchedEffect(onboardingDone) {
        if (onboardingDone) lockController.init()
    }

    val autoBackupEnabled by appSettings.observeBoolean(PrefKeys.AUTO_BACKUP_ENABLED)
        .collectAsStateWithLifecycle(initialValue = appSettings.getBoolean(PrefKeys.AUTO_BACKUP_ENABLED))

    LaunchedEffect(autoBackupEnabled) {
        if (autoBackupEnabled) autoBackupManager.start(this)
        else autoBackupManager.stop()
    }

    val themeMode by appSettingsRepo.observeThemeMode()
        .collectAsStateWithLifecycle(initialValue = ThemeMode.Auto)
    val isDark = when (themeMode) {
        ThemeMode.Light -> false
        ThemeMode.Dark -> true
        ThemeMode.Auto -> isSystemInDarkTheme()
    }

    val useCurrencySymbol by appSettingsRepo.observeUseCurrencySymbol()
        .collectAsStateWithLifecycle(initialValue = false)

    MoneyMTheme(darkTheme = isDark) {
        CompositionLocalProvider(LocalUseCurrencySymbol provides useCurrencySymbol) {
            when {
                !onboardingDone -> OnboardingNav()
                isLocked == null -> Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MM.colors.bg)
                ) // loading
                isLocked == true -> PinUnlockScreen(onUnlocked = { lockController.unlock() })
                else -> MainNav(lockController = lockController)
            }
        }
    }
}
