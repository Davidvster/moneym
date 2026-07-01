package com.dv.moneym.di

import com.dv.moneym.AppInitializer
import com.dv.moneym.AppLockController
import com.dv.moneym.AutoBackupManager
import com.dv.moneym.core.model.BudgetId
import com.dv.moneym.core.model.RecurringTransactionId
import com.dv.moneym.core.model.TransactionId
import com.dv.moneym.core.security.PinHasher
import com.dv.moneym.core.security.PinManager
import com.dv.moneym.data.accounts.db.AccountsRoomDatabase
import com.dv.moneym.data.aichat.db.AiChatRoomDatabase
import com.dv.moneym.data.sync.DeviceRegistryManager
import com.dv.moneym.data.sync.SyncEngine
import com.dv.moneym.data.backup.BackupExporter
import com.dv.moneym.data.backup.BackupImporter
import com.dv.moneym.data.backup.BackupRestorer
import com.dv.moneym.data.backup.DbBackupManager
import com.dv.moneym.data.budgets.db.BudgetsRoomDatabase
import com.dv.moneym.data.categories.db.CategoriesRoomDatabase
import com.dv.moneym.data.overview.db.OverviewRoomDatabase
import com.dv.moneym.data.transactions.db.TransactionsRoomDatabase
import com.dv.moneym.feature.aianalysis.ActiveChatHolder
import com.dv.moneym.feature.aianalysis.AnalyzeViewModel
import com.dv.moneym.feature.aianalysis.history.AnalyzeHistoryViewModel
import com.dv.moneym.feature.aianalysis.usecase.BuildFinanceSnapshotUseCase
import com.dv.moneym.feature.aianalysis.usecase.BuildFinanceToolsetUseCase
import com.dv.moneym.feature.aimodels.AiModelsViewModel
import com.dv.moneym.feature.budgets.create.BudgetCreateViewModel
import com.dv.moneym.feature.budgets.list.BudgetListViewModel
import com.dv.moneym.feature.categories.domain.ArchiveCategoryUseCase
import com.dv.moneym.feature.categories.domain.DeleteCategoryUseCase
import com.dv.moneym.feature.categories.list.CategoryListViewModel
import com.dv.moneym.feature.onboarding.currency.OnboardingCurrencyViewModel
import com.dv.moneym.feature.onboarding.restore.OnboardingRestoreViewModel
import com.dv.moneym.feature.onboarding.security.OnboardingSecurityViewModel
import com.dv.moneym.core.model.SuggestionSource
import com.dv.moneym.data.banksync.BankSyncRepository
import com.dv.moneym.data.walletsync.WalletSyncRepository
import com.dv.moneym.feature.banksync.suggestions.SuggestionsViewModel
import com.dv.moneym.feature.banksync.suggestions.SuggestionSourceType
import com.dv.moneym.feature.banksync.home.BankSyncHomeViewModel
import com.dv.moneym.feature.banksync.credentials.BankSyncCredentialsViewModel
import com.dv.moneym.feature.banksync.bankpicker.BankPickerViewModel
import com.dv.moneym.feature.walletsync.home.WalletSyncHomeViewModel
import com.dv.moneym.feature.banksync.usecase.AcceptSuggestionUseCase
import com.dv.moneym.feature.banksync.usecase.CompleteConnectionUseCase
import com.dv.moneym.feature.banksync.usecase.ConnectBankUseCase
import com.dv.moneym.feature.banksync.usecase.FindDuplicateUseCase
import com.dv.moneym.feature.banksync.usecase.ParseRedirectCodeUseCase
import com.dv.moneym.feature.sync.PendingDeletionsViewModel
import com.dv.moneym.feature.sync.SyncSettingsViewModel
import com.dv.moneym.feature.overview.OverviewPeriod
import com.dv.moneym.feature.overview.OverviewAiWidgetBuilderViewModel
import com.dv.moneym.feature.overview.OverviewViewModel
import com.dv.moneym.feature.overview.a2ui.BuildOverviewWidgetPromptUseCase
import com.dv.moneym.feature.overview.page.OverviewPageViewModel
import com.dv.moneym.feature.overview.usecase.BuildBudgetProgressUseCase
import com.dv.moneym.feature.overview.usecase.BuildCategoryBreakdownUseCase
import com.dv.moneym.feature.overview.usecase.BuildCategoryTrendsUseCase
import com.dv.moneym.feature.overview.usecase.BuildCumulativeSeriesUseCase
import com.dv.moneym.feature.overview.usecase.BuildOverviewPageStateUseCase
import com.dv.moneym.feature.overview.usecase.ResolveOverviewBlocksUseCase
import com.dv.moneym.feature.overview.usecase.ResolvePeriodRangeUseCase
import com.dv.moneym.feature.security.setup.PinSetupViewModel
import com.dv.moneym.feature.security.unlock.PinUnlockViewModel
import com.dv.moneym.feature.settings.overview.SecuritySettingsViewModel
import com.dv.moneym.feature.settings.overview.OverviewSettingsViewModel
import com.dv.moneym.feature.settings.overview.SettingsOverviewViewModel
import com.dv.moneym.feature.settings.overview.backuprestore.BackupRestoreViewModel
import com.dv.moneym.feature.settings.overview.export.ExportViewModel
import com.dv.moneym.feature.settings.overview.importdata.CsvImportHolder
import com.dv.moneym.feature.settings.overview.importdata.ImportDataViewModel
import com.dv.moneym.feature.settings.overview.importdata.usecase.PrepareImportPreviewUseCase
import com.dv.moneym.feature.settings.overview.locale.LanguagePickerViewModel
import com.dv.moneym.feature.settings.overview.transactiondisplay.TxListDisplayViewModel
import com.dv.moneym.feature.settings.paymentmodes.PaymentModeListViewModel
import com.dv.moneym.feature.settings.recurring.RecurringListViewModel
import com.dv.moneym.feature.settings.wallet.AddWalletViewModel
import com.dv.moneym.feature.settings.wallet.EditWalletCurrencyViewModel
import com.dv.moneym.feature.settings.wallet.EditWalletViewModel
import com.dv.moneym.feature.settings.wallet.WalletManageViewModel
import com.dv.moneym.feature.transactionedit.RecurringEditViewModel
import com.dv.moneym.feature.transactionedit.TransactionEditDraft
import com.dv.moneym.feature.transactionedit.TransactionEditViewModel
import com.dv.moneym.feature.transactionedit.domain.DeleteTransactionUseCase
import com.dv.moneym.feature.transactionedit.domain.GetTransactionUseCase
import com.dv.moneym.feature.transactionedit.domain.UpsertTransactionUseCase
import com.dv.moneym.feature.transactionedit.usecase.ApplyTransactionEditDraftUseCase
import com.dv.moneym.feature.transactionedit.usecase.BuildNoteSuggestionsUseCase
import com.dv.moneym.feature.transactionedit.usecase.ComputeCategoryBudgetRemainingUseCase
import com.dv.moneym.feature.transactionedit.usecase.SaveTransactionEditUseCase
import com.dv.moneym.feature.transactionedit.usecase.SelectNoteUseCase
import com.dv.moneym.feature.transactionedit.usecase.SuggestNotesUseCase
import com.dv.moneym.feature.transactionedit.usecase.ValidateAndBuildTransactionUseCase
import com.dv.moneym.feature.transactions.list.TransactionListEphemeralState
import com.dv.moneym.feature.transactions.list.TransactionListViewModel
import com.dv.moneym.feature.transactions.list.page.TransactionPageViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.core.qualifier.named
import org.koin.dsl.module

val coreSecurityModule = module {
    single { PinHasher() }
    single { PinManager(get(), get(), get()) }
    single { AppLockController(get()) }
    single { AppInitializer(get(), get(), get(), get(), get(), get(), get()) }
}

val featureTransactionsModule = module {
    single { TransactionListEphemeralState() }
    viewModel {
        TransactionListViewModel(
            transactionRepository = get(),
            categoryRepository = get(),
            accountRepository = get(),
            appSettingsRepository = get(),
            ephemeralState = get(),
            syncStatus = get<SyncEngine>(),
            syncPuller = get<SyncEngine>(),
            bankSyncStatus = get(),
            walletSyncStatus = get(),
            clock = get(),
            transactionSavedSignal = get(),
            savedStateHandle = get(),
        )
    }
    viewModel { params ->
        TransactionPageViewModel(
            yearMonth = params.get(),
            transactionRepository = get(),
            recurringTransactionRepository = get(),
            categoryRepository = get(),
            accountRepository = get(),
            paymentModeRepository = get(),
            appSettingsRepository = get(),
            clock = get(),
            ephemeralState = get(),
        )
    }
}

val featureTransactionEditModule = module {
    single { UpsertTransactionUseCase(get()) }
    single { DeleteTransactionUseCase(get()) }
    single { GetTransactionUseCase(get()) }
    single { ComputeCategoryBudgetRemainingUseCase(get(), get()) }
    single { ValidateAndBuildTransactionUseCase() }
    single { SuggestNotesUseCase() }
    single { ApplyTransactionEditDraftUseCase() }
    single { BuildNoteSuggestionsUseCase(get(), get()) }
    single { SelectNoteUseCase(get()) }
    single { SaveTransactionEditUseCase(get(), get(), get()) }
    viewModel { params ->
        TransactionEditViewModel(
            editingId = params.getOrNull<TransactionId>(),
            draft = params.getOrNull<TransactionEditDraft>(),
            getTransaction = get(),
            deleteTransaction = get(),
            validateAndBuildTransaction = get(),
            saveTransactionEdit = get(),
            applyTransactionEditDraft = get(),
            buildNoteSuggestions = get(),
            selectNote = get(),
            categoryRepository = get(),
            accountRepository = get(),
            appSettingsRepository = get(),
            paymentModeRepository = get(),
            computeBudgetRemaining = get(),
            dispatchers = get(),
            clock = get(),
            suggestionSources = mapOf(
                SuggestionSourceType.BANK.name to get(named(SuggestionSourceType.BANK.name)),
                SuggestionSourceType.WALLET.name to get(named(SuggestionSourceType.WALLET.name)),
            ),
            transactionSavedSignal = get(),
            savedStateHandle = get(),
        )
    }
    viewModel { params ->
        RecurringEditViewModel(
            ruleId = params.get<RecurringTransactionId>(),
            recurringRepo = get(),
            categoryRepository = get(),
            accountRepository = get(),
            paymentModeRepository = get(),
            appSettingsRepository = get(),
            dispatchers = get(),
            clock = get(),
            savedStateHandle = get(),
        )
    }
    viewModel { RecurringListViewModel(recurringRepo = get(), categoryRepository = get()) }
}

val featureSecurityModule = module {
    viewModel {
        PinSetupViewModel(
            pinManager = get(),
            dispatchers = get(),
            biometricAuth = get(),
            settings = get(),
            savedStateHandle = get(),
        )
    }
    viewModelOf(::PinUnlockViewModel)
}

val dataBackupModule = module {
    single { BackupExporter(get(), get(), get(), get(), get(), get(), get()) }
    single { BackupImporter(get(), get(), get(), get(), get()) }
    single { BackupRestorer(get(), get(), get(), get(), get(), get()) }
    single { CsvImportHolder() }
    single {
        DbBackupManager(get()) {
            get<CategoriesRoomDatabase>().close()
            get<AccountsRoomDatabase>().close()
            get<TransactionsRoomDatabase>().close()
            get<BudgetsRoomDatabase>().close()
            get<OverviewRoomDatabase>().close()
            get<AiChatRoomDatabase>().close()
        }
    }
    single { AutoBackupManager(get(), get(), get(), get(), get(), get(), get(), get(), get()) }
}

val featureSettingsModule = module {
    viewModel {
        OverviewSettingsViewModel(
            overviewRepository = get(),
        )
    }
    viewModel {
        SettingsOverviewViewModel(
            appSettingsRepository = get(),
            accountRepository = get(),
            savedStateHandle = get(),
        )
    }
    viewModel {
        SecuritySettingsViewModel(
            settings = get(),
            pinManager = get(),
            biometricAuth = get(),
            dispatchers = get(),
            savedStateHandle = get(),
        )
    }
    viewModel {
        ExportViewModel(
            exporter = get(),
            importer = get(),
            dispatchers = get(),
            savedStateHandle = get(),
        )
    }
    viewModel {
        LanguagePickerViewModel(
            appSettingsRepository = get(),
            localeController = get(),
            savedStateHandle = get(),
        )
    }
    single { PrepareImportPreviewUseCase() }
    viewModel {
        ImportDataViewModel(
            holder = get(),
            categoryRepository = get(),
            accountRepository = get(),
            transactionRepository = get(),
            prepareImportPreview = get(),
            dispatchers = get(),
            clock = get(),
            savedStateHandle = get(),
        )
    }
    viewModel {
        BackupRestoreViewModel(
            dbBackupManager = get(),
            backupCodec = get(),
            appSettings = get(),
            dispatchers = get(),
            googleAuthManager = getOrNull(),
            remoteBackupManager = getOrNull(),
            sessionPassphrase = getOrNull(),
            syncPassphraseStore = getOrNull(),
            syncBootstrap = getOrNull(),
            syncPuller = getOrNull<SyncEngine>(),
            filePlatform = get(),
            savedStateHandle = get(),
        )
    }
    viewModel {
        TxListDisplayViewModel(
            appSettingsRepository = get(),
            savedStateHandle = get(),
        )
    }
    viewModel {
        PaymentModeListViewModel(
            paymentModeRepository = get(),
            savedStateHandle = get(),
        )
    }
}

val featureWalletModule = module {
    viewModel {
        WalletManageViewModel(
            accountRepository = get(),
            transactionRepository = get(),
            appSettingsRepository = get(),
            savedStateHandle = get(),
        )
    }
    viewModel {
        AddWalletViewModel(
            accountRepository = get(),
            savedStateHandle = get(),
        )
    }
    viewModel { params ->
        EditWalletCurrencyViewModel(
            accountId = params.get(),
            currentCurrency = params.get(),
            accountRepository = get(),
            transactionRepository = get(),
            dispatchers = get(),
            savedStateHandle = get(),
        )
    }
    viewModel { params ->
        EditWalletViewModel(
            accountId = params.get(),
            accountRepository = get(),
            savedStateHandle = get(),
        )
    }
}

val featureCategoriesModule = module {
    single { ArchiveCategoryUseCase(get(), get()) }
    single { DeleteCategoryUseCase(get(), get()) }
    viewModelOf(::CategoryListViewModel)
}

val featureSyncModule = module {
    viewModel { PendingDeletionsViewModel(get()) }
    viewModel {
        SyncSettingsViewModel(
            registry = get<DeviceRegistryManager>(),
            appSettings = get(),
            syncPuller = get<SyncEngine>(),
            savedStateHandle = get(),
        )
    }
}

val featureBankSyncModule = module {
    single { ConnectBankUseCase(client = get(), clock = get()) }
    single {
        CompleteConnectionUseCase(
            client = get(),
            credentialsStore = get(),
            bankSyncRepository = get(),
            appSettings = get(),
        )
    }
    single { ParseRedirectCodeUseCase() }
    single { FindDuplicateUseCase(transactionRepository = get()) }
    single {
        AcceptSuggestionUseCase(
            transactionRepository = get(),
            clock = get(),
        )
    }
    single<SuggestionSource>(named(SuggestionSourceType.BANK.name)) { get<BankSyncRepository>() }
    single<SuggestionSource>(named(SuggestionSourceType.WALLET.name)) { get<WalletSyncRepository>() }
    viewModel { (sourceType: SuggestionSourceType) ->
        SuggestionsViewModel(
            sourceType = sourceType,
            source = get(named(sourceType.name)),
            accountRepository = get(),
            categoryRepository = get(),
            acceptSuggestion = get(),
            findDuplicate = get(),
            clock = get(),
            savedStateHandle = get(),
        )
    }
    viewModel {
        BankSyncHomeViewModel(
            credentialsStore = get(),
            client = get(),
            bankSyncRepository = get(),
            engine = get(),
            appSettings = get(),
            accountRepository = get(),
            savedStateHandle = get(),
        )
    }
    viewModel {
        BankSyncCredentialsViewModel(
            credentialsStore = get(),
            client = get(),
            appSettings = get(),
            savedStateHandle = get(),
        )
    }
    viewModel {
        BankPickerViewModel(
            client = get(),
            connectBank = get(),
            completeConnection = get(),
            parseRedirectCode = get(),
            callbackBus = get(),
            savedStateHandle = get(),
        )
    }
}

val featureWalletSyncModule = module {
    viewModel {
        WalletSyncHomeViewModel(
            notificationAccess = get(),
            installedAppsProvider = get(),
            walletSyncRepository = get(),
            appSettings = get(),
        )
    }
}

val featureBudgetsModule = module {
    viewModelOf(::BudgetListViewModel)
    viewModel { params ->
        BudgetCreateViewModel(
            budgetId = params.getOrNull<BudgetId>(),
            budgetRepository = get(),
            categoryRepository = get(),
            accountRepository = get(),
            clock = get(),
            dispatchers = get(),
            savedStateHandle = get(),
        )
    }
}

val featureOnboardingModule = module {
    viewModel {
        OnboardingCurrencyViewModel(
            accountRepository = get(),
            appSettings = get(),
            savedStateHandle = get(),
        )
    }
    viewModel {
        OnboardingRestoreViewModel(
            dbBackupManager = get(),
            backupCodec = get(),
            appSettings = get(),
            dispatchers = get(),
            googleAuthManager = getOrNull(),
            remoteBackupManager = getOrNull(),
            savedStateHandle = get(),
        )
    }
    viewModel {
        OnboardingSecurityViewModel(
            settings = get(),
            pinManager = get(),
            biometricAuth = get(),
            savedStateHandle = get(),
        )
    }
}

val featureOverviewModule = module {
    single { BuildBudgetProgressUseCase() }
    single { ResolvePeriodRangeUseCase() }
    single { BuildCategoryBreakdownUseCase() }
    single { BuildCategoryTrendsUseCase() }
    single { BuildCumulativeSeriesUseCase() }
    single { BuildOverviewPageStateUseCase(get(), get(), get(), get(), get()) }
    single { ResolveOverviewBlocksUseCase() }
    single { BuildOverviewWidgetPromptUseCase() }
    viewModelOf(::OverviewViewModel)
    viewModel { params ->
        OverviewAiWidgetBuilderViewModel(
            widgetId = params.getOrNull<Long>(),
            overviewRepository = get(),
            registry = get(),
            appSettings = get(),
            dispatchers = get(),
            clock = get(),
            buildPrompt = get(),
        )
    }
    viewModel { params ->
        OverviewPageViewModel(
            period = params.get<OverviewPeriod>(),
            transactionRepository = get(),
            categoryRepository = get(),
            accountRepository = get(),
            appSettingsRepository = get(),
            budgetRepository = get(),
            overviewRepository = get(),
            buildOverviewPageState = get(),
            resolveOverviewBlocks = get(),
            clock = get(),
        )
    }
}

val featureAianalysisModule = module {
    single { BuildFinanceSnapshotUseCase(get(), get(), get(), get(), get()) }
    single { BuildFinanceToolsetUseCase(get(), get(), get(), get(), get()) }
    viewModel { params ->
        AnalyzeViewModel(
            year = params.get(),
            month = params.get(),
            registry = get(),
            buildSnapshot = get(),
            buildToolset = get(),
            appSettings = get(),
            dispatchers = get(),
            aiChatRepository = get(),
            transactionRepository = get(),
            llmModelRepository = get(),
            localeController = get(),
            clock = get(),
            activeChatHolder = get(),
            savedStateHandle = get(),
        )
    }
    single { ActiveChatHolder() }
    viewModel { AnalyzeHistoryViewModel(aiChatRepository = get(), activeChatHolder = get()) }
}

val featureAiModelsModule = module {
    viewModel { AiModelsViewModel(get()) }
}
