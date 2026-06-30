package com.dv.moneym.feature.aianalysis

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.serialization.saved
import androidx.lifecycle.viewModelScope
import com.dv.moneym.core.ai.AiAvailability
import com.dv.moneym.core.ai.AiEngineId
import com.dv.moneym.core.ai.AiEngineRegistry
import com.dv.moneym.core.ai.AiGroundingMode
import com.dv.moneym.core.ai.AppManagedToolLoop
import com.dv.moneym.core.ai.ChatMessage
import com.dv.moneym.core.ai.ChatRole
import com.dv.moneym.core.ai.Grounding
import com.dv.moneym.core.common.AppClock
import com.dv.moneym.core.common.DispatcherProvider
import com.dv.moneym.core.common.LocaleController
import com.dv.moneym.core.common.SupportedLanguage
import com.dv.moneym.core.datastore.AppSettings
import com.dv.moneym.core.datastore.PrefKeys
import com.dv.moneym.data.aichat.AiChatRepository
import com.dv.moneym.data.llmmodels.LlmModelRepository
import com.dv.moneym.data.transactions.TransactionRepository
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
import kotlinx.coroutines.withTimeoutOrNull

class AnalyzeViewModel(
    private val year: Int,
    private val month: Int,
    private val registry: AiEngineRegistry,
    private val buildSnapshot: BuildFinanceSnapshotUseCase,
    private val buildToolset: BuildFinanceToolsetUseCase,
    private val appSettings: AppSettings,
    private val dispatchers: DispatcherProvider,
    private val aiChatRepository: AiChatRepository,
    private val transactionRepository: TransactionRepository,
    private val llmModelRepository: LlmModelRepository,
    private val localeController: LocaleController,
    private val clock: AppClock,
    private val activeChatHolder: ActiveChatHolder,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val appManagedToolLoop = AppManagedToolLoop()

    private val _state by savedStateHandle.saved { MutableStateFlow(AnalyzeUiState(groundingMode = loadGroundingMode())) }
    internal val state = _state
        .onStart {
            startEngineProbe()
            loadYearBounds()
            observeActiveLocalModel()
        }
        .stateIn(viewModelScope, SharingStarted.Lazily, _state.value)

    // The on-device (custom) engine never blocks, so it is emitted immediately and the picker
    // shows right away. Built-in engines (Gemini Nano on Android, Apple Intelligence on iOS) are
    // probed afterwards in the background — their availability check can block on a device without
    // platform support, and must never stall the UI. They are added only when actually available.
    private fun startEngineProbe() {
        viewModelScope.launch {
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

    // Snapshot grounding summarises a whole year, so the user can point it at any year that has
    // data. Bounds come from the stored transaction range; the year the screen was opened on is
    // the default, clamped into range.
    private fun loadYearBounds() {
        viewModelScope.launch {
            val earliest = withContext(dispatchers.io) { transactionRepository.getEarliestTransactionDate() }
            val latest = withContext(dispatchers.io) { transactionRepository.getLatestTransactionDate() }
            val currentYear = clock.today().year
            val minYear = earliest?.year ?: minOf(year, currentYear)
            val maxYear = maxOf(latest?.year ?: currentYear, year)
            _state.update {
                it.copy(
                    minYear = minYear,
                    maxYear = maxYear,
                    selectedYear = (it.selectedYear ?: year).coerceIn(minYear, maxYear),
                )
            }
        }
    }

    // The on-device engine label only says "On-device model"; surface which downloaded model is
    // actually active so the picker tells the user what custom model will answer.
    private fun observeActiveLocalModel() {
        viewModelScope.launch {
            llmModelRepository.observeModels().collect { models ->
                val active = models.firstOrNull { it.active && it.downloaded }
                _state.update { it.copy(localModelNameKey = active?.model?.displayNameKey) }
            }
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
        _state.update { st ->
            // resolveSelection already honours the user's persisted choice, then prefers an
            // available engine over a merely downloadable one.
            val selected = resolveSelection(engines)
            st.copy(
                engines = engines,
                selectedEngine = selected?.id,
                available = selected?.available ?: false,
                needsModelDownload = selected?.needsDownload ?: false,
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

    fun onIntent(intent: AnalyzeIntent) {
        when (intent) {
            is AnalyzeIntent.InputChanged -> _state.update { it.copy(input = intent.text) }
            is AnalyzeIntent.SendMessage -> sendMessage(intent.text)
            is AnalyzeIntent.GroundingModeChanged -> changeGroundingMode(intent.mode)
            is AnalyzeIntent.YearChanged -> changeYear(intent.year)
            is AnalyzeIntent.EngineChanged -> changeEngine(intent.id)
            AnalyzeIntent.RefreshEngines -> startEngineProbe()
            AnalyzeIntent.DismissFallbackNotice -> _state.update { it.copy(showToolsFallbackNotice = false) }
            AnalyzeIntent.ClearError -> _state.update { it.copy(error = null) }
            AnalyzeIntent.NewChat -> newChat()
            is AnalyzeIntent.ResumeConversation -> resumeConversation(intent.id)
            AnalyzeIntent.CheckPending -> checkPending()
        }
    }

    private fun newChat() {
        _state.update {
            it.copy(messages = emptyList(), currentConversationId = null, input = "", error = null, isGenerating = false)
        }
    }

    private fun resumeConversation(id: Long) {
        viewModelScope.launch {
            val messages = aiChatRepository.loadMessages(id)
            _state.update { it.copy(messages = messages, currentConversationId = id, input = "", error = null) }
        }
    }

    // Picks up a resume/new-chat action chosen on the full-screen history (delivered via the
    // holder), invoked when the analyze screen resumes after the history is popped.
    private fun checkPending() {
        when {
            activeChatHolder.pendingNewChat -> {
                activeChatHolder.pendingNewChat = false
                newChat()
            }
            activeChatHolder.pendingConversationId != null -> {
                val id = activeChatHolder.pendingConversationId!!
                activeChatHolder.pendingConversationId = null
                resumeConversation(id)
            }
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
                showToolsFallbackNotice = false,
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

            ensureConversation(trimmed, selectedId)

            val responseLanguage =
                SupportedLanguage.responseLanguageNameForTag(localeController.getCurrentLanguageTag())
            val initialMessages = _state.value.messages
            val replyFlow = if (_state.value.groundingMode == AiGroundingMode.TOOLS) {
                val tools = buildToolset(year, month)
                if (engine.supportsTools) {
                    engine.streamReply(initialMessages, Grounding.Tools(tools), responseLanguage)
                } else {
                    appManagedToolLoop.streamReply(engine, initialMessages, tools, responseLanguage)
                }
            } else {
                val snapshotYear = _state.value.selectedYear ?: year
                val grounding = Grounding.Snapshot(withContext(dispatchers.io) { buildSnapshot(snapshotYear, month) })
                engine.streamReply(initialMessages, grounding, responseLanguage)
            }

            val assistantIndex = _state.value.messages.size
            _state.update { it.copy(messages = it.messages + ChatMessage(ChatRole.ASSISTANT, "")) }

            replyFlow
                .catch { _state.update { it.copy(error = AnalyzeError.GenerationFailed, isGenerating = false) } }
                .onCompletion {
                    _state.update { it.copy(isGenerating = false) }
                    persistConversation()
                }
                .collect { delta -> appendDelta(assistantIndex, delta) }
        }
    }

    private suspend fun ensureConversation(firstUserText: String, engineId: AiEngineId?) {
        if (_state.value.currentConversationId != null) return
        val now = clock.now().toEpochMilliseconds()
        val title = firstUserText.take(CONVERSATION_TITLE_MAX).trim()
        val id = aiChatRepository.createConversation(title, engineId?.name, year, month, now)
        _state.update { it.copy(currentConversationId = id) }
    }

    private suspend fun persistConversation() {
        val id = _state.value.currentConversationId ?: return
        aiChatRepository.replaceMessages(id, _state.value.messages, clock.now().toEpochMilliseconds())
    }

    private fun appendDelta(index: Int, delta: String) {
        _state.update { current ->
            val messages = current.messages.toMutableList()
            val existing = messages[index]
            messages[index] = existing.copy(content = existing.content + delta)
            current.copy(messages = messages)
        }
    }

    private fun changeYear(newYear: Int) {
        _state.update { st ->
            val min = st.minYear
            val max = st.maxYear
            val clamped = if (min != null && max != null) newYear.coerceIn(min, max) else newYear
            st.copy(selectedYear = clamped)
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

    private companion object {
        const val BUILTIN_PROBE_TIMEOUT_MS = 3000L
        const val CONVERSATION_TITLE_MAX = 40
    }
}
