package com.dv.moneym.core.ai

import kotlinx.coroutines.flow.Flow

interface AiEngine {
    val supportsTools: Boolean
    suspend fun availability(): AiAvailability
    fun streamReply(messages: List<ChatMessage>, grounding: Grounding): Flow<String>
}
