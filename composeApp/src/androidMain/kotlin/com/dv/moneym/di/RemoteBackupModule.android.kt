package com.dv.moneym.di

import android.content.Context
import com.dv.moneym.BuildConfig
import com.dv.moneym.core.oauth.AndroidGoogleAuthManager
import com.dv.moneym.core.oauth.GoogleAuthManager
import com.dv.moneym.core.oauth.GoogleOAuthConfig
import org.koin.core.module.Module
import org.koin.dsl.module

actual fun remoteBackupPlatformModule(): Module = module {
    single {
        GoogleOAuthConfig(
            clientId = null,
            serverClientId = BuildConfig.GOOGLE_OAUTH_SERVER_CLIENT_ID,
        )
    }
    single<GoogleAuthManager> {
        AndroidGoogleAuthManager(
            context = get<Context>(),
            config = get(),
            appSettings = get(),
            dispatchers = get(),
        )
    }
}
