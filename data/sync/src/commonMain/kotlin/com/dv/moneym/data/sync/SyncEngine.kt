package com.dv.moneym.data.sync

import co.touchlab.kermit.Logger
import com.dv.moneym.core.common.DispatcherProvider
import com.dv.moneym.core.datastore.AppSettings
import com.dv.moneym.core.datastore.PrefKeys
import com.dv.moneym.core.security.BackupCryptoError
import com.dv.moneym.data.accounts.AccountRepository
import com.dv.moneym.data.budgets.BudgetRepository
import com.dv.moneym.data.categories.CategoryRepository
import com.dv.moneym.data.remotebackup.RemoteBackupManager
import com.dv.moneym.data.remotebackup.SessionPassphrase
import com.dv.moneym.data.remotebackup.SyncPassphraseStore
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
    private val syncPassphraseStore: SyncPassphraseStore,
    private val dispatchers: DispatcherProvider,
    private val pendingDeletionStore: PendingDeletionStore,
    private val conflictStore: SyncConflictStore,
    private val accountRepository: AccountRepository,
    private val categoryRepository: CategoryRepository,
    private val paymentModeRepository: PaymentModeRepository,
    private val transactionRepository: TransactionRepository,
    private val recurringTransactionRepository: RecurringTransactionRepository,
    private val budgetRepository: BudgetRepository,
    private val deviceRegistryManager: DeviceRegistryManager? = null,
    private val remoteBackupManager: RemoteBackupManager? = null,
    private val nowMs: () -> Long = { kotlin.time.Clock.System.now().toEpochMilliseconds() },
    private val debounceMs: Long = DEFAULT_DEBOUNCE_MS,
) : SyncDeletionController, SyncStatusProvider, SyncPuller, SyncConflictController, SyncBootstrap {

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

    override val conflict: Flow<SyncConflict?> = conflictStore.conflict

    private val _lastSyncedMs = MutableStateFlow(appSettings.getLong(PrefKeys.LAST_SYNC_PULL_MS))
    override val lastSyncedMs: Flow<Long> = _lastSyncedMs.asStateFlow()

    @OptIn(FlowPreview::class)
    fun start(scope: CoroutineScope) {
        job?.cancel()
        job = scope.launch(dispatchers.io) {
            launch { pushPulse.debounce(debounceMs).collect { push() } }
            launch {
                dataChanges()
                    .drop(1)
                    .debounce(debounceMs)
                    .collect {
                        enqueuePush()
                        // Cloud sync also drives the point-in-time snapshot-history safety net.
                        remoteBackupManager?.enqueueUpload()
                    }
            }
        }
        remoteBackupManager?.start(scope)
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
        remoteBackupManager?.stop()
    }

    fun enqueuePush() {
        if (!enabled()) return
        pushPulse.tryEmit(Unit)
    }

    override suspend fun pullNow(): Result<Unit> {
        if (!enabled()) return Result.success(Unit)
        return pull()
    }

    override suspend fun syncNow(): Result<Unit> {
        if (!enabled()) return Result.success(Unit)
        return pull().also { if (it.isSuccess) push() }
    }

    private fun enabled(): Boolean =
        appSettings.getBoolean(PrefKeys.CROSS_DEVICE_SYNC_ENABLED, defaultValue = false)

    suspend fun pull(): Result<Unit> = lock.withLock {
        runCatching {
            if (conflictStore.current() != null) {
                logger.w { "Sync pull paused: unresolved password conflict" }
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
            val remote = decodeRemoteOrRaiseConflict(bytes) ?: run {
                _runtime.value = SyncRuntimeState.Idle
                return@runCatching
            }
            val local = exporter.export()
            val result = reconciler.reconcile(local = local, remote = remote)
            _runtime.value = SyncRuntimeState.Applying
            applier.apply(result.toApply)
            pendingDeletionStore.replaceAll(result.pendingDeletions)
            val syncedAt = nowMs()
            appSettings.putLong(PrefKeys.LAST_SYNC_PULL_MS, syncedAt)
            _lastSyncedMs.value = syncedAt
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

    override suspend fun resolveWithPassword(passphrase: CharArray, makeAuthoritative: Boolean): Result<Unit> {
        val prepared = lock.withLock {
            runCatching {
                if (!makeAuthoritative) {
                    // Validate the password against the current remote; codec.open throws
                    // BackupCryptoError.WrongPassphrase on a mismatch, leaving the conflict intact.
                    val bytes = withContext(dispatchers.io) { store.readSnapshotBytes() }
                    if (bytes != null && codec.isEncryptedEnvelope(bytes)) {
                        withContext(dispatchers.io) { codec.open(bytes, passphrase) }
                    }
                }
                sessionPassphrase.set(passphrase)
                syncPassphraseStore.persist(passphrase)
                appSettings.putBoolean(PrefKeys.REMOTE_BACKUP_ENCRYPT, true)
                conflictStore.clear()
            }
        }
        return prepared.fold(
            onSuccess = { if (makeAuthoritative) push() else pull() },
            onFailure = { t ->
                logger.w(t) { "resolveWithPassword failed" }
                Result.failure(t)
            },
        )
    }

    override suspend fun resolveWithPlaintext(): Result<Unit> {
        // If the remote is already plaintext (a downgrade we can read), pull to merge it; if it is
        // encrypted and unreadable, overriding to plaintext means our local state becomes the remote.
        val overrideUnreadable = conflictStore.current()?.remoteEncrypted ?: false
        lock.withLock {
            sessionPassphrase.clear()
            syncPassphraseStore.clear()
            appSettings.putBoolean(PrefKeys.REMOTE_BACKUP_ENCRYPT, false)
            conflictStore.clear()
        }
        return if (overrideUnreadable) push() else pull()
    }

    override suspend fun remoteState(): RemoteSyncState {
        val bytes = withContext(dispatchers.io) { store.readSnapshotBytes() } ?: return RemoteSyncState.NONE
        return if (codec.isEncryptedEnvelope(bytes)) RemoteSyncState.ENCRYPTED else RemoteSyncState.PLAINTEXT
    }

    override suspend fun canDecrypt(passphrase: CharArray): Boolean {
        val bytes = withContext(dispatchers.io) { store.readSnapshotBytes() } ?: return true
        if (!codec.isEncryptedEnvelope(bytes)) return true
        return runCatching { withContext(dispatchers.io) { codec.open(bytes, passphrase) } }.isSuccess
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

    /**
     * Decode the remote snapshot, or raise a durable [SyncConflict] and return null when this
     * device cannot reconcile its encryption config with the remote's. Network/parse failures are
     * left to the caller's runCatching (they surface as [SyncRuntimeState.Error], not a conflict).
     */
    private suspend fun decodeRemoteOrRaiseConflict(bytes: ByteArray): SyncSnapshot? {
        val remoteEncrypted = codec.isEncryptedEnvelope(bytes)
        if (remoteEncrypted) {
            val pass = sessionPassphrase.get()
            if (pass == null) {
                logger.w { "Sync conflict: remote encrypted, no session passphrase" }
                conflictStore.set(SyncConflict(remoteEncrypted = true))
                return null
            }
            return try {
                val snapshot = withContext(dispatchers.io) { codec.open(bytes, pass) }
                if (!encryptEnabled()) appSettings.putBoolean(PrefKeys.REMOTE_BACKUP_ENCRYPT, true)
                snapshot
            } catch (_: BackupCryptoError.WrongPassphrase) {
                logger.w { "Sync conflict: wrong passphrase for remote" }
                conflictStore.set(SyncConflict(remoteEncrypted = true))
                null
            }
        }
        // remote plaintext
        if (encryptEnabled()) {
            logger.w { "Sync conflict: remote plaintext but this device is configured encrypted" }
            conflictStore.set(SyncConflict(remoteEncrypted = false))
            return null
        }
        return codec.decode(bytes)
    }

    private suspend fun pushInternal() {
        if (conflictStore.current() != null) {
            logger.w { "push paused: unresolved password conflict" }
            _runtime.value = SyncRuntimeState.Idle
            return
        }
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
