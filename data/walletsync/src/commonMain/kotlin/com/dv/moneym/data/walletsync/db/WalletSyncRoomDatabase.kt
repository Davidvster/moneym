package com.dv.moneym.data.walletsync.db

import androidx.room.AutoMigration
import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor

@Suppress("NO_ACTUAL_FOR_EXPECT")
expect object WalletSyncRoomDatabaseConstructor : RoomDatabaseConstructor<WalletSyncRoomDatabase>

@Database(
    entities = [WalletSuggestionEntity::class],
    version = 2,
)
@ConstructedBy(WalletSyncRoomDatabaseConstructor::class)
abstract class WalletSyncRoomDatabase : RoomDatabase() {
    abstract fun walletSuggestionDao(): WalletSuggestionDao
}
