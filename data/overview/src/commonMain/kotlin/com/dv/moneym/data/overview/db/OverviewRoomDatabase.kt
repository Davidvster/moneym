package com.dv.moneym.data.overview.db

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor

@Suppress("NO_ACTUAL_FOR_EXPECT")
expect object OverviewRoomDatabaseConstructor : RoomDatabaseConstructor<OverviewRoomDatabase>

@Database(
    entities = [OverviewLayoutBlockEntity::class, OverviewAiWidgetEntity::class],
    version = 1,
)
@ConstructedBy(OverviewRoomDatabaseConstructor::class)
abstract class OverviewRoomDatabase : RoomDatabase() {
    abstract fun overviewDao(): OverviewDao
}
