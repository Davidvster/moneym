package com.dv.moneym.feature.overview

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dv.moneym.core.ai.AiAvailability
import com.dv.moneym.core.ai.AiEngineId
import com.dv.moneym.core.ai.AiEngineRegistry
import com.dv.moneym.core.ai.ChatMessage
import com.dv.moneym.core.ai.ChatRole
import com.dv.moneym.core.ai.Grounding
import com.dv.moneym.core.common.AppClock
import com.dv.moneym.core.common.DispatcherProvider
import com.dv.moneym.core.datastore.AppSettings
import com.dv.moneym.core.datastore.PrefKeys
import com.dv.moneym.data.overview.OverviewAiWidget
import com.dv.moneym.data.overview.OverviewRepository
import com.dv.moneym.feature.overview.a2ui.BuildOverviewWidgetPromptUseCase
import com.dv.moneym.feature.overview.a2ui.OverviewA2UiValidationResult
import com.dv.moneym.feature.overview.a2ui.OverviewA2UiValidator
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.time.Instant

class OverviewAiWidgetBuilderViewModel(
    private val widgetId: Long?,
    private val overviewRepository: OverviewRepository,
    private val registry: AiEngineRegistry,
    private val appSettings: AppSettings,
    private val dispatchers: DispatcherProvider,
    private val clock: AppClock,
    private val buildPrompt: BuildOverviewWidgetPromptUseCase,
) : ViewModel() {

    private val validator = OverviewA2UiValidator()
    private var existingWidget: OverviewAiWidget? = null
    private var lastGeneratedAt: Instant? = null
    private var lastGenerationEngineId: String? = null

    private val _state = MutableStateFlow(OverviewAiWidgetBuilderUiState())
    internal val state = _state
        .stateIn(viewModelScope, SharingStarted.Lazily, _state.value)

    private val _effects = Channel<OverviewAiWidgetBuilderEffect>(Channel.BUFFERED)
    internal val effects = _effects.receiveAsFlow()

    init {
        viewModelScope.launch {
            val widget = widgetId?.let { id -> overviewRepository.observeAiWidgets().first().firstOrNull { it.id == id } }
            existingWidget = widget
            lastGeneratedAt = widget?.lastGeneratedAt
            lastGenerationEngineId = widget?.lastGenerationEngineId
            _state.value = OverviewAiWidgetBuilderUiState(
                isLoading = false,
                title = widget?.title.orEmpty(),
                prompt = widget?.prompt.orEmpty(),
                a2uiJson = widget?.a2uiJson.orEmpty(),
                previewJson = widget?.a2uiJson.orEmpty(),
                canSave = widget?.a2uiJson?.let { isValid(it) } == true,
            )
        }
    }

    fun onIntent(intent: OverviewAiWidgetBuilderIntent) {
        when (intent) {
            is OverviewAiWidgetBuilderIntent.TitleChanged -> updateTitle(intent.title)
            is OverviewAiWidgetBuilderIntent.PromptChanged -> updatePrompt(intent.prompt)
            is OverviewAiWidgetBuilderIntent.JsonChanged -> updateJson(intent.json)
            OverviewAiWidgetBuilderIntent.Generate -> generate()
            OverviewAiWidgetBuilderIntent.Save -> save()
            OverviewAiWidgetBuilderIntent.ClearError -> _state.update { it.copy(error = null) }
        }
    }

    private fun updateTitle(title: String) {
        _state.update { it.copy(title = title.take(TITLE_MAX), error = null, canSave = canSave(title, it.a2uiJson)) }
    }

    private fun updatePrompt(prompt: String) {
        _state.update { it.copy(prompt = prompt.take(PROMPT_MAX), error = null) }
    }

    private fun updateJson(json: String) {
        val trimmed = json.take(JSON_MAX)
        _state.update {
            it.copy(
                a2uiJson = trimmed,
                previewJson = trimmed.takeIf(::isValid).orEmpty(),
                error = if (trimmed.isBlank() || isValid(trimmed)) null else OverviewAiWidgetBuilderError.InvalidJson,
                canSave = canSave(it.title, trimmed),
            )
        }
    }

    private fun generate() {
        val snapshot = _state.value
        val prompt = snapshot.prompt.trim()
        val title = snapshot.title.trim()
        if (prompt.isEmpty()) {
            _state.update { it.copy(error = OverviewAiWidgetBuilderError.EmptyPrompt) }
            return
        }
        if (title.isEmpty()) {
            _state.update { it.copy(error = OverviewAiWidgetBuilderError.EmptyTitle) }
            return
        }

        val selectedId = appSettings.getString(PrefKeys.AI_ENGINE_ID)?.let { runCatching { AiEngineId.valueOf(it) }.getOrNull() }
            ?: registry.all().firstOrNull()?.id
        val engine = selectedId?.let { registry.byId(it) }
        if (engine == null) {
            _state.update { it.copy(error = OverviewAiWidgetBuilderError.MissingEngine) }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isGenerating = true, error = null) }
            val availability = withContext(dispatchers.io) { runCatching { engine.availability() }.getOrNull() }
            if (availability != AiAvailability.AVAILABLE) {
                _state.update { it.copy(isGenerating = false, error = OverviewAiWidgetBuilderError.EngineUnavailable) }
                return@launch
            }

            val result = runCatching {
                val chunks = mutableListOf<String>()
                engine.streamReply(
                    messages = listOf(ChatMessage(ChatRole.USER, buildPrompt(prompt, title))),
                    grounding = Grounding.Snapshot("MoneyM overview widget catalog only. Return constrained A2UI JSON."),
                    responseLanguage = null,
                ).collect { chunks += it }
                OverviewA2UiValidator.stripMarkdownFence(chunks.joinToString(""))
            }.getOrNull()

            if (result == null || !isValid(result)) {
                _state.update {
                    it.copy(
                        isGenerating = false,
                        error = OverviewAiWidgetBuilderError.InvalidJson,
                        a2uiJson = result.orEmpty(),
                        previewJson = "",
                        canSave = false,
                    )
                }
                return@launch
            }

            val now = clock.now()
            lastGeneratedAt = now
            lastGenerationEngineId = engine.id.name
            _state.update {
                it.copy(
                    isGenerating = false,
                    a2uiJson = result,
                    previewJson = result,
                    error = null,
                    canSave = canSave(it.title, result),
                )
            }
        }
    }

    private fun save() {
        val snapshot = _state.value
        val title = snapshot.title.trim()
        val prompt = snapshot.prompt.trim()
        val json = snapshot.a2uiJson.trim()
        if (title.isEmpty()) {
            _state.update { it.copy(error = OverviewAiWidgetBuilderError.EmptyTitle) }
            return
        }
        if (!isValid(json)) {
            _state.update { it.copy(error = OverviewAiWidgetBuilderError.InvalidJson) }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isSaving = true, error = null) }
            val now = clock.now()
            val existing = existingWidget
            val sortOrder = existing?.sortOrder
                ?: overviewRepository.observeAiWidgets().first().maxOfOrNull { it.sortOrder + 1 }
                ?: 0
            overviewRepository.upsertAiWidget(
                OverviewAiWidget(
                    id = existing?.id ?: 0L,
                    title = title,
                    prompt = prompt,
                    a2uiJson = json,
                    enabled = existing?.enabled ?: true,
                    sortOrder = sortOrder,
                    createdAt = existing?.createdAt ?: now,
                    updatedAt = now,
                    lastGeneratedAt = lastGeneratedAt,
                    lastGenerationEngineId = lastGenerationEngineId,
                ),
            )
            _state.update { it.copy(isSaving = false, canSave = true) }
            _effects.send(OverviewAiWidgetBuilderEffect.Saved)
        }
    }

    private fun canSave(title: String, json: String): Boolean =
        title.isNotBlank() && isValid(json)

    private fun isValid(json: String): Boolean =
        validator.validate(json) is OverviewA2UiValidationResult.Valid

    private companion object {
        const val TITLE_MAX = 80
        const val PROMPT_MAX = 1200
        const val JSON_MAX = 6000
    }
}
