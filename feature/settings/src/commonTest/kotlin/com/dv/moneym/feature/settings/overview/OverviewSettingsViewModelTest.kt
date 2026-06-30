package com.dv.moneym.feature.settings.overview

import app.cash.turbine.test
import com.dv.moneym.data.overview.OverviewAiWidget
import com.dv.moneym.data.overview.OverviewLayoutBlock
import com.dv.moneym.core.testing.FakeOverviewRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
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

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class OverviewSettingsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val epoch = Instant.fromEpochMilliseconds(0)

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun vm(repository: FakeOverviewRepository = FakeOverviewRepository()) =
        OverviewSettingsViewModel(repository)

    @Test
    fun emitsDefaultBuiltInRowsWhenLayoutIsEmpty() = runTest(testDispatcher) {
        val vm = vm()

        vm.state.test {
            val state = awaitItem()
            assertEquals(defaultOverviewBuiltInBlocks().map { it.blockId }, state.builtInBlocks.map { it.blockId })
            assertTrue(state.builtInBlocks.all { it.visible })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun togglesBuiltInVisibilityAndPersistsIt() = runTest(testDispatcher) {
        val repository = FakeOverviewRepository()
        val vm = vm(repository)

        vm.state.test {
            awaitItem()
            vm.onIntent(
                OverviewSettingsIntent.SetBuiltInBlockVisible(
                    blockId = defaultOverviewBuiltInBlocks().first().blockId,
                    visible = false,
                ),
            )
            val updated = awaitItem()
            assertFalse(updated.builtInBlocks.first().visible)
            assertFalse(repository.layout.blocks.first().visible)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun reordersBuiltInBlocksAndPersistsOrder() = runTest(testDispatcher) {
        val repository = FakeOverviewRepository()
        val vm = vm(repository)
        val reordered = defaultOverviewBuiltInBlocks().map { it.blockId }.toMutableList().apply {
            add(1, removeAt(3))
        }

        vm.state.test {
            awaitItem()
            vm.onIntent(OverviewSettingsIntent.ReorderBuiltInBlocks(reordered))
            val updated = awaitItem()
            assertEquals(
                reordered,
                updated.builtInBlocks.map { it.blockId },
            )
            assertEquals(
                updated.builtInBlocks.map { it.blockId },
                repository.layout.blocks.map { it.blockId },
            )
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun resetBuiltInBlocksRestoresDefaults() = runTest(testDispatcher) {
        val repository = FakeOverviewRepository()
        repository.replaceLayout(
            listOf(
                OverviewLayoutBlock(
                    blockId = defaultOverviewBuiltInBlocks()[3].blockId,
                    sortOrder = 0,
                    visible = false,
                ),
                OverviewLayoutBlock(
                    blockId = defaultOverviewBuiltInBlocks()[0].blockId,
                    sortOrder = 1,
                    visible = true,
                ),
            ),
        )
        val vm = vm(repository)

        vm.state.test {
            skipItems(1)
            advanceUntilIdle()
            var current = awaitItem()
            while (current.builtInBlocks == defaultOverviewBuiltInBlocks()) {
                current = awaitItem()
            }
            vm.onIntent(OverviewSettingsIntent.ResetBuiltInBlocks)
            advanceUntilIdle()
            var updated = awaitItem()
            while (updated.builtInBlocks != defaultOverviewBuiltInBlocks()) {
                updated = awaitItem()
            }
            assertEquals(defaultOverviewBuiltInBlocks().map { it.blockId }, updated.builtInBlocks.map { it.blockId })
            assertTrue(updated.builtInBlocks.all { it.visible })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun togglesAiWidgetEnabledAndPersistsIt() = runTest(testDispatcher) {
        val repository = FakeOverviewRepository()
        val id = repository.upsertAiWidget(
            OverviewAiWidget(
                title = "Widget",
                prompt = "Prompt",
                a2uiJson = "{}",
                enabled = true,
                sortOrder = 0,
                createdAt = epoch,
                updatedAt = epoch,
            ),
        )
        val vm = vm(repository)

        vm.state.test {
            var state = awaitItem()
            while (state.aiWidgets.isEmpty()) {
                state = awaitItem()
            }
            assertEquals(1, state.aiWidgets.size)
            vm.onIntent(OverviewSettingsIntent.SetAiWidgetEnabled(id, false))
            advanceUntilIdle()
            var updated = awaitItem()
            while (updated.aiWidgets.first().enabled) {
                updated = awaitItem()
            }
            assertFalse(updated.aiWidgets.first().enabled)
            assertFalse(repository.widgets.first().enabled)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun emitsBuilderEffectsForCreateAndEdit() = runTest(testDispatcher) {
        val vm = vm()

        vm.effects.test {
            vm.onIntent(OverviewSettingsIntent.CreateAiWidget)
            assertEquals(OverviewSettingsEffect.OpenAiWidgetBuilder(null), awaitItem())

            vm.onIntent(OverviewSettingsIntent.EditAiWidget(42L))
            assertEquals(OverviewSettingsEffect.OpenAiWidgetBuilder(42L), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }
}
