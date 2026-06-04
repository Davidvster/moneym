package com.dv.moneym.data.categories.db

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

@Suppress("NO_ACTUAL_FOR_EXPECT")
expect object CategoriesRoomDatabaseConstructor : RoomDatabaseConstructor<CategoriesRoomDatabase>

@Database(entities = [CategoryEntity::class], version = 3)
@ConstructedBy(CategoriesRoomDatabaseConstructor::class)
abstract class CategoriesRoomDatabase : RoomDatabase() {
    abstract fun categoryDao(): CategoryDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(connection: SQLiteConnection) {
                connection.execSQL("ALTER TABLE Category ADD COLUMN sync_id TEXT")
                connection.execSQL("ALTER TABLE Category ADD COLUMN deleted INTEGER NOT NULL DEFAULT 0")
                connection.execSQL("UPDATE Category SET sync_id = lower(hex(randomblob(16))) WHERE sync_id IS NULL")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(connection: SQLiteConnection) {
                connection.execSQL("ALTER TABLE Category ADD COLUMN sort_order INTEGER NOT NULL DEFAULT 0")
                connection.execSQL("UPDATE Category SET sort_order = id")
            }
        }
    }
}
