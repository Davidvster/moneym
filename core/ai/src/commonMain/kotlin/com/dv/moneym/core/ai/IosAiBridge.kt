package com.dv.moneym.core.ai

import kotlinx.coroutines.flow.Flow

interface IosAiBridge {
    suspend fun isAvailable(): Boolean
    fun streamReply(prompt: String): Flow<String>
}
