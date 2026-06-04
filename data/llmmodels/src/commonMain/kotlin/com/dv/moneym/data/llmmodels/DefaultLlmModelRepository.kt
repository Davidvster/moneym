package com.dv.moneym.data.llmmodels

import com.dv.moneym.core.datastore.AppSettings
import com.dv.moneym.core.datastore.PrefKeys
import com.dv.moneym.core.security.SecureStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val HF_TOKEN_KEY = "hf_token"

class DefaultLlmModelRepository(
    private val appSettings: AppSettings,
    private val secureStore: SecureStore,
    private val fileStore: ModelFileStore,
    private val downloader: LlmModelDownloader,
    private val scope: CoroutineScope,
    private val catalog: List<LlmModel> = LlmModelCatalog.models,
) : LlmModelRepository {

    private val progress = MutableStateFlow<Map<String, Float?>>(emptyMap())
    private val downloadedRevision = MutableStateFlow(0)
    private val tokenRevision = MutableStateFlow(0)
    private val jobs = mutableMapOf<String, Job>()

    override fun observeModels(): Flow<List<LlmModelState>> =
        combine(
            appSettings.observeString(PrefKeys.AI_ACTIVE_MODEL_ID, null),
            progress,
            downloadedRevision,
        ) { activeId, progressMap, _ ->
            catalog.map { model ->
                LlmModelState(
                    model = model,
                    downloaded = isDownloaded(model),
                    active = model.id == activeId,
                    progress = progressMap[model.id],
                )
            }
        }

    override suspend fun download(id: String) {
        val model = catalog.firstOrNull { it.id == id } ?: return
        jobs[id]?.cancel()
        progress.update { it + (id to 0f) }
        val job = scope.launch {
            try {
                downloader.download(model).collect { p ->
                    progress.update { it + (id to p.fraction) }
                }
                progress.update { it - id }
                downloadedRevision.update { it + 1 }
            } catch (e: Throwable) {
                progress.update { it - id }
                downloadedRevision.update { it + 1 }
                throw e
            }
        }
        jobs[id] = job
        job.join()
    }

    override fun cancel(id: String) {
        jobs.remove(id)?.cancel()
        progress.update { it - id }
        scope.launch {
            catalog.firstOrNull { it.id == id }?.let { fileStore.deletePart(it.fileName) }
        }
    }

    override suspend fun delete(id: String) {
        val model = catalog.firstOrNull { it.id == id } ?: return
        fileStore.deletePart(model.fileName)
        fileStore.deleteFinal(model.fileName)
        if (appSettings.getString(PrefKeys.AI_ACTIVE_MODEL_ID, null) == id) {
            appSettings.remove(PrefKeys.AI_ACTIVE_MODEL_ID)
        }
        downloadedRevision.update { it + 1 }
    }

    override suspend fun setActive(id: String) {
        appSettings.putString(PrefKeys.AI_ACTIVE_MODEL_ID, id)
    }

    override suspend fun activeModelPath(): String? {
        val id = appSettings.getString(PrefKeys.AI_ACTIVE_MODEL_ID, null) ?: return null
        val model = catalog.firstOrNull { it.id == id } ?: return null
        if (!fileStore.finalExists(model.fileName)) return null
        return fileStore.finalPath(model.fileName)
    }

    override suspend fun setHfToken(token: String) {
        secureStore.put(HF_TOKEN_KEY, token.encodeToByteArray())
        tokenRevision.update { it + 1 }
    }

    override fun observeHasToken(): Flow<Boolean> =
        tokenRevision.map { secureStore.get(HF_TOKEN_KEY) != null }

    private suspend fun isDownloaded(model: LlmModel): Boolean {
        if (!fileStore.finalExists(model.fileName)) return false
        if (model.sha256.isEmpty()) return true
        return fileStore.finalSize(model.fileName) == model.sizeBytes
    }
}
