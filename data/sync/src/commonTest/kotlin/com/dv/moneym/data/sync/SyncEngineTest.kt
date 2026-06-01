package com.dv.moneym.data.sync

import com.dv.moneym.core.datastore.PrefKeys
import com.dv.moneym.core.model.Account
import com.dv.moneym.core.model.AccountId
import com.dv.moneym.core.model.AccountType
import com.dv.moneym.core.model.Category
import com.dv.moneym.core.model.CategoryId
import com.dv.moneym.core.model.CurrencyCode
import com.dv.moneym.core.model.TransactionType
import com.dv.moneym.core.testing.FakeAccountRepository
import com.dv.moneym.core.testing.FakeBudgetRepository
import com.dv.moneym.core.testing.FakeCategoryRepository
import com.dv.moneym.core.testing.FakeRecurringTransactionRepository
import com.dv.moneym.core.testing.FakeTransactionRepository
import com.dv.moneym.core.common.DispatcherProvider
import com.dv.moneym.core.testing.runTestWithDispatchers
import com.dv.moneym.data.remotebackup.SessionPassphrase
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Instant

class SyncEngineTest {

    private val epoch = Instant.fromEpochMilliseconds(0)
    private val eur = CurrencyCode("EUR")

    private class Device {
        val accounts = FakeAccountRepository()
        val categories = FakeCategoryRepository()
        val paymentModes = FakePaymentModeRepository()
        val transactions = FakeTransactionRepository()
        val recurring = FakeRecurringTransactionRepository()
        val budgets = FakeBudgetRepository()
    }

    private fun account(id: Long, updatedAt: Long = 0L, name: String = "Acc $id") = Account(
        id = AccountId(id),
        name = name,
        type = AccountType.CASH,
        currency = CurrencyCode("EUR"),
        isDefault = false,
        archived = false,
        createdAt = Instant.fromEpochMilliseconds(0),
        updatedAt = Instant.fromEpochMilliseconds(updatedAt),
    )

    private fun category(id: Long) = Category(
        id = CategoryId(id),
        name = "Cat $id",
        iconKey = "icon",
        colorHex = "#FFFFFF",
        isUserCreated = false,
        archived = false,
        createdAt = epoch,
        updatedAt = epoch,
        type = TransactionType.EXPENSE,
    )

    private fun exporter(device: Device, deviceId: String, settings: InMemoryAppSettings): SyncExporter {
        settings.putString(PrefKeys.DEVICE_ID, deviceId)
        return SyncExporter(
            accountRepository = device.accounts,
            categoryRepository = device.categories,
            paymentModeRepository = device.paymentModes,
            transactionRepository = device.transactions,
            recurringTransactionRepository = device.recurring,
            budgetRepository = device.budgets,
            deviceIdentity = DeviceIdentity(settings),
            nowMs = { 999L },
        )
    }

    private fun applier(device: Device) = SyncApplier(
        accountRepository = device.accounts,
        categoryRepository = device.categories,
        paymentModeRepository = device.paymentModes,
        transactionRepository = device.transactions,
        recurringTransactionRepository = device.recurring,
        budgetRepository = device.budgets,
    )

    private fun engine(
        device: Device,
        store: SyncRemoteStore,
        settings: InMemoryAppSettings,
        dispatchers: DispatcherProvider,
        deviceId: String = "device-A",
        encrypt: Boolean = false,
        passphrase: SessionPassphrase = SessionPassphrase(),
        pendingDeletionStore: PendingDeletionStore = PendingDeletionStore(settings),
    ): SyncEngine {
        settings.putBoolean(PrefKeys.REMOTE_BACKUP_ENCRYPT, encrypt)
        return SyncEngine(
            exporter = exporter(device, deviceId, settings),
            reconciler = SyncReconciler(),
            applier = applier(device),
            codec = SyncSnapshotCodec(crypto = FakeBackupCrypto(), appVersion = "test"),
            store = store,
            appSettings = settings,
            sessionPassphrase = passphrase,
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
    fun pullOnEmptyRemotePushesLocalSeed() = runTestWithDispatchers { dispatchers ->
        val device = Device().apply {
            accounts.addAll(listOf(account(1)))
            categories.addAll(listOf(category(2)))
        }
        val provider = FakeRemoteBackupProvider()
        val store = SyncRemoteStore(provider)
        val settings = InMemoryAppSettings()
        val engine = engine(device, store, settings, dispatchers)

        engine.pull().getOrThrow()

        val bytes = store.readSnapshotBytes()
        assertTrue(bytes != null)
        val codec = SyncSnapshotCodec(crypto = FakeBackupCrypto(), appVersion = "test")
        val remote = codec.open(bytes, null)
        assertEquals(1, remote.accounts.size)
        assertEquals(1, remote.categories.size)
    }

    @Test
    fun pullAppliesRemoteAddsAndExposesPendingDeletionsWithoutDeleting() = runTestWithDispatchers { dispatchers ->
        // Remote authored by another device: one new account (add) + a tombstone for a live local account.
        val remoteDevice = Device().apply {
            accounts.addAll(listOf(account(50, updatedAt = 100L, name = "Remote new")))
        }
        val remoteSettings = InMemoryAppSettings()
        val remoteSnapshot = exporter(remoteDevice, "device-B", remoteSettings).export()
        val remoteAccSyncId = remoteSnapshot.accounts.single().syncId

        val provider = FakeRemoteBackupProvider()
        val store = SyncRemoteStore(provider)

        // Local device has an account whose syncId will appear as a tombstone in remote.
        val localDevice = Device().apply { accounts.addAll(listOf(account(1, name = "Local live"))) }
        val localSettings = InMemoryAppSettings()
        val pendingDeletionStore = PendingDeletionStore(localSettings)
        val engine = engine(localDevice, store, localSettings, dispatchers, pendingDeletionStore = pendingDeletionStore)
        val localAccSyncId = localDevice.accounts.exportForSync().single().syncId!!

        // Compose the remote bytes: the new account + a tombstone of the local account's syncId.
        val tombstone = remoteSnapshot.accounts.single().copy(
            syncId = localAccSyncId,
            name = "gone",
            deleted = true,
            updatedAt = 200L,
        )
        val composedRemote = remoteSnapshot.copy(
            accounts = listOf(remoteSnapshot.accounts.single(), tombstone),
        )
        val codec = SyncSnapshotCodec(crypto = FakeBackupCrypto(), appVersion = "test")
        store.writeSnapshotBytes(codec.seal(composedRemote, null))

        engine.pull().getOrThrow()

        // Remote add applied.
        assertTrue(localDevice.accounts.accounts.any { it.name == "Remote new" })
        // Local live account NOT deleted (pending only).
        assertTrue(localDevice.accounts.accounts.any { it.name == "Local live" })
        val pd = pendingDeletionStore.current().single()
        assertEquals(SyncEntityType.ACCOUNT, pd.entityType)
        assertEquals(localAccSyncId, pd.syncId)
        assertEquals("Local live", pd.label)
        // Sanity: remote new account had its own distinct syncId.
        assertTrue(remoteAccSyncId != localAccSyncId)
    }

    @Test
    fun pushWritesSnapshotEqualToLocalExportPlaintext() = runTestWithDispatchers { dispatchers ->
        val device = Device().apply {
            accounts.addAll(listOf(account(1)))
            categories.addAll(listOf(category(2)))
        }
        val provider = FakeRemoteBackupProvider()
        val store = SyncRemoteStore(provider)
        val settings = InMemoryAppSettings()
        val engine = engine(device, store, settings, dispatchers)
        val expected = exporter(device, "device-A", settings).export()

        engine.push().getOrThrow()

        val codec = SyncSnapshotCodec(crypto = FakeBackupCrypto(), appVersion = "test")
        val written = codec.open(store.readSnapshotBytes()!!, null)
        assertEquals(expected, written)
    }

    @Test
    fun pushRoundTripsWithEncryptionUsingSessionPassphrase() = runTestWithDispatchers { dispatchers ->
        val device = Device().apply { accounts.addAll(listOf(account(1))) }
        val provider = FakeRemoteBackupProvider()
        val store = SyncRemoteStore(provider)
        val settings = InMemoryAppSettings()
        val passphrase = SessionPassphrase().apply { set("pw".toCharArray()) }
        val engine = engine(device, store, settings, dispatchers, encrypt = true, passphrase = passphrase)
        val expected = exporter(device, "device-A", settings).export()

        engine.push().getOrThrow()

        val codec = SyncSnapshotCodec(crypto = FakeBackupCrypto(), appVersion = "test")
        val written = codec.open(store.readSnapshotBytes()!!, "pw".toCharArray())
        assertEquals(expected.accounts, written.accounts)
    }

    @Test
    fun pushSkippedWhenEncryptionOnButNoPassphrase() = runTestWithDispatchers { dispatchers ->
        val device = Device().apply { accounts.addAll(listOf(account(1))) }
        val provider = FakeRemoteBackupProvider()
        val store = SyncRemoteStore(provider)
        val settings = InMemoryAppSettings()
        val engine = engine(device, store, settings, dispatchers, encrypt = true)

        engine.push().getOrThrow()

        assertNull(store.readSnapshotBytes())
    }
}
