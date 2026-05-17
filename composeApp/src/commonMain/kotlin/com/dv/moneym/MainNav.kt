package com.dv.moneym

import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import com.dv.moneym.core.navigation.ModalKey
import com.dv.moneym.core.ui.TabRoute
import com.dv.moneym.platform.FilePlatform
import com.dv.moneym.feature.categories.ui.CategoriesKey
import com.dv.moneym.feature.categories.ui.CategoryEditKey
import com.dv.moneym.feature.categories.ui.categoriesEntry
import com.dv.moneym.feature.categories.ui.categoryEditEntry
import com.dv.moneym.feature.overview.ui.OverviewKey
import com.dv.moneym.feature.overview.ui.overviewEntry
import com.dv.moneym.feature.security.ui.PinSetupKey
import com.dv.moneym.feature.security.ui.pinSetupEntry
import com.dv.moneym.feature.settings.ui.CurrencyPickerKey
import com.dv.moneym.feature.settings.ui.ExportDataKey
import com.dv.moneym.feature.settings.ui.LanguagePickerKey
import com.dv.moneym.feature.settings.ui.SettingsKey
import com.dv.moneym.feature.settings.ui.TxListDisplayKey
import com.dv.moneym.feature.settings.ui.WalletManageKey
import com.dv.moneym.feature.settings.ui.currencyPickerEntry
import com.dv.moneym.feature.settings.ui.exportDataEntry
import com.dv.moneym.feature.settings.ui.languagePickerEntry
import com.dv.moneym.feature.settings.ui.settingsEntry
import com.dv.moneym.feature.settings.ui.txListDisplayEntry
import com.dv.moneym.feature.settings.ui.walletManageEntry
import com.dv.moneym.feature.transactionedit.ui.TransactionEditKey
import com.dv.moneym.feature.transactionedit.ui.transactionEditEntry
import com.dv.moneym.feature.transactions.ui.TransactionsKey
import com.dv.moneym.feature.transactions.ui.transactionsEntry
import com.dv.moneym.feature.settings.presentation.SettingsViewModel
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

private val TAB_ORDER: List<NavKey> = listOf(TransactionsKey, OverviewKey, SettingsKey)

private fun tabSlideDirection(from: NavKey, to: NavKey): Int {
    val fi = TAB_ORDER.indexOfFirst { it::class == from::class }
    val ti = TAB_ORDER.indexOfFirst { it::class == to::class }
    return when {
        fi < 0 || ti < 0 -> 0
        ti > fi -> 1
        else -> -1
    }
}

@Composable
internal fun MainNav(lockController: AppLockController) {
    val tabBackStack = remember { TabBackStack(TransactionsKey) }
    val filePlatform = koinInject<FilePlatform>()
    // Shared SettingsViewModel so we can call refreshPinState after pin setup
    val settingsViewModel: SettingsViewModel = koinViewModel()

    NavDisplay(
        backStack = tabBackStack.backStack,
        onBack = { tabBackStack.removeLast() },
        transitionSpec = {
            when {
                initialState.key is ModalKey -> ContentTransform(
                    slideInVertically(animationSpec = tween(350)),
                    slideOutVertically(animationSpec = tween(350)),
                )
                else -> {
                    val fromKey = initialState.key as? NavKey ?: return@NavDisplay fadeIn(tween(220)) togetherWith fadeOut(tween(220))
                    val toKey = targetState.key as? NavKey ?: return@NavDisplay fadeIn(tween(220)) togetherWith fadeOut(tween(220))
                    val dir = tabSlideDirection(fromKey, toKey)
                    if (dir != 0)
                        slideInHorizontally(tween(300)) { it * dir } togetherWith slideOutHorizontally(tween(300)) { -it * dir }
                    else
                        fadeIn(tween(220)) togetherWith fadeOut(tween(220))
                }
            }
        },
        popTransitionSpec = {
            when {
                initialState.key is ModalKey -> ContentTransform(
                    slideInVertically(animationSpec = tween(350)),
                    slideOutVertically(animationSpec = tween(350)),
                )
                else -> {
                    val fromKey = initialState.key as? NavKey ?: return@NavDisplay fadeIn(tween(220)) togetherWith fadeOut(tween(220))
                    val toKey = targetState.key as? NavKey ?: return@NavDisplay fadeIn(tween(220)) togetherWith fadeOut(tween(220))
                    val dir = tabSlideDirection(fromKey, toKey)
                    if (dir != 0)
                        slideInHorizontally(tween(300)) { -it * dir } togetherWith slideOutHorizontally(tween(300)) { it * dir }
                    else
                        fadeIn(tween(220)) togetherWith fadeOut(tween(220))
                }
            }
        },
        predictivePopTransitionSpec = {
            when {
                initialState.key is ModalKey -> ContentTransform(
                    slideInVertically(animationSpec = tween(350)),
                    slideOutVertically(animationSpec = tween(350)),
                )
                else -> {
                    val fromKey = initialState.key as? NavKey ?: return@NavDisplay fadeIn(tween(220)) togetherWith fadeOut(tween(220))
                    val toKey = targetState.key as? NavKey ?: return@NavDisplay fadeIn(tween(220)) togetherWith fadeOut(tween(220))
                    val dir = tabSlideDirection(fromKey, toKey)
                    if (dir != 0)
                        slideInHorizontally(tween(300)) { -it * dir } togetherWith slideOutHorizontally(tween(300)) { it * dir }
                    else
                        fadeIn(tween(220)) togetherWith fadeOut(tween(220))
                }
            }
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
                viewModel = settingsViewModel,
                // From settings, we push PinSetupKey(isChangePinFlow = true) for change PIN
                onNavigateToPinSetup = { tabBackStack.push(PinSetupKey(isChangePinFlow = true)) },
                onNavigateToCategories = { tabBackStack.push(CategoriesKey) },
                onNavigateToTxDisplay = { tabBackStack.push(TxListDisplayKey) },
                onNavigateToCurrency = { tabBackStack.push(CurrencyPickerKey) },
                onNavigateToLanguage = { tabBackStack.push(LanguagePickerKey) },
                onNavigateToExport = { tabBackStack.push(ExportDataKey) },
                onNavigateToWallets = { tabBackStack.push(WalletManageKey) },
                onTabSelected = { route ->
                    when (route) {
                        TabRoute.Transactions -> tabBackStack.switchTab(TransactionsKey)
                        TabRoute.Overview -> tabBackStack.switchTab(OverviewKey)
                        TabRoute.Settings -> tabBackStack.switchTab(SettingsKey)
                    }
                },
                onExportReady = { fileName, content, mimeType ->
                    filePlatform.saveFile(fileName, content, mimeType)
                },
                onImportRequested = {
                    filePlatform.openTextFile()
                },
            )
            // Setup flow (isChangePinFlow = false by default)
            pinSetupEntry(onDone = {
                lockController.init()
                // Refresh pin state in settings so toggles reflect actual storage truth
                settingsViewModel.refreshPinState()
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
            exportDataEntry(
                onBack = { tabBackStack.removeLast() },
                onExportReady = { fileName, content, mimeType ->
                    filePlatform.saveFile(fileName, content, mimeType)
                },
            )
            walletManageEntry(onBack = { tabBackStack.removeLast() })
        },
    )
}
