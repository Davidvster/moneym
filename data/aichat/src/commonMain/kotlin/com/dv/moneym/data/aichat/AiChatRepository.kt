package com.dv.moneym.data.aichat

import com.dv.moneym.core.ai.ChatMessage
import kotlinx.coroutines.flow.Flow

interface AiChatRepository {
    fun observeConversations(): Flow<List<ChatConversation>>
    suspend fun createConversation(title: String, engineId: String?, year: Int, month: Int, now: Long): Long
    suspend fun loadMessages(conversationId: Long): List<ChatMessage>
    suspend fun replaceMessages(conversationId: Long, messages: List<ChatMessage>, now: Long)
    suspend fun delete(conversationId: Long)
}
