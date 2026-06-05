# Phase 3 — Password / encryption as a synced conflict

**Goal:** stop silently skipping sync when the device's encryption config disagrees with the
remote. Surface a durable conflict (like pending deletions) the user can resolve, and enforce
the same password across devices.

## Changes (data/sync)
- `SyncSnapshotCodec.isEncryptedEnvelope(bytes)` — a plaintext snapshot and an encrypted envelope
  both start with `{`, so detect an envelope by parsing the required `EncryptedBackup` fields.
- `SyncConflict(remoteEncrypted: Boolean)` — `true` = remote encrypted but we can't decrypt
  (no/wrong password); `false` = remote plaintext but we're configured encrypted (downgrade).
- `SyncConflictStore` (AppSettings blob `SYNC_CONFLICT_BLOB`) — durable + observable, mirrors
  `PendingDeletionStore`.
- `SyncStatusProvider.conflict: Flow<SyncConflict?>` added.
- `SyncConflictController` interface (impl by `SyncEngine`):
  - `resolveWithPassword(pass, makeAuthoritative)` — `false`: validate by decrypting remote
    (WrongPassphrase → stays conflicted, result fails); `true`: override, re-encrypt remote.
    On success persist (Phase 1) + set encrypt on + clear conflict + pull/push.
  - `resolveWithPlaintext()` — drop encryption; if remote was readable plaintext → pull/merge,
    else (override unreadable) → push local as the new plaintext remote.
- `SyncEngine.pull()` — replaced the silent passphrase skip with `decodeRemoteOrRaiseConflict()`:
  raises `SyncConflict` instead of skipping; adopts the remote's mode on a successful decrypt;
  network/parse errors still surface as `Error`, only decrypt/auth failures become conflicts.
  Pull and push both no-op while a conflict is set.

## DI / tests
- `SyncModule.kt` — `SyncConflictStore` single; `SyncEngine` gains `syncPassphraseStore` +
  `conflictStore`; bind `SyncConflictController`.
- `SyncEngineConflictTest` — encrypted-without-pass raises conflict; resolveWithPassword merges;
  plaintext-while-encrypted raises conflict; resolveWithPlaintext merges; wrong password keeps
  conflict (uses a passphrase-binding fake crypto).
- Updated existing SyncEngine test constructors + the tx-list `SyncStatusProvider` fake.

## Verify
- `./gradlew :data:sync:testDebugUnitTest :feature:transactions:testDebugUnitTest`
- `./gradlew :composeApp:assembleDebug :composeApp:linkDebugFrameworkIosSimulatorArm64`
