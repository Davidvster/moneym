# Phase 5 — Tx-list sync status: tappable banner + bottom sheet

**Goal:** make sync visible and manually triggerable from the transaction list.

## Changes
- `SyncStatusProvider` gains `lastSyncedMs: Flow<Long>`; `SyncPuller` gains `syncNow()`
  (pull + push). `SyncEngine` exposes `lastSyncedMs` (StateFlow seeded from `LAST_SYNC_PULL_MS`,
  bumped on each successful pull) and implements `syncNow()`.
- `TransactionListUiState`: `hasSyncConflict`, `lastSyncedMs`, `showSyncSheet`.
- `TransactionListViewModel`: folds `conflict` + `lastSyncedMs` into state; intents
  `ShowSyncSheet` / `SyncNow`; injects `SyncPuller`.
- `TransactionListScreen`:
  - `SyncBanner` is tappable (opens the sheet) and now also shows a transient **Synced ✓** for
    a few seconds after a sync, and a **Sync paused — action needed** row on conflict.
  - New `SyncStatusSheet` (Material3 `ModalBottomSheet`): current status, last-synced time,
    a **Sync now** button (disabled while syncing/conflicted), a pending-deletions review row,
    and a conflict hint pointing to Backup settings.
- Strings (en/de/es/it). Test fakes updated for the new interface members.

## Gotcha
Compose-resources accessors are extension properties in the `…generated.resources` package and
must be **explicitly imported** per key in each screen — adding a `stringResource(Res.string.X)`
without the matching `import …resources.X` fails as "Unresolved reference" even though the
generated accessor exists.

## Verify
- `:feature:transactions:testDebugUnitTest :feature:sync:testDebugUnitTest :data:sync:testDebugUnitTest`
  + assembleDebug + iOS link green.
