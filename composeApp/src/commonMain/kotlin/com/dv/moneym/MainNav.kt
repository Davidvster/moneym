package com.dv.moneym

import androidx.compose.animation.ContentTransform
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
import com.dv.moneym.feature.categories.edit.categoryEditEntry
import com.dv.moneym.feature.categories.list.CategoriesKey
import com.dv.moneym.feature.categories.list.categoriesEntry
import com.dv.moneym.feature.overview.OverviewKey
import com.dv.moneym.feature.overview.overviewEntry
import com.dv.moneym.feature.security.setup.PinSetupKey
import com.dv.moneym.feature.security.setup.pinSetupEntry
import com.dv.moneym.feature.settings.overview.CurrencyPickerKey
import com.dv.moneym.feature.settings.overview.LanguagePickerKey
import com.dv.moneym.feature.settings.overview.PaymentModeListKey
import com.dv.moneym.feature.settings.overview.SecuritySettingsViewModel
import com.dv.moneym.feature.settings.overview.SettingsKey
import com.dv.moneym.feature.settings.overview.TxListDisplayKey
import com.dv.moneym.feature.settings.overview.currencypicker.currencyPickerEntry
import com.dv.moneym.feature.settings.overview.export.ExportDataKey
import com.dv.moneym.feature.settings.overview.export.exportDataEntry
import com.dv.moneym.feature.settings.overview.locale.languagePickerEntry
import com.dv.moneym.feature.settings.overview.settingsEntry
import com.dv.moneym.feature.settings.overview.transactiondisplay.txListDisplayEntry
import com.dv.moneym.feature.settings.paymentmodes.paymentModeListEntry
import com.dv.moneym.feature.settings.wallet.AddWalletCurrencyPickerKey
import com.dv.moneym.feature.settings.wallet.AddWalletKey
import com.dv.moneym.feature.settings.wallet.AddWalletViewModel
import com.dv.moneym.feature.settings.wallet.WalletManageKey
import com.dv.moneym.feature.settings.wallet.addWalletCurrencyPickerEntry
import com.dv.moneym.feature.settings.wallet.addWalletEntry
import com.dv.moneym.feature.settings.wallet.walletManageEntry
import com.dv.moneym.feature.transactionedit.TransactionEditKey
import com.dv.moneym.feature.transactionedit.transactionEditEntry
import com.dv.moneym.feature.transactions.list.TransactionsKey
import com.dv.moneym.feature.transactions.list.transactionsEntry
import com.dv.moneym.platform.FilePlatform
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
    // Shared SecuritySettingsViewModel so we can call refreshPinState after pin setup
    val securitySettingsViewModel: SecuritySettingsViewModel = koinViewModel()
    // Shared AddWalletViewModel so both AddWalletScreen and its currency picker share state
    val addWalletViewModel: AddWalletViewModel = koinViewModel()

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
                    val fromKey = initialState.key as? NavKey
                        ?: return@NavDisplay fadeIn(tween(220)) togetherWith fadeOut(tween(220))
                    val toKey = targetState.key as? NavKey
                        ?: return@NavDisplay fadeIn(tween(220)) togetherWith fadeOut(tween(220))
                    val dir = tabSlideDirection(fromKey, toKey)
                    if (dir != 0)
                        slideInHorizontally(tween(300)) { it * dir } togetherWith slideOutHorizontally(
                            tween(300)
                        ) { -it * dir }
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
                    val fromKey = initialState.key as? NavKey
                        ?: return@NavDisplay fadeIn(tween(220)) togetherWith fadeOut(tween(220))
                    val toKey = targetState.key as? NavKey
                        ?: return@NavDisplay fadeIn(tween(220)) togetherWith fadeOut(tween(220))
                    val dir = tabSlideDirection(fromKey, toKey)
                    if (dir != 0)
                        slideInHorizontally(tween(300)) { -it * dir } togetherWith slideOutHorizontally(
                            tween(300)
                        ) { it * dir }
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
                    val fromKey = initialState.key as? NavKey
                        ?: return@NavDisplay fadeIn(tween(220)) togetherWith fadeOut(tween(220))
                    val toKey = targetState.key as? NavKey
                        ?: return@NavDisplay fadeIn(tween(220)) togetherWith fadeOut(tween(220))
                    val dir = tabSlideDirection(fromKey, toKey)
                    if (dir != 0)
                        slideInHorizontally(tween(300)) { -it * dir } togetherWith slideOutHorizontally(
                            tween(300)
                        ) { it * dir }
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
                securityViewModel = securitySettingsViewModel,
                // From settings, we push PinSetupKey(isChangePinFlow = true) for change PIN
                onNavigateToPinSetup = { tabBackStack.push(PinSetupKey(isChangePinFlow = true)) },
                onNavigateToCategories = { tabBackStack.push(CategoriesKey) },
                onNavigateToTxDisplay = { tabBackStack.push(TxListDisplayKey) },
                onNavigateToCurrency = { tabBackStack.push(CurrencyPickerKey) },
                onNavigateToLanguage = { tabBackStack.push(LanguagePickerKey) },
                onNavigateToExport = { tabBackStack.push(ExportDataKey) },
                onNavigateToWallets = { tabBackStack.push(WalletManageKey) },
                onNavigateToPaymentModes = { tabBackStack.push(PaymentModeListKey) },
                onTabSelected = { route ->
                    when (route) {
                        TabRoute.Transactions -> tabBackStack.switchTab(TransactionsKey)
                        TabRoute.Overview -> tabBackStack.switchTab(OverviewKey)
                        TabRoute.Settings -> tabBackStack.switchTab(SettingsKey)
                    }
                },
            )
            // Setup flow (isChangePinFlow = false by default)
            pinSetupEntry(onDone = {
                lockController.init()
                // Refresh pin state in settings so toggles reflect actual storage truth
                securitySettingsViewModel.refreshPinState()
                tabBackStack.removeLast()
            })
            categoriesEntry(
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
            paymentModeListEntry(onBack = { tabBackStack.removeLast() })
            walletManageEntry(
                onBack = { tabBackStack.removeLast() },
                onNavigateToAddWallet = { tabBackStack.push(AddWalletKey) },
            )
            addWalletEntry(
                viewModel = addWalletViewModel,
                onBack = { tabBackStack.removeLast() },
                onNavigateToCurrencyPicker = { tabBackStack.push(AddWalletCurrencyPickerKey) },
                onConfirm = { name, currency ->
                    addWalletViewModel.addWallet(name, currency)
                    tabBackStack.removeLast()
                },
            )
            addWalletCurrencyPickerEntry(
                currentCurrency = { addWalletViewModel.selectedCurrency.value },
                onBack = { tabBackStack.removeLast() },
                onCurrencySelected = { code -> addWalletViewModel.setCurrency(code) },
            )
        },
    )
}
