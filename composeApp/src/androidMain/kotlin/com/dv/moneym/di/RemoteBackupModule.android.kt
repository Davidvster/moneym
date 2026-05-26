package com.dv.moneym.di

import android.content.Context
import com.dv.moneym.BuildConfig
import com.dv.moneym.core.oauth.AndroidAuthorizationLauncher
import com.dv.moneym.core.oauth.AndroidSecureTokenStore
import com.dv.moneym.core.oauth.AuthorizationLauncher
import com.dv.moneym.core.oauth.GoogleOAuthConfig
import com.dv.moneym.core.oauth.SecureTokenStore
import org.koin.core.module.Module
import org.koin.dsl.module

actual fun remoteBackupPlatformModule(): Module = module {
    single {
        GoogleOAuthConfig(
            clientId = BuildConfig.GOOGLE_OAUTH_CLIENT_ID,
            redirectUri = GoogleOAuthConfig.DEFAULT_REDIRECT,
        )
    }
    single<AuthorizationLauncher> { AndroidAuthorizationLauncher(get<Context>()) }
    single<SecureTokenStore> { AndroidSecureTokenStore(get<Context>(), dispatchers = get()) }
}
