package com.dv.moneym.data.categories.db

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor

@Suppress("NO_ACTUAL_FOR_EXPECT")
expect object CategoriesRoomDatabaseConstructor : RoomDatabaseConstructor<CategoriesRoomDatabase>

@Database(entities = [CategoryEntity::class], version = 1)
@ConstructedBy(CategoriesRoomDatabaseConstructor::class)
abstract class CategoriesRoomDatabase : RoomDatabase() {
    abstract fun categoryDao(): CategoryDao
}
