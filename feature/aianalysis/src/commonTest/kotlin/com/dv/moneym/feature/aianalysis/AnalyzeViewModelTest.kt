package com.dv.moneym.feature.aianalysis

import com.dv.moneym.core.ai.AiAvailability
import com.dv.moneym.core.ai.AiEngine
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
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private val clock = FixedClock(Instant.parse("2026-05-15T12:00:00Z"))
    private val appSettings = FakeAppSettings()

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

    private fun makeVm(engine: AiEngine) = AnalyzeViewModel(
        year = 2026,
        month = 5,
        engine = engine,
        buildSnapshot = snapshotUseCase(),
        buildToolset = toolsetUseCase(),
        appSettings = appSettings,
        dispatchers = TestDispatcherProvider(testDispatcher),
    )

    @Test
    fun sendMessageAppendsUserAndStreamsAssistantDeltas() = runTest(testDispatcher) {
        val vm = makeVm(FakeAiEngine(deltas = listOf("Hel", "lo")))
        vm.onIntent(AnalyzeIntent.SendMessage("hi"))
        testDispatcher.scheduler.advanceUntilIdle()

        val state = vm.state.value
        assertEquals(2, state.messages.size)
        assertEquals(ChatRole.USER, state.messages[0].role)
        assertEquals("hi", state.messages[0].content)
        assertEquals(ChatRole.ASSISTANT, state.messages[1].role)
        assertEquals("Hello", state.messages[1].content)
        assertFalse(state.isGenerating)
    }

    @Test
    fun blankMessageIgnored() = runTest(testDispatcher) {
        val vm = makeVm(FakeAiEngine())
        vm.onIntent(AnalyzeIntent.SendMessage("   "))
        testDispatcher.scheduler.advanceUntilIdle()
        assertTrue(vm.state.value.messages.isEmpty())
    }

    @Test
    fun groundingModeChangePersistsToAppSettings() = runTest(testDispatcher) {
        val vm = makeVm(FakeAiEngine())
        vm.onIntent(AnalyzeIntent.GroundingModeChanged(AiGroundingMode.TOOLS))
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(AiGroundingMode.TOOLS, vm.state.value.groundingMode)
        assertEquals(AiGroundingMode.TOOLS.name, appSettings.getString(PrefKeys.AI_GROUNDING_MODE))
    }

    @Test
    fun groundingModeLoadedFromAppSettingsOnInit() = runTest(testDispatcher) {
        appSettings.putString(PrefKeys.AI_GROUNDING_MODE, AiGroundingMode.TOOLS.name)
        val vm = makeVm(FakeAiEngine(supportsTools = false))
        assertEquals(AiGroundingMode.TOOLS, vm.state.value.groundingMode)
    }

    @Test
    fun toolsModeFallsBackToSnapshotWhenEngineHasNoToolSupport() = runTest(testDispatcher) {
        appSettings.putString(PrefKeys.AI_GROUNDING_MODE, AiGroundingMode.TOOLS.name)
        val engine = FakeAiEngine(supportsTools = false)
        val vm = makeVm(engine)
        vm.onIntent(AnalyzeIntent.SendMessage("analyze"))
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(vm.state.value.showToolsFallbackNotice)
        assertTrue(engine.lastGrounding is Grounding.Snapshot)
    }

    @Test
    fun toolsModeUsesToolsWhenEngineSupportsThem() = runTest(testDispatcher) {
        appSettings.putString(PrefKeys.AI_GROUNDING_MODE, AiGroundingMode.TOOLS.name)
        val engine = FakeAiEngine(supportsTools = true)
        val vm = makeVm(engine)
        vm.onIntent(AnalyzeIntent.SendMessage("analyze"))
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(vm.state.value.showToolsFallbackNotice)
        assertTrue(engine.lastGrounding is Grounding.Tools)
    }

    @Test
    fun availabilityReflectedFromEngine() = runTest(testDispatcher) {
        val vm = makeVm(FakeAiEngine(availability = AiAvailability.UNAVAILABLE))
        testDispatcher.scheduler.advanceUntilIdle()
        assertFalse(vm.state.value.available)
    }
}
