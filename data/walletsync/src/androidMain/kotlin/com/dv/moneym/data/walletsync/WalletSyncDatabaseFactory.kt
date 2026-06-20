package com.dv.moneym.data.walletsync

import android.content.Context
import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.dv.moneym.data.walletsync.db.MIGRATION_WALLET_SYNC_1_2
import com.dv.moneym.data.walletsync.db.WalletSyncRoomDatabase
import kotlinx.coroutines.Dispatchers

fun createWalletSyncDatabase(context: Context): WalletSyncRoomDatabase =
    Room.databaseBuilder<WalletSyncRoomDatabase>(context = context, name = "moneym_walletsync.db")
        .setDriver(BundledSQLiteDriver())
        .addMigrations(MIGRATION_WALLET_SYNC_1_2)
        .setQueryCoroutineContext(Dispatchers.IO)
        .build()
