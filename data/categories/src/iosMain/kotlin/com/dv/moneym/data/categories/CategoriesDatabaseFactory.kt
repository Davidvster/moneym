package com.dv.moneym.data.categories

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.dv.moneym.data.categories.db.CategoriesRoomDatabase
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.Dispatchers
import platform.Foundation.NSApplicationSupportDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDomainMask

@OptIn(ExperimentalForeignApi::class)
fun createCategoriesDatabase(): CategoriesRoomDatabase {
    val appSupport = NSFileManager.defaultManager.URLForDirectory(
        NSApplicationSupportDirectory, NSUserDomainMask, null, true, null
    )!!.path!!
    NSFileManager.defaultManager.createDirectoryAtPath(
        appSupport, withIntermediateDirectories = true, attributes = null, error = null
    )
    return Room.databaseBuilder<CategoriesRoomDatabase>(name = "$appSupport/moneym_categories.db")
        .setDriver(BundledSQLiteDriver())
        .addMigrations(CategoriesRoomDatabase.MIGRATION_1_2, CategoriesRoomDatabase.MIGRATION_2_3)
        .setQueryCoroutineContext(Dispatchers.Default)
        .build()
}
