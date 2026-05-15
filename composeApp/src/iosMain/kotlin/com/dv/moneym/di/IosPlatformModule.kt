package com.dv.moneym.di

import com.dv.moneym.core.database.IosSqlDriverFactory
import com.dv.moneym.core.database.SqlDriverFactory
import com.dv.moneym.core.security.BiometricAuthenticator
import com.dv.moneym.core.security.BiometricAuthenticatorImpl
import com.dv.moneym.core.security.IosSecureStore
import com.dv.moneym.core.security.SecureStore
import org.koin.dsl.module

fun iosPlatformModule() = module {
    single<SqlDriverFactory> { IosSqlDriverFactory() }
    single<SecureStore> { IosSecureStore() }
    single<BiometricAuthenticator> { BiometricAuthenticatorImpl() }
}
