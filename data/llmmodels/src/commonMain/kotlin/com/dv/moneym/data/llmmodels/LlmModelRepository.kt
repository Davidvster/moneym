package com.dv.moneym.data.llmmodels

import kotlinx.coroutines.flow.Flow

interface LlmModelRepository {
    fun observeModels(): Flow<List<LlmModelState>>
    suspend fun download(id: String)
    fun cancel(id: String)
    suspend fun delete(id: String)
    suspend fun setActive(id: String)
    suspend fun activeModelPath(): String?
}
