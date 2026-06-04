package com.dv.moneym.feature.aianalysis

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.serialization.saved
import androidx.lifecycle.viewModelScope
import com.dv.moneym.core.ai.AiAvailability
import com.dv.moneym.core.ai.AiEngineId
import com.dv.moneym.core.ai.AiEngineRegistry
import com.dv.moneym.core.ai.AiGroundingMode
import com.dv.moneym.core.ai.ChatMessage
import com.dv.moneym.core.ai.ChatRole
import com.dv.moneym.core.ai.Grounding
import com.dv.moneym.core.common.DispatcherProvider
import com.dv.moneym.core.datastore.AppSettings
import com.dv.moneym.core.datastore.PrefKeys
import com.dv.moneym.feature.aianalysis.usecase.BuildFinanceSnapshotUseCase
import com.dv.moneym.feature.aianalysis.usecase.BuildFinanceToolsetUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AnalyzeViewModel(
    private val year: Int,
    private val month: Int,
    private val registry: AiEngineRegistry,
    private val buildSnapshot: BuildFinanceSnapshotUseCase,
    private val buildToolset: BuildFinanceToolsetUseCase,
    private val appSettings: AppSettings,
    private val dispatchers: DispatcherProvider,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val _state by savedStateHandle.saved { MutableStateFlow(AnalyzeUiState(groundingMode = loadGroundingMode())) }
    internal val state = _state.onStart { init() }.stateIn(viewModelScope, SharingStarted.Lazily, _state.value)

    private suspend fun init() {
        val availabilities = withContext(dispatchers.io) { registry.availabilities() }
        val options = registry.all().map { engine ->
            val availability = availabilities[engine.id]
            AiEngineOption(
                id = engine.id,
                available = availability == AiAvailability.AVAILABLE,
                needsDownload = engine.id == AiEngineId.LOCAL_LLM &&
                    availability == AiAvailability.DOWNLOADABLE,
            )
        }
        val selected = resolveSelection(options)
        _state.update {
            it.copy(
                engines = options,
                selectedEngine = selected?.id,
                available = selected?.available ?: false,
                needsModelDownload = selected?.needsDownload ?: false,
            )
        }
    }

    private fun resolveSelection(options: List<AiEngineOption>): AiEngineOption? {
        val persisted = appSettings.getString(PrefKeys.AI_ENGINE_ID)
        options.firstOrNull { it.id.name == persisted }?.let { return it }
        return options.firstOrNull { it.available } ?: options.firstOrNull()
    }

    fun onIntent(intent: AnalyzeIntent) {
        when (intent) {
            is AnalyzeIntent.InputChanged -> _state.update { it.copy(input = intent.text) }
            is AnalyzeIntent.SendMessage -> sendMessage(intent.text)
            is AnalyzeIntent.GroundingModeChanged -> changeGroundingMode(intent.mode)
            is AnalyzeIntent.EngineChanged -> changeEngine(intent.id)
            AnalyzeIntent.DismissFallbackNotice -> _state.update { it.copy(showToolsFallbackNotice = false) }
            AnalyzeIntent.ClearError -> _state.update { it.copy(error = null) }
        }
    }

    private fun changeEngine(id: AiEngineId) {
        appSettings.putString(PrefKeys.AI_ENGINE_ID, id.name)
        _state.update { current ->
            val option = current.engines.firstOrNull { it.id == id }
            current.copy(
                selectedEngine = id,
                available = option?.available ?: false,
                needsModelDownload = option?.needsDownload ?: false,
            )
        }
    }

    private fun sendMessage(text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty() || _state.value.isGenerating) return

        val selectedId = _state.value.selectedEngine
        val engine = selectedId?.let { registry.byId(it) }
        if (engine == null) {
            _state.update { it.copy(needsModelDownload = true) }
            return
        }

        val userMessage = ChatMessage(ChatRole.USER, trimmed)
        _state.update {
            it.copy(
                messages = it.messages + userMessage,
                input = "",
                isGenerating = true,
                error = null,
            )
        }

        viewModelScope.launch {
            if (engine.id == AiEngineId.LOCAL_LLM) {
                val availability = withContext(dispatchers.io) { engine.availability() }
                if (availability != AiAvailability.AVAILABLE) {
                    _state.update {
                        it.copy(
                            messages = it.messages.dropLast(1),
                            isGenerating = false,
                            needsModelDownload = true,
                        )
                    }
                    return@launch
                }
            }

            val useTools = _state.value.groundingMode == AiGroundingMode.TOOLS && engine.supportsTools
            val grounding = if (useTools) {
                Grounding.Tools(buildToolset(year, month))
            } else {
                if (_state.value.groundingMode == AiGroundingMode.TOOLS) {
                    _state.update { it.copy(showToolsFallbackNotice = true) }
                }
                Grounding.Snapshot(withContext(dispatchers.io) { buildSnapshot(year, month) })
            }

            val assistantIndex = _state.value.messages.size
            _state.update { it.copy(messages = it.messages + ChatMessage(ChatRole.ASSISTANT, "")) }

            engine.streamReply(_state.value.messages.dropLast(1), grounding)
                .catch { _state.update { it.copy(error = AnalyzeError.GenerationFailed, isGenerating = false) } }
                .onCompletion { _state.update { it.copy(isGenerating = false) } }
                .collect { delta -> appendDelta(assistantIndex, delta) }
        }
    }

    private fun appendDelta(index: Int, delta: String) {
        _state.update { current ->
            val messages = current.messages.toMutableList()
            val existing = messages[index]
            messages[index] = existing.copy(content = existing.content + delta)
            current.copy(messages = messages)
        }
    }

    private fun changeGroundingMode(mode: AiGroundingMode) {
        appSettings.putString(PrefKeys.AI_GROUNDING_MODE, mode.name)
        _state.update { it.copy(groundingMode = mode) }
    }

    private fun loadGroundingMode(): AiGroundingMode {
        val raw = appSettings.getString(PrefKeys.AI_GROUNDING_MODE)
        return AiGroundingMode.entries.firstOrNull { it.name == raw } ?: AiGroundingMode.SNAPSHOT
    }
}
