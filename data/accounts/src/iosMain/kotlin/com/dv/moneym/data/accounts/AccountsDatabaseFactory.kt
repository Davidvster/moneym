package com.dv.moneym.data.accounts

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.dv.moneym.data.accounts.db.AccountsRoomDatabase
import com.dv.moneym.data.accounts.db.MIGRATION_ACCOUNTS_1_2
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.Dispatchers
import platform.Foundation.NSApplicationSupportDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDomainMask

@OptIn(ExperimentalForeignApi::class)
fun createAccountsDatabase(): AccountsRoomDatabase {
    val appSupport = NSFileManager.defaultManager.URLForDirectory(
        NSApplicationSupportDirectory, NSUserDomainMask, null, true, null
    )!!.path!!
    NSFileManager.defaultManager.createDirectoryAtPath(
        appSupport, withIntermediateDirectories = true, attributes = null, error = null
    )
    return Room.databaseBuilder<AccountsRoomDatabase>(name = "$appSupport/moneym_accounts.db")
        .setDriver(BundledSQLiteDriver())
        .addMigrations(MIGRATION_ACCOUNTS_1_2)
        .setQueryCoroutineContext(Dispatchers.Default)
        .build()
}
