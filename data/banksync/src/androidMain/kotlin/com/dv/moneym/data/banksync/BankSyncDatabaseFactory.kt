package com.dv.moneym.data.banksync

import android.content.Context
import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.dv.moneym.data.banksync.db.BankSyncRoomDatabase
import kotlinx.coroutines.Dispatchers

fun createBankSyncDatabase(context: Context): BankSyncRoomDatabase =
    Room.databaseBuilder<BankSyncRoomDatabase>(context = context, name = "moneym_banksync.db")
        .setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(Dispatchers.IO)
        .build()
