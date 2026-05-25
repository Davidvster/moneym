package com.dv.moneym.data.budgets.db

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor

@Suppress("NO_ACTUAL_FOR_EXPECT")
expect object BudgetsRoomDatabaseConstructor : RoomDatabaseConstructor<BudgetsRoomDatabase>

@Database(entities = [BudgetEntity::class], version = 1)
@ConstructedBy(BudgetsRoomDatabaseConstructor::class)
abstract class BudgetsRoomDatabase : RoomDatabase() {
    abstract fun budgetDao(): BudgetDao
}
