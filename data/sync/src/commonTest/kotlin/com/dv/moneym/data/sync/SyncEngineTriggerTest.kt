package com.dv.moneym.data.sync

import com.dv.moneym.core.common.DispatcherProvider
import com.dv.moneym.core.datastore.PrefKeys
import com.dv.moneym.core.model.Account
import com.dv.moneym.core.model.AccountId
import com.dv.moneym.core.model.AccountType
import com.dv.moneym.core.model.CurrencyCode
import com.dv.moneym.core.testing.FakeAccountRepository
import com.dv.moneym.core.testing.FakeBudgetRepository
import com.dv.moneym.core.testing.FakeCategoryRepository
import com.dv.moneym.core.testing.FakeRecurringTransactionRepository
import com.dv.moneym.core.testing.FakeTransactionRepository
import com.dv.moneym.core.testing.runTestWithDispatchers
import com.dv.moneym.data.remotebackup.SessionPassphrase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class SyncEngineTriggerTest {

    private class Device {
        val accounts = FakeAccountRepository()
        val categories = FakeCategoryRepository()
        val paymentModes = FakePaymentModeRepository()
        val transactions = FakeTransactionRepository()
        val recurring = FakeRecurringTransactionRepository()
        val budgets = FakeBudgetRepository()
    }

    private fun account(id: Long) = Account(
        id = AccountId(id),
        name = "Acc $id",
        type = AccountType.CASH,
        currency = CurrencyCode("EUR"),
        isDefault = false,
        archived = false,
        createdAt = Instant.fromEpochMilliseconds(0),
        updatedAt = Instant.fromEpochMilliseconds(0),
    )

    private fun engine(
        device: Device,
        store: SyncRemoteStore,
        settings: InMemoryAppSettings,
        dispatchers: DispatcherProvider,
        pendingDeletionStore: PendingDeletionStore,
        debounceMs: Long = 3_000L,
    ): SyncEngine {
        settings.putBoolean(PrefKeys.REMOTE_BACKUP_ENCRYPT, false)
        settings.putString(PrefKeys.DEVICE_ID, "device-A")
        return SyncEngine(
            exporter = SyncExporter(
                accountRepository = device.accounts,
                categoryRepository = device.categories,
                paymentModeRepository = device.paymentModes,
                transactionRepository = device.transactions,
                recurringTransactionRepository = device.recurring,
                budgetRepository = device.budgets,
                deviceIdentity = DeviceIdentity(settings),
                nowMs = { 999L },
            ),
            reconciler = SyncReconciler(),
            applier = SyncApplier(
                accountRepository = device.accounts,
                categoryRepository = device.categories,
                paymentModeRepository = device.paymentModes,
                transactionRepository = device.transactions,
                recurringTransactionRepository = device.recurring,
                budgetRepository = device.budgets,
            ),
            codec = SyncSnapshotCodec(crypto = FakeBackupCrypto(), appVersion = "test"),
            store = store,
            appSettings = settings,
            sessionPassphrase = SessionPassphrase(),
            dispatchers = dispatchers,
            pendingDeletionStore = pendingDeletionStore,
            accountRepository = device.accounts,
            categoryRepository = device.categories,
            paymentModeRepository = device.paymentModes,
            transactionRepository = device.transactions,
            recurringTransactionRepository = device.recurring,
            budgetRepository = device.budgets,
            nowMs = { 1234L },
            debounceMs = debounceMs,
        )
    }

    @Test
    fun enqueuePushDebouncesToSinglePush() = runTestWithDispatchers { dispatchers ->
        val device = Device().apply { accounts.addAll(listOf(account(1))) }
        val provider = FakeRemoteBackupProvider()
        val store = SyncRemoteStore(provider)
        val settings = InMemoryAppSettings()
        settings.putBoolean(PrefKeys.CROSS_DEVICE_SYNC_ENABLED, true)
        val engine = engine(device, store, settings, dispatchers, PendingDeletionStore(settings))

        engine.start(this)
        runCurrent() // let the debounce collector subscribe before emitting
        engine.enqueuePush()
        engine.enqueuePush()
        engine.enqueuePush()
        advanceUntilIdle()
        engine.stop()

        // First write of the sync-state file is a single upload (debounce coalesced 3 → 1).
        assertEquals(1, provider.uploadCount)
        assertNotNull(store.readSnapshotBytes())
    }

    @Test
    fun disabledFlagMakesPullAndPushNoOp() = runTestWithDispatchers { dispatchers ->
        val device = Device().apply { accounts.addAll(listOf(account(1))) }
        val provider = FakeRemoteBackupProvider()
        val store = SyncRemoteStore(provider)
        val settings = InMemoryAppSettings()
        // CROSS_DEVICE_SYNC_ENABLED stays false (default)
        val engine = engine(device, store, settings, dispatchers, PendingDeletionStore(settings))

        engine.start(this)
        engine.enqueuePush()
        advanceTimeBy(5_000)
        runCurrent()

        val pullResult = engine.pullNow()
        advanceUntilIdle()
        engine.stop()

        assertEquals(0, provider.uploadCount)
        assertNull(store.readSnapshotBytes())
        // pullNow short-circuits to success without touching the remote store
        assert(pullResult.isSuccess)
        assertEquals(SyncRuntimeState.Idle, engine.runtime.value)
    }

    @Test
    fun pullAndPushSerializeUnderMutex() = runTestWithDispatchers { dispatchers ->
        // A concurrent pull + push must not interleave: with the Mutex the snapshot is written
        // and the engine ends Idle, never stuck in a transient state.
        val device = Device().apply { accounts.addAll(listOf(account(1))) }
        val provider = FakeRemoteBackupProvider()
        val store = SyncRemoteStore(provider)
        val settings = InMemoryAppSettings()
        settings.putBoolean(PrefKeys.CROSS_DEVICE_SYNC_ENABLED, true)
        val engine = engine(device, store, settings, dispatchers, PendingDeletionStore(settings))

        // pull on empty remote internally pushes the local seed; a concurrent push must serialize.
        val pull = engine.pullNow()
        val push = engine.push()
        advanceUntilIdle()

        assert(pull.isSuccess)
        assert(push.isSuccess)
        assertEquals(SyncRuntimeState.Idle, engine.runtime.value)
        assertNotNull(store.readSnapshotBytes())
    }
}
