package com.dv.moneym.core.ai

import kotlinx.coroutines.flow.Flow

interface LocalLlmRunner {
    suspend fun isModelLoaded(): Boolean
    suspend fun loadModel(path: String): Boolean
    fun streamReply(prompt: String): Flow<String>
}
