package com.dv.moneym.data.accounts.db

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor

@Suppress("NO_ACTUAL_FOR_EXPECT")
expect object AccountsRoomDatabaseConstructor : RoomDatabaseConstructor<AccountsRoomDatabase>

@Database(entities = [AccountEntity::class], version = 3)
@ConstructedBy(AccountsRoomDatabaseConstructor::class)
abstract class AccountsRoomDatabase : RoomDatabase() {
    abstract fun accountDao(): AccountDao
}
