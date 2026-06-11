package com.dv.moneym.data.banksync.db

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor

@Suppress("NO_ACTUAL_FOR_EXPECT")
expect object BankSyncRoomDatabaseConstructor : RoomDatabaseConstructor<BankSyncRoomDatabase>

@Database(
    entities = [BankAccountEntity::class, BankSuggestionEntity::class],
    version = 1,
)
@ConstructedBy(BankSyncRoomDatabaseConstructor::class)
abstract class BankSyncRoomDatabase : RoomDatabase() {
    abstract fun bankAccountDao(): BankAccountDao
    abstract fun bankSuggestionDao(): BankSuggestionDao
}
