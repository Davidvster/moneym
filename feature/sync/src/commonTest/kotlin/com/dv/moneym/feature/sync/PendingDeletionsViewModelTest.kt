package com.dv.moneym.feature.sync

import app.cash.turbine.test
import com.dv.moneym.data.sync.PendingDeletion
import com.dv.moneym.data.sync.SyncDeletionController
import com.dv.moneym.data.sync.SyncEntityType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

private class FakeSyncDeletionController(
    initial: List<PendingDeletion> = emptyList(),
) : SyncDeletionController {
    private val flow = MutableStateFlow(initial)
    override val pendingDeletions: Flow<List<PendingDeletion>> = flow

    var resolvedWith: Set<String>? = null
        private set

    override suspend fun resolveDeletions(confirmedSyncIds: Set<String>): Result<Unit> {
        resolvedWith = confirmedSyncIds
        return Result.success(Unit)
    }
}

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class PendingDeletionsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @BeforeTest fun setUp() { Dispatchers.setMain(testDispatcher) }
    @AfterTest fun tearDown() { Dispatchers.resetMain() }

    private fun pd(type: SyncEntityType, id: String, label: String = id) =
        PendingDeletion(entityType = type, syncId = id, label = label, remoteUpdatedAt = 0L)

    @Test
    fun builds_grouped_state_all_checked_by_default() = runTest(testDispatcher) {
        val controller = FakeSyncDeletionController(
            listOf(
                pd(SyncEntityType.ACCOUNT, "a1", "Main"),
                pd(SyncEntityType.TRANSACTION, "t1", "Coffee"),
                pd(SyncEntityType.TRANSACTION, "t2", "Lunch"),
            ),
        )
        val vm = PendingDeletionsViewModel(controller)

        vm.state.test {
            skipItems(1) // initial empty default
            val s = awaitItem()
            assertEquals(2, s.groups.size)
            // group order: ACCOUNT before TRANSACTION
            assertEquals(SyncEntityType.ACCOUNT, s.groups[0].type)
            assertEquals(SyncEntityType.TRANSACTION, s.groups[1].type)
            assertEquals(2, s.groups[1].items.size)
            assertTrue(s.groups.all { g -> g.items.all { it.checked } })
            assertEquals(3, s.selectedCount)
        }
    }

    @Test
    fun toggle_item_flips_its_checked() = runTest(testDispatcher) {
        val controller = FakeSyncDeletionController(
            listOf(pd(SyncEntityType.TRANSACTION, "t1"), pd(SyncEntityType.TRANSACTION, "t2")),
        )
        val vm = PendingDeletionsViewModel(controller)

        vm.state.test {
            skipItems(1)
            awaitItem() // populated, all checked
            vm.onIntent(PendingDeletionsIntent.ToggleItem("t1"))
            val s = awaitItem()
            val items = s.groups.single().items
            assertFalse(items.first { it.syncId == "t1" }.checked)
            assertTrue(items.first { it.syncId == "t2" }.checked)
            assertEquals(1, s.selectedCount)
        }
    }

    @Test
    fun toggle_group_unchecks_then_rechecks_all() = runTest(testDispatcher) {
        val controller = FakeSyncDeletionController(
            listOf(pd(SyncEntityType.BUDGET, "b1"), pd(SyncEntityType.BUDGET, "b2")),
        )
        val vm = PendingDeletionsViewModel(controller)

        vm.state.test {
            skipItems(1)
            awaitItem() // all checked
            vm.onIntent(PendingDeletionsIntent.ToggleGroup(SyncEntityType.BUDGET))
            val unchecked = awaitItem()
            assertTrue(unchecked.groups.single().items.none { it.checked })
            assertEquals(0, unchecked.selectedCount)

            vm.onIntent(PendingDeletionsIntent.ToggleGroup(SyncEntityType.BUDGET))
            val rechecked = awaitItem()
            assertTrue(rechecked.groups.single().items.all { it.checked })
            assertEquals(2, rechecked.selectedCount)
        }
    }

    @Test
    fun confirm_resolves_with_checked_set_and_emits_done() = runTest(testDispatcher) {
        val controller = FakeSyncDeletionController(
            listOf(pd(SyncEntityType.CATEGORY, "c1"), pd(SyncEntityType.CATEGORY, "c2")),
        )
        val vm = PendingDeletionsViewModel(controller)

        // Materialize state and uncheck c2 so only c1 is confirmed.
        vm.state.test {
            skipItems(1)
            awaitItem()
            vm.onIntent(PendingDeletionsIntent.ToggleItem("c2"))
            awaitItem()
            cancelAndIgnoreRemainingEvents()
        }

        vm.effects.test {
            vm.onIntent(PendingDeletionsIntent.ConfirmSelected)
            assertEquals(PendingDeletionsEffect.Done, awaitItem())
        }
        assertEquals(setOf("c1"), controller.resolvedWith)
    }

    @Test
    fun cancel_emits_done_without_resolving() = runTest(testDispatcher) {
        val controller = FakeSyncDeletionController(
            listOf(pd(SyncEntityType.RECURRING, "r1")),
        )
        val vm = PendingDeletionsViewModel(controller)

        vm.effects.test {
            vm.onIntent(PendingDeletionsIntent.Cancel)
            assertEquals(PendingDeletionsEffect.Done, awaitItem())
        }
        assertNull(controller.resolvedWith)
    }
}
