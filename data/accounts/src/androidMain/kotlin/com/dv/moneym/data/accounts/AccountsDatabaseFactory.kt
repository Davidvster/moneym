package com.dv.moneym.data.accounts

import android.content.Context
import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.dv.moneym.data.accounts.db.AccountsRoomDatabase
import kotlinx.coroutines.Dispatchers

fun createAccountsDatabase(context: Context): AccountsRoomDatabase =
    Room.databaseBuilder<AccountsRoomDatabase>(context = context, name = "moneym_accounts.db")
        .setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(Dispatchers.IO)
        .build()
