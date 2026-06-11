package com.dv.moneym.data.transactions

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.dv.moneym.data.transactions.db.TransactionsRoomDatabase
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.Dispatchers
import platform.Foundation.NSApplicationSupportDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDomainMask

@OptIn(ExperimentalForeignApi::class)
fun createTransactionsDatabase(): TransactionsRoomDatabase {
    val appSupport = NSFileManager.defaultManager.URLForDirectory(
        NSApplicationSupportDirectory, NSUserDomainMask, null, true, null
    )!!.path!!
    NSFileManager.defaultManager.createDirectoryAtPath(
        appSupport, withIntermediateDirectories = true, attributes = null, error = null
    )
    return Room.databaseBuilder<TransactionsRoomDatabase>(name = "$appSupport/moneym_transactions.db")
        .setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(Dispatchers.Default)
        .addMigrations(
            TransactionsRoomDatabase.MIGRATION_1_2,
            TransactionsRoomDatabase.MIGRATION_2_3,
            TransactionsRoomDatabase.MIGRATION_3_4,
        )
        .build()
}
