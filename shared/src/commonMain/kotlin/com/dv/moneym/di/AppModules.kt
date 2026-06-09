package com.dv.moneym.di

import com.dv.moneym.core.ai.AiEngine
import com.dv.moneym.core.ai.AiEngineRegistry
import com.dv.moneym.core.ai.LocalLlmAiEngine
import com.dv.moneym.core.common.AppClock
import com.dv.moneym.core.common.DefaultAppClock
import com.dv.moneym.core.common.DefaultDispatcherProvider
import com.dv.moneym.core.common.DispatcherProvider
import com.dv.moneym.core.common.TransactionSavedSignal
import com.dv.moneym.data.llmmodels.LlmModelRepository
import com.dv.moneym.core.datastore.AppSettings
import com.dv.moneym.core.datastore.AppSettingsRepository
import com.dv.moneym.core.datastore.DefaultAppSettings
import com.dv.moneym.core.datastore.DefaultAppSettingsRepository
import com.russhwolf.settings.Settings
import org.koin.core.module.Module
import org.koin.core.qualifier.named
import org.koin.dsl.module

val coreCommonModule: Module = module {
    single<DispatcherProvider> { DefaultDispatcherProvider() }
    single<AppClock> { DefaultAppClock() }
    single { TransactionSavedSignal() }
}

val coreAiModule: Module = module {
    single<AiEngine>(named("localLlm")) {
        LocalLlmAiEngine(
            runner = get(),
            activeModelPath = { get<LlmModelRepository>().activeModelPath() },
        )
    }
    single { AiEngineRegistry(getAll()) }
}

val coreDatastoreModule: Module = module {
    single { Settings() }
    single<AppSettings> { DefaultAppSettings(get()) }
    single<AppSettingsRepository> { DefaultAppSettingsRepository(get()) }
}

val appModules: List<Module> = listOf(
    coreCommonModule,
    coreAiModule,
    coreDatastoreModule,
    coreSecurityModule,
    remoteBackupCommonModule,
    remoteBackupPlatformModule(),
    syncCommonModule,
    dataCategoriesModule,
    dataAccountsModule,
    dataTransactionsModule,
    dataBudgetsModule,
    dataAichatModule,
    dataBackupModule,
    dataLlmModelsModule,
    featureTransactionsModule,
    featureTransactionEditModule,
    featureSecurityModule,
    featureSettingsModule,
    featureWalletModule,
    featureCategoriesModule,
    featureSyncModule,
    featureBudgetsModule,
    featureOnboardingModule,
    featureOverviewModule,
    featureAianalysisModule,
    featureAiModelsModule,
)
