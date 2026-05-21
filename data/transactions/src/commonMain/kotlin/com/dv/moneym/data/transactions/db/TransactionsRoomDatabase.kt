package com.dv.moneym.data.transactions.db

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor

@Suppress("NO_ACTUAL_FOR_EXPECT")
expect object TransactionsRoomDatabaseConstructor : RoomDatabaseConstructor<TransactionsRoomDatabase>

@Database(entities = [TransactionEntity::class, PaymentModeEntity::class], version = 1)
@ConstructedBy(TransactionsRoomDatabaseConstructor::class)
abstract class TransactionsRoomDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
    abstract fun paymentModeDao(): PaymentModeDao
}
