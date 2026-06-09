package com.dv.moneym.core.testing

import com.dv.moneym.core.ai.ChatMessage
import com.dv.moneym.data.aichat.AiChatRepository
import com.dv.moneym.data.aichat.ChatConversation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

class FakeAiChatRepository : AiChatRepository {
    private val conversations = MutableStateFlow<List<ChatConversation>>(emptyList())
    private val messages = mutableMapOf<Long, List<ChatMessage>>()
    private var nextId = 1L

    override fun observeConversations(): Flow<List<ChatConversation>> =
        conversations.map { list -> list.sortedByDescending { it.updatedAt } }

    override suspend fun createConversation(title: String, engineId: String?, year: Int, month: Int, now: Long): Long {
        val id = nextId++
        conversations.update { it + ChatConversation(id, title, now, now) }
        messages[id] = emptyList()
        return id
    }

    override suspend fun loadMessages(conversationId: Long): List<ChatMessage> =
        messages[conversationId].orEmpty()

    override suspend fun replaceMessages(conversationId: Long, messages: List<ChatMessage>, now: Long) {
        this.messages[conversationId] = messages
        conversations.update { list ->
            list.map { if (it.id == conversationId) it.copy(updatedAt = now) else it }
        }
    }

    override suspend fun delete(conversationId: Long) {
        messages.remove(conversationId)
        conversations.update { list -> list.filterNot { it.id == conversationId } }
    }
}
