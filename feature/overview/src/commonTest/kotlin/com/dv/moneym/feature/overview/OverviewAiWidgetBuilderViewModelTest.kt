package com.dv.moneym.feature.overview

import app.cash.turbine.test
import com.dv.moneym.core.ai.AiAvailability
import com.dv.moneym.core.ai.AiEngine
import com.dv.moneym.core.ai.AiEngineId
import com.dv.moneym.core.ai.AiEngineRegistry
import com.dv.moneym.core.ai.ChatMessage
import com.dv.moneym.core.ai.Grounding
import com.dv.moneym.core.common.DispatcherProvider
import com.dv.moneym.core.datastore.PrefKeys
import com.dv.moneym.core.testing.FakeAppSettings
import com.dv.moneym.core.testing.FakeLlmModelRepository
import com.dv.moneym.core.testing.FakeOverviewRepository
import com.dv.moneym.core.testing.FixedClock
import com.dv.moneym.core.testing.TestDispatcherProvider
import com.dv.moneym.core.testing.runTestWithDispatchers
import com.dv.moneym.data.overview.OverviewAiWidget
import com.dv.moneym.feature.overview.a2ui.BuildOverviewWidgetPromptUseCase
import com.dv.moneym.feature.overview.a2ui.sampleA2UiJson
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Instant

class OverviewAiWidgetBuilderViewModelTest {
    private val now = Instant.parse("2026-05-10T12:00:00Z")

    @Test
    fun generateSuccessBuildsPreviewJson() = runTestWithDispatchers { dispatchers ->
        val engine = FakeAiEngine(reply = sampleA2UiJson)
        val appSettings = FakeAppSettings().apply {
            putString(PrefKeys.AI_ENGINE_ID, AiEngineId.LOCAL_LLM.name)
        }
        val vm = makeVm(engine = engine, appSettings = appSettings, dispatchers = dispatchers)

        vm.state.test {
            skipItems(1)
            awaitLoaded()
            vm.onIntent(OverviewAiWidgetBuilderIntent.TitleChanged("Cash flow"))
            vm.onIntent(OverviewAiWidgetBuilderIntent.PromptChanged("Show my cash flow"))
            vm.onIntent(OverviewAiWidgetBuilderIntent.Generate)

            val generated = awaitUntil { it.previewJson == sampleA2UiJson && it.canSave }
            assertEquals(sampleA2UiJson, generated.a2uiJson)
            assertTrue(engine.lastPrompt.contains("Allowed component types"))
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun generateInvalidJsonShowsFailure() = runTestWithDispatchers { dispatchers ->
        val vm = makeVm(engine = FakeAiEngine(reply = """{"type":"script","code":"bad"}"""), dispatchers = dispatchers)

        vm.state.test {
            skipItems(1)
            awaitLoaded()
            vm.onIntent(OverviewAiWidgetBuilderIntent.TitleChanged("Bad"))
            vm.onIntent(OverviewAiWidgetBuilderIntent.PromptChanged("Make a widget"))
            vm.onIntent(OverviewAiWidgetBuilderIntent.Generate)

            val failed = awaitUntil { it.error == OverviewAiWidgetBuilderError.InvalidJson }
            assertFalse(failed.canSave)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun saveNewWidgetUpsertsRepository() = runTestWithDispatchers { dispatchers ->
        val repo = FakeOverviewRepository()
        val vm = makeVm(repository = repo, dispatchers = dispatchers)

        vm.effects.test {
            vm.onIntent(OverviewAiWidgetBuilderIntent.TitleChanged("Cash flow"))
            vm.onIntent(OverviewAiWidgetBuilderIntent.PromptChanged("Show my cash flow"))
            vm.onIntent(OverviewAiWidgetBuilderIntent.JsonChanged(sampleA2UiJson))
            vm.onIntent(OverviewAiWidgetBuilderIntent.Save)

            assertEquals(OverviewAiWidgetBuilderEffect.Saved, awaitItem())
            assertEquals(1, repo.widgets.size)
            assertEquals("Cash flow", repo.widgets.first().title)
            assertTrue(repo.widgets.first().enabled)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun editExistingWidgetLoadsAndSaves() = runTestWithDispatchers { dispatchers ->
        val repo = FakeOverviewRepository()
        repo.replaceAiWidgets(
            listOf(
                OverviewAiWidget(
                    id = 10,
                    title = "Old",
                    prompt = "Old prompt",
                    a2uiJson = sampleA2UiJson,
                    enabled = false,
                    sortOrder = 7,
                    createdAt = now,
                    updatedAt = now,
                ),
            ),
        )
        val vm = makeVm(widgetId = 10, repository = repo, dispatchers = dispatchers)

        vm.state.test {
            skipItems(1)
            val loaded = awaitLoaded()
            assertEquals("Old", loaded.title)
            vm.onIntent(OverviewAiWidgetBuilderIntent.TitleChanged("Updated"))
            vm.onIntent(OverviewAiWidgetBuilderIntent.Save)
            awaitUntil { !it.isSaving }
            assertEquals("Updated", repo.widgets.first().title)
            assertFalse(repo.widgets.first().enabled)
            assertEquals(7, repo.widgets.first().sortOrder)
            cancelAndIgnoreRemainingEvents()
        }
    }

    private fun makeVm(
        widgetId: Long? = null,
        repository: FakeOverviewRepository = FakeOverviewRepository(),
        engine: FakeAiEngine = FakeAiEngine(reply = sampleA2UiJson),
        appSettings: FakeAppSettings = FakeAppSettings().apply {
            putString(PrefKeys.AI_ENGINE_ID, AiEngineId.LOCAL_LLM.name)
        },
        dispatchers: DispatcherProvider = TestDispatcherProvider(),
    ) = OverviewAiWidgetBuilderViewModel(
        widgetId = widgetId,
        overviewRepository = repository,
        registry = AiEngineRegistry(listOf(engine)),
        appSettings = appSettings,
        dispatchers = dispatchers,
        llmModelRepository = FakeLlmModelRepository(),
        clock = FixedClock(now),
        buildPrompt = BuildOverviewWidgetPromptUseCase(),
    )

    private suspend fun app.cash.turbine.ReceiveTurbine<OverviewAiWidgetBuilderUiState>.awaitLoaded():
            OverviewAiWidgetBuilderUiState = awaitUntil { !it.isLoading }

    private suspend fun app.cash.turbine.ReceiveTurbine<OverviewAiWidgetBuilderUiState>.awaitUntil(
        predicate: (OverviewAiWidgetBuilderUiState) -> Boolean,
    ): OverviewAiWidgetBuilderUiState {
        var item = awaitItem()
        while (!predicate(item)) item = awaitItem()
        return item
    }

    private class FakeAiEngine(
        private val reply: String,
        private val availability: AiAvailability = AiAvailability.AVAILABLE,
    ) : AiEngine {
        var lastPrompt: String = ""
        override val id: AiEngineId = AiEngineId.LOCAL_LLM
        override val supportsTools: Boolean = false
        override suspend fun availability(): AiAvailability = availability
        override fun streamReply(
            messages: List<ChatMessage>,
            grounding: Grounding,
            responseLanguage: String?,
        ): Flow<String> {
            lastPrompt = messages.last().content
            return flowOf(reply)
        }
    }
}
