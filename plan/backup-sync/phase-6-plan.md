# Phase 6 — Sync triggers + transaction-screen banner (goes live)

**Status: DONE** — SyncEngine start/stop/enqueuePush/pullNow self-gated by CROSS_DEVICE_SYNC_ENABLED; startup pull (AppInitializer), on-change push (AutoBackupManager debounce), foreground pull (AppLifecycleObserver); tx-list SyncBanner (syncing spinner + pending-deletions review row) via SyncStatusProvider iface, nav pushes PendingDeletionsKey. i18n strings. All builds + tests green.

## Goal
Wire automatic sync behind `PrefKeys.CROSS_DEVICE_SYNC_ENABLED` (default false): startup pull, on-change push (reuse existing debounce), foreground pull. Add a sync-in-progress banner + a "N pending deletions — review" tappable row above the transaction list header. After this phase sync actually runs.

## SyncEngine trigger API (add)
Mirror `RemoteBackupManager` (debounce pulse + Mutex already present):
- `fun start(scope: CoroutineScope)` / `fun stop()` — launch a job collecting a debounced push pulse (`MutableSharedFlow` + `.debounce(DEFAULT_DEBOUNCE_MS)`), calling `push()`.
- `fun enqueuePush()` — `tryEmit` to the pulse; early-return if `!enabled()`.
- `suspend fun pullNow(): Result<Unit>` — wraps existing `pull()`; early-return if `!enabled()`.
- `private fun enabled() = appSettings.getBoolean(PrefKeys.CROSS_DEVICE_SYNC_ENABLED, false)`.
- Expose `pendingDeletionCount: Flow<Int>` (from `pendingDeletionStore.count`).
- **Self-gate**: all triggers no-op when disabled, so callers don't repeat the check.

## Triggers
1. **AppInitializer** — add `syncEngine: SyncEngine` param; at end of `initialize()` call `syncEngine.pullNow()` (self-gated). Update its `single { AppInitializer(...) }` registration in `composeApp/.../di/FeatureModules.kt`.
2. **AutoBackupManager** — add `syncEngine: SyncEngine? = null` param; in the debounced `.collect { … }` block, after `remoteBackupManager?.enqueueUpload()`, add `syncEngine?.enqueuePush()`. In `start(scope)` also `syncEngine?.start(scope)`; in `stop()` also `syncEngine?.stop()`. Wire the new arg where AutoBackupManager is constructed (DI).
3. **AppLifecycleObserver** — add `syncEngine: SyncEngine? = null` param; on `ON_RESUME`, after `lockController.onForeground()`, `scope.launch { syncEngine?.pullNow() }` (self-gated). Wire the arg where it's constructed (`App.kt`).

## Transaction-list banner
- `feature/transactions/.../list/TransactionListUiState.kt` — add `isSyncInProgress: Boolean = false`, `pendingDeletionCount: Int = 0`.
- `feature/transactions/.../list/TransactionListViewModel.kt` — inject `SyncEngine`; collect `syncEngine.runtime` (map to `isSyncInProgress = runtime != Idle && != Error`) and `syncEngine.pendingDeletionCount`; fold both into state. Update its DI registration in FeatureModules (SyncEngine is in `syncCommonModule`).
- `feature/transactions/.../list/TransactionListScreen.kt` — render, above `TransactionListHeader` inside `TransactionListContent`, an `AnimatedVisibility` banner: a thin row with a spinner + "Syncing…" when `isSyncInProgress`; a tappable row "N items removed on another device — review" when `pendingDeletionCount > 0` → calls a new `onNavigateToPendingDeletions` lambda. Strings in composeResources (en/de/es/it), no literals in Kotlin.
- `transactionsEntry(...)` (in TransactionListScreen.kt) — add `onNavigateToPendingDeletions: () -> Unit` param, thread to the screen.
- `composeApp/.../MainNav.kt` — in `transactionsEntry(...)` registration pass `onNavigateToPendingDeletions = { tabBackStack.add(PendingDeletionsKey) }` (import the key). `PendingDeletionsKey`/`pendingDeletionsEntry` already registered in Phase 5b (its `onDone` pops back).

## App.kt
- Pass `syncEngine` into `AppLifecycleObserver(...)`.
- Where `autoBackupManager.start(scope)` is started, ensure SyncEngine starts/stops with it (handled via AutoBackupManager change) — or start/stop SyncEngine in the same `LaunchedEffect` that observes enable toggles. Keep it consistent with how `AutoBackupManager` lifecycle is managed today.

## Reference files (read these)
- data/sync/.../SyncEngine.kt (current pull/push/Mutex), data/remotebackup/.../RemoteBackupManager.kt (start/stop/enqueue/debounce pulse pattern to mirror).
- composeApp/.../App.kt (how AutoBackupManager + AppLifecycleObserver are constructed/started; the LaunchedEffect lifecycle).
- composeApp/.../di/FeatureModules.kt (AppInitializer + TransactionListViewModel registrations), DataModules/AppModules for AutoBackupManager construction.
- feature/transactions/.../list/{TransactionListScreen,TransactionListViewModel,TransactionListUiState}.kt.
- composeApp/.../MainNav.kt (transactionsEntry registration + tabBackStack add/pop).
- feature/sync/.../PendingDeletionsScreen.kt (PendingDeletionsKey).

## Tests
- `SyncEngineTriggerTest` (data/sync): `enqueuePush` debounces to one push; disabled flag → pull/push no-op; pull and push serialize (don't interleave) under the Mutex.
- `TransactionListViewModelTest` (Turbine): `isSyncInProgress` reflects a fake engine runtime flow; `pendingDeletionCount` reflects the count flow.

## Verification
- `./gradlew :data:sync:compileDebugKotlinAndroid :feature:transactions:compileDebugKotlinAndroid :composeApp:assembleDebug :composeApp:linkDebugFrameworkIosSimulatorArm64`
- `./gradlew :data:sync:testDebugUnitTest :feature:transactions:testDebugUnitTest`

## Notes
- Everything self-gated behind `CROSS_DEVICE_SYNC_ENABLED` (default false) — no behavior change until Phase 7's toggle turns it on.
- If `TransactionListViewModel` test infra makes SyncEngine hard to fake, reuse the `SyncDeletionController`-style minimal-interface trick (or expose runtime+count via a small interface the VM depends on).
- Known-unrelated `:data:budgets` FakeBudgetRepositoryTest failure — ignore.
