package com.dv.moneym.di

import com.dv.moneym.data.sync.DeviceIdentity
import com.dv.moneym.data.sync.PendingDeletionStore
import com.dv.moneym.data.sync.SyncApplier
import com.dv.moneym.data.sync.SyncDeletionController
import com.dv.moneym.data.sync.SyncEngine
import com.dv.moneym.data.sync.SyncExporter
import com.dv.moneym.data.sync.SyncReconciler
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
    single { SyncReconciler() }
    single { SyncApplier(get(), get(), get(), get(), get(), get()) }
    single { PendingDeletionStore(appSettings = get()) }
    single {
        SyncEngine(
            exporter = get(),
            reconciler = get(),
            applier = get(),
            codec = get(),
            store = get(),
            appSettings = get(),
            sessionPassphrase = get(),
            dispatchers = get(),
            pendingDeletionStore = get(),
            accountRepository = get(),
            categoryRepository = get(),
            paymentModeRepository = get(),
            transactionRepository = get(),
            recurringTransactionRepository = get(),
            budgetRepository = get(),
        )
    }
    single<SyncDeletionController> { get<SyncEngine>() }
}
