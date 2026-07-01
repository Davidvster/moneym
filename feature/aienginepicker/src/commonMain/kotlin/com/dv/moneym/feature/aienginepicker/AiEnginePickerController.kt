package com.dv.moneym.feature.aienginepicker

import com.dv.moneym.core.ai.AiAvailability
import com.dv.moneym.core.ai.AiEngineId
import com.dv.moneym.core.ai.AiEngineRegistry
import com.dv.moneym.core.common.DispatcherProvider
import com.dv.moneym.core.datastore.AppSettings
import com.dv.moneym.core.datastore.PrefKeys
import com.dv.moneym.data.llmmodels.LlmModelRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

class AiEnginePickerController(
    private val registry: AiEngineRegistry,
    private val appSettings: AppSettings,
    private val dispatchers: DispatcherProvider,
    private val llmModelRepository: LlmModelRepository,
) {
    private var probeJob: Job? = null
    private var modelJob: Job? = null

    private val _state = MutableStateFlow(AiEnginePickerState())
    val state: StateFlow<AiEnginePickerState> = _state

    fun start(scope: CoroutineScope) {
        refresh(scope)
        modelJob?.cancel()
        modelJob = scope.launch {
            llmModelRepository.observeModels().collect { models ->
                val active = models.firstOrNull { it.active && it.downloaded }
                _state.update { it.copy(localModelNameKey = active?.model?.displayNameKey) }
            }
        }
    }

    fun refresh(scope: CoroutineScope) {
        probeJob?.cancel()
        probeJob = scope.launch {
            val local = localOption()
            emitEngines(listOfNotNull(local))

            val builtIns = registry.all()
                .filter { it.id != AiEngineId.LOCAL_LLM }
                .mapNotNull { engine ->
                    val availability = withTimeoutOrNull(BUILTIN_PROBE_TIMEOUT_MS) {
                        withContext(dispatchers.io) { runCatching { engine.availability() }.getOrNull() }
                    }
                    if (availability == AiAvailability.AVAILABLE) {
                        AiEngineOption(id = engine.id, available = true, needsDownload = false)
                    } else {
                        null
                    }
                }
            if (builtIns.isNotEmpty()) emitEngines(builtIns + listOfNotNull(local))
        }
    }

    fun select(id: AiEngineId) {
        appSettings.putString(PrefKeys.AI_ENGINE_ID, id.name)
        _state.update { current ->
            val option = current.engines.firstOrNull { it.id == id }
            current.copy(
                selectedEngine = id,
                selectedAvailable = option?.available ?: false,
                selectedNeedsDownload = option?.needsDownload ?: false,
            )
        }
    }

    private suspend fun localOption(): AiEngineOption? {
        val engine = registry.byId(AiEngineId.LOCAL_LLM) ?: return null
        val availability = withContext(dispatchers.io) { runCatching { engine.availability() }.getOrNull() }
        return AiEngineOption(
            id = engine.id,
            available = availability == AiAvailability.AVAILABLE,
            needsDownload = availability == AiAvailability.DOWNLOADABLE,
        )
    }

    private fun emitEngines(engines: List<AiEngineOption>) {
        _state.update { current ->
            val selected = resolveSelection(engines)
            current.copy(
                engines = engines,
                selectedEngine = selected?.id,
                selectedAvailable = selected?.available ?: false,
                selectedNeedsDownload = selected?.needsDownload ?: false,
            )
        }
    }

    private fun resolveSelection(options: List<AiEngineOption>): AiEngineOption? {
        val persisted = appSettings.getString(PrefKeys.AI_ENGINE_ID)
        options.firstOrNull { it.id.name == persisted }?.let { return it }
        return options.firstOrNull { it.available }
            ?: options.firstOrNull { it.needsDownload }
            ?: options.firstOrNull()
    }

    private companion object {
        const val BUILTIN_PROBE_TIMEOUT_MS = 3000L
    }
}
