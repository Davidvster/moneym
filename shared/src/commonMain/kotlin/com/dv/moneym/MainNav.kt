package com.dv.moneym

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.dv.moneym.core.designsystem.MM
import com.dv.moneym.core.ui.TabRoute
import com.dv.moneym.feature.aianalysis.AnalyzeKey
import com.dv.moneym.feature.aianalysis.analyzeEntry
import com.dv.moneym.feature.aianalysis.history.AnalyzeHistoryKey
import com.dv.moneym.feature.aianalysis.history.analyzeHistoryEntry
import com.dv.moneym.feature.aimodels.AiModelsKey
import com.dv.moneym.feature.aimodels.aiModelsEntry
import com.dv.moneym.feature.budgets.create.BudgetCreateKey
import com.dv.moneym.feature.budgets.create.budgetCreateEntry
import com.dv.moneym.feature.budgets.list.BudgetListKey
import com.dv.moneym.feature.budgets.list.budgetListEntry
import com.dv.moneym.feature.categories.list.CategoriesKey
import com.dv.moneym.feature.categories.list.categoriesEntry
import com.dv.moneym.feature.overview.OverviewKey
import com.dv.moneym.feature.overview.overviewEntry
import com.dv.moneym.feature.security.setup.PinSetupKey
import com.dv.moneym.feature.security.setup.pinSetupEntry
import com.dv.moneym.feature.sync.PendingDeletionsKey
import com.dv.moneym.feature.sync.pendingDeletionsEntry
import com.dv.moneym.feature.banksync.suggestions.BankSuggestionsKey
import com.dv.moneym.feature.banksync.suggestions.WalletSuggestionsKey
import com.dv.moneym.feature.banksync.home.BankSyncSettingsKey
import com.dv.moneym.feature.banksync.suggestions.bankSuggestionsEntry
import com.dv.moneym.feature.banksync.suggestions.walletSuggestionsEntry
import com.dv.moneym.feature.banksync.home.bankSyncSettingsEntry
import com.dv.moneym.feature.walletsync.home.WalletSyncSettingsKey
import com.dv.moneym.feature.walletsync.home.walletSyncSettingsEntry
import com.dv.moneym.platform.walletSyncSupported
import com.dv.moneym.feature.banksync.credentials.BankSyncCredentialsKey
import com.dv.moneym.feature.banksync.credentials.bankSyncCredentialsEntry
import com.dv.moneym.feature.banksync.bankpicker.BankPickerKey
import com.dv.moneym.feature.banksync.bankpicker.bankPickerEntry
import com.dv.moneym.feature.sync.SyncSettingsKey
import com.dv.moneym.feature.sync.syncSettingsEntry
import com.dv.moneym.feature.settings.overview.LanguagePickerKey
import com.dv.moneym.feature.settings.overview.PaymentModeListKey
import com.dv.moneym.feature.settings.overview.SecuritySettingsIntent
import com.dv.moneym.feature.settings.overview.SecuritySettingsViewModel
import com.dv.moneym.feature.settings.overview.SettingsKey
import com.dv.moneym.feature.about.AboutKey
import com.dv.moneym.feature.about.LibrariesKey
import com.dv.moneym.feature.about.aboutEntry
import com.dv.moneym.feature.about.librariesEntry
import com.dv.moneym.feature.infopage.InfoPageKey
import com.dv.moneym.feature.infopage.infoPageEntry
import com.dv.moneym.feature.settings.overview.TxListDisplayKey
import com.dv.moneym.feature.settings.overview.backuprestore.BackupRestoreKey
import com.dv.moneym.feature.settings.overview.backuprestore.backupRestoreEntry
import com.dv.moneym.feature.settings.overview.export.ExportDataKey
import com.dv.moneym.feature.settings.overview.export.exportDataEntry
import com.dv.moneym.feature.settings.overview.importdata.CsvImportHolder
import com.dv.moneym.feature.settings.overview.importdata.ImportDataKey
import com.dv.moneym.feature.settings.overview.importdata.importDataEntry
import com.dv.moneym.feature.settings.overview.locale.languagePickerEntry
import com.dv.moneym.feature.settings.overview.settingsEntry
import com.dv.moneym.feature.settings.overview.transactiondisplay.txListDisplayEntry
import com.dv.moneym.feature.settings.paymentmodes.paymentModeListEntry
import com.dv.moneym.feature.settings.recurring.RecurringListKey
import com.dv.moneym.feature.settings.recurring.recurringListEntry
import com.dv.moneym.feature.settings.wallet.AddWalletCurrencyPickerKey
import com.dv.moneym.feature.settings.wallet.AddWalletKey
import com.dv.moneym.feature.settings.wallet.AddWalletViewModel
import com.dv.moneym.feature.settings.wallet.EditWalletCurrencyKey
import com.dv.moneym.feature.settings.wallet.EditWalletKey
import com.dv.moneym.feature.settings.wallet.WalletManageKey
import com.dv.moneym.feature.settings.wallet.addWalletCurrencyPickerEntry
import com.dv.moneym.feature.settings.wallet.addWalletEntry
import com.dv.moneym.feature.settings.wallet.editWalletCurrencyEntry
import com.dv.moneym.feature.settings.wallet.editWalletEntry
import com.dv.moneym.feature.settings.wallet.walletManageEntry
import com.dv.moneym.feature.transactionedit.RecurringEditKey
import com.dv.moneym.feature.transactionedit.TransactionEditKey
import com.dv.moneym.feature.transactionedit.recurringEditEntry
import com.dv.moneym.feature.transactionedit.transactionEditEntry
import com.dv.moneym.feature.transactions.list.TransactionsKey
import com.dv.moneym.feature.transactions.list.transactionsEntry
import com.dv.moneym.platform.FilePlatform
import com.dv.moneym.platform.rememberFilePicker
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

private val modalTransitionMeta: Map<String, Any> =
    NavDisplay.transitionSpec {
        slideInVertically(initialOffsetY = { it }, animationSpec = tween(350)) togetherWith
                ExitTransition.KeepUntilTransitionsFinished
    } + NavDisplay.popTransitionSpec {
        EnterTransition.None togetherWith
                slideOutVertically(targetOffsetY = { it }, animationSpec = tween(350))
    } + NavDisplay.predictivePopTransitionSpec { _ ->
        EnterTransition.None togetherWith
                slideOutVertically(targetOffsetY = { it }, animationSpec = tween(350))
    }

@Composable
internal fun MainNav(lockController: AppLockController) {
    val tabBackStack = remember { TabBackStack(TransactionsKey) }
    val filePlatform = koinInject<FilePlatform>()
    val csvImportHolder = koinInject<CsvImportHolder>()
    // Shared SecuritySettingsViewModel so we can call refreshPinState after pin setup
    val securitySettingsViewModel: SecuritySettingsViewModel = koinViewModel()
    // Shared AddWalletViewModel so both AddWalletScreen and its currency picker share state
    val addWalletViewModel: AddWalletViewModel = koinViewModel()

    val csvFilePicker = rememberFilePicker { content ->
        if (content != null) {
            csvImportHolder.content = content
            tabBackStack.push(ImportDataKey)
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(MM.colors.bg)) {
    NavDisplay(
        backStack = tabBackStack.backStack,
        onBack = { tabBackStack.removeLast() },
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator(),
        ),
        transitionSpec = {
            fadeIn(tween(220)) togetherWith fadeOut(tween(220))
        },
        popTransitionSpec = {
            fadeIn(tween(220)) togetherWith fadeOut(tween(220))
        },
        predictivePopTransitionSpec = { _ ->
            fadeIn(tween(220)) togetherWith fadeOut(tween(220))
        },
        entryProvider = entryProvider {
            transactionsEntry(
                onAddTransaction = { tabBackStack.push(TransactionEditKey()) },
                onEditTransaction = { id -> tabBackStack.push(TransactionEditKey(id.value)) },
                onEditRecurring = { id -> tabBackStack.push(RecurringEditKey(id.value)) },
                onTabSelected = { route ->
                    when (route) {
                        TabRoute.Transactions -> tabBackStack.switchTab(TransactionsKey)
                        TabRoute.Overview -> tabBackStack.switchTab(OverviewKey)
                        TabRoute.Settings -> tabBackStack.switchTab(SettingsKey)
                    }
                },
                onNavigateToPendingDeletions = { tabBackStack.push(PendingDeletionsKey) },
                onNavigateToBankSuggestions = { tabBackStack.push(BankSuggestionsKey) },
                onNavigateToWalletSuggestions = { tabBackStack.push(WalletSuggestionsKey) },
            )
            transactionEditEntry(
                onDismiss = { tabBackStack.removeLast() },
                metadata = modalTransitionMeta
            )
            recurringEditEntry(
                onDismiss = { tabBackStack.removeLast() },
                metadata = modalTransitionMeta
            )
            recurringListEntry(
                onBack = { tabBackStack.removeLast() },
                onEdit = { id -> tabBackStack.push(RecurringEditKey(id.value)) },
                onCreateNew = { tabBackStack.push(RecurringEditKey(0L)) },
                metadata = modalTransitionMeta,
            )
            overviewEntry(
                onTabSelected = { route ->
                    when (route) {
                        TabRoute.Transactions -> tabBackStack.switchTab(TransactionsKey)
                        TabRoute.Overview -> tabBackStack.switchTab(OverviewKey)
                        TabRoute.Settings -> tabBackStack.switchTab(SettingsKey)
                    }
                },
                onAnalyze = { year, month -> tabBackStack.push(AnalyzeKey(year, month)) },
            )
            settingsEntry(
                securityViewModel = securitySettingsViewModel,
                // From settings, we push PinSetupKey(isChangePinFlow = true) for change PIN
                onNavigateToPinSetup = { tabBackStack.push(PinSetupKey(isChangePinFlow = true)) },
                onNavigateToCategories = { tabBackStack.push(CategoriesKey) },
                onNavigateToBudgets = { tabBackStack.push(BudgetListKey) },
                onNavigateToRecurring = { tabBackStack.push(RecurringListKey) },
                onNavigateToTxDisplay = { tabBackStack.push(TxListDisplayKey) },
                onNavigateToLanguage = { tabBackStack.push(LanguagePickerKey) },
                onNavigateToExport = { tabBackStack.push(ExportDataKey) },
                onNavigateToBankSync = { tabBackStack.push(BankSyncSettingsKey) },
                onNavigateToWalletSync = if (walletSyncSupported) {
                    { tabBackStack.push(WalletSyncSettingsKey) }
                } else {
                    null
                },
                onNavigateToWallets = { tabBackStack.push(WalletManageKey) },
                onNavigateToPaymentModes = { tabBackStack.push(PaymentModeListKey) },
                onNavigateToBackupRestore = { tabBackStack.push(BackupRestoreKey) },
                onNavigateToAbout = { tabBackStack.push(AboutKey) },
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
                securitySettingsViewModel.onIntent(SecuritySettingsIntent.RefreshPinState)
                tabBackStack.removeLast()
            }, metadata = modalTransitionMeta)
            categoriesEntry(
                onBack = { tabBackStack.removeLast() },
                metadata = modalTransitionMeta,
            )
            analyzeEntry(
                onBack = { tabBackStack.removeLast() },
                onManageModels = { tabBackStack.push(AiModelsKey) },
                onShowHistory = { year, month -> tabBackStack.push(AnalyzeHistoryKey(year, month)) },
                metadata = modalTransitionMeta,
            )
            analyzeHistoryEntry(
                onBack = { tabBackStack.removeLast() },
                metadata = modalTransitionMeta,
            )
            aiModelsEntry(
                onBack = { tabBackStack.removeLast() },
                metadata = modalTransitionMeta,
            )
            pendingDeletionsEntry(
                onDone = { tabBackStack.removeLast() },
                metadata = modalTransitionMeta,
            )
            syncSettingsEntry(
                onBack = { tabBackStack.removeLast() },
                metadata = modalTransitionMeta,
            )
            bankSyncSettingsEntry(
                onBack = { tabBackStack.removeLast() },
                onOpenSuggestions = { tabBackStack.push(BankSuggestionsKey) },
                onNavigateToCredentials = { tabBackStack.push(BankSyncCredentialsKey) },
                onNavigateToBankPicker = { tabBackStack.push(BankPickerKey) },
                onNavigateToInfo = { tabBackStack.push(InfoPageKey("banksync")) },
                metadata = modalTransitionMeta,
            )
            bankSyncCredentialsEntry(
                onBack = { tabBackStack.removeLast() },
                onContinueToBankPicker = { tabBackStack.push(BankPickerKey) },
                metadata = modalTransitionMeta,
            )
            bankPickerEntry(
                onBack = { tabBackStack.removeLast() },
                onConnected = { tabBackStack.popTo(BankSyncSettingsKey) },
                metadata = modalTransitionMeta,
            )
            bankSuggestionsEntry(
                onBack = { tabBackStack.removeLast() },
                metadata = modalTransitionMeta,
            )
            walletSyncSettingsEntry(
                onBack = { tabBackStack.removeLast() },
                onOpenSuggestions = { tabBackStack.push(WalletSuggestionsKey) },
                metadata = modalTransitionMeta,
            )
            walletSuggestionsEntry(
                onBack = { tabBackStack.removeLast() },
                metadata = modalTransitionMeta,
            )
            budgetListEntry(
                onBack = { tabBackStack.removeLast() },
                onCreate = { tabBackStack.push(BudgetCreateKey()) },
                onEdit = { id -> tabBackStack.push(BudgetCreateKey(id.value)) },
                metadata = modalTransitionMeta,
            )
            budgetCreateEntry(
                onBack = { tabBackStack.removeLast() },
                metadata = modalTransitionMeta,
            )
            txListDisplayEntry(
                onBack = { tabBackStack.removeLast() },
                metadata = modalTransitionMeta
            )
            languagePickerEntry(
                onBack = { tabBackStack.removeLast() },
                metadata = modalTransitionMeta
            )
            exportDataEntry(
                onBack = { tabBackStack.removeLast() },
                onExportReady = { fileName, content, mimeType ->
                    filePlatform.saveFile(fileName, content, mimeType)
                },
                onImportSourceSelected = { format ->
                    csvImportHolder.format = format
                    csvFilePicker()
                },
                metadata = modalTransitionMeta,
            )
            importDataEntry(onBack = { tabBackStack.removeLast() }, metadata = modalTransitionMeta)
            backupRestoreEntry(
                onBack = { tabBackStack.removeLast() },
                onNavigateToInfo = { tabBackStack.push(InfoPageKey("backup")) },
                onNavigateToSync = { tabBackStack.push(SyncSettingsKey) },
                metadata = modalTransitionMeta
            )
            infoPageEntry(
                onBack = { tabBackStack.removeLast() },
            )
            aboutEntry(
                onBack = { tabBackStack.removeLast() },
                onNavigateToLibraries = { tabBackStack.push(LibrariesKey) },
                metadata = modalTransitionMeta,
            )
            librariesEntry(
                onBack = { tabBackStack.removeLast() },
                metadata = modalTransitionMeta,
            )
            paymentModeListEntry(
                onBack = { tabBackStack.removeLast() },
                metadata = modalTransitionMeta
            )
            walletManageEntry(
                onBack = { tabBackStack.removeLast() },
                onNavigateToAddWallet = { tabBackStack.push(AddWalletKey) },
                onNavigateToEditWallet = { id, currency ->
                    tabBackStack.push(EditWalletKey(id, currency))
                },
                metadata = modalTransitionMeta,
            )
            editWalletEntry(
                onBack = { tabBackStack.removeLast() },
                onNavigateToCurrency = { id, currency ->
                    tabBackStack.push(EditWalletCurrencyKey(id, currency))
                },
                metadata = modalTransitionMeta,
            )
            editWalletCurrencyEntry(
                onBack = { tabBackStack.removeLast() },
                metadata = modalTransitionMeta,
            )
            addWalletEntry(
                viewModel = addWalletViewModel,
                onBack = { tabBackStack.removeLast() },
                onNavigateToCurrencyPicker = { tabBackStack.push(AddWalletCurrencyPickerKey) },
                onConfirm = { name, currency ->
                    addWalletViewModel.addWallet(name, currency)
                    tabBackStack.removeLast()
                },
                metadata = modalTransitionMeta,
            )
            addWalletCurrencyPickerEntry(
                viewModel = addWalletViewModel,
                currentCurrency = { addWalletViewModel.selectedCurrency.value },
                onBack = { tabBackStack.removeLast() },
                onCurrencySelected = { code -> addWalletViewModel.setCurrency(code) },
                metadata = modalTransitionMeta,
            )
        },
    )
    }
}
