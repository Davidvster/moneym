# Phase 2 — Decouple sync lifecycle from local auto-backup

**Goal:** sync push must run whenever cross-device sync is on, independent of the local
file-backup toggle. Previously `SyncEngine.start()` and the on-change `enqueuePush()` lived
inside `AutoBackupManager`, which `App.kt` only starts when `AUTO_BACKUP_ENABLED`.

## Changes
- `SyncEngine.start(scope)` now launches two children: the existing push-pulse collector **and**
  a `dataChanges()` observer (`combine` of all 6 repo `observeAll()` flows → `drop(1)` →
  `debounce` → `enqueuePush()`). The engine already injects all 6 repos.
- `AutoBackupManager` — drop the `syncEngine` param and its `enqueuePush()/start()/stop()` calls.
  It is now purely about local + remote *file backup*.
- `App.kt` — new `LaunchedEffect(syncEnabled)` keyed on `CROSS_DEVICE_SYNC_ENABLED` that
  `syncEngine.start(this)` / `stop()`, separate from the auto-backup effect.
- DI: drop the `SyncEngine` arg from the `AutoBackupManager` single.

## Verify
- builds + `:data:sync:testDebugUnitTest` (existing trigger tests still green)
- Manual: sync ON, local auto-backup OFF → adding a transaction writes the remote sync-state.
