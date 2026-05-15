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
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import com.dv.moneym.core.designsystem.MoneyMIcons
import com.dv.moneym.core.navigation.ModalKey
import com.dv.moneym.feature.categories.ui.CategoriesKey
import com.dv.moneym.feature.categories.ui.CategoryEditKey
import com.dv.moneym.feature.categories.ui.categoriesEntry
import com.dv.moneym.feature.categories.ui.categoryEditEntry
import com.dv.moneym.feature.overview.ui.OverviewKey
import com.dv.moneym.feature.overview.ui.overviewEntry
import com.dv.moneym.feature.security.ui.PinSetupKey
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

@Composable
internal fun MainNav(lockController: AppLockController) {
    val tabBackStack = remember { TabBackStack(TransactionsKey) }
    val currentTop = tabBackStack.backStack.lastOrNull()
    val isModal = currentTop is ModalKey

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
                if (targetState.key is ModalKey)
                    (slideInVertically(tween(300)) { it } + fadeIn(tween(300))) togetherWith fadeOut(tween(150))
                else
                    fadeIn(tween(220)) togetherWith fadeOut(tween(220))
            },
            popTransitionSpec = {
                if (initialState.key is ModalKey)
                    fadeIn(tween(220)) togetherWith (slideOutVertically(tween(300)) { it } + fadeOut(tween(200)))
                else
                    fadeIn(tween(220)) togetherWith fadeOut(tween(220))
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
