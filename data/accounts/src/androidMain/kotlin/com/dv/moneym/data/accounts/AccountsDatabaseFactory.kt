package com.dv.moneym.data.accounts

import android.content.Context
import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.dv.moneym.data.accounts.db.AccountsRoomDatabase
import com.dv.moneym.data.accounts.db.MIGRATION_ACCOUNTS_1_2
import com.dv.moneym.data.accounts.db.MIGRATION_ACCOUNTS_2_3
import kotlinx.coroutines.Dispatchers

fun createAccountsDatabase(context: Context): AccountsRoomDatabase =
    Room.databaseBuilder<AccountsRoomDatabase>(context = context, name = "moneym_accounts.db")
        .setDriver(BundledSQLiteDriver())
        .addMigrations(MIGRATION_ACCOUNTS_1_2, MIGRATION_ACCOUNTS_2_3)
        .setQueryCoroutineContext(Dispatchers.IO)
        .build()
