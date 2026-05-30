package com.dv.moneym.di

import com.dv.moneym.core.oauth.GoogleAuthManager
import com.dv.moneym.core.security.BackupCrypto
import com.dv.moneym.core.security.DefaultBackupCrypto
import com.dv.moneym.data.remotebackup.RemoteBackupManager
import com.dv.moneym.data.remotebackup.RemoteBackupProvider
import com.dv.moneym.data.remotebackup.SessionPassphrase
import com.dv.moneym.data.remotebackup.google.GoogleDriveBackupClient
import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.koin.core.module.Module
import org.koin.dsl.module

private const val APP_VERSION = "1.0"

expect fun remoteBackupPlatformModule(): Module

internal expect fun createHttpEngine(): HttpClientEngine

val remoteBackupCommonModule: Module = module {
    single<BackupCrypto> { DefaultBackupCrypto(dispatchers = get()) }
    single { SessionPassphrase() }
    single {
        HttpClient(createHttpEngine()) {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true; isLenient = true })
            }
        }
    }
    single<RemoteBackupProvider> {
        GoogleDriveBackupClient(
            httpClient = get(),
            accessTokenProvider = { get<GoogleAuthManager>().accessToken() },
        )
    }
    single {
        RemoteBackupManager(
            dbBackupManager = get(),
            crypto = get(),
            provider = get(),
            appSettings = get(),
            sessionPassphrase = get(),
            dispatchers = get(),
            appVersion = APP_VERSION,
        )
    }
}
