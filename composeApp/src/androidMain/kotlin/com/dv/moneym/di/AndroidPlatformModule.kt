package com.dv.moneym.di

import android.content.Context
import com.dv.moneym.core.database.AndroidSqlDriverFactory
import com.dv.moneym.core.database.SqlDriverFactory
import com.dv.moneym.core.security.AndroidSecureStore
import com.dv.moneym.core.security.BiometricAuthenticator
import com.dv.moneym.core.security.BiometricAuthenticatorImpl
import com.dv.moneym.core.security.SecureStore
import org.koin.dsl.module

fun androidPlatformModule(context: Context) = module {
    single<SqlDriverFactory> { AndroidSqlDriverFactory(context) }
    single<SecureStore> { AndroidSecureStore(context) }
    single<BiometricAuthenticator> { BiometricAuthenticatorImpl() }
}
