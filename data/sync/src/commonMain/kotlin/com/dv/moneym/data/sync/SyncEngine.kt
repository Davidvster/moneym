package com.dv.moneym.data.sync

import co.touchlab.kermit.Logger
import com.dv.moneym.core.common.DispatcherProvider
import com.dv.moneym.core.datastore.AppSettings
import com.dv.moneym.core.datastore.PrefKeys
import com.dv.moneym.data.remotebackup.SessionPassphrase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Serialized pull / push of the remote sync snapshot. No automatic triggers (Phase 6),
 * no push gating / devices.json (Phase 5/7), no durable pending-deletion store (Phase 5).
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
    private val nowMs: () -> Long = { kotlin.time.Clock.System.now().toEpochMilliseconds() },
) {

    private val logger = Logger.withTag("SyncEngine")
    private val lock = Mutex()

    private val _runtime = MutableStateFlow<SyncRuntimeState>(SyncRuntimeState.Idle)
    val runtime: StateFlow<SyncRuntimeState> = _runtime.asStateFlow()

    private val _pendingDeletions = MutableStateFlow<List<PendingDeletion>>(emptyList())
    val pendingDeletions: StateFlow<List<PendingDeletion>> = _pendingDeletions.asStateFlow()

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
                return@runCatching
            }
            val remote = withContext(dispatchers.io) { codec.open(bytes, passphrase()) }
            val local = exporter.export()
            val result = reconciler.reconcile(local = local, remote = remote)
            _runtime.value = SyncRuntimeState.Applying
            applier.apply(result.toApply)
            _pendingDeletions.value = result.pendingDeletions
            appSettings.putLong(PrefKeys.LAST_SYNC_PULL_MS, nowMs())
            _runtime.value = SyncRuntimeState.Idle
        }.onFailure { t ->
            _runtime.value = SyncRuntimeState.Error(t.message ?: "Sync pull failed")
            logger.e(t) { "Sync pull failed" }
        }
    }

    suspend fun push(): Result<Unit> = lock.withLock {
        runCatching { pushInternal() }
            .onFailure { t ->
                _runtime.value = SyncRuntimeState.Error(t.message ?: "Sync push failed")
                logger.e(t) { "Sync push failed" }
            }
    }

    private suspend fun pushInternal() {
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

    private fun encryptEnabled(): Boolean =
        appSettings.getBoolean(PrefKeys.REMOTE_BACKUP_ENCRYPT, defaultValue = true)

    private fun passphrase(): CharArray? = if (encryptEnabled()) sessionPassphrase.get() else null
}
