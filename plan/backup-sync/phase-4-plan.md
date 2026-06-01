# Phase 4 — Reconcile engine + auto-apply adds/edits

**Status: DONE** — upsertFromSync apply API on 6 repos (incoming syncId + remote timestamps preserved), pure SyncReconciler (LWW truth table), SyncApplier (FK-safe order, syncId->PK resolve, MISSING sentinel), SyncEngine (Mutex pull/push, runtime + pendingDeletions StateFlows, encrypt-gated). 35 data/sync tests green. All builds pass.

## Goal
The core merge engine. Given a remote `SyncSnapshot` and the local one, reconcile by `syncId` with **last-write-wins on `updatedAt`**, **auto-apply incoming adds/edits**, and **collect pending deletions** (compute, don't apply). FK-safe apply order; FK syncIds resolved to local Long PKs; incoming syncId preserved. Highest-risk phase → heavy unit tests. Still no automatic triggers (Phase 6) and no durable pending-deletion store / push gating (Phase 5).

## New per-repo apply API (accounts, categories, transactions[tx, recurring, paymentMode], budgets)
1. **DAO**: `@Query("SELECT * FROM <Table> WHERE sync_id = :syncId LIMIT 1") suspend fun selectBySyncId(syncId: String): XxxEntity?`
2. **LocalDataSource** interface + impl: `suspend fun upsertFromSync(row: XxxSyncRow): Long`
   - impl: `val existing = dao.selectBySyncId(requireNotNull(row.syncId)); return if (existing == null) dao.insert(<entity from row, id=0, syncId=row.syncId, deleted=row.deleted, FKs=row.<longFk>, timestamps=row.createdAt/updatedAt>) else { dao.update(existing.copy(<content from row>, updatedAt = row.updatedAt, deleted = row.deleted)); existing.id }`
   - Use the existing entity-construction style already in each `SqlDelight*DataSource`. Preserve remote `createdAt`/`updatedAt` (don't stamp `now`) so LWW stays consistent across devices.
3. **Repository** interface + impl: `suspend fun upsertFromSync(row: XxxSyncRow): Long = dataSource.upsertFromSync(row)`
4. **Fake** (core/testing, 5 fakes): implement `upsertFromSync`. Fakes must now **store the real syncId per row** (keep an internal `MutableMap<String, Long>` syncId→id + the model list) so that (a) an incoming syncId updates the same logical row and returns a stable PK, and (b) `exportForSync()` emits the stored syncId (not the synthetic `"sync-x-$id"`). Refactor the Phase-3 synthetic-syncId fakes accordingly. PaymentMode has no production fake — extend the in-test one from Phase 3.

## data/sync — new types
### `SyncEntityType.kt`
`enum class SyncEntityType { ACCOUNT, CATEGORY, PAYMENT_MODE, TRANSACTION, RECURRING, BUDGET }`

### `ReconcileModels.kt`
```kotlin
data class PendingDeletion(
    val entityType: SyncEntityType,
    val syncId: String,
    val label: String,          // human label for the confirm screen (name / amount+date / etc.)
    val remoteUpdatedAt: Long,
)
data class ReconcileResult(
    val toApply: SyncSnapshot,              // adds + LWW-winning edits, tombstones excluded
    val pendingDeletions: List<PendingDeletion>,
)
```

### `SyncReconciler.kt` — PURE, no deps
`fun reconcile(local: SyncSnapshot, remote: SyncSnapshot): ReconcileResult`
- Index local lists by syncId per type. For each remote item of each type:
  - **not in local**: if `!remote.deleted` → include in `toApply`; if `remote.deleted` → no-op (already absent).
  - **in local (both)**:
    - `remote.deleted && !local.deleted` → **PendingDeletion** (build label from the local/remote row).
    - `remote.deleted && local.deleted` → no-op.
    - `!remote.deleted`: if `remote.updatedAt > local.updatedAt` → include in `toApply` (edit, LWW); else keep local (no-op).
- Local-only items (syncId absent in remote) → no-op here (the next push re-exports them).
- **Tie-break**: strictly `>` means remote wins only when newer; equal `updatedAt` = keep local (no thrash).
- `toApply` is a `SyncSnapshot` containing only the winning remote rows per type (carry remote F1 FK syncIds through unchanged); `generatedAtMs`/`originDeviceId` copied from remote.
- Labels: account/category/paymentMode = name; transaction = "amountMinor currency · occurredOn"; recurring = note ?: "recurring"; budget = name.

### `SyncApplier.kt`
`suspend fun apply(toApply: SyncSnapshot)` — applies in **FK-safe order**: paymentModes → categories → accounts → recurring → budgets → transactions.
- Seed local syncId→PK maps from `repo.exportForSync()` for accounts, categories, paymentModes, recurring (so FKs to unchanged local parents resolve).
- For each item, build the module's `*SyncRow` with **Long FKs resolved** from the maps; call `repo.upsertFromSync(row)`; record returned PK back into the relevant map (so later children resolve newly-applied parents).
- Required FK unresolved → skip that row (log). Nullable FK absent → null.
- Constructor takes the 6 repos.

### `SyncEngine.kt`
Mirror `RemoteBackupManager` structure (Mutex, runtime StateFlow, debounce pulse for push).
- Constructor: `SyncEngine(exporter: SyncExporter, reconciler: SyncReconciler, applier: SyncApplier, codec: SyncSnapshotCodec, store: SyncRemoteStore, appSettings: AppSettings, sessionPassphrase: SessionPassphrase, dispatchers: DispatcherProvider, nowMs)`.
- `runtime: StateFlow<SyncRuntimeState>` — new sealed `SyncRuntimeState { Idle, Pulling, Applying, Pushing, Error(msg) }` (separate from RemoteBackupRuntimeState).
- `val pendingDeletions: StateFlow<List<PendingDeletion>>` (in-memory for now; Phase 5 swaps to a durable store).
- `suspend fun pull(): Result<Unit>` (under Mutex): `store.readSnapshotBytes()` → if null → `push()` (first device seeds remote) and return; else `codec.open(bytes, passphrase())` → `local = exporter.export()` → `reconciler.reconcile(local, remote)` → `applier.apply(result.toApply)` → set `pendingDeletions` → `appSettings.putLong(LAST_SYNC_PULL_MS, now)`. Runtime transitions Pulling→Applying→Idle; Error on throw.
- `suspend fun push(): Result<Unit>` (under Mutex): `local = exporter.export()` → `codec.seal(local, passphrase())` → `store.writeSnapshotBytes(bytes)`. (Push gating + devices.json deferred to Phase 5/7.)
- `private fun passphrase(): CharArray?` = if `appSettings.getBoolean(REMOTE_BACKUP_ENCRYPT, true)` then `sessionPassphrase.get()` else null. If encrypt on but passphrase null → skip (return early, log) — mirror RemoteBackupManager guard.
- Koin: register `SyncReconciler`, `SyncApplier`, `SyncEngine` in `composeApp/.../di/SyncModule.kt` (`single { ... }`). No triggers call them yet.

## Tests (commonTest in data/sync — highest value)
- **`SyncReconcilerTest`** (pure): remote-add → toApply; remote-edit newer → toApply; remote-edit older → no-op; equal updatedAt → no-op (tie-break); remote tombstone vs live local → pendingDeletion with correct type+label; both-deleted → no-op; remote tombstone vs absent local → no-op; local-only → not in toApply.
- **`SyncApplierTest`** (fakes): FK syncId→PK resolution (a transaction whose category/account come from `toApply` resolves to the applied PKs); parent inserted before child; incoming syncId preserved (exportForSync afterwards shows it); edit matches existing local row by syncId (no duplicate). Reuse core/testing fakes + in-test PaymentMode fake.
- **`SyncEngineTest`** (fakes + a fake `SyncRemoteStore`/provider + `FakeBackupCrypto`): pull on empty remote pushes local seed; pull applies adds/edits and exposes pendingDeletions WITHOUT deleting; push writes a snapshot equal to local export (round-trip through codec plaintext).

## Verification
- `./gradlew :data:accounts:compileDebugKotlinAndroid :data:categories:compileDebugKotlinAndroid :data:transactions:compileDebugKotlinAndroid :data:budgets:compileDebugKotlinAndroid :data:sync:compileDebugKotlinAndroid`
- `./gradlew :composeApp:assembleDebug`
- `./gradlew :composeApp:linkDebugFrameworkIosSimulatorArm64`
- `./gradlew :data:sync:testDebugUnitTest` + testDebugUnitTest for any feature/data module whose Fake changed.

## Notes
- Preserve remote timestamps on apply (do NOT stamp `now`) — LWW depends on it.
- `upsertFromSync` must NOT go through the normal `insert` path that generates a fresh UUID; it inserts the entity with the incoming syncId.
- Known-unrelated `:data:budgets` FakeBudgetRepositoryTest failure — ignore.
