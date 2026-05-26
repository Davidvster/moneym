# Phase 8.4 — RemoteBackupManager + auto-upload wiring

Orchestrator that ties together the pieces from Phases 8.1–8.3: `DbBackupManager.export()` → `BackupCrypto.encrypt(...)` → `RemoteBackupProvider.upload(...)`, plus the symmetric restore path.

## Files

### `data:remotebackup` (new)
- `RemoteBackupManager.kt` — coalesces upload requests via `MutableSharedFlow.debounce(5 s)`. Holds a `StateFlow<RemoteBackupRuntimeState>` (`Idle | Encrypting | Uploading | Downloading | Decrypting | Restoring | Error`). `restoreLatest(passphrase)` runs the inverse flow. Catches all errors, logs via Kermit, surfaces a human-readable message in the runtime state — never throws upward into the auto-backup loop.
- `RemoteBackupRuntimeState.kt` — sealed hierarchy.
- `SessionPassphrase.kt` — in-memory passphrase holder, defensive `copyOf` on read, zeroize on `clear`.
- `commonTest/.../SessionPassphraseTest.kt`.

### `core:datastore`
New `PrefKeys`:
- `AUTO_REMOTE_BACKUP_ENABLED`
- `LAST_REMOTE_BACKUP_TIME_MS`
- `REMOTE_BACKUP_PROVIDER_ID`
- `REMOTE_BACKUP_ACCOUNT_EMAIL`

### `composeApp`
- `AutoBackupManager.kt` — accepts optional `RemoteBackupManager?`. After every local save it calls `remoteBackupManager?.enqueueUpload()`. The remote manager's debounce loop is started/stopped alongside the local loop.
- `AppLifecycleObserver.kt` — accepts optional `RemoteBackupManager?`; on `ON_PAUSE` launches `flushNow()` on a supervisor scope so a pending debounce-window upload reaches Drive before the app suspends.
- `composeApp/build.gradle.kts` — adds `:data:remotebackup` and `:core:oauth` as project deps.

## Storage format

Each remote backup is a single Drive file `moneym-backup.bin` (in `appDataFolder`) containing the JSON-encoded `EncryptedBackup` envelope. Drive `appProperties` mirror the envelope's `schema`/`appVersion`/`createdAt` for cheap listing without download.

## Failure handling

- Upload failures are caught at every level; the auto-backup loop continues to receive pulses for subsequent mutations.
- A second `enqueueUpload()` while an upload is in flight re-emits one pulse so the latest state is uploaded next; never queues more than one.
- A restore failure leaves the local DB untouched (decryption fails before `DbBackupManager.restore` is called).

## Build safety

`AutoBackupManager` / `AppLifecycleObserver` accept the remote manager as nullable; if Phase 8.6 doesn't bind it (config missing), the existing behavior is unchanged.

## Verification

```
./gradlew :data:remotebackup:compileDebugKotlinAndroid \
          :composeApp:compileDebugKotlinAndroid \
          :data:remotebackup:testDebugUnitTest
```

All green.
