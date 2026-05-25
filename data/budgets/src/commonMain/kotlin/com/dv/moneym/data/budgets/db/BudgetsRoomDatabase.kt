package com.dv.moneym.data.budgets.db

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Suppress("NO_ACTUAL_FOR_EXPECT")
expect object BudgetsRoomDatabaseConstructor : RoomDatabaseConstructor<BudgetsRoomDatabase>

@Database(entities = [BudgetEntity::class], version = 2)
@ConstructedBy(BudgetsRoomDatabaseConstructor::class)
abstract class BudgetsRoomDatabase : RoomDatabase() {
    abstract fun budgetDao(): BudgetDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE Budget ADD COLUMN account_id INTEGER NOT NULL DEFAULT 0")
            }
        }
    }
}
