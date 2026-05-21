package com.dv.moneym.di

import android.content.Context
import com.dv.moneym.core.common.LocaleController
import com.dv.moneym.core.security.AndroidSecureStore
import com.dv.moneym.core.security.BiometricAuthenticator
import com.dv.moneym.core.security.BiometricAuthenticatorImpl
import com.dv.moneym.core.security.SecureStore
import com.dv.moneym.data.accounts.createAccountsDatabase
import com.dv.moneym.data.categories.createCategoriesDatabase
import com.dv.moneym.data.transactions.createTransactionsDatabase
import com.dv.moneym.locale.AndroidLocaleController
import com.dv.moneym.platform.DbPlatform
import com.dv.moneym.platform.FilePlatform
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

fun androidPlatformModule(context: Context) = module {
    single { createCategoriesDatabase(androidContext()) }
    single { createAccountsDatabase(androidContext()) }
    single { createTransactionsDatabase(androidContext()) }
    single<SecureStore> { AndroidSecureStore(context) }
    single<BiometricAuthenticator> { BiometricAuthenticatorImpl() }
    single<LocaleController> { AndroidLocaleController(get()) }
    single { FilePlatform(context) }
    single { DbPlatform(context) }
}
