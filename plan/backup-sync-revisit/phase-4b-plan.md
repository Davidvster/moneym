# Phase 4b — Unified Cloud sync UI + same-password join

UI half of Phase 4: one toggle, one password prompt per device, join-with-existing-password.

## Changes
- `BackupRestoreViewModel`:
  - Inject `SyncBootstrap` + `SyncPuller`.
  - State: `cloudSyncEnabled`, `cloudEnableStep` (Create / JoinEncrypted / JoinPlaintext),
    `cloudBusy`, `cloudJoinError`.
  - Intents: `CloudSyncToggled`, `CloudCreateSubmitted`, `CloudJoinSubmitted`,
    `CloudJoinPlaintextConfirmed`, `CloudEnableDismissed`.
  - Enable flow detects remote state → Create (seed), JoinEncrypted (validate password via
    `canDecrypt`, wrong → error), or JoinPlaintext (confirm). `enableCloud()` sets both
    `CROSS_DEVICE_SYNC_ENABLED` + `AUTO_REMOTE_BACKUP_ENABLED` and kicks `pullNow()`.
  - Disconnect / delete-remote also clear the unified flags.
- `BackupRestoreScreen`: the "Google Drive backup" card now shows a single **Cloud sync** toggle
  (replaces the separate remote-auto toggle); when on, a **Devices** row → SyncSettings, plus
  Backup now / Restore / Delete / status. New `CloudJoinDialog`; Create reuses `PasswordDialog`;
  plaintext-join is a confirm dialog. Removed the standalone Cross-device-sync nav card.
- `SyncSettingsScreen`: dropped its enable toggle (enable lives on the unified screen); keeps
  rename + device list.
- DI: `BackupRestoreViewModel` gains `syncBootstrap`/`syncPuller`; feature/settings now deps
  data/sync. New strings (en/de/es/it).

## Verify
- `:feature:settings:testDebugUnitTest :feature:sync:testDebugUnitTest` + assembleDebug + iOS link green.
- Manual: device A enable → create password → data on Drive; device B enable → prompted for the
  same password → A's transactions appear.
