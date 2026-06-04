# Phase 1 — Persist the passphrase (fix the cold-start gate)

**Goal:** make existing sync survive app restarts. Today `SessionPassphrase` is memory-only;
`REMOTE_BACKUP_ENCRYPT` defaults true, so after every restart `SyncEngine.pull()`/`push()`
silently skip ("no session passphrase"). Persist the passphrase in `SecureStore` and reload it
into `SessionPassphrase` at boot before the first pull.

## Changes

### `data/remotebackup` — `SyncPassphraseStore`
New class beside `SessionPassphrase`, wrapping `SecureStore` (already a dep of the module):
- `suspend fun persist(passphrase: CharArray)` → `secureStore.put(KEY, bytes)`
- `suspend fun load(): CharArray?` → decode stored bytes
- `suspend fun clear()` → `secureStore.remove(KEY)`
- `suspend fun hydrate(session: SessionPassphrase)` → load → if non-null `session.set(it)`
- KEY = `"sync.passphrase"`. UTF-8 via `concatToString().encodeToByteArray()` /
  `decodeToString().toCharArray()`.

### Boot hydration
`AppInitializer.initialize()` — inject `SyncPassphraseStore`; call `hydrate(sessionPassphrase)`
**before** `syncEngine.pullNow()`. Inject `SessionPassphrase` too.

### Persist on set / clear on disconnect
`BackupRestoreViewModel`:
- `handlePasswordSubmitted` — after `sessionPassphrase?.set(value)` (encrypt branch), also
  `syncPassphraseStore?.persist(value)` (before the `value.fill(' ')`).
- `handleDisconnectGoogle` — after `sessionPassphrase?.clear()`, also `syncPassphraseStore?.clear()`.
- Inject `syncPassphraseStore: SyncPassphraseStore? = null`.

### DI
- `RemoteBackupModule.kt` — `single { SyncPassphraseStore(secureStore = get()) }`.
- `AppInitializer` registration (FeatureModules.kt) — add the two new `get()` args.
- `BackupRestoreViewModel` registration (FeatureModules.kt) — add `syncPassphraseStore = get()`.

## Tests
- `SyncPassphraseStoreTest` (data/remotebackup, `InMemorySecureStore`): persist→load round-trip;
  clear removes; hydrate sets SessionPassphrase when present, no-op when absent.

## Verify
- `./gradlew :data:remotebackup:compileDebugKotlinAndroid :composeApp:assembleDebug :composeApp:linkDebugFrameworkIosSimulatorArm64`
- `./gradlew :data:remotebackup:testDebugUnitTest :feature:settings:testDebugUnitTest`
- Manual: set password, kill app, reopen → SyncEngine pull no longer skips.
