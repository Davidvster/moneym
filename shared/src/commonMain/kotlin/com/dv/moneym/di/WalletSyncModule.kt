package com.dv.moneym.di

import com.dv.moneym.data.walletsync.DefaultWalletSyncStatusProvider
import com.dv.moneym.data.walletsync.NotificationParser
import com.dv.moneym.data.walletsync.WalletSyncRepository
import com.dv.moneym.data.walletsync.WalletSyncStatusProvider
import com.dv.moneym.data.walletsync.createWalletSyncRepository
import com.dv.moneym.feature.walletsync.usecase.EnrichWalletSuggestionUseCase
import org.koin.core.module.Module
import org.koin.dsl.module

val walletSyncCommonModule: Module = module {
    single { NotificationParser() }
    single<WalletSyncRepository> { createWalletSyncRepository(db = get()) }
    single<WalletSyncStatusProvider> {
        DefaultWalletSyncStatusProvider(
            appSettings = get(),
            repository = get()
        )
    }
    single { EnrichWalletSuggestionUseCase() }
}
