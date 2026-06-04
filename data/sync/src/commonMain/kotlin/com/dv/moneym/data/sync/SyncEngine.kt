package com.dv.moneym.data.sync

import co.touchlab.kermit.Logger
import com.dv.moneym.core.common.DispatcherProvider
import com.dv.moneym.core.datastore.AppSettings
import com.dv.moneym.core.datastore.PrefKeys
import com.dv.moneym.data.accounts.AccountRepository
import com.dv.moneym.data.budgets.BudgetRepository
import com.dv.moneym.data.categories.CategoryRepository
import com.dv.moneym.data.remotebackup.SessionPassphrase
import com.dv.moneym.data.transactions.PaymentModeRepository
import com.dv.moneym.data.transactions.RecurringTransactionRepository
import com.dv.moneym.data.transactions.TransactionRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Serialized pull / push of the remote sync snapshot. Pull recomputes pending deletions into a
 * durable [PendingDeletionStore]; push is gated while any pending deletion is unresolved.
 */
class SyncEngine(
    private val exporter: SyncExporter,
    private val reconciler: SyncReconciler,
    private val applier: SyncApplier,
    private val codec: SyncSnapshotCodec,
    private val store: SyncRemoteStore,
    private val appSettings: AppSettings,
    private val sessionPassphrase: SessionPassphrase,
    private val dispatchers: DispatcherProvider,
    private val pendingDeletionStore: PendingDeletionStore,
    private val accountRepository: AccountRepository,
    private val categoryRepository: CategoryRepository,
    private val paymentModeRepository: PaymentModeRepository,
    private val transactionRepository: TransactionRepository,
    private val recurringTransactionRepository: RecurringTransactionRepository,
    private val budgetRepository: BudgetRepository,
    private val deviceRegistryManager: DeviceRegistryManager? = null,
    private val nowMs: () -> Long = { kotlin.time.Clock.System.now().toEpochMilliseconds() },
    private val debounceMs: Long = DEFAULT_DEBOUNCE_MS,
) : SyncDeletionController, SyncStatusProvider, SyncPuller {

    private val logger = Logger.withTag("SyncEngine")
    private val lock = Mutex()
    private val pushPulse = MutableSharedFlow<Unit>(extraBufferCapacity = 64)
    private var job: Job? = null

    private val _runtime = MutableStateFlow<SyncRuntimeState>(SyncRuntimeState.Idle)
    val runtime: StateFlow<SyncRuntimeState> = _runtime.asStateFlow()

    override val isSyncing: Flow<Boolean> =
        runtime.map { it != SyncRuntimeState.Idle && it !is SyncRuntimeState.Error }

    override val pendingDeletionCount: Flow<Int> = pendingDeletionStore.count

    override val pendingDeletions: Flow<List<PendingDeletion>> = pendingDeletionStore.pending

    @OptIn(FlowPreview::class)
    fun start(scope: CoroutineScope) {
        job?.cancel()
        job = scope.launch(dispatchers.io) {
            launch { pushPulse.debounce(debounceMs).collect { push() } }
            launch {
                dataChanges()
                    .drop(1)
                    .debounce(debounceMs)
                    .collect { enqueuePush() }
            }
        }
    }

    /** Emits whenever any synced entity changes, so an edit on this device pushes without
     *  riding on the local-backup lifecycle. */
    private fun dataChanges(): Flow<Unit> =
        combine(
            accountRepository.observeAll(),
            categoryRepository.observeAll(),
            paymentModeRepository.observeAll(),
            transactionRepository.observeAll(),
            recurringTransactionRepository.observeAll(),
        ) { _, _, _, _, _ -> Unit }
            .combine(budgetRepository.observeAll()) { _, _ -> Unit }

    fun stop() {
        job?.cancel()
        job = null
    }

    fun enqueuePush() {
        if (!enabled()) return
        pushPulse.tryEmit(Unit)
    }

    override suspend fun pullNow(): Result<Unit> {
        if (!enabled()) return Result.success(Unit)
        return pull()
    }

    private fun enabled(): Boolean =
        appSettings.getBoolean(PrefKeys.CROSS_DEVICE_SYNC_ENABLED, defaultValue = false)

    suspend fun pull(): Result<Unit> = lock.withLock {
        runCatching {
            if (encryptEnabled() && sessionPassphrase.get() == null) {
                logger.w { "Sync pull skipped: encryption on but no session passphrase" }
                return@runCatching
            }
            _runtime.value = SyncRuntimeState.Pulling
            val bytes = withContext(dispatchers.io) { store.readSnapshotBytes() }
            if (bytes == null) {
                _runtime.value = SyncRuntimeState.Idle
                pushInternal()
                touchSelfQuietly()
                return@runCatching
            }
            val remote = withContext(dispatchers.io) { codec.open(bytes, passphrase()) }
            val local = exporter.export()
            val result = reconciler.reconcile(local = local, remote = remote)
            _runtime.value = SyncRuntimeState.Applying
            applier.apply(result.toApply)
            pendingDeletionStore.replaceAll(result.pendingDeletions)
            appSettings.putLong(PrefKeys.LAST_SYNC_PULL_MS, nowMs())
            _runtime.value = SyncRuntimeState.Idle
            touchSelfQuietly()
        }.onFailure { t ->
            _runtime.value = SyncRuntimeState.Error(t.message ?: "Sync pull failed")
            logger.e(t) { "Sync pull failed" }
        }
    }

    suspend fun push(): Result<Unit> = lock.withLock {
        runCatching {
            pushInternal()
            touchSelfQuietly()
        }
            .onFailure { t ->
                _runtime.value = SyncRuntimeState.Error(t.message ?: "Sync push failed")
                logger.e(t) { "Sync push failed" }
            }
    }

    /**
     * Confirm/decline pending remote deletions. Confirmed syncIds are tombstoned locally; the
     * rest are revived (updatedAt bumped so the live local row beats the remote tombstone on the
     * next push). Clears the store, then pushes.
     */
    override suspend fun resolveDeletions(confirmedSyncIds: Set<String>): Result<Unit> = lock.withLock {
        runCatching {
            val now = nowMs()
            for (pending in pendingDeletionStore.current()) {
                val confirmed = pending.syncId in confirmedSyncIds
                route(pending.entityType, pending.syncId, now, confirmed)
            }
            pendingDeletionStore.clear()
            pushInternal()
        }.onFailure { t ->
            _runtime.value = SyncRuntimeState.Error(t.message ?: "Resolve deletions failed")
            logger.e(t) { "Resolve deletions failed" }
        }
    }

    private suspend fun route(type: SyncEntityType, syncId: String, now: Long, confirmed: Boolean) {
        when (type) {
            SyncEntityType.ACCOUNT ->
                if (confirmed) accountRepository.markDeletedBySyncId(syncId, now) else accountRepository.reviveBySyncId(syncId, now)
            SyncEntityType.CATEGORY ->
                if (confirmed) categoryRepository.markDeletedBySyncId(syncId, now) else categoryRepository.reviveBySyncId(syncId, now)
            SyncEntityType.PAYMENT_MODE ->
                if (confirmed) paymentModeRepository.markDeletedBySyncId(syncId, now) else paymentModeRepository.reviveBySyncId(syncId, now)
            SyncEntityType.TRANSACTION ->
                if (confirmed) transactionRepository.markDeletedBySyncId(syncId, now) else transactionRepository.reviveBySyncId(syncId, now)
            SyncEntityType.RECURRING ->
                if (confirmed) recurringTransactionRepository.markDeletedBySyncId(syncId, now) else recurringTransactionRepository.reviveBySyncId(syncId, now)
            SyncEntityType.BUDGET ->
                if (confirmed) budgetRepository.markDeletedBySyncId(syncId, now) else budgetRepository.reviveBySyncId(syncId, now)
        }
    }

    private suspend fun pushInternal() {
        val pending = pendingDeletionStore.current()
        if (pending.isNotEmpty()) {
            logger.w { "push paused: ${pending.size} pending deletions" }
            _runtime.value = SyncRuntimeState.Idle
            return
        }
        if (encryptEnabled() && sessionPassphrase.get() == null) {
            logger.w { "Sync push skipped: encryption on but no session passphrase" }
            return
        }
        _runtime.value = SyncRuntimeState.Pushing
        val local = exporter.export()
        val bytes = withContext(dispatchers.io) { codec.seal(local, passphrase()) }
        withContext(dispatchers.io) { store.writeSnapshotBytes(bytes) }
        _runtime.value = SyncRuntimeState.Idle
    }

    private suspend fun touchSelfQuietly() {
        runCatching { deviceRegistryManager?.touchSelf() }
            .onFailure { logger.w(it) { "device registry touchSelf failed" } }
    }

    private fun encryptEnabled(): Boolean =
        appSettings.getBoolean(PrefKeys.REMOTE_BACKUP_ENCRYPT, defaultValue = true)

    private fun passphrase(): CharArray? = if (encryptEnabled()) sessionPassphrase.get() else null

    companion object {
        const val DEFAULT_DEBOUNCE_MS = 3_000L
    }
}
