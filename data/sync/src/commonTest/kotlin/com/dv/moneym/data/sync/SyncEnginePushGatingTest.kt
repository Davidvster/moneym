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
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.time.Instant

class SyncEnginePushGatingTest {

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
        )
    }

    @Test
    fun pushIsNoOpWhileStoreNonEmptyAndResumesAfterClear() = runTestWithDispatchers { dispatchers ->
        val device = Device().apply { accounts.addAll(listOf(account(1))) }
        val store = SyncRemoteStore(FakeRemoteBackupProvider())
        val settings = InMemoryAppSettings()
        val pendingStore = PendingDeletionStore(settings)
        pendingStore.replaceAll(
            listOf(PendingDeletion(SyncEntityType.ACCOUNT, "sync-x", "X", 1L)),
        )
        val engine = engine(device, store, settings, dispatchers, pendingStore)

        engine.push().getOrThrow()
        assertNull(store.readSnapshotBytes())

        pendingStore.clear()
        engine.push().getOrThrow()
        assertNotNull(store.readSnapshotBytes())
    }
}
