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
import com.dv.moneym.core.testing.FakeAccountRepository
import com.dv.moneym.core.testing.FakeAppSettings
import com.dv.moneym.core.testing.FakeBudgetRepository
import com.dv.moneym.core.testing.FakeCategoryRepository
import com.dv.moneym.core.testing.FakeTransactionRepository
import com.dv.moneym.core.testing.FixedClock
import com.dv.moneym.core.testing.TestDispatcherProvider
import com.dv.moneym.feature.aianalysis.usecase.BuildFinanceSnapshotUseCase
import com.dv.moneym.feature.aianalysis.usecase.BuildFinanceToolsetUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
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

    override suspend fun availability(): AiAvailability = availability

    override fun streamReply(messages: List<ChatMessage>, grounding: Grounding): Flow<String> {
        lastGrounding = grounding
        return deltas.asFlow()
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class AnalyzeViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        appSettings = FakeAppSettings()
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private val clock = FixedClock(Instant.parse("2026-05-15T12:00:00Z"))
    private var appSettings = FakeAppSettings()

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
    fun availabilityReflectedFromSelectedEngine() = runTest(testDispatcher) {
        val vm = makeVm(registryOf(FakeAiEngine(id = AiEngineId.GEMINI_NANO, availability = AiAvailability.UNAVAILABLE)))
        val job = launch { vm.state.collect {} }
        testDispatcher.scheduler.advanceUntilIdle()
        assertFalse(vm.state.value.available)
        job.cancel()
    }
}
