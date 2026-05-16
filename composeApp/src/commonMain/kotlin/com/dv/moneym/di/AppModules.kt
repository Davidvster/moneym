package com.dv.moneym.di

import com.dv.moneym.core.common.AppClock
import com.dv.moneym.core.common.DefaultAppClock
import com.dv.moneym.core.common.DefaultDispatcherProvider
import com.dv.moneym.core.common.DispatcherProvider
import com.dv.moneym.core.datastore.AppSettings
import com.dv.moneym.core.datastore.AppSettingsRepository
import com.dv.moneym.core.datastore.DefaultAppSettings
import com.dv.moneym.core.datastore.DefaultAppSettingsRepository
import com.russhwolf.settings.Settings
import org.koin.core.module.Module
import org.koin.dsl.module

val coreCommonModule: Module = module {
    single<DispatcherProvider> { DefaultDispatcherProvider() }
    single<AppClock> { DefaultAppClock() }
}

val coreDatastoreModule: Module = module {
    single { Settings() }
    single<AppSettings> { DefaultAppSettings(get()) }
    single<AppSettingsRepository> { DefaultAppSettingsRepository(get()) }
}

val appModules: List<Module> = listOf(
    coreCommonModule,
    coreDatastoreModule,
    coreSecurityModule,
    dataCategoriesModule,
    dataAccountsModule,
    dataTransactionsModule,
    dataBackupModule,
    featureTransactionsModule,
    featureTransactionEditModule,
    featureSecurityModule,
    featureSettingsModule,
    featureCategoriesModule,
    featureOnboardingModule,
    featureOverviewModule,
)
