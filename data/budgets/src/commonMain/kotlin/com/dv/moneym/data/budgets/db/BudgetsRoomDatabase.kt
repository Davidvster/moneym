package com.dv.moneym.data.budgets.db

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

@Suppress("NO_ACTUAL_FOR_EXPECT")
expect object BudgetsRoomDatabaseConstructor : RoomDatabaseConstructor<BudgetsRoomDatabase>

@Database(entities = [BudgetEntity::class], version = 3)
@ConstructedBy(BudgetsRoomDatabaseConstructor::class)
abstract class BudgetsRoomDatabase : RoomDatabase() {
    abstract fun budgetDao(): BudgetDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(connection: SQLiteConnection) {
                connection.execSQL("ALTER TABLE Budget ADD COLUMN account_id INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(connection: SQLiteConnection) {
                connection.execSQL("ALTER TABLE Budget ADD COLUMN sync_id TEXT")
                connection.execSQL("ALTER TABLE Budget ADD COLUMN deleted INTEGER NOT NULL DEFAULT 0")
                connection.execSQL("UPDATE Budget SET sync_id = lower(hex(randomblob(16))) WHERE sync_id IS NULL")
            }
        }
    }
}
