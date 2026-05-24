package com.dv.moneym.di

import com.dv.moneym.AppInitializer
import com.dv.moneym.AppLockController
import com.dv.moneym.core.model.TransactionId
import com.dv.moneym.core.security.PinHasher
import com.dv.moneym.core.security.PinManager
import com.dv.moneym.AutoBackupManager
import com.dv.moneym.data.accounts.db.AccountsRoomDatabase
import com.dv.moneym.data.backup.BackupExporter
import com.dv.moneym.data.backup.DbBackupManager
import com.dv.moneym.data.backup.BackupImporter
import com.dv.moneym.data.backup.BackupRestorer
import com.dv.moneym.data.categories.db.CategoriesRoomDatabase
import com.dv.moneym.data.transactions.db.TransactionsRoomDatabase
import com.dv.moneym.feature.categories.domain.ArchiveCategoryUseCase
import com.dv.moneym.feature.categories.list.CategoryListViewModel
import com.dv.moneym.feature.onboarding.currency.OnboardingCurrencyViewModel
import com.dv.moneym.feature.onboarding.security.OnboardingSecurityViewModel
import com.dv.moneym.feature.overview.page.OverviewPageViewModel
import com.dv.moneym.feature.overview.OverviewPeriod
import com.dv.moneym.feature.overview.OverviewViewModel
import com.dv.moneym.feature.security.setup.PinSetupViewModel
import com.dv.moneym.feature.security.unlock.PinUnlockViewModel
import com.dv.moneym.feature.settings.overview.SecuritySettingsViewModel
import com.dv.moneym.feature.settings.overview.SettingsOverviewViewModel
import com.dv.moneym.feature.settings.overview.backuprestore.BackupRestoreViewModel
import com.dv.moneym.feature.settings.overview.currencypicker.CurrencyPickerViewModel
import com.dv.moneym.feature.settings.overview.export.ExportViewModel
import com.dv.moneym.feature.settings.overview.importdata.CsvImportHolder
import com.dv.moneym.feature.settings.overview.importdata.ImportDataViewModel
import com.dv.moneym.feature.settings.overview.locale.LanguagePickerViewModel
import com.dv.moneym.feature.settings.overview.transactiondisplay.TxListDisplayViewModel
import com.dv.moneym.feature.settings.paymentmodes.PaymentModeListViewModel
import com.dv.moneym.feature.settings.wallet.AddWalletViewModel
import com.dv.moneym.feature.settings.wallet.EditWalletCurrencyViewModel
import com.dv.moneym.feature.settings.wallet.WalletManageViewModel
import com.dv.moneym.feature.transactionedit.TransactionEditViewModel
import com.dv.moneym.feature.transactionedit.domain.DeleteTransactionUseCase
import com.dv.moneym.feature.transactionedit.domain.GetTransactionUseCase
import com.dv.moneym.feature.transactionedit.domain.UpsertTransactionUseCase
import com.dv.moneym.feature.transactions.list.TransactionListEphemeralState
import com.dv.moneym.feature.transactions.list.TransactionListViewModel
import com.dv.moneym.feature.transactions.list.page.TransactionPageViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val coreSecurityModule = module {
    single { PinHasher() }
    single { PinManager(get(), get(), get()) }
    single { AppLockController(get()) }
    single { AppInitializer(get(), get(), get(), get()) }
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
            clock = get(),
            savedStateHandle = get(),
        )
    }
    viewModel { params ->
        TransactionPageViewModel(
            yearMonth = params.get(),
            transactionRepository = get(),
            categoryRepository = get(),
            accountRepository = get(),
            paymentModeRepository = get(),
            appSettingsRepository = get(),
            ephemeralState = get(),
        )
    }
}

val featureTransactionEditModule = module {
    single { UpsertTransactionUseCase(get()) }
    single { DeleteTransactionUseCase(get()) }
    single { GetTransactionUseCase(get()) }
    viewModel { params ->
        TransactionEditViewModel(
            editingId = params.getOrNull<TransactionId>(),
            getTransaction = get(),
            upsertTransaction = get(),
            deleteTransaction = get(),
            categoryRepository = get(),
            accountRepository = get(),
            transactionRepository = get(),
            appSettingsRepository = get(),
            paymentModeRepository = get(),
            dispatchers = get(),
            clock = get(),
            savedStateHandle = get(),
        )
    }
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
    single { BackupExporter(get(), get(), get(), get()) }
    single { BackupImporter(get(), get(), get()) }
    single { BackupRestorer(get(), get(), get(), get()) }
    single { CsvImportHolder() }
    single {
        DbBackupManager(get()) {
            get<CategoriesRoomDatabase>().close()
            get<AccountsRoomDatabase>().close()
            get<TransactionsRoomDatabase>().close()
        }
    }
    single { AutoBackupManager(get(), get(), get(), get(), get(), get(), get()) }
}

val featureSettingsModule = module {
    viewModel {
        SettingsOverviewViewModel(
            appSettingsRepository = get(),
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
        CurrencyPickerViewModel(
            appSettingsRepository = get(),
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
    viewModel {
        ImportDataViewModel(
            holder = get(),
            categoryRepository = get(),
            accountRepository = get(),
            transactionRepository = get(),
            dispatchers = get(),
            clock = get(),
            savedStateHandle = get(),
        )
    }
    viewModel {
        BackupRestoreViewModel(
            dbBackupManager = get(),
            appSettings = get(),
            dispatchers = get(),
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
}

val featureCategoriesModule = module {
    single { ArchiveCategoryUseCase(get(), get()) }
    viewModelOf(::CategoryListViewModel)
}

val featureOnboardingModule = module {
    viewModel {
        OnboardingCurrencyViewModel(
            accountRepository = get(),
            dbBackupManager = get(),
            appSettings = get(),
            dispatchers = get(),
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
    viewModelOf(::OverviewViewModel)
    viewModel { params ->
        OverviewPageViewModel(
            period = params.get<OverviewPeriod>(),
            transactionRepository = get(),
            categoryRepository = get(),
            accountRepository = get(),
            appSettingsRepository = get(),
            clock = get(),
        )
    }
}
