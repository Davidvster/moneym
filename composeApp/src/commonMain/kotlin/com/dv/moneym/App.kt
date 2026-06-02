package com.dv.moneym

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import com.dv.moneym.core.datastore.AppSettings
import com.dv.moneym.core.datastore.AppSettingsRepository
import com.dv.moneym.core.datastore.PrefKeys
import com.dv.moneym.core.designsystem.MM
import com.dv.moneym.core.designsystem.MoneyMTheme
import com.dv.moneym.core.model.ThemeMode
import com.dv.moneym.core.ui.LocalUseCurrencySymbol
import com.dv.moneym.data.sync.SyncEngine
import com.dv.moneym.di.appModules
import com.dv.moneym.feature.security.unlock.PinUnlockScreen
import com.dv.moneym.platform.AppRestartCoordinator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.compose.koinInject
import org.koin.core.context.loadKoinModules
import org.koin.core.context.startKoin
import org.koin.core.context.unloadKoinModules
import org.koin.core.module.Module
import org.koin.mp.KoinPlatformTools

// ── Entry point ───────────────────────────────────────────────────────────────

/**
 * Starts the global Koin graph once. Called by each platform entry point before [App]. Also wires
 * [AppRestartCoordinator] so backup restore can restart Koin in-process. Idempotent.
 */
fun initKoin(platformModules: List<Module>) {
    if (KoinPlatformTools.defaultContext().getOrNull() != null) return
    AppRestartCoordinator.scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    // Reload (not stopKoin) so the '_root_' scope is never closed — closing it crashes any
    // in-flight composition with ClosedScopeException. unload+load swaps every singleton
    // (fresh DB + repos) while keeping the scope alive.
    val allModules = appModules + platformModules
    AppRestartCoordinator.restartKoin = {
        unloadKoinModules(allModules)
        loadKoinModules(allModules)
    }
    startKoin { modules(allModules) }
}

@Composable
fun App() {
    val phase by AppRestartCoordinator.phase.collectAsStateWithLifecycle()
    val epoch by AppRestartCoordinator.epoch.collectAsStateWithLifecycle()
    when (phase) {
        AppRestartCoordinator.Phase.Restarting -> RestartingScreen()
        // key(epoch) remounts after an in-process restore. ViewModels live in the host's
        // ViewModelStore (not tied to composition), so clear it on each new epoch — otherwise
        // koinViewModel() would hand back ViewModels built against the old (closed-DB) Koin graph.
        // Keep the host's own LocalViewModelStoreOwner so SavedStateHandle injection still works.
        // No-op on first startup (store empty).
        AppRestartCoordinator.Phase.Idle -> key(epoch) {
            val storeOwner = LocalViewModelStoreOwner.current
            remember(epoch) { storeOwner?.viewModelStore?.clear() }
            AppContent()
        }
    }
}

// Koin-free placeholder shown while the graph is torn down and rebuilt during restore.
@Composable
private fun RestartingScreen() {
    MoneyMTheme(darkTheme = isSystemInDarkTheme()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MM.colors.bg),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator(color = MM.colors.accent)
        }
    }
}

@Composable
private fun AppContent() {
    val initializer = koinInject<AppInitializer>()
    val lockController = koinInject<AppLockController>()
    val appSettings = koinInject<AppSettings>()
    val appSettingsRepo = koinInject<AppSettingsRepository>()
    val autoBackupManager = koinInject<AutoBackupManager>()
    val syncEngine = koinInject<SyncEngine>()

    LaunchedEffect(Unit) { initializer.initialize() }

    val lifecycleOwner = LocalLifecycleOwner.current
    val lifecycleObserver = remember { AppLifecycleObserver(lockController, syncEngine = syncEngine) }
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

    LaunchedEffect(themeMode) { applyAppNightMode(themeMode) }

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
