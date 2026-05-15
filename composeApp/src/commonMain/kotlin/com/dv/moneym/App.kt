package com.dv.moneym

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.dv.moneym.core.common.AppLogger
import com.dv.moneym.core.datastore.AppSettings
import com.dv.moneym.core.datastore.PrefKeys
import com.dv.moneym.core.designsystem.MoneyMIcons
import com.dv.moneym.core.designsystem.MoneyMTheme
import com.dv.moneym.core.model.CategoryId
import com.dv.moneym.core.model.TransactionId
import com.dv.moneym.data.accounts.SeedAccountsUseCase
import com.dv.moneym.data.categories.SeedCategoriesUseCase
import com.dv.moneym.di.appModules
import com.dv.moneym.feature.categories.ui.CategoryEditScreen
import com.dv.moneym.feature.categories.ui.CategoryListScreen
import com.dv.moneym.feature.onboarding.presentation.OnboardingViewModel
import com.dv.moneym.feature.onboarding.ui.OnboardingScreen
import com.dv.moneym.feature.overview.ui.OverviewScreen
import com.dv.moneym.feature.security.ui.PinSetupScreen
import com.dv.moneym.feature.security.ui.PinUnlockScreen
import com.dv.moneym.feature.settings.ui.SettingsScreen
import com.dv.moneym.feature.transactionedit.ui.TransactionEditScreen
import com.dv.moneym.feature.transactions.ui.TransactionListScreen
import org.koin.compose.KoinApplication
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.module.Module

private val logger = AppLogger.tag("App")

sealed interface AppScreen {
    data object Onboarding : AppScreen
    data object Transactions : AppScreen
    data class TransactionEdit(val id: TransactionId?, val sessionKey: String = kotlin.random.Random.nextLong().toString()) : AppScreen
    data object Overview : AppScreen
    data object Settings : AppScreen
    data class PinSetup(val returnTo: AppScreen = Settings) : AppScreen
    data object Categories : AppScreen
    data class CategoryEdit(val id: CategoryId?, val sessionKey: String = kotlin.random.Random.nextLong().toString()) : AppScreen
}

@Composable
fun App(platformModules: List<Module> = emptyList()) {
    KoinApplication(application = { modules(appModules + platformModules) }) {
        AppContent()
    }
}

@Composable
private fun AppContent() {
    val seedCategories = koinInject<SeedCategoriesUseCase>()
    val seedAccounts = koinInject<SeedAccountsUseCase>()
    val lockController = koinInject<AppLockController>()
    val appSettings = koinInject<AppSettings>()

    LaunchedEffect(Unit) {
        seedCategories()
        seedAccounts()
        lockController.init()
        logger.d { "App bootstrap complete" }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> lockController.onBackground()
                Lifecycle.Event.ON_RESUME -> lockController.onForeground()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
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
            !onboardingDone -> {
                // Keep OnboardingViewModel alive while we may briefly push to PinSetup
                val onboardingVm = koinViewModel<OnboardingViewModel>()
                var screen: AppScreen by remember { mutableStateOf(AppScreen.Onboarding) }
                when (val s = screen) {
                    AppScreen.Onboarding -> OnboardingScreen(
                        onNavigateToPinSetup = { screen = AppScreen.PinSetup(AppScreen.Onboarding) },
                        onComplete = { /* appSettings write triggers recompose */ },
                        viewModel = onboardingVm,
                    )
                    is AppScreen.PinSetup -> PinSetupScreen(
                        onDone = {
                            onboardingVm.onReturnFromPinSetup()
                            screen = s.returnTo
                        },
                    )
                    else -> {}
                }
            }
            else -> MainNav(lockController = lockController)
        }
    }
}

private fun AppScreen.isModal() = this is AppScreen.TransactionEdit ||
        this is AppScreen.CategoryEdit ||
        this is AppScreen.PinSetup ||
        this == AppScreen.Categories

@Composable
private fun MainNav(lockController: AppLockController) {
    var screen: AppScreen by remember { mutableStateOf(AppScreen.Transactions) }

    AnimatedContent(
        targetState = screen,
        transitionSpec = {
            when {
                targetState.isModal() ->
                    slideInVertically(tween(300)) { it } + fadeIn(tween(300)) togetherWith
                            fadeOut(tween(150))
                initialState.isModal() ->
                    fadeIn(tween(220)) togetherWith
                            slideOutVertically(tween(300)) { it } + fadeOut(tween(200))
                else ->
                    fadeIn(tween(220)) togetherWith fadeOut(tween(220))
            }
        },
        label = "MainNav",
    ) { s ->
        when (s) {
            is AppScreen.TransactionEdit -> TransactionEditScreen(
                transactionId = s.id,
                sessionKey = s.sessionKey,
                onDismiss = { screen = AppScreen.Transactions },
            )
            is AppScreen.PinSetup -> PinSetupScreen(
                onDone = { lockController.init(); screen = s.returnTo },
            )
            is AppScreen.CategoryEdit -> CategoryEditScreen(
                categoryId = s.id,
                sessionKey = s.sessionKey,
                onDismiss = { screen = AppScreen.Categories },
            )
            AppScreen.Categories -> CategoryListScreen(
                onEditCategory = { id -> screen = AppScreen.CategoryEdit(id) },
                onBack = { screen = AppScreen.Settings },
            )
            else -> {
                Scaffold(
                    bottomBar = {
                        NavigationBar {
                            NavigationBarItem(
                                selected = screen is AppScreen.Transactions,
                                onClick = { screen = AppScreen.Transactions },
                                icon = { Icon(MoneyMIcons.List, contentDescription = "Transactions") },
                                label = { Text("Transactions") },
                            )
                            NavigationBarItem(
                                selected = screen is AppScreen.Overview,
                                onClick = { screen = AppScreen.Overview },
                                icon = { Icon(MoneyMIcons.Home, contentDescription = "Overview") },
                                label = { Text("Overview") },
                            )
                            NavigationBarItem(
                                selected = screen is AppScreen.Settings || screen == AppScreen.Categories,
                                onClick = { screen = AppScreen.Settings },
                                icon = { Icon(MoneyMIcons.Settings, contentDescription = "Settings") },
                                label = { Text("Settings") },
                            )
                        }
                    },
                ) { padding ->
                    AnimatedContent(
                        targetState = screen,
                        transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(220)) },
                        label = "TabContent",
                    ) { tabScreen ->
                        Box(modifier = Modifier.padding(padding)) {
                            when (tabScreen) {
                                AppScreen.Transactions -> TransactionListScreen(
                                    onAddTransaction = { screen = AppScreen.TransactionEdit(null) },
                                    onEditTransaction = { id -> screen = AppScreen.TransactionEdit(id) },
                                )
                                AppScreen.Overview -> OverviewScreen()
                                AppScreen.Settings -> SettingsScreen(
                                    onNavigateToPinSetup = { screen = AppScreen.PinSetup(AppScreen.Settings) },
                                    onNavigateToCategories = { screen = AppScreen.Categories },
                                )
                                else -> Unit
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PlaceholderScreen(message: String) {
    Box(contentAlignment = Alignment.Center) {
        Text(message, style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
