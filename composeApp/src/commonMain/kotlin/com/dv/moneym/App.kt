package com.dv.moneym

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import com.dv.moneym.core.datastore.AppSettings
import com.dv.moneym.core.datastore.PrefKeys
import com.dv.moneym.core.designsystem.MoneyMIcons
import com.dv.moneym.core.designsystem.MoneyMTheme
import com.dv.moneym.di.appModules
import com.dv.moneym.feature.categories.ui.CategoriesKey
import com.dv.moneym.feature.categories.ui.CategoryEditKey
import com.dv.moneym.feature.categories.ui.categoriesEntry
import com.dv.moneym.feature.categories.ui.categoryEditEntry
import com.dv.moneym.feature.onboarding.presentation.OnboardingViewModel
import com.dv.moneym.feature.onboarding.ui.OnboardingKey
import com.dv.moneym.feature.onboarding.ui.OnboardingPinSetupKey
import com.dv.moneym.feature.onboarding.ui.onboardingEntry
import com.dv.moneym.feature.overview.ui.OverviewKey
import com.dv.moneym.feature.overview.ui.overviewEntry
import com.dv.moneym.feature.security.ui.PinSetupKey
import com.dv.moneym.feature.security.ui.PinSetupScreen
import com.dv.moneym.feature.security.ui.PinUnlockScreen
import com.dv.moneym.feature.security.ui.pinSetupEntry
import com.dv.moneym.feature.settings.ui.SettingsKey
import com.dv.moneym.feature.settings.ui.settingsEntry
import com.dv.moneym.feature.transactionedit.ui.TransactionEditKey
import com.dv.moneym.feature.transactionedit.ui.transactionEditEntry
import com.dv.moneym.feature.transactions.ui.TransactionsKey
import com.dv.moneym.feature.transactions.ui.transactionsEntry
import moneym.composeapp.generated.resources.Res
import moneym.composeapp.generated.resources.nav_tab_overview
import moneym.composeapp.generated.resources.nav_tab_settings
import moneym.composeapp.generated.resources.nav_tab_transactions
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.KoinApplication
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
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

// ── Onboarding flow ───────────────────────────────────────────────────────────

@Composable
private fun OnboardingNav() {
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

// ── Main navigation ───────────────────────────────────────────────────────────

@Composable
private fun MainNav(lockController: AppLockController) {
    val tabBackStack = remember { TabBackStack(TransactionsKey) }
    val currentTop = tabBackStack.backStack.lastOrNull()
    val isModal = currentTop is TransactionEditKey ||
        currentTop is CategoryEditKey ||
        currentTop is PinSetupKey ||
        currentTop is CategoriesKey

    Scaffold(
        bottomBar = {
            if (!isModal) {
                NavigationBar {
                    NavigationBarItem(
                        selected = tabBackStack.currentTab == TransactionsKey,
                        onClick = { tabBackStack.switchTab(TransactionsKey) },
                        icon = { Icon(MoneyMIcons.List, contentDescription = null) },
                        label = { Text(stringResource(Res.string.nav_tab_transactions)) },
                    )
                    NavigationBarItem(
                        selected = tabBackStack.currentTab == OverviewKey,
                        onClick = { tabBackStack.switchTab(OverviewKey) },
                        icon = { Icon(MoneyMIcons.Home, contentDescription = null) },
                        label = { Text(stringResource(Res.string.nav_tab_overview)) },
                    )
                    NavigationBarItem(
                        selected = tabBackStack.currentTab == SettingsKey,
                        onClick = { tabBackStack.switchTab(SettingsKey) },
                        icon = { Icon(MoneyMIcons.Settings, contentDescription = null) },
                        label = { Text(stringResource(Res.string.nav_tab_settings)) },
                    )
                }
            }
        },
    ) { padding ->
        NavDisplay(
            backStack = tabBackStack.backStack,
            onBack = { tabBackStack.removeLast() },
            modifier = Modifier.padding(padding),
            transitionSpec = {
                val key = targetState.key
                when {
                    key is TransactionEditKey || key is CategoryEditKey ||
                        key is PinSetupKey || key is CategoriesKey ->
                        (slideInVertically(tween(300)) { it } + fadeIn(tween(300))) togetherWith fadeOut(tween(150))
                    else -> fadeIn(tween(220)) togetherWith fadeOut(tween(220))
                }
            },
            popTransitionSpec = {
                val key = initialState.key
                when {
                    key is TransactionEditKey || key is CategoryEditKey ||
                        key is PinSetupKey || key is CategoriesKey ->
                        fadeIn(tween(220)) togetherWith (slideOutVertically(tween(300)) { it } + fadeOut(tween(200)))
                    else -> fadeIn(tween(220)) togetherWith fadeOut(tween(220))
                }
            },
            entryProvider = entryProvider {
                transactionsEntry(
                    onAddTransaction = { tabBackStack.push(TransactionEditKey()) },
                    onEditTransaction = { id -> tabBackStack.push(TransactionEditKey(id.value)) },
                )
                transactionEditEntry(onDismiss = { tabBackStack.removeLast() })
                overviewEntry()
                settingsEntry(
                    onNavigateToPinSetup = { tabBackStack.push(PinSetupKey) },
                    onNavigateToCategories = { tabBackStack.push(CategoriesKey) },
                )
                pinSetupEntry(onDone = {
                    lockController.init()
                    tabBackStack.removeLast()
                })
                categoriesEntry(
                    onEditCategory = { id -> tabBackStack.push(CategoryEditKey(id?.value)) },
                    onBack = { tabBackStack.removeLast() },
                )
                categoryEditEntry(onDismiss = { tabBackStack.removeLast() })
            },
        )
    }
}

// ── Tab back stack ────────────────────────────────────────────────────────────
//
// Per-tab stacks following the Nav3 TopLevelBackStack recipe. The flat
// `backStack` is observed by NavDisplay; only the last entry is rendered.

private class TabBackStack(startTab: NavKey) {

    private val stacks: LinkedHashMap<NavKey, SnapshotStateList<NavKey>> = linkedMapOf(
        startTab to mutableStateListOf(startTab)
    )

    private val _currentTab = mutableStateOf<NavKey>(startTab)
    var currentTab: NavKey
        get() = _currentTab.value
        private set(value) { _currentTab.value = value }

    val backStack: SnapshotStateList<NavKey> = mutableStateListOf(startTab)

    fun switchTab(tab: NavKey) {
        if (stacks[tab] == null) {
            stacks[tab] = mutableStateListOf(tab)
        } else {
            stacks.remove(tab)?.let { stacks[tab] = it }
        }
        currentTab = tab
        rebuildBackStack()
    }

    fun push(key: NavKey) {
        stacks[currentTab]?.add(key)
        rebuildBackStack()
    }

    fun removeLast() {
        val removed = stacks[currentTab]?.removeLastOrNull() ?: return
        if (removed == currentTab) {
            stacks.remove(currentTab)
            currentTab = stacks.keys.lastOrNull() ?: return
        }
        rebuildBackStack()
    }

    private fun rebuildBackStack() {
        backStack.clear()
        backStack.addAll(stacks.flatMap { (_, stack) -> stack })
    }
}
