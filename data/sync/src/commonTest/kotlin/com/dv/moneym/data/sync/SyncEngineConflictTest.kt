package com.dv.moneym.data.sync

import com.dv.moneym.core.common.DispatcherProvider
import com.dv.moneym.core.datastore.PrefKeys
import com.dv.moneym.core.model.Account
import com.dv.moneym.core.model.AccountId
import com.dv.moneym.core.model.AccountType
import com.dv.moneym.core.model.CurrencyCode
import com.dv.moneym.core.security.BackupCrypto
import com.dv.moneym.core.security.BackupCryptoError
import com.dv.moneym.core.security.CipherParams
import com.dv.moneym.core.security.EncryptedBackup
import com.dv.moneym.core.security.KdfParams
import com.dv.moneym.core.testing.FakeAccountRepository
import com.dv.moneym.core.testing.FakeBudgetRepository
import com.dv.moneym.core.testing.FakeCategoryRepository
import com.dv.moneym.core.testing.FakeRecurringTransactionRepository
import com.dv.moneym.core.testing.FakeTransactionRepository
import com.dv.moneym.core.testing.InMemorySecureStore
import com.dv.moneym.core.testing.runTestWithDispatchers
import com.dv.moneym.data.remotebackup.SessionPassphrase
import com.dv.moneym.data.remotebackup.SyncPassphraseStore
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Instant

/** A passphrase-binding fake so wrong-password validation actually throws WrongPassphrase. */
@OptIn(ExperimentalEncodingApi::class)
private class PwCrypto : BackupCrypto {
    override suspend fun encrypt(
        plain: ByteArray,
        passphrase: CharArray,
        schema: Int,
        appVersion: String,
        createdAt: Long,
    ): EncryptedBackup = EncryptedBackup(
        schema = schema,
        createdAt = createdAt,
        appVersion = appVersion,
        kdf = KdfParams(iter = 1, saltB64 = Base64.encode(passphrase.concatToString().encodeToByteArray())),
        cipher = CipherParams(ivB64 = "", ctB64 = Base64.encode(plain)),
    )

    override suspend fun decrypt(envelope: EncryptedBackup, passphrase: CharArray): ByteArray {
        val expected = Base64.encode(passphrase.concatToString().encodeToByteArray())
        if (envelope.kdf.saltB64 != expected) throw BackupCryptoError.WrongPassphrase()
        return Base64.decode(envelope.cipher.ctB64)
    }
}

class SyncEngineConflictTest {

    private val eur = CurrencyCode("EUR")

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
        currency = eur,
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
        deviceId: String,
        crypto: BackupCrypto,
        encrypt: Boolean,
        passphrase: SessionPassphrase = SessionPassphrase(),
    ): SyncEngine {
        settings.putBoolean(PrefKeys.REMOTE_BACKUP_ENCRYPT, encrypt)
        settings.putString(PrefKeys.DEVICE_ID, deviceId)
        val exporter = SyncExporter(
            accountRepository = device.accounts,
            categoryRepository = device.categories,
            paymentModeRepository = device.paymentModes,
            transactionRepository = device.transactions,
            recurringTransactionRepository = device.recurring,
            budgetRepository = device.budgets,
            deviceIdentity = DeviceIdentity(settings),
            nowMs = { 999L },
        )
        val applier = SyncApplier(
            accountRepository = device.accounts,
            categoryRepository = device.categories,
            paymentModeRepository = device.paymentModes,
            transactionRepository = device.transactions,
            recurringTransactionRepository = device.recurring,
            budgetRepository = device.budgets,
        )
        return SyncEngine(
            exporter = exporter,
            reconciler = SyncReconciler(),
            applier = applier,
            codec = SyncSnapshotCodec(crypto = crypto, appVersion = "test"),
            store = store,
            appSettings = settings,
            sessionPassphrase = passphrase,
            syncPassphraseStore = SyncPassphraseStore(InMemorySecureStore()),
            dispatchers = dispatchers,
            pendingDeletionStore = PendingDeletionStore(settings),
            conflictStore = SyncConflictStore(settings),
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
    fun encryptedRemoteWithoutPassphrase_raisesConflict() = runTestWithDispatchers { dispatchers ->
        val provider = FakeRemoteBackupProvider()
        val crypto = FakeBackupCrypto()
        // Device A seeds an encrypted remote.
        val a = Device().apply { accounts.addAll(listOf(account(1))) }
        val aSettings = InMemoryAppSettings()
        engine(a, SyncRemoteStore(provider), aSettings, dispatchers, "A", crypto, encrypt = true,
            passphrase = SessionPassphrase().apply { set("pw".toCharArray()) }).pull()

        // Device B has no passphrase.
        val b = Device()
        val bSettings = InMemoryAppSettings()
        val bEngine = engine(b, SyncRemoteStore(provider), bSettings, dispatchers, "B", crypto, encrypt = false)
        bEngine.pull()

        assertEquals(SyncConflict(remoteEncrypted = true), SyncConflictStore(bSettings).current())
        assertTrue(b.accounts.accounts.isEmpty(), "no data applied while conflicted")
    }

    @Test
    fun resolveWithPassword_clearsConflictAndMerges() = runTestWithDispatchers { dispatchers ->
        val provider = FakeRemoteBackupProvider()
        val crypto = FakeBackupCrypto()
        val a = Device().apply { accounts.addAll(listOf(account(1))) }
        engine(a, SyncRemoteStore(provider), InMemoryAppSettings(), dispatchers, "A", crypto, encrypt = true,
            passphrase = SessionPassphrase().apply { set("pw".toCharArray()) }).pull()

        val b = Device()
        val bSettings = InMemoryAppSettings()
        val bEngine = engine(b, SyncRemoteStore(provider), bSettings, dispatchers, "B", crypto, encrypt = false)
        bEngine.pull()
        assertEquals(SyncConflict(remoteEncrypted = true), SyncConflictStore(bSettings).current())

        bEngine.resolveWithPassword("pw".toCharArray(), makeAuthoritative = false).getOrThrow()

        assertNull(SyncConflictStore(bSettings).current())
        assertEquals(1, b.accounts.accounts.size)
    }

    @Test
    fun plaintextRemoteWhileEncrypted_raisesConflict() = runTestWithDispatchers { dispatchers ->
        val provider = FakeRemoteBackupProvider()
        val crypto = FakeBackupCrypto()
        val a = Device().apply { accounts.addAll(listOf(account(1))) }
        engine(a, SyncRemoteStore(provider), InMemoryAppSettings(), dispatchers, "A", crypto, encrypt = false).pull()

        val b = Device()
        val bSettings = InMemoryAppSettings()
        val bEngine = engine(b, SyncRemoteStore(provider), bSettings, dispatchers, "B", crypto, encrypt = true,
            passphrase = SessionPassphrase().apply { set("pw".toCharArray()) })
        bEngine.pull()

        assertEquals(SyncConflict(remoteEncrypted = false), SyncConflictStore(bSettings).current())
    }

    @Test
    fun resolveWithPlaintext_clearsConflictAndMerges() = runTestWithDispatchers { dispatchers ->
        val provider = FakeRemoteBackupProvider()
        val crypto = FakeBackupCrypto()
        val a = Device().apply { accounts.addAll(listOf(account(1))) }
        engine(a, SyncRemoteStore(provider), InMemoryAppSettings(), dispatchers, "A", crypto, encrypt = false).pull()

        val b = Device()
        val bSettings = InMemoryAppSettings()
        val bEngine = engine(b, SyncRemoteStore(provider), bSettings, dispatchers, "B", crypto, encrypt = true,
            passphrase = SessionPassphrase().apply { set("pw".toCharArray()) })
        bEngine.pull()

        bEngine.resolveWithPlaintext().getOrThrow()

        assertNull(SyncConflictStore(bSettings).current())
        assertEquals(false, bSettings.getBoolean(PrefKeys.REMOTE_BACKUP_ENCRYPT, defaultValue = true))
        assertEquals(1, b.accounts.accounts.size)
    }

    @Test
    fun resolveWithWrongPassword_keepsConflict() = runTestWithDispatchers { dispatchers ->
        val provider = FakeRemoteBackupProvider()
        val crypto = PwCrypto()
        val a = Device().apply { accounts.addAll(listOf(account(1))) }
        engine(a, SyncRemoteStore(provider), InMemoryAppSettings(), dispatchers, "A", crypto, encrypt = true,
            passphrase = SessionPassphrase().apply { set("pw".toCharArray()) }).pull()

        val b = Device()
        val bSettings = InMemoryAppSettings()
        val bEngine = engine(b, SyncRemoteStore(provider), bSettings, dispatchers, "B", crypto, encrypt = false)
        bEngine.pull()
        assertEquals(SyncConflict(remoteEncrypted = true), SyncConflictStore(bSettings).current())

        val result = bEngine.resolveWithPassword("wrong".toCharArray(), makeAuthoritative = false)
        assertTrue(result.isFailure)
        assertEquals(SyncConflict(remoteEncrypted = true), SyncConflictStore(bSettings).current())
    }
}
