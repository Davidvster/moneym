package com.dv.moneym.data.transactions

import android.content.Context
import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.dv.moneym.data.transactions.db.TransactionsRoomDatabase
import kotlinx.coroutines.Dispatchers

fun createTransactionsDatabase(context: Context): TransactionsRoomDatabase =
    Room.databaseBuilder<TransactionsRoomDatabase>(context = context, name = "moneym_transactions.db")
        .fallbackToDestructiveMigrationFrom(false, 1)
        .setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(Dispatchers.IO)
        .build()
