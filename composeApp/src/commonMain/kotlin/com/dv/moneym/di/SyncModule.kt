package com.dv.moneym.di

import com.dv.moneym.data.sync.DeviceIdentity
import com.dv.moneym.data.sync.SyncExporter
import com.dv.moneym.data.sync.SyncRemoteStore
import com.dv.moneym.data.sync.SyncSnapshotCodec
import org.koin.core.module.Module
import org.koin.dsl.module

private const val APP_VERSION = "1.0"

val syncCommonModule: Module = module {
    single { DeviceIdentity(appSettings = get()) }
    single { SyncSnapshotCodec(crypto = get(), appVersion = APP_VERSION) }
    single { SyncRemoteStore(provider = get()) }
    single { SyncExporter(get(), get(), get(), get(), get(), get(), get()) }
}
