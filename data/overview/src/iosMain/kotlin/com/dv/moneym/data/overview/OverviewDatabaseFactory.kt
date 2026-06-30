package com.dv.moneym.data.overview

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.dv.moneym.data.overview.db.OverviewRoomDatabase
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.Dispatchers
import platform.Foundation.NSApplicationSupportDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDomainMask

@OptIn(ExperimentalForeignApi::class)
fun createOverviewDatabase(): OverviewRoomDatabase {
    val appSupport = NSFileManager.defaultManager.URLForDirectory(
        NSApplicationSupportDirectory, NSUserDomainMask, null, true, null
    )!!.path!!
    NSFileManager.defaultManager.createDirectoryAtPath(
        appSupport, withIntermediateDirectories = true, attributes = null, error = null
    )
    return Room.databaseBuilder<OverviewRoomDatabase>(name = "$appSupport/moneym_overview.db")
        .setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(Dispatchers.Default)
        .build()
}
