package com.dv.moneym.di

import com.dv.moneym.core.oauth.GoogleAuthManager
import com.dv.moneym.core.oauth.GoogleOAuthConfig
import com.dv.moneym.core.oauth.IosGoogleAuthManager
import org.koin.core.module.Module
import org.koin.dsl.module
import platform.Foundation.NSBundle

actual fun remoteBackupPlatformModule(): Module = module {
    single {
        val clientId = NSBundle.mainBundle.objectForInfoDictionaryKey("GIDClientID") as? String
        GoogleOAuthConfig(
            clientId = clientId?.takeIf { it.isNotBlank() },
        )
    }
    single<GoogleAuthManager> {
        IosGoogleAuthManager(
            config = get(),
            appSettings = get(),
        )
    }
}
