package com.dv.moneym.data.categories

import android.content.Context
import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.dv.moneym.data.categories.db.CategoriesRoomDatabase
import kotlinx.coroutines.Dispatchers

fun createCategoriesDatabase(context: Context): CategoriesRoomDatabase =
    Room.databaseBuilder<CategoriesRoomDatabase>(context = context, name = "moneym_categories.db")
        .setDriver(BundledSQLiteDriver())
        .addMigrations(CategoriesRoomDatabase.MIGRATION_1_2, CategoriesRoomDatabase.MIGRATION_2_3)
        .setQueryCoroutineContext(Dispatchers.IO)
        .build()
