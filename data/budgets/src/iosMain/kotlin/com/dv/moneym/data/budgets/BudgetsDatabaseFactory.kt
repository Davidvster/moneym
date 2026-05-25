package com.dv.moneym.data.budgets

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.dv.moneym.data.budgets.db.BudgetsRoomDatabase
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.Dispatchers
import platform.Foundation.NSApplicationSupportDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDomainMask

@OptIn(ExperimentalForeignApi::class)
fun createBudgetsDatabase(): BudgetsRoomDatabase {
    val appSupport = NSFileManager.defaultManager.URLForDirectory(
        NSApplicationSupportDirectory, NSUserDomainMask, null, true, null
    )!!.path!!
    NSFileManager.defaultManager.createDirectoryAtPath(
        appSupport, withIntermediateDirectories = true, attributes = null, error = null
    )
    return Room.databaseBuilder<BudgetsRoomDatabase>(name = "$appSupport/moneym_budgets.db")
        .setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(Dispatchers.Default)
        .addMigrations(BudgetsRoomDatabase.MIGRATION_1_2)
        .build()
}
