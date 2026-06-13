package com.dv.moneym.data.walletsync

import android.content.Context
import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.dv.moneym.data.walletsync.db.WalletSyncRoomDatabase
import kotlinx.coroutines.Dispatchers

fun createWalletSyncDatabase(context: Context): WalletSyncRoomDatabase =
    Room.databaseBuilder<WalletSyncRoomDatabase>(context = context, name = "moneym_walletsync.db")
        .setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(Dispatchers.IO)
        .build()
