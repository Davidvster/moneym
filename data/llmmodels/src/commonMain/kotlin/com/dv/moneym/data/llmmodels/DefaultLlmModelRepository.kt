package com.dv.moneym.data.llmmodels

import com.dv.moneym.core.common.LocalModelRuntime
import com.dv.moneym.core.common.NoopLocalModelRuntime
import com.dv.moneym.core.datastore.AppSettings
import com.dv.moneym.core.datastore.PrefKeys
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class DefaultLlmModelRepository(
    private val appSettings: AppSettings,
    private val fileStore: ModelFileStore,
    private val downloader: LlmModelDownloader,
    private val scope: CoroutineScope,
    private val runtime: LocalModelRuntime = NoopLocalModelRuntime,
    private val catalog: List<LlmModel> = LlmModelCatalog.models,
) : LlmModelRepository {

    private val progress = MutableStateFlow<Map<String, DownloadProgress?>>(emptyMap())
    private val downloadedRevision = MutableStateFlow(0)
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
        progress.update { it + (id to DownloadProgress(0L, model.sizeBytes)) }
        val result = CompletableDeferred<Unit>()
        val job = scope.launch {
            val speed = DownloadSpeedMeter()
            try {
                downloader.download(model).collect { p ->
                    progress.update { it + (id to p.copy(bytesPerSecond = speed.update(p.bytesRead))) }
                }
                // A freshly downloaded model becomes the active one automatically.
                appSettings.putString(PrefKeys.AI_ACTIVE_MODEL_ID, id)
                progress.update { it - id }
                downloadedRevision.update { it + 1 }
                result.complete(Unit)
            } catch (e: CancellationException) {
                progress.update { it - id }
                result.complete(Unit)
                throw e
            } catch (e: Throwable) {
                progress.update { it - id }
                downloadedRevision.update { it + 1 }
                result.completeExceptionally(e)
            }
        }
        jobs[id] = job
        result.await()
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
        // Release the native engine first: while it holds the model file open, unlinking the blob
        // does not reclaim its disk blocks, and the compiled copy stays in the runtime cache.
        runtime.releaseAndClearCache()
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

    private suspend fun isDownloaded(model: LlmModel): Boolean {
        if (!fileStore.finalExists(model.fileName)) return false
        if (model.sha256.isEmpty()) return true
        return fileStore.finalSize(model.fileName) == model.sizeBytes
    }
}
