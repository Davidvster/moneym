package com.dv.moneym.data.banksync

import com.dv.moneym.core.datastore.PrefKeys
import com.dv.moneym.core.testing.FakeAppSettings
import com.dv.moneym.core.testing.FakeBankSyncRepository
import com.dv.moneym.core.testing.FakeTransactionRepository
import com.dv.moneym.core.testing.FixedClock
import com.dv.moneym.core.testing.InMemorySecureStore
import com.dv.moneym.core.model.CurrencyCode
import com.dv.moneym.core.model.Money
import com.dv.moneym.core.model.AccountId
import com.dv.moneym.core.model.CategoryId
import com.dv.moneym.core.model.Transaction
import com.dv.moneym.core.model.TransactionId
import com.dv.moneym.core.model.TransactionType
import com.dv.moneym.core.model.UNSAVED_TRANSACTION_ID
import com.dv.moneym.data.banksync.internal.platformCryptographyProvider
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlin.time.Instant
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate

private class ScriptedEnableBankingClient : EnableBankingClient {
    val pages = mutableMapOf<String, MutableList<EbTransactionsPage>>()
    var fetchCount = 0
    var failure: EbError? = null

    fun script(accountUid: String, vararg scripted: EbTransactionsPage) {
        pages[accountUid] = scripted.toMutableList()
    }

    override suspend fun validateCredentials(credentials: EbCredentials) = Result.success(Unit)
    override suspend fun listBanks(country: String) = Result.success(emptyList<EbBank>())
    override suspend fun startAuth(bank: EbBank, redirectUrl: String, validUntil: Instant, state: String) =
        Result.success(EbAuthStart("https://example/auth"))

    override suspend fun createSession(code: String) =
        Result.success(EbSessionInfo("sess", emptyList()))

    override suspend fun getSessionStatus(sessionId: String) =
        Result.success(EbSessionStatus("AUTHORIZED"))

    override suspend fun fetchTransactions(
        accountUid: String,
        dateFrom: LocalDate,
        continuationKey: String?,
    ): Result<EbTransactionsPage> {
        failure?.let { return Result.failure(it) }
        fetchCount++
        val queue = pages[accountUid] ?: return Result.success(EbTransactionsPage(emptyList()))
        return Result.success(if (queue.isEmpty()) EbTransactionsPage(emptyList()) else queue.removeAt(0))
    }

    override suspend fun deleteSession(sessionId: String) = Result.success(Unit)
}

class BankSyncEngineTest {

    private val clock = FixedClock(Instant.parse("2026-06-10T12:00:00Z"))
    private val settings = FakeAppSettings()
    private val client = ScriptedEnableBankingClient()
    private val bankRepo = FakeBankSyncRepository()
    private val txRepo = FakeTransactionRepository()
    private val secureStore = InMemorySecureStore()
    private val credentialsStore = EnableBankingCredentialsStore(secureStore)

    private fun engine() = BankSyncEngine(
        client = client,
        credentialsStore = credentialsStore,
        bankSyncRepository = bankRepo,
        transactionRepository = txRepo,
        externalIdResolver = ExternalIdResolver(platformCryptographyProvider()),
        appSettings = settings,
        clock = clock,
    )

    private suspend fun connect() {
        credentialsStore.saveCredentials(EbCredentials("app-1", "pem"))
        credentialsStore.saveSessionId("sess-1")
    }

    private fun tx(ref: String?, amount: Long = 1250, date: LocalDate = LocalDate(2026, 6, 8)) =
        EbTransactionData(
            entryReference = ref,
            amountMinor = amount,
            currency = "EUR",
            direction = EbDirection.DEBIT,
            bookingDate = date,
            description = "desc-$ref",
        )

    private fun account(uid: String = "acc-1", lastSynced: LocalDate? = null) = BankAccountLink(
        uid = uid,
        bankName = "Tatra",
        currency = "EUR",
        lastSyncedDate = lastSynced,
    )

    @Test
    fun insertsNewSuggestionsAndAdvancesCursor() = runTest {
        connect()
        bankRepo.upsertAccounts(listOf(account()))
        client.script("acc-1", EbTransactionsPage(listOf(tx("r1"), tx("r2", amount = 500))))

        engine().syncNow().getOrThrow()

        assertEquals(2, bankRepo.suggestions.size)
        assertTrue(bankRepo.suggestions.all { it.status == SuggestionStatus.PENDING })
        assertEquals(clock.today(), bankRepo.accounts.single().lastSyncedDate)
        assertTrue(settings.getLong(PrefKeys.BANK_SYNC_LAST_SYNC_MS) > 0)
    }

    @Test
    fun paginatesThroughContinuationKeys() = runTest {
        connect()
        bankRepo.upsertAccounts(listOf(account()))
        client.script(
            "acc-1",
            EbTransactionsPage(listOf(tx("r1")), continuationKey = "k1"),
            EbTransactionsPage(listOf(tx("r2"))),
        )

        engine().syncNow().getOrThrow()

        assertEquals(2, bankRepo.suggestions.size)
        assertEquals(2, client.fetchCount)
    }

    @Test
    fun skipsAlreadyKnownSuggestionsAndImportedTransactions() = runTest {
        connect()
        bankRepo.upsertAccounts(listOf(account()))
        client.script("acc-1", EbTransactionsPage(listOf(tx("r1"), tx("r2"), tx("r3"))))
        bankRepo.insertSuggestionsIfNew(
            listOf(
                BankSuggestion(
                    id = 0, externalId = "eb:acc-1:r1", bankAccountUid = "acc-1",
                    amountMinor = 1250, currency = "EUR", direction = EbDirection.DEBIT,
                    bookingDate = LocalDate(2026, 6, 8), fetchedAt = 0,
                )
            )
        )
        val importedId = txRepo.upsert(
            Transaction(
                id = UNSAVED_TRANSACTION_ID,
                type = TransactionType.EXPENSE,
                amount = Money(1250, CurrencyCode("EUR")),
                occurredOn = LocalDate(2026, 6, 8),
                note = null,
                categoryId = CategoryId(1),
                accountId = AccountId(1),
                createdAt = Instant.fromEpochMilliseconds(0),
                updatedAt = Instant.fromEpochMilliseconds(0),
            )
        )
        txRepo.setExternalId(TransactionId(importedId.value), "eb:acc-1:r2")

        engine().syncNow().getOrThrow()

        assertEquals(2, bankRepo.suggestions.size)
        assertEquals(listOf("eb:acc-1:r1", "eb:acc-1:r3"), bankRepo.suggestions.map { it.externalId })
    }

    @Test
    fun rejectedSuggestionIsNeverReinserted() = runTest {
        connect()
        bankRepo.upsertAccounts(listOf(account()))
        client.script(
            "acc-1",
            EbTransactionsPage(listOf(tx("r1"))),
            EbTransactionsPage(listOf(tx("r1"))),
        )

        val e = engine()
        e.syncNow().getOrThrow()
        bankRepo.reject(bankRepo.suggestions.single().id, decidedAt = 1)

        e.syncNow().getOrThrow()

        val only = bankRepo.suggestions.single()
        assertEquals(SuggestionStatus.REJECTED, only.status)
    }

    @Test
    fun skipsPendingNotBookedTransactions() = runTest {
        connect()
        bankRepo.upsertAccounts(listOf(account()))
        client.script(
            "acc-1",
            EbTransactionsPage(listOf(tx("r1"), tx("r2").copy(booked = false))),
        )

        engine().syncNow().getOrThrow()

        assertEquals(listOf("eb:acc-1:r1"), bankRepo.suggestions.map { it.externalId })
    }

    @Test
    fun autoSyncRespectsToggleAndThrottle() = runTest {
        connect()
        bankRepo.upsertAccounts(listOf(account()))
        client.script("acc-1", EbTransactionsPage(listOf(tx("r1"))))

        val e = engine()
        e.autoSyncIfDue().getOrThrow()
        assertEquals(0, client.fetchCount)

        settings.putBoolean(PrefKeys.BANK_SYNC_AUTO_ENABLED, true)
        e.autoSyncIfDue().getOrThrow()
        assertEquals(1, client.fetchCount)

        client.script("acc-1", EbTransactionsPage(listOf(tx("r2"))))
        e.autoSyncIfDue().getOrThrow()
        assertEquals(1, client.fetchCount)

        clock.advanceTo(Instant.parse("2026-06-10T19:00:00Z"))
        e.autoSyncIfDue().getOrThrow()
        assertEquals(2, client.fetchCount)
    }

    @Test
    fun sessionErrorsSurfaceAsReconnectRequired() = runTest {
        connect()
        bankRepo.upsertAccounts(listOf(account()))
        client.failure = EbError.SessionExpired("expired")

        val e = engine()
        e.syncNow()

        val state = e.runtime.value
        assertIs<BankSyncRuntimeState.Error>(state)
        assertEquals(BankSyncFailureReason.Auth, state.reason)
        assertTrue(state.reconnectRequired)
    }

    @Test
    fun networkErrorsSurfaceAsRetryableFailure() = runTest {
        connect()
        bankRepo.upsertAccounts(listOf(account()))
        client.failure = EbError.Network(RuntimeException("offline"))

        val e = engine()
        val result = e.syncNow()

        assertTrue(result.isFailure)
        val state = assertIs<BankSyncRuntimeState.Error>(e.runtime.value)
        assertEquals(BankSyncFailureReason.Network, state.reason)
        assertFalse(state.reconnectRequired)
    }

    @Test
    fun notConnectedAutoSyncIsNoop() = runTest {
        settings.putBoolean(PrefKeys.BANK_SYNC_AUTO_ENABLED, true)
        engine().autoSyncIfDue().getOrThrow()
        assertEquals(0, client.fetchCount)
    }
}
