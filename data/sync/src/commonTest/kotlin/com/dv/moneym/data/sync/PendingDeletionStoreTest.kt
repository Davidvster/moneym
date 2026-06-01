package com.dv.moneym.data.sync

import app.cash.turbine.test
import com.dv.moneym.core.datastore.PrefKeys
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

class PendingDeletionStoreTest {

    private fun deletion(syncId: String, type: SyncEntityType = SyncEntityType.ACCOUNT) =
        PendingDeletion(entityType = type, syncId = syncId, label = "label-$syncId", remoteUpdatedAt = 1L)

    @Test
    fun replaceAllThenCurrentRoundTrips() {
        val settings = InMemoryAppSettings()
        val store = PendingDeletionStore(settings)

        val list = listOf(deletion("a"), deletion("b", SyncEntityType.BUDGET))
        store.replaceAll(list)

        store.current() shouldBe list
    }

    @Test
    fun replaceAllWithEmptyClearsBlob() {
        val settings = InMemoryAppSettings()
        val store = PendingDeletionStore(settings)
        store.replaceAll(listOf(deletion("a")))

        store.replaceAll(emptyList())

        store.current() shouldBe emptyList()
        settings.getString(PrefKeys.PENDING_DELETION_BLOB) shouldBe null
    }

    @Test
    fun clearRemovesAll() {
        val settings = InMemoryAppSettings()
        val store = PendingDeletionStore(settings)
        store.replaceAll(listOf(deletion("a")))

        store.clear()

        store.current() shouldBe emptyList()
    }

    @Test
    fun currentOnEmptyBlobIsEmpty() {
        val store = PendingDeletionStore(InMemoryAppSettings())
        store.current() shouldBe emptyList()
    }

    @Test
    fun currentOnGarbageBlobIsEmpty() {
        val settings = InMemoryAppSettings()
        settings.putString(PrefKeys.PENDING_DELETION_BLOB, "not-json")
        val store = PendingDeletionStore(settings)

        store.current() shouldBe emptyList()
    }

    @Test
    fun hasPendingAndCountFlowsReflectStore() = runTest {
        val settings = InMemoryAppSettings()
        val store = PendingDeletionStore(settings)

        store.hasPending.test {
            awaitItem() shouldBe false
            store.replaceAll(listOf(deletion("a"), deletion("b")))
            awaitItem() shouldBe true
            store.clear()
            awaitItem() shouldBe false
            cancelAndIgnoreRemainingEvents()
        }

        store.count.test {
            awaitItem() shouldBe 0
            store.replaceAll(listOf(deletion("a"), deletion("b")))
            awaitItem() shouldBe 2
            cancelAndIgnoreRemainingEvents()
        }
    }
}
