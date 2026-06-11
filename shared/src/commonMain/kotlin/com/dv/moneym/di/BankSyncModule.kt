package com.dv.moneym.di

import com.dv.moneym.data.banksync.EnableBankingClient
import com.dv.moneym.data.banksync.EnableBankingCredentialsStore
import com.dv.moneym.data.banksync.ExternalIdResolver
import com.dv.moneym.data.banksync.createEnableBankingClient
import org.koin.core.module.Module
import org.koin.dsl.module

val bankSyncCommonModule: Module = module {
    single { EnableBankingCredentialsStore(secureStore = get()) }
    single { ExternalIdResolver() }
    single<EnableBankingClient> {
        createEnableBankingClient(
            httpClient = get(),
            credentialsStore = get(),
            clock = get(),
        )
    }
}
