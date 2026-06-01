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
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Instant

class SyncEngineDeletionResolveTest {

    private class Device {
        val accounts = FakeAccountRepository()
        val categories = FakeCategoryRepository()
        val paymentModes = FakePaymentModeRepository()
        val transactions = FakeTransactionRepository()
        val recurring = FakeRecurringTransactionRepository()
        val budgets = FakeBudgetRepository()
    }

    private fun account(id: Long, updatedAt: Long = 0L) = Account(
        id = AccountId(id),
        name = "Acc $id",
        type = AccountType.CASH,
        currency = CurrencyCode("EUR"),
        isDefault = false,
        archived = false,
        createdAt = Instant.fromEpochMilliseconds(0),
        updatedAt = Instant.fromEpochMilliseconds(updatedAt),
    )

    private fun engine(
        device: Device,
        settings: InMemoryAppSettings,
        dispatchers: DispatcherProvider,
        pendingDeletionStore: PendingDeletionStore,
        now: Long = 5000L,
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
            store = SyncRemoteStore(FakeRemoteBackupProvider()),
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
            nowMs = { now },
        )
    }

    @Test
    fun confirmTombstonesAndDeclineRevivesThenClearsStore() = runTestWithDispatchers { dispatchers ->
        val device = Device().apply {
            accounts.addAll(listOf(account(1, updatedAt = 100L), account(2, updatedAt = 100L)))
        }
        val confirmSyncId = device.accounts.exportForSync().single { it.id == 1L }.syncId!!
        val declineSyncId = device.accounts.exportForSync().single { it.id == 2L }.syncId!!

        val settings = InMemoryAppSettings()
        val pendingStore = PendingDeletionStore(settings)
        pendingStore.replaceAll(
            listOf(
                PendingDeletion(SyncEntityType.ACCOUNT, confirmSyncId, "Acc 1", 200L),
                PendingDeletion(SyncEntityType.ACCOUNT, declineSyncId, "Acc 2", 200L),
            ),
        )
        val engine = engine(device, settings, dispatchers, pendingStore, now = 5000L)

        engine.resolveDeletions(confirmedSyncIds = setOf(confirmSyncId)).getOrThrow()

        // Confirmed: tombstoned — excluded from observeAll, exported with deleted=true.
        assertFalse(device.accounts.accounts.any { it.id.value == 1L })
        val confirmedRow = device.accounts.exportForSync().single { it.syncId == confirmSyncId }
        assertTrue(confirmedRow.deleted)
        assertEquals(5000L, confirmedRow.updatedAt)

        // Declined: kept live, updatedAt bumped to now, not deleted.
        assertTrue(device.accounts.accounts.any { it.id.value == 2L })
        val declinedRow = device.accounts.exportForSync().single { it.syncId == declineSyncId }
        assertFalse(declinedRow.deleted)
        assertEquals(5000L, declinedRow.updatedAt)

        // Store cleared.
        assertTrue(pendingStore.current().isEmpty())
    }
}
