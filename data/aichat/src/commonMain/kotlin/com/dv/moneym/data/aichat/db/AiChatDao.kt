package com.dv.moneym.data.aichat.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface AiChatDao {
    @Query("SELECT * FROM ChatConversation WHERE deleted = 0 ORDER BY updated_at DESC")
    fun observeConversations(): Flow<List<ChatConversationEntity>>

    @Insert
    suspend fun insertConversation(entity: ChatConversationEntity): Long

    @Update
    suspend fun updateConversation(entity: ChatConversationEntity)

    @Query("SELECT * FROM ChatConversation WHERE id = :id")
    suspend fun conversationById(id: Long): ChatConversationEntity?

    @Query("UPDATE ChatConversation SET title = :title, updated_at = :now WHERE id = :id")
    suspend fun touchConversation(id: Long, title: String, now: Long)

    @Query("DELETE FROM ChatConversation WHERE id = :id")
    suspend fun deleteConversation(id: Long)

    @Query("SELECT * FROM ChatMessageRow WHERE conversation_id = :conversationId ORDER BY order_index ASC")
    suspend fun messagesFor(conversationId: Long): List<ChatMessageEntity>

    @Query("DELETE FROM ChatMessageRow WHERE conversation_id = :conversationId")
    suspend fun deleteMessagesFor(conversationId: Long)

    @Insert
    suspend fun insertMessages(messages: List<ChatMessageEntity>)
}
