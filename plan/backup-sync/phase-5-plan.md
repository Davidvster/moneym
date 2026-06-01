# Phase 5 — Deletion semantics + confirmation screen + upload gating

**Status: IN PROGRESS**

Split into **5a (data + engine)** and **5b (feature UI)** — two builder runs, committed separately.

## Why deletion semantics first
For the confirm-screen feature to work end-to-end, deletes must be **tombstones**, not hard rows-gone:
- A LOCAL user delete must leave `deleted=1` so it propagates on push (hard delete → row absent locally → remote re-adds it = resurrection).
- An applied REMOTE deletion must hide from the UI → normal read queries must filter `deleted = 0`.
- `deleteAll()` (restore/import wipe) stays a HARD delete.

---

## Phase 5a — deletion semantics + PendingDeletionStore + SyncEngine gating

### 1. Filter tombstones from normal reads (add `deleted = 0`)
Edit these DAO queries (user-facing reads only; leave `selectById`, `selectBySyncId`, `selectAllForSync` UNfiltered — internal paths):
- **AccountDao**: `selectAll` (`WHERE deleted = 0`), `selectDefault` (`WHERE is_default = 1 AND deleted = 0`), `countAll` (`WHERE deleted = 0`).
- **CategoryDao**: `selectAll` (`WHERE deleted = 0`), `selectActive` (`WHERE archived = 0 AND deleted = 0`), `countAll` (`WHERE deleted = 0`).
- **TransactionDao**: `selectAll`, `selectByMonth`, `selectByCategory`, `selectByType`, `selectByCategoryAndType`, `getEarliestDate`, `getLatestDate`, `selectDistinctDates`, `countByRecurringId` — add `deleted = 0` (AND-combined with existing WHERE). `convertCurrencyForAccount` — add `AND deleted = 0`.
- **RecurringTransactionDao**: `selectAll` (`WHERE deleted = 0`).
- **BudgetDao**: `selectAll` (`WHERE deleted = 0`), `selectByAccount` (`WHERE (account_id = :accountId OR account_id = 0) AND deleted = 0`).
- **PaymentModeDao**: `selectAll` (`WHERE deleted = 0`), `countAll` (`WHERE deleted = 0`).

### 2. Convert user delete → soft delete (tombstone)
- Each DAO: add `@Query("UPDATE <Table> SET deleted = 1, updated_at = :now WHERE id = :id") suspend fun softDeleteById(id: Long, now: Long)`.
- Each DataSource interface+impl: `suspend fun softDelete(id: Long, now: Long)`.
- Each Repository impl: change `delete(id)` to call `dataSource.softDelete(id.value, now)` (stamp `now`). Keep `deleteAll()` HARD (unchanged).
- **TransactionRepository.deleteByAccountId**: convert to soft cascade — DAO `@Query("UPDATE TransactionEntry SET deleted = 1, updated_at = :now WHERE account_id = :accountId") suspend fun softDeleteByAccountId(accountId, now)`; repo calls it. (So deleting a wallet tombstones its transactions too.)
- Fakes: `delete` now marks the stored model deleted (or removes from the live list but retains for export with deleted=true). Fakes must keep tombstoned rows available to `exportForSync()` with `deleted=true`, and exclude them from `observeAll`/`count`. Update all 5 fakes + in-test PaymentMode fake.

### 3. markDeleted / revive by syncId (for confirm/decline)
- Each DAO: `@Query("UPDATE <Table> SET deleted = 1, updated_at = :now WHERE sync_id = :syncId") suspend fun markDeletedBySyncId(syncId, now)` and `@Query("UPDATE <Table> SET updated_at = :now WHERE sync_id = :syncId") suspend fun touchBySyncId(syncId, now)`.
- DataSource + Repository: `suspend fun markDeletedBySyncId(syncId: String, now: Long)` and `suspend fun reviveBySyncId(syncId: String, now: Long)` (touch = bump updatedAt so the live local row beats the remote tombstone next push).
- Fakes: implement both (operate on the syncId→model map).

### 4. `PendingDeletionStore` (data/sync)
- Make `PendingDeletion` `@Serializable` (Phase-4 type).
- `PendingDeletionStore(appSettings: AppSettings)`:
  - `fun replaceAll(list: List<PendingDeletion>)` → write JSON to `PrefKeys.PENDING_DELETION_BLOB` (empty list → `remove`).
  - `fun current(): List<PendingDeletion>` (decode; empty on absent/parse-fail).
  - `fun clear()`.
  - `val pending: Flow<List<PendingDeletion>>` from `appSettings.observeString(PENDING_DELETION_BLOB)` mapped through decode.
  - `val hasPending: Flow<Boolean>` / `val count: Flow<Int>` derived.

### 5. SyncEngine changes
- Inject `PendingDeletionStore` + the 6 repos (or a small `SyncDeletionResolver(repos…)` it delegates to).
- `pull()`: after reconcile, `pendingDeletionStore.replaceAll(result.pendingDeletions)` (recompute each pull — self-healing). Drop the in-memory pendingDeletions StateFlow; expose `pendingDeletions: StateFlow<List<PendingDeletion>>` (or Flow) sourced from the store. `applier.apply(toApply)` still runs (adds/edits auto-applied).
- `push()`: **no-op early if `pendingDeletionStore.current().isNotEmpty()`** (log "push paused: N pending deletions").
- `suspend fun resolveDeletions(confirmedSyncIds: Set<String>)`: read store; for each pending in confirmed → `repo.markDeletedBySyncId(syncId, now)`; for the rest (declined) → `repo.reviveBySyncId(syncId, now)`; `store.clear()`; then `push()` (or enqueue). Route by `entityType` to the right repo.

### 5a tests (commonTest)
- `PendingDeletionStoreTest` — replaceAll/current/clear round-trip; hasPending/count flows; empty blob.
- `SyncEnginePushGatingTest` — push is a no-op while store non-empty; resumes after clear.
- `SyncEngineDeletionResolveTest` (fakes) — confirm → row tombstoned (export shows deleted=true, observeAll excludes it); decline → row kept + updatedAt bumped; store cleared.
- A repo soft-delete test (e.g. accounts) — `delete` hides from `observeAll` but `exportForSync` still yields it with `deleted=true`.

### 5a verification
- `./gradlew :data:accounts:compileDebugKotlinAndroid :data:categories:compileDebugKotlinAndroid :data:transactions:compileDebugKotlinAndroid :data:budgets:compileDebugKotlinAndroid :data:sync:compileDebugKotlinAndroid`
- `./gradlew :composeApp:assembleDebug :composeApp:linkDebugFrameworkIosSimulatorArm64`
- `./gradlew :data:sync:testDebugUnitTest` + testDebugUnitTest for every module whose Fake/queries changed (accounts, categories, transactions, budgets, + feature modules that read them — run feature:overview/transactions/categories/budgets/settings to catch behavior regressions from the deleted=0 filters).

---

## Phase 5b — feature/sync deletion confirmation UI

### New module `feature/sync`
Use the `feature-module` skill. Mirror an existing simple feature (e.g. `feature/categories`). Depends on `data/sync`, `core/ui`, `core/designsystem`, `core/model`, `core/navigation`. commonMain composeResources for strings (en + de/es/it).

### Files
- `PendingDeletionsUiState.kt` + `PendingDeletionsIntent.kt`:
  - State: `groups: List<DeletionGroup>` where `DeletionGroup(type, titleResKey, items: List<DeletionItem(syncId, label, checked)>)`; `selectedCount`, `isResolving`.
  - Intent: `ToggleItem(syncId)`, `ToggleGroup(type)`, `ConfirmSelected`, `Cancel`.
- `PendingDeletionsViewModel.kt` (public, intent-only per CLAUDE.md): observes `syncEngine.pendingDeletions`, builds grouped state (checked = will be deleted; default all checked). `ConfirmSelected` → `syncEngine.resolveDeletions(checkedSyncIds)` then emit a one-shot "done" effect to pop nav. `Cancel` → pop without resolving (stays pending).
- `PendingDeletionsScreen.kt` — `@Serializable data object PendingDeletionsKey : NavKey` + `EntryProviderScope<NavKey>.pendingDeletionsEntry(onDone: () -> Unit)`. Grouped checkbox list (`MmCard`, a checkbox row, `MmButton` confirm/cancel). Headers per type ("Wallets", "Transactions", "Budgets", "Recurring", "Categories", "Payment modes"). Dumb UI — all state from VM.
- Strings: group titles + screen title + confirm/cancel + explanatory subtitle, in en/de/es/it.

### Wiring
- `composeApp/.../di/FeatureModules.kt` — register `PendingDeletionsViewModel` (public) in a new `featureSyncModule`; add to `appModules`.
- `composeApp/.../MainNav.kt` — register `pendingDeletionsEntry(onDone = { tabBackStack.removeLastOrNull-equivalent })`. (The banner that NAVIGATES here is Phase 6; just wire the entry now.)

### 5b tests
- `PendingDeletionsViewModelTest` (Turbine): grouped state built from engine flow; toggle item/group flips checks; `ConfirmSelected` calls `resolveDeletions` with the checked set; `Cancel` doesn't resolve.

### 5b verification
- `./gradlew :feature:sync:compileDebugKotlinAndroid :composeApp:assembleDebug :composeApp:linkDebugFrameworkIosSimulatorArm64`
- `./gradlew :feature:sync:testDebugUnitTest`

---

## Notes
- `deleteAll()` stays HARD (restore wipe). Only `delete(id)` / `deleteByAccountId` become soft.
- No UNIQUE constraints exist beyond PKs, so tombstoned rows won't block re-inserts.
- Known-unrelated `:data:budgets` FakeBudgetRepositoryTest failure — ignore.
