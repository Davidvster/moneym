package com.dv.moneym.di

import android.content.Context
import com.dv.moneym.core.ai.AiEngine
import com.dv.moneym.core.ai.AndroidLocalLlmRunner
import com.dv.moneym.core.ai.GeminiNanoAiEngine
import com.dv.moneym.core.ai.LocalLlmRunner
import com.dv.moneym.core.common.LocaleController
import com.dv.moneym.core.security.AndroidSecureStore
import com.dv.moneym.core.security.BiometricAuthenticator
import com.dv.moneym.core.security.BiometricAuthenticatorImpl
import com.dv.moneym.core.security.SecureStore
import com.dv.moneym.data.accounts.createAccountsDatabase
import com.dv.moneym.data.budgets.createBudgetsDatabase
import com.dv.moneym.data.categories.createCategoriesDatabase
import com.dv.moneym.data.transactions.createTransactionsDatabase
import com.dv.moneym.locale.AndroidLocaleController
import com.dv.moneym.platform.DbPlatform
import com.dv.moneym.platform.FilePlatform
import org.koin.dsl.module

fun androidPlatformModule(context: Context) = module {
    single<Context> { context }
    single { createCategoriesDatabase(context) }
    single { createAccountsDatabase(context) }
    single { createTransactionsDatabase(context) }
    single { createBudgetsDatabase(context) }
    single<SecureStore> { AndroidSecureStore(context) }
    single<BiometricAuthenticator> { BiometricAuthenticatorImpl() }
    single<LocaleController> { AndroidLocaleController(get()) }
    single { FilePlatform(context) }
    single { DbPlatform(context) }
    single<AiEngine> { GeminiNanoAiEngine() }
    single<LocalLlmRunner> { AndroidLocalLlmRunner(get()) }
}
