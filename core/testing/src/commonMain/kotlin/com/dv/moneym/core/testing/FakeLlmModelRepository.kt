package com.dv.moneym.core.testing

import com.dv.moneym.data.llmmodels.LlmModel
import com.dv.moneym.data.llmmodels.LlmModelCatalog
import com.dv.moneym.data.llmmodels.LlmModelRepository
import com.dv.moneym.data.llmmodels.LlmModelState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

class FakeLlmModelRepository(
    private val catalog: List<LlmModel> = LlmModelCatalog.models,
) : LlmModelRepository {

    private data class Entry(val downloaded: Boolean = false, val progress: Float? = null)

    private val entries = MutableStateFlow(catalog.associate { it.id to Entry() })
    private val activeId = MutableStateFlow<String?>(null)
    private val hasToken = MutableStateFlow(false)

    override fun observeModels(): Flow<List<LlmModelState>> =
        entries.map { map ->
            catalog.map { model ->
                val entry = map[model.id] ?: Entry()
                LlmModelState(
                    model = model,
                    downloaded = entry.downloaded,
                    active = model.id == activeId.value,
                    progress = entry.progress,
                )
            }
        }

    override suspend fun download(id: String) {
        entries.update { it + (id to Entry(downloaded = true, progress = null)) }
    }

    override fun cancel(id: String) {
        entries.update { it + (id to Entry(downloaded = false, progress = null)) }
    }

    override suspend fun delete(id: String) {
        entries.update { it + (id to Entry(downloaded = false, progress = null)) }
        if (activeId.value == id) activeId.value = null
    }

    override suspend fun setActive(id: String) {
        activeId.value = id
    }

    override suspend fun activeModelPath(): String? {
        val id = activeId.value ?: return null
        val entry = entries.value[id] ?: return null
        if (!entry.downloaded) return null
        val model = catalog.firstOrNull { it.id == id } ?: return null
        return "/fake/models/${model.fileName}"
    }

    override suspend fun setHfToken(token: String) {
        hasToken.value = token.isNotEmpty()
    }

    override fun observeHasToken(): Flow<Boolean> = hasToken
}
