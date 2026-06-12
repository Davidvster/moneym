package com.dv.moneym.di

import com.dv.moneym.data.banksync.BankAuthCallbackBus
import com.dv.moneym.data.banksync.BankSyncEngine
import com.dv.moneym.data.banksync.BankSyncRepository
import com.dv.moneym.data.banksync.EnableBankingClient
import com.dv.moneym.data.banksync.EnableBankingCredentialsStore
import com.dv.moneym.data.banksync.ExternalIdResolver
import com.dv.moneym.data.banksync.createBankSyncRepository
import com.dv.moneym.data.banksync.createEnableBankingClient
import org.koin.core.module.Module
import org.koin.dsl.module

val bankSyncCommonModule: Module = module {
    single { BankAuthCallbackBus() }
    single { EnableBankingCredentialsStore(secureStore = get()) }
    single { ExternalIdResolver() }
    single<EnableBankingClient> {
        createEnableBankingClient(
            httpClient = get(),
            credentialsStore = get(),
            clock = get(),
        )
    }
    single<BankSyncRepository> { createBankSyncRepository(db = get()) }
    single {
        BankSyncEngine(
            client = get(),
            credentialsStore = get(),
            bankSyncRepository = get(),
            transactionRepository = get(),
            externalIdResolver = get(),
            appSettings = get(),
            clock = get(),
        )
    }
}
