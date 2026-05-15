package com.dv.moneym.di

import com.dv.moneym.AppInitializer
import com.dv.moneym.AppLockController
import com.dv.moneym.core.model.CategoryId
import com.dv.moneym.core.model.TransactionId
import com.dv.moneym.core.security.PinHasher
import com.dv.moneym.core.security.PinManager
import com.dv.moneym.feature.categories.domain.ArchiveCategoryUseCase
import com.dv.moneym.feature.categories.presentation.CategoryEditViewModel
import com.dv.moneym.feature.categories.presentation.CategoryListViewModel
import com.dv.moneym.feature.onboarding.presentation.OnboardingViewModel
import com.dv.moneym.feature.overview.presentation.OverviewViewModel
import com.dv.moneym.feature.security.presentation.PinSetupViewModel
import com.dv.moneym.feature.security.presentation.PinUnlockViewModel
import com.dv.moneym.feature.settings.presentation.SettingsViewModel
import com.dv.moneym.feature.transactionedit.domain.DeleteTransactionUseCase
import com.dv.moneym.feature.transactionedit.domain.GetTransactionUseCase
import com.dv.moneym.feature.transactionedit.domain.UpsertTransactionUseCase
import com.dv.moneym.feature.transactionedit.presentation.TransactionEditViewModel
import com.dv.moneym.feature.transactions.presentation.TransactionListViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val coreSecurityModule = module {
    single { PinHasher() }
    single { PinManager(get(), get(), get()) }
    single { AppLockController(get()) }
    single { AppInitializer(get(), get(), get()) }
}

val featureTransactionsModule = module {
    viewModelOf(::TransactionListViewModel)
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
            dispatchers = get(),
            clock = get(),
        )
    }
}

val featureSecurityModule = module {
    viewModelOf(::PinSetupViewModel)
    viewModelOf(::PinUnlockViewModel)
}

val dataBackupModule = module {
    single { com.dv.moneym.data.backup.BackupExporter(get(), get(), get(), get()) }
    single { com.dv.moneym.data.backup.BackupImporter(get(), get(), get()) }
}

val featureSettingsModule = module {
    viewModelOf(::SettingsViewModel)
}

val featureCategoriesModule = module {
    single { ArchiveCategoryUseCase(get(), get()) }
    viewModelOf(::CategoryListViewModel)
    viewModel { params ->
        CategoryEditViewModel(
            editingId = params.getOrNull<CategoryId>(),
            repository = get(),
            dispatchers = get(),
            clock = get(),
        )
    }
}

val featureOnboardingModule = module {
    viewModelOf(::OnboardingViewModel)
}

val featureOverviewModule = module {
    viewModelOf(::OverviewViewModel)
}
