package com.dv.moneym.data.budgets

import android.content.Context
import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.dv.moneym.data.budgets.db.BudgetsRoomDatabase
import kotlinx.coroutines.Dispatchers

fun createBudgetsDatabase(context: Context): BudgetsRoomDatabase =
    Room.databaseBuilder<BudgetsRoomDatabase>(context = context, name = "moneym_budgets.db")
        .setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(Dispatchers.IO)
        .build()
