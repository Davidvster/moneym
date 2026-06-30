package com.dv.moneym.data.overview

import android.content.Context
import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.dv.moneym.data.overview.db.OverviewRoomDatabase
import kotlinx.coroutines.Dispatchers

fun createOverviewDatabase(context: Context): OverviewRoomDatabase =
    Room.databaseBuilder<OverviewRoomDatabase>(context = context, name = "moneym_overview.db")
        .setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(Dispatchers.IO)
        .build()
