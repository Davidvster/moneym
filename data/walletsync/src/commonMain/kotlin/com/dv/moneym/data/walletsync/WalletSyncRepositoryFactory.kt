package com.dv.moneym.data.walletsync

import com.dv.moneym.data.walletsync.db.WalletSyncRoomDatabase
import com.dv.moneym.data.walletsync.internal.WalletSyncRepositoryImpl

fun createWalletSyncRepository(db: WalletSyncRoomDatabase): WalletSyncRepository =
    WalletSyncRepositoryImpl(db)
