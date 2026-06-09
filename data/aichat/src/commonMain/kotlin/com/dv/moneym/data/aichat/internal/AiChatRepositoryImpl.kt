package com.dv.moneym.data.aichat.internal

import com.dv.moneym.core.ai.ChatMessage
import com.dv.moneym.core.ai.ChatRole
import com.dv.moneym.data.aichat.AiChatRepository
import com.dv.moneym.data.aichat.ChatConversation
import com.dv.moneym.data.aichat.db.AiChatRoomDatabase
import com.dv.moneym.data.aichat.db.ChatConversationEntity
import com.dv.moneym.data.aichat.db.ChatMessageEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

internal class AiChatRepositoryImpl(
    private val db: AiChatRoomDatabase,
) : AiChatRepository {

    private val dao get() = db.aiChatDao()

    override fun observeConversations(): Flow<List<ChatConversation>> =
        dao.observeConversations().map { rows ->
            rows.map { ChatConversation(it.id, it.title, it.createdAt, it.updatedAt) }
        }

    @OptIn(ExperimentalUuidApi::class)
    override suspend fun createConversation(title: String, engineId: String?, year: Int, month: Int, now: Long): Long =
        dao.insertConversation(
            ChatConversationEntity(
                title = title,
                engineId = engineId,
                year = year,
                month = month,
                createdAt = now,
                updatedAt = now,
                syncId = Uuid.random().toString(),
            )
        )

    override suspend fun loadMessages(conversationId: Long): List<ChatMessage> =
        dao.messagesFor(conversationId).map { ChatMessage(ChatRole.valueOf(it.role), it.content) }

    override suspend fun replaceMessages(conversationId: Long, messages: List<ChatMessage>, now: Long) {
        dao.deleteMessagesFor(conversationId)
        dao.insertMessages(
            messages.mapIndexed { index, message ->
                ChatMessageEntity(
                    conversationId = conversationId,
                    role = message.role.name,
                    content = message.content,
                    orderIndex = index,
                    createdAt = now,
                )
            }
        )
        dao.conversationById(conversationId)?.let { dao.touchConversation(conversationId, it.title, now) }
    }

    override suspend fun delete(conversationId: Long) {
        dao.deleteMessagesFor(conversationId)
        dao.deleteConversation(conversationId)
    }
}
