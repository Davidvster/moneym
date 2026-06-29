package com.dv.moneym.di

import com.dv.moneym.core.ai.AiEngine
import com.dv.moneym.core.ai.IosFoundationModelsAiEngine
import com.dv.moneym.core.ai.IosLocalLlmRunner
import com.dv.moneym.core.ai.LocalLlmRunner
import com.dv.moneym.core.common.LocalModelRuntime
import com.dv.moneym.core.common.LocaleController
import com.dv.moneym.core.security.BiometricAuthenticator
import com.dv.moneym.core.security.BiometricAuthenticatorImpl
import com.dv.moneym.core.security.IosSecureStore
import com.dv.moneym.core.security.SecureStore
import com.dv.moneym.data.accounts.createAccountsDatabase
import com.dv.moneym.data.aichat.createAiChatDatabase
import com.dv.moneym.data.banksync.createBankSyncDatabase
import com.dv.moneym.data.walletsync.createWalletSyncDatabase
import com.dv.moneym.data.budgets.createBudgetsDatabase
import com.dv.moneym.data.categories.createCategoriesDatabase
import com.dv.moneym.data.transactions.createTransactionsDatabase
import com.dv.moneym.locale.IosLocaleController
import com.dv.moneym.platform.AppInfo
import com.dv.moneym.platform.DbPlatform
import com.dv.moneym.platform.FilePlatform
import com.dv.moneym.platform.InstalledAppsProvider
import com.dv.moneym.platform.NoopInstalledAppsProvider
import com.dv.moneym.platform.NoopNotificationAccessController
import com.dv.moneym.platform.NotificationAccessController
import org.koin.core.qualifier.named
import org.koin.dsl.module

fun iosPlatformModule() = module {
    single { createCategoriesDatabase() }
    single { createAccountsDatabase() }
    single { createTransactionsDatabase() }
    single { createBudgetsDatabase() }
    single { createAiChatDatabase() }
    single { createBankSyncDatabase() }
    single { createWalletSyncDatabase() }
    single<NotificationAccessController> { NoopNotificationAccessController() }
    single<InstalledAppsProvider> { NoopInstalledAppsProvider() }
    single<SecureStore> { IosSecureStore() }
    single<BiometricAuthenticator> { BiometricAuthenticatorImpl() }
    single<LocaleController> { IosLocaleController() }
    single { AppInfo() }
    single { FilePlatform() }
    single { DbPlatform() }
    single<AiEngine>(named("appleIntelligence")) { IosFoundationModelsAiEngine() }
    single { IosLocalLlmRunner() }
    single<LocalLlmRunner> { get<IosLocalLlmRunner>() }
    single<LocalModelRuntime> { get<IosLocalLlmRunner>() }
}
