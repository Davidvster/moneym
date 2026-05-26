package com.dv.moneym.di

import com.dv.moneym.core.oauth.AuthorizationLauncher
import com.dv.moneym.core.oauth.GoogleOAuthConfig
import com.dv.moneym.core.oauth.IosAuthorizationLauncher
import com.dv.moneym.core.oauth.IosSecureTokenStore
import com.dv.moneym.core.oauth.SecureTokenStore
import org.koin.core.module.Module
import org.koin.dsl.module
import platform.Foundation.NSBundle

actual fun remoteBackupPlatformModule(): Module = module {
    single {
        val clientId = NSBundle.mainBundle.objectForInfoDictionaryKey("GoogleOAuthClientId") as? String
        GoogleOAuthConfig(
            clientId = clientId?.takeIf { it.isNotBlank() },
            redirectUri = GoogleOAuthConfig.DEFAULT_REDIRECT,
        )
    }
    single<AuthorizationLauncher> { IosAuthorizationLauncher() }
    single<SecureTokenStore> { IosSecureTokenStore(dispatchers = get()) }
}
