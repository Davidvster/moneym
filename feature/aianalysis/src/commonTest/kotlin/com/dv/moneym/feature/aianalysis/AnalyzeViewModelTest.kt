package com.dv.moneym.feature.aianalysis

import androidx.lifecycle.SavedStateHandle
import com.dv.moneym.core.ai.AiAvailability
import com.dv.moneym.core.ai.AiEngine
import com.dv.moneym.core.ai.AiEngineId
import com.dv.moneym.core.ai.AiEngineRegistry
import com.dv.moneym.core.ai.AiGroundingMode
import com.dv.moneym.core.ai.ChatMessage
import com.dv.moneym.core.ai.ChatRole
import com.dv.moneym.core.ai.Grounding
import com.dv.moneym.core.datastore.PrefKeys
import com.dv.moneym.core.model.AccountId
import com.dv.moneym.core.model.CategoryId
import com.dv.moneym.core.model.CurrencyCode
import com.dv.moneym.core.model.Money
import com.dv.moneym.core.model.Transaction
import com.dv.moneym.core.model.TransactionId
import com.dv.moneym.core.model.TransactionType
import com.dv.moneym.core.testing.FakeAccountRepository
import com.dv.moneym.core.testing.FakeAiChatRepository
import com.dv.moneym.core.testing.FakeAppSettings
import com.dv.moneym.core.testing.FakeBudgetRepository
import com.dv.moneym.core.testing.FakeCategoryRepository
import com.dv.moneym.core.testing.FakeLlmModelRepository
import com.dv.moneym.core.testing.FakeTransactionRepository
import com.dv.moneym.core.testing.FixedClock
import com.dv.moneym.core.testing.TestDispatcherProvider
import com.dv.moneym.feature.aianalysis.usecase.BuildFinanceSnapshotUseCase
import com.dv.moneym.feature.aianalysis.usecase.BuildFinanceToolsetUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.LocalDate
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Instant

private class FakeAiEngine(
    override val id: AiEngineId,
    override val supportsTools: Boolean = false,
    private val availability: AiAvailability = AiAvailability.AVAILABLE,
    private val deltas: List<String> = listOf("Hello", " ", "world"),
) : AiEngine {
    var lastGrounding: Grounding? = null
    var lastResponseLanguage: String? = null

    override suspend fun availability(): AiAvailability = availability

    override fun streamReply(
        messages: List<ChatMessage>,
        grounding: Grounding,
        responseLanguage: String?,
    ): Flow<String> {
        lastGrounding = grounding
        lastResponseLanguage = responseLanguage
        return deltas.asFlow()
    }
}

private class FakeLocaleController(var tag: String = "en") :
    com.dv.moneym.core.common.LocaleController {
    override fun applyLocale(languageTag: String) { tag = languageTag }
    override fun getCurrentLanguageTag(): String = tag
}

@OptIn(ExperimentalCoroutinesApi::class)
class AnalyzeViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        appSettings = FakeAppSettings()
        aiChatRepository = FakeAiChatRepository()
        activeChatHolder = ActiveChatHolder()
        transactionRepository = FakeTransactionRepository()
        localeController = FakeLocaleController()
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private val clock = FixedClock(Instant.parse("2026-05-15T12:00:00Z"))
    private var appSettings = FakeAppSettings()
    private var aiChatRepository = FakeAiChatRepository()
    private var activeChatHolder = ActiveChatHolder()
    private var transactionRepository = FakeTransactionRepository()
    private var localeController = FakeLocaleController()

    private fun snapshotUseCase() = BuildFinanceSnapshotUseCase(
        transactionRepository = FakeTransactionRepository(),
        accountRepository = FakeAccountRepository(),
        categoryRepository = FakeCategoryRepository(),
        budgetRepository = FakeBudgetRepository(),
        clock = clock,
    )

    private fun toolsetUseCase() = BuildFinanceToolsetUseCase(
        transactionRepository = FakeTransactionRepository(),
        accountRepository = FakeAccountRepository(),
        categoryRepository = FakeCategoryRepository(),
        budgetRepository = FakeBudgetRepository(),
        clock = clock,
    )

    private fun makeVm(registry: AiEngineRegistry) = AnalyzeViewModel(
        year = 2026,
        month = 5,
        registry = registry,
        buildSnapshot = snapshotUseCase(),
        buildToolset = toolsetUseCase(),
        appSettings = appSettings,
        dispatchers = TestDispatcherProvider(testDispatcher),
        aiChatRepository = aiChatRepository,
        transactionRepository = transactionRepository,
        llmModelRepository = FakeLlmModelRepository(),
        localeController = localeController,
        clock = clock,
        activeChatHolder = activeChatHolder,
        savedStateHandle = SavedStateHandle(),
    )

    private fun registryOf(vararg engines: AiEngine) = AiEngineRegistry(engines.toList())

    private fun availableEngine(
        id: AiEngineId = AiEngineId.GEMINI_NANO,
        supportsTools: Boolean = false,
        deltas: List<String> = listOf("Hello", " ", "world"),
    ) = FakeAiEngine(id = id, supportsTools = supportsTools, deltas = deltas)

    private fun downloadableLocalEngine() = FakeAiEngine(
        id = AiEngineId.LOCAL_LLM,
        availability = AiAvailability.DOWNLOADABLE,
    )

    @Test
    fun enginesListBuiltFromRegistry() = runTest(testDispatcher) {
        val vm = makeVm(registryOf(availableEngine(), downloadableLocalEngine()))
        val job = launch { vm.state.collect {} }
        testDispatcher.scheduler.advanceUntilIdle()

        val state = vm.state.value
        assertEquals(2, state.engines.size)
        val nano = state.engines.first { it.id == AiEngineId.GEMINI_NANO }
        val local = state.engines.first { it.id == AiEngineId.LOCAL_LLM }
        assertTrue(nano.available)
        assertFalse(local.available)
        assertTrue(local.needsDownload)
        assertEquals(AiEngineId.GEMINI_NANO, state.selectedEngine)
        job.cancel()
    }

    @Test
    fun unavailableEngineHiddenAndDownloadableSelected() = runTest(testDispatcher) {
        val unavailableNano = FakeAiEngine(
            id = AiEngineId.GEMINI_NANO,
            availability = AiAvailability.UNAVAILABLE,
        )
        val vm = makeVm(registryOf(unavailableNano, downloadableLocalEngine()))
        val job = launch { vm.state.collect {} }
        testDispatcher.scheduler.advanceUntilIdle()

        val state = vm.state.value
        assertEquals(1, state.engines.size)
        assertEquals(AiEngineId.LOCAL_LLM, state.engines.single().id)
        assertEquals(AiEngineId.LOCAL_LLM, state.selectedEngine)
        assertTrue(state.needsModelDownload)
        job.cancel()
    }

    @Test
    fun staleUnavailablePersistedEngineIgnored() = runTest(testDispatcher) {
        appSettings.putString(PrefKeys.AI_ENGINE_ID, AiEngineId.GEMINI_NANO.name)
        val unavailableNano = FakeAiEngine(
            id = AiEngineId.GEMINI_NANO,
            availability = AiAvailability.UNAVAILABLE,
        )
        val vm = makeVm(registryOf(unavailableNano, downloadableLocalEngine()))
        val job = launch { vm.state.collect {} }
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(AiEngineId.LOCAL_LLM, vm.state.value.selectedEngine)
        job.cancel()
    }

    @Test
    fun engineChangedPersistsAndSwitches() = runTest(testDispatcher) {
        val vm = makeVm(registryOf(availableEngine(), downloadableLocalEngine()))
        val job = launch { vm.state.collect {} }
        testDispatcher.scheduler.advanceUntilIdle()

        vm.onIntent(AnalyzeIntent.EngineChanged(AiEngineId.LOCAL_LLM))
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(AiEngineId.LOCAL_LLM, vm.state.value.selectedEngine)
        assertEquals(AiEngineId.LOCAL_LLM.name, appSettings.getString(PrefKeys.AI_ENGINE_ID))
        assertTrue(vm.state.value.needsModelDownload)
        job.cancel()
    }

    @Test
    fun yearBoundsComeFromTransactionRangeAndClamp() = runTest(testDispatcher) {
        transactionRepository.upsert(yearTxn(2023))
        transactionRepository.upsert(yearTxn(2026))
        val vm = makeVm(registryOf(availableEngine()))
        val job = launch { vm.state.collect {} }
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(2023, vm.state.value.minYear)
        assertEquals(2026, vm.state.value.maxYear)
        // entry year is the default
        assertEquals(2026, vm.state.value.selectedYear)

        vm.onIntent(AnalyzeIntent.YearChanged(2024))
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(2024, vm.state.value.selectedYear)

        // out-of-range requests clamp into [min, max]
        vm.onIntent(AnalyzeIntent.YearChanged(2019))
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(2023, vm.state.value.selectedYear)
        vm.onIntent(AnalyzeIntent.YearChanged(2030))
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(2026, vm.state.value.selectedYear)
        job.cancel()
    }

    @Test
    fun selectedYearDrivesSnapshotPeriod() = runTest(testDispatcher) {
        transactionRepository.upsert(yearTxn(2024))
        transactionRepository.upsert(yearTxn(2026))
        val engine = availableEngine()
        val vm = makeVm(registryOf(engine))
        val job = launch { vm.state.collect {} }
        testDispatcher.scheduler.advanceUntilIdle()

        vm.onIntent(AnalyzeIntent.YearChanged(2024))
        vm.onIntent(AnalyzeIntent.SendMessage("hi"))
        testDispatcher.scheduler.advanceUntilIdle()

        val snapshot = engine.lastGrounding as Grounding.Snapshot
        assertTrue(snapshot.text.contains("Year: 2024"), snapshot.text)
        job.cancel()
    }

    @Test
    fun responseLanguageResolvedFromLocaleAndPassedToEngine() = runTest(testDispatcher) {
        localeController.tag = "de-DE"
        val engine = availableEngine()
        val vm = makeVm(registryOf(engine))
        val job = launch { vm.state.collect {} }
        testDispatcher.scheduler.advanceUntilIdle()

        vm.onIntent(AnalyzeIntent.SendMessage("hi"))
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("German", engine.lastResponseLanguage)
        job.cancel()
    }

    @Test
    fun englishLocaleSendsNoLanguageDirective() = runTest(testDispatcher) {
        localeController.tag = "en"
        val engine = availableEngine()
        val vm = makeVm(registryOf(engine))
        val job = launch { vm.state.collect {} }
        testDispatcher.scheduler.advanceUntilIdle()

        vm.onIntent(AnalyzeIntent.SendMessage("hi"))
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(null, engine.lastResponseLanguage)
        job.cancel()
    }

    private fun yearTxn(year: Int) = Transaction(
        id = TransactionId(0),
        type = TransactionType.EXPENSE,
        amount = Money(1000, CurrencyCode("EUR")),
        occurredOn = LocalDate(year, 5, 1),
        note = null,
        categoryId = CategoryId(1),
        accountId = AccountId(1),
        createdAt = Instant.fromEpochMilliseconds(0),
        updatedAt = Instant.fromEpochMilliseconds(0),
    )

    @Test
    fun selectingDownloadableLocalEngineThenSendShowsNeedsDownload() = runTest(testDispatcher) {
        val vm = makeVm(registryOf(availableEngine(), downloadableLocalEngine()))
        val job = launch { vm.state.collect {} }
        testDispatcher.scheduler.advanceUntilIdle()

        vm.onIntent(AnalyzeIntent.EngineChanged(AiEngineId.LOCAL_LLM))
        vm.onIntent(AnalyzeIntent.SendMessage("hi"))
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(vm.state.value.needsModelDownload)
        assertTrue(vm.state.value.messages.none { it.role == ChatRole.ASSISTANT })
        assertFalse(vm.state.value.isGenerating)
        job.cancel()
    }

    @Test
    fun selectingAvailableEngineThenSendStreamsDeltas() = runTest(testDispatcher) {
        val vm = makeVm(registryOf(availableEngine(deltas = listOf("Hel", "lo")), downloadableLocalEngine()))
        val job = launch { vm.state.collect {} }
        testDispatcher.scheduler.advanceUntilIdle()

        vm.onIntent(AnalyzeIntent.SendMessage("hi"))
        testDispatcher.scheduler.advanceUntilIdle()

        val state = vm.state.value
        assertEquals(2, state.messages.size)
        assertEquals(ChatRole.USER, state.messages[0].role)
        assertEquals("hi", state.messages[0].content)
        assertEquals(ChatRole.ASSISTANT, state.messages[1].role)
        assertEquals("Hello", state.messages[1].content)
        assertFalse(state.isGenerating)
        job.cancel()
    }

    @Test
    fun blankMessageIgnored() = runTest(testDispatcher) {
        val vm = makeVm(registryOf(availableEngine()))
        backgroundScope.launch { vm.state.collect {} }
        vm.onIntent(AnalyzeIntent.SendMessage("   "))
        testDispatcher.scheduler.advanceUntilIdle()
        assertTrue(vm.state.value.messages.isEmpty())
    }

    @Test
    fun groundingModeChangePersistsToAppSettings() = runTest(testDispatcher) {
        val vm = makeVm(registryOf(availableEngine()))
        val job = launch { vm.state.collect {} }
        testDispatcher.scheduler.advanceUntilIdle()
        vm.onIntent(AnalyzeIntent.GroundingModeChanged(AiGroundingMode.TOOLS))
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(AiGroundingMode.TOOLS, vm.state.value.groundingMode)
        assertEquals(AiGroundingMode.TOOLS.name, appSettings.getString(PrefKeys.AI_GROUNDING_MODE))
        job.cancel()
    }

    @Test
    fun groundingModeLoadedFromAppSettingsOnInit() = runTest(testDispatcher) {
        appSettings.putString(PrefKeys.AI_GROUNDING_MODE, AiGroundingMode.TOOLS.name)
        val vm = makeVm(registryOf(availableEngine(supportsTools = false)))
        assertEquals(AiGroundingMode.TOOLS, vm.state.value.groundingMode)
    }

    @Test
    fun toolsModeFallsBackToSnapshotWhenEngineHasNoToolSupport() = runTest(testDispatcher) {
        appSettings.putString(PrefKeys.AI_GROUNDING_MODE, AiGroundingMode.TOOLS.name)
        val engine = availableEngine(supportsTools = false)
        val vm = makeVm(registryOf(engine))
        val job = launch { vm.state.collect {} }
        testDispatcher.scheduler.advanceUntilIdle()
        vm.onIntent(AnalyzeIntent.SendMessage("analyze"))
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(vm.state.value.showToolsFallbackNotice)
        assertTrue(engine.lastGrounding is Grounding.Snapshot)
        job.cancel()
    }

    @Test
    fun toolsModeUsesToolsWhenEngineSupportsThem() = runTest(testDispatcher) {
        appSettings.putString(PrefKeys.AI_GROUNDING_MODE, AiGroundingMode.TOOLS.name)
        val engine = availableEngine(supportsTools = true)
        val vm = makeVm(registryOf(engine))
        val job = launch { vm.state.collect {} }
        testDispatcher.scheduler.advanceUntilIdle()
        vm.onIntent(AnalyzeIntent.SendMessage("analyze"))
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(vm.state.value.showToolsFallbackNotice)
        assertTrue(engine.lastGrounding is Grounding.Tools)
        job.cancel()
    }

    @Test
    fun sendingMessagePersistsConversation() = runTest(testDispatcher) {
        val vm = makeVm(registryOf(availableEngine(deltas = listOf("Hi"))))
        val job = launch { vm.state.collect {} }
        testDispatcher.scheduler.advanceUntilIdle()

        vm.onIntent(AnalyzeIntent.SendMessage("how much did I spend?"))
        testDispatcher.scheduler.advanceUntilIdle()

        val id = vm.state.value.currentConversationId
        assertTrue(id != null)
        val conversations = aiChatRepository.observeConversations().first()
        assertEquals(1, conversations.size)
        assertEquals("how much did I spend?", conversations.single().title)
        assertEquals(2, aiChatRepository.loadMessages(id).size)
        job.cancel()
    }

    @Test
    fun resumeConversationLoadsMessages() = runTest(testDispatcher) {
        val id = aiChatRepository.createConversation("Old chat", null, 2026, 5, 0L)
        aiChatRepository.replaceMessages(
            id,
            listOf(ChatMessage(ChatRole.USER, "earlier question"), ChatMessage(ChatRole.ASSISTANT, "earlier answer")),
            0L,
        )
        val vm = makeVm(registryOf(availableEngine()))
        val job = launch { vm.state.collect {} }
        testDispatcher.scheduler.advanceUntilIdle()

        vm.onIntent(AnalyzeIntent.ResumeConversation(id))
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(id, vm.state.value.currentConversationId)
        assertEquals(2, vm.state.value.messages.size)
        assertEquals("earlier question", vm.state.value.messages[0].content)
        job.cancel()
    }

    @Test
    fun newChatResetsConversation() = runTest(testDispatcher) {
        val vm = makeVm(registryOf(availableEngine(deltas = listOf("Hi"))))
        val job = launch { vm.state.collect {} }
        testDispatcher.scheduler.advanceUntilIdle()

        vm.onIntent(AnalyzeIntent.SendMessage("first"))
        testDispatcher.scheduler.advanceUntilIdle()
        assertTrue(vm.state.value.currentConversationId != null)

        vm.onIntent(AnalyzeIntent.NewChat)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(null, vm.state.value.currentConversationId)
        assertTrue(vm.state.value.messages.isEmpty())
        job.cancel()
    }

    @Test
    fun checkPendingConsumesHolderResume() = runTest(testDispatcher) {
        val id = aiChatRepository.createConversation("Resumed", null, 2026, 5, 0L)
        aiChatRepository.replaceMessages(id, listOf(ChatMessage(ChatRole.USER, "q")), 0L)
        activeChatHolder.pendingConversationId = id
        val vm = makeVm(registryOf(availableEngine()))
        val job = launch { vm.state.collect {} }
        testDispatcher.scheduler.advanceUntilIdle()

        vm.onIntent(AnalyzeIntent.CheckPending)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(id, vm.state.value.currentConversationId)
        assertEquals(null, activeChatHolder.pendingConversationId)
        job.cancel()
    }

    @Test
    fun availabilityReflectedFromSelectedEngine() = runTest(testDispatcher) {
        val vm = makeVm(registryOf(FakeAiEngine(id = AiEngineId.GEMINI_NANO, availability = AiAvailability.UNAVAILABLE)))
        val job = launch { vm.state.collect {} }
        testDispatcher.scheduler.advanceUntilIdle()
        assertFalse(vm.state.value.available)
        job.cancel()
    }
}
