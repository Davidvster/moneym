package com.dv.moneym

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import com.dv.moneym.core.navigation.ModalKey
import com.dv.moneym.core.ui.TabRoute
import com.dv.moneym.feature.categories.ui.CategoriesKey
import com.dv.moneym.feature.categories.ui.CategoryEditKey
import com.dv.moneym.feature.categories.ui.categoriesEntry
import com.dv.moneym.feature.categories.ui.categoryEditEntry
import com.dv.moneym.feature.overview.ui.OverviewKey
import com.dv.moneym.feature.overview.ui.overviewEntry
import com.dv.moneym.feature.security.ui.PinSetupKey
import com.dv.moneym.feature.security.ui.pinSetupEntry
import com.dv.moneym.feature.settings.ui.CurrencyPickerKey
import com.dv.moneym.feature.settings.ui.LanguagePickerKey
import com.dv.moneym.feature.settings.ui.SettingsKey
import com.dv.moneym.feature.settings.ui.TxListDisplayKey
import com.dv.moneym.feature.settings.ui.currencyPickerEntry
import com.dv.moneym.feature.settings.ui.languagePickerEntry
import com.dv.moneym.feature.settings.ui.settingsEntry
import com.dv.moneym.feature.settings.ui.txListDisplayEntry
import com.dv.moneym.feature.transactionedit.ui.TransactionEditKey
import com.dv.moneym.feature.transactionedit.ui.transactionEditEntry
import com.dv.moneym.feature.transactions.ui.TransactionsKey
import com.dv.moneym.feature.transactions.ui.transactionsEntry

@Composable
internal fun MainNav(lockController: AppLockController) {
    val tabBackStack = remember { TabBackStack(TransactionsKey) }

    NavDisplay(
        backStack = tabBackStack.backStack,
        onBack = { tabBackStack.removeLast() },
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
                onTabSelected = { route ->
                    when (route) {
                        TabRoute.Transactions -> tabBackStack.switchTab(TransactionsKey)
                        TabRoute.Overview -> tabBackStack.switchTab(OverviewKey)
                        TabRoute.Settings -> tabBackStack.switchTab(SettingsKey)
                    }
                },
            )
            transactionEditEntry(onDismiss = { tabBackStack.removeLast() })
            overviewEntry(
                onTabSelected = { route ->
                    when (route) {
                        TabRoute.Transactions -> tabBackStack.switchTab(TransactionsKey)
                        TabRoute.Overview -> tabBackStack.switchTab(OverviewKey)
                        TabRoute.Settings -> tabBackStack.switchTab(SettingsKey)
                    }
                },
            )
            settingsEntry(
                onNavigateToPinSetup = { tabBackStack.push(PinSetupKey) },
                onNavigateToCategories = { tabBackStack.push(CategoriesKey) },
                onNavigateToTxDisplay = { tabBackStack.push(TxListDisplayKey) },
                onNavigateToCurrency = { tabBackStack.push(CurrencyPickerKey) },
                onNavigateToLanguage = { tabBackStack.push(LanguagePickerKey) },
                onTabSelected = { route ->
                    when (route) {
                        TabRoute.Transactions -> tabBackStack.switchTab(TransactionsKey)
                        TabRoute.Overview -> tabBackStack.switchTab(OverviewKey)
                        TabRoute.Settings -> tabBackStack.switchTab(SettingsKey)
                    }
                },
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
            txListDisplayEntry(onBack = { tabBackStack.removeLast() })
            currencyPickerEntry(onBack = { tabBackStack.removeLast() })
            languagePickerEntry(onBack = { tabBackStack.removeLast() })
        },
    )
}
