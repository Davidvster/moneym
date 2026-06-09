package com.dv.moneym.data.aichat.db

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor

@Suppress("NO_ACTUAL_FOR_EXPECT")
expect object AiChatRoomDatabaseConstructor : RoomDatabaseConstructor<AiChatRoomDatabase>

@Database(
    entities = [ChatConversationEntity::class, ChatMessageEntity::class],
    version = 1,
)
@ConstructedBy(AiChatRoomDatabaseConstructor::class)
abstract class AiChatRoomDatabase : RoomDatabase() {
    abstract fun aiChatDao(): AiChatDao
}
