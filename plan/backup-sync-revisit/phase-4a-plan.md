# Phase 4a — Cloud backend unification (history rides the cloud flag)

Backend half of Phase 4: make the periodic snapshot-history safety net run under cross-device
sync rather than the local file-backup toggle, and add a detector the join UI needs.

## Changes
- `AutoBackupManager` is now **local file backup only** — dropped `RemoteBackupManager` and its
  start/stop/enqueue.
- `SyncEngine` owns the cloud lifecycle: optional `remoteBackupManager` injected; `start()` also
  starts it and its data-change observer also calls `enqueueUpload()`; `stop()` stops it.
  So enabling cross-device sync (App's `CROSS_DEVICE_SYNC_ENABLED` effect, Phase 2) now runs
  both the merge engine and the snapshot history.
- `SyncBootstrap` interface (`remoteState()` → NONE/ENCRYPTED/PLAINTEXT, `canDecrypt(pass)`),
  implemented by `SyncEngine`, for the join/enable flow.
- DI: `AutoBackupManager` loses its `RemoteBackupManager` arg; `SyncEngine` gains
  `remoteBackupManager = getOrNull()`; bind `SyncBootstrap`.

## Verify
- `:data:sync:testDebugUnitTest` green; `:composeApp:assembleDebug` + iOS link green.
