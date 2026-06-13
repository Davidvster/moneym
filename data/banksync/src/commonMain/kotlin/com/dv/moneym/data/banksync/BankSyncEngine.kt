package com.dv.moneym.data.banksync

import co.touchlab.kermit.Logger
import com.dv.moneym.core.common.AppClock
import com.dv.moneym.core.datastore.AppSettings
import com.dv.moneym.core.datastore.PrefKeys
import com.dv.moneym.data.transactions.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus

class BankSyncEngine(
    private val client: EnableBankingClient,
    private val credentialsStore: EnableBankingCredentialsStore,
    private val bankSyncRepository: BankSyncRepository,
    private val transactionRepository: TransactionRepository,
    private val externalIdResolver: ExternalIdResolver,
    private val appSettings: AppSettings,
    private val clock: AppClock,
    private val autoSyncMinIntervalMs: Long = DEFAULT_AUTO_SYNC_MIN_INTERVAL_MS,
) : BankSyncStatusProvider {
    private val logger = Logger.withTag("BankSyncEngine")
    private val lock = Mutex()

    private val _runtime = MutableStateFlow<BankSyncRuntimeState>(BankSyncRuntimeState.Idle)
    val runtime: StateFlow<BankSyncRuntimeState> = _runtime.asStateFlow()

    override val isEnabled: Flow<Boolean>
        get() = appSettings.observeBoolean(PrefKeys.BANK_SYNC_CONFIGURED, defaultValue = false)
    override val isSyncing: Flow<Boolean>
        get() = runtime.map { it is BankSyncRuntimeState.Running }
    override val pendingCount: Flow<Int>
        get() = bankSyncRepository.observePendingCount()
    override val lastSyncedMs: Flow<Long>
        get() = runtime.map { appSettings.getLong(PrefKeys.BANK_SYNC_LAST_SYNC_MS, defaultValue = 0L) }
    override suspend fun requestSync() { syncNow() }

    suspend fun isConnected(): Boolean =
        credentialsStore.loadCredentials() != null && credentialsStore.loadSessionId() != null

    suspend fun autoSyncIfDue(): Result<Unit> {
        if (!appSettings.getBoolean(PrefKeys.BANK_SYNC_AUTO_ENABLED, defaultValue = false)) {
            return Result.success(Unit)
        }
        if (!isConnected()) return Result.success(Unit)
        val last = appSettings.getLong(PrefKeys.BANK_SYNC_LAST_SYNC_MS, defaultValue = 0L)
        val now = clock.now().toEpochMilliseconds()
        if (now - last < autoSyncMinIntervalMs) {
            logger.d { "Bank auto-sync skipped: last sync ${now - last}ms ago" }
            return Result.success(Unit)
        }
        return syncNow()
    }

    suspend fun syncNow(): Result<Unit> = lock.withLock {
        runCatching {
            if (!isConnected()) throw EbError.Unauthorized("Bank connection is not configured")
            _runtime.value = BankSyncRuntimeState.Running
            val today = clock.today()
            for (account in bankSyncRepository.getEnabledAccounts()) {
                syncAccount(account, today)
            }
            appSettings.putLong(PrefKeys.BANK_SYNC_LAST_SYNC_MS, clock.now().toEpochMilliseconds())
            _runtime.value = BankSyncRuntimeState.Idle
        }.onFailure { t ->
            val reconnect = t is EbError.SessionExpired || t is EbError.Unauthorized
            _runtime.value = BankSyncRuntimeState.Error(
                message = t.message ?: "Bank sync failed",
                reconnectRequired = reconnect,
            )
            logger.e(t) { "Bank sync failed" }
        }
    }

    private suspend fun syncAccount(account: BankAccountLink, today: LocalDate) {
        val initialSync = account.lastSyncedDate == null
        val dateFrom = account.lastSyncedDate?.minus(DatePeriod(days = OVERLAP_DAYS))
            ?: today.minus(DatePeriod(days = INITIAL_WINDOW_DAYS))

        val fetched = try {
            fetchAllPages(account.uid, dateFrom)
        } catch (e: EbError.Api) {
            if (!initialSync) throw e
            logger.w { "Initial ${INITIAL_WINDOW_DAYS}d window rejected (${e.message}), retrying with ${FALLBACK_WINDOW_DAYS}d" }
            fetchAllPages(account.uid, today.minus(DatePeriod(days = FALLBACK_WINDOW_DAYS)))
        }
        if (fetched.isEmpty()) {
            bankSyncRepository.advanceCursor(account.uid, today, clock.now().toEpochMilliseconds())
            return
        }

        val rawIds = fetched.map { tx ->
            externalIdResolver.resolve(
                accountUid = account.uid,
                entryReference = tx.entryReference,
                bookingDate = tx.bookingDate,
                amountMinor = tx.amountMinor,
                currency = tx.currency,
                description = tx.description,
            )
        }
        val externalIds = externalIdResolver.disambiguate(rawIds)

        val knownSuggestions = bankSyncRepository.filterKnownExternalIds(externalIds)
        val now = clock.now().toEpochMilliseconds()
        val candidates = fetched.zip(externalIds)
            .filter { (_, externalId) -> externalId !in knownSuggestions }
            .filter { (_, externalId) -> !transactionRepository.existsByExternalId(externalId) }
            .map { (tx, externalId) ->
                BankSuggestion(
                    id = 0,
                    externalId = externalId,
                    bankAccountUid = account.uid,
                    amountMinor = tx.amountMinor,
                    currency = tx.currency,
                    direction = tx.direction,
                    bookingDate = tx.bookingDate,
                    valueDate = tx.valueDate,
                    description = tx.description,
                    counterparty = tx.counterparty,
                    fetchedAt = now,
                )
            }
        val inserted = bankSyncRepository.insertSuggestionsIfNew(candidates)
        logger.d { "Bank sync ${account.uid}: ${fetched.size} fetched, $inserted new suggestions" }
        bankSyncRepository.advanceCursor(account.uid, today, now)
    }

    private suspend fun fetchAllPages(accountUid: String, dateFrom: LocalDate): List<EbTransactionData> {
        val fetched = mutableListOf<EbTransactionData>()
        var continuationKey: String? = null
        do {
            val page = client.fetchTransactions(accountUid, dateFrom, continuationKey).getOrThrow()
            fetched += page.transactions.filter { it.booked }
            continuationKey = page.continuationKey
        } while (continuationKey != null)
        return fetched
    }

    companion object {
        const val DEFAULT_AUTO_SYNC_MIN_INTERVAL_MS = 6 * 60 * 60 * 1000L
        const val OVERLAP_DAYS = 5
        const val INITIAL_WINDOW_DAYS = 730
        const val FALLBACK_WINDOW_DAYS = 90
    }
}
