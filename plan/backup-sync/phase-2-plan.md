# Phase 2 — Sync snapshot format + Drive named-file I/O

**Status: DONE** — data/sync module created (DTOs, codec, store, DeviceIdentity), provider extended with findByName+updateContents, DeviceInfo expect/actual, 5 PrefKeys, syncCommonModule wired. All 4 builds green.

## Goal
Stand up a new `data/sync` Kotlin Multiplatform module with: syncId-keyed snapshot DTOs, a device-registry DTO, a JSON codec (optional encryption reusing `BackupCrypto`), a `SyncRemoteStore` that reads/writes **stable named files** on Drive (named-file upsert), the Drive-provider methods needed for that, and a `DeviceIdentity` helper. **Plumbing only** — nothing reads the local DB or applies anything yet. App behavior unchanged (new module not yet triggered).

## New module: `data/sync`
Mirror `data/remotebackup` exactly. Use the `feature-module` skill for the wiring steps.

1. `settings.gradle.kts` — add `include(":data:sync")` after `:data:remotebackup` (line ~53).
2. `data/sync/build.gradle.kts` — copy `data/remotebackup/build.gradle.kts`, change:
   - framework `baseName = "DataSync"`
   - `android { namespace = "com.dv.moneym.data.sync" }`
   - commonMain deps: `kotlinx.serialization.json`, `kotlinx.coroutines.core`, `kotlinx.datetime`, `kermit`, `projects.core.common`, `projects.core.security`, `projects.core.datastore`, `projects.core.platform`, `projects.data.remotebackup`. (NO repo modules yet — those join in Phase 3. NO ktor needed here; provider is injected.)
   - commonTest deps: `kotlin.test`, `kotlinx.coroutines.test`. (Add `projects.core.testing` if you reuse `FakeAppSettings`; otherwise use a tiny in-memory AppSettings in the test.)
3. Add `:data:sync` to `composeApp/build.gradle.kts` dependencies (find where `projects.data.remotebackup` is listed and add alongside).

## Files to create (commonMain, package `com.dv.moneym.data.sync`)

### `SyncSnapshot.kt` — `@Serializable` DTOs, **FK carried by syncId, never Long PK**
```
SyncAccount(syncId, name, type, currency, isDefault, archived, colorHex, deleted, createdAt, updatedAt)
SyncCategory(syncId, name, iconKey, colorHex, isUserCreated, archived, categoryType, deleted, createdAt, updatedAt)
SyncPaymentMode(syncId, name, deleted, createdAt, updatedAt)
SyncTransaction(syncId, type, amountMinor, currency, occurredOn, note?, categorySyncId, accountSyncId, paymentModeSyncId?, recurringSyncId?, deleted, createdAt, updatedAt)
SyncRecurring(syncId, type, amountMinor, currency, note?, categorySyncId, accountSyncId, paymentModeSyncId?, startDate, freqUnit, freqInterval, dayOfWeek?, dayOfMonth?, useLastDay, endKind, endCount?, endDate?, lastMaterializedDate?, deleted, createdAt, updatedAt)
SyncBudget(syncId, name, amountMinor, currency, categorySyncId?, accountSyncId, periodType, startYearMonth, recurringMonths?, deleted, createdAt, updatedAt)
SyncSnapshot(formatVersion: Int = 1, generatedAtMs: Long, originDeviceId: String, accounts, categories, paymentModes, recurring, budgets, transactions) — all lists default emptyList()
```
Field names/types mirror `data/backup/ExportDtos.kt` (same shapes), but with `syncId`/`*SyncId` strings replacing Long ids and FKs, plus a `deleted: Boolean` on each. All-default-emptyList lists + `formatVersion` for forward-compat.

### `DeviceRegistry.kt`
```
@Serializable DeviceEntry(id, displayName, platform, lastSyncMs)
@Serializable DeviceRegistry(formatVersion: Int = 1, devices: List<DeviceEntry> = emptyList())
```

### `SyncSnapshotCodec.kt`
- `private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }`
- `encode(snapshot): ByteArray` and `decode(bytes): SyncSnapshot` (plaintext).
- `suspend fun seal(snapshot, passphrase: CharArray?): ByteArray` — if passphrase null → plaintext JSON bytes; else encrypt via `crypto.encrypt(plain, passphrase, schema, appVersion, createdAt)` then `BackupEnvelopeJson.encodeBytes(envelope)`.
- `suspend fun open(bytes, passphrase: CharArray?): SyncSnapshot` — detect envelope by first byte `{` AND presence of envelope fields (reuse the `isEncryptedEnvelope` trick from `RemoteBackupManager`: first non-ws byte `{` is ambiguous since plaintext JSON also starts `{`). **Disambiguate** by trying `BackupEnvelopeJson.decode` and checking for `cipher`/`kdf` — simplest: a 1-byte magic prefix is risky; instead, encode plaintext snapshots are also `{`. Use this rule: if `passphrase != null` treat as envelope (decrypt then decode); if null, decode plaintext. The caller (Phase 4 engine) knows whether encryption is enabled via `REMOTE_BACKUP_ENCRYPT`, so pass that intent in. Keep `seal`/`open` symmetric and passphrase-driven.
- Constructor: `SyncSnapshotCodec(crypto: BackupCrypto, appVersion: String, schemaVersion: Int = 1)`.
- Mirror `RemoteBackupManager` encrypt/decrypt flow (lines 122-148, 98-113).

### `SyncRemoteStore.kt`
- Constructor: `SyncRemoteStore(provider: RemoteBackupProvider)`.
- Constants: `SYNC_STATE_FILE = "moneym-sync-state.json"`, `DEVICES_FILE = "moneym-devices.json"` (both safe from `pruneOldBackups` which filters prefix `moneym-backup`).
- `suspend fun readSnapshotBytes(): ByteArray?` — `provider.findByName(SYNC_STATE_FILE)?.let { provider.download(it) }`.
- `suspend fun writeSnapshotBytes(bytes): RemoteFileRef` — `findByName` → if present `provider.updateContents(ref, bytes)` else `provider.upload(bytes, SYNC_STATE_FILE)`.
- `readDevicesBytes()` / `writeDevicesBytes(bytes)` — same upsert against `DEVICES_FILE`.

### `DeviceIdentity.kt`
- Constructor: `DeviceIdentity(appSettings: AppSettings)`.
- `fun deviceId(): String` — read `PrefKeys.DEVICE_ID`; if absent generate `Uuid.random().toString()`, persist, return.
- `fun deviceName(): String` — read `PrefKeys.DEVICE_NAME`; if absent default to `deviceModelName()`, persist, return.
- `fun setDeviceName(name)` — persist `PrefKeys.DEVICE_NAME`.
- `fun platform(): String` — `deviceModelName()` is the model; platform string e.g. from a second expect or just embed in name. Keep `platform()` = `devicePlatformName()` (see expect/actual below).

## Extend the Drive provider
`data/remotebackup/.../RemoteBackupProvider.kt` — add to interface (with default impls so nothing else breaks):
```kotlin
suspend fun findByName(name: String): RemoteFileRef? = list(limit = 100).firstOrNull { it.name == name }
suspend fun updateContents(ref: RemoteFileRef, bytes: ByteArray, properties: Map<String, String> = emptyMap()): RemoteFileRef
```
`GoogleDriveBackupClient.kt` — implement `updateContents` via `PATCH $DRIVE_UPLOAD_BASE/files/{ref.id}?uploadType=media&fields=$SINGLE_FILE_FIELDS` with raw `setBody(bytes)` + auth header, parse `DriveFileDto`. (`findByName` can use the default, or override for an exact-name query.)

## expect/actual: device model + platform name
`core/platform/src/commonMain/.../DeviceInfo.kt`:
```kotlin
expect fun deviceModelName(): String
expect fun devicePlatformName(): String   // "Android" / "iOS"
```
- `androidMain/.../DeviceInfo.android.kt`: `actual fun deviceModelName() = android.os.Build.MODEL`; `actual fun devicePlatformName() = "Android"`.
- `iosMain/.../DeviceInfo.ios.kt`: `actual fun deviceModelName() = UIDevice.currentDevice.name` (fallback `.model` if blank); `actual fun devicePlatformName() = "iOS"`. (`import platform.UIKit.UIDevice`.)
These are top-level funs — no DI/Context needed.

## PrefKeys additions
`core/datastore/.../AppSettings.kt` PrefKeys — add:
```kotlin
const val DEVICE_ID = "pref.device_id"
const val DEVICE_NAME = "pref.device_name"
const val LAST_SYNC_PULL_MS = "pref.last_sync_pull_ms"
const val CROSS_DEVICE_SYNC_ENABLED = "pref.cross_device_sync_enabled"
const val PENDING_DELETION_BLOB = "pref.pending_deletion_blob"
```

## Koin wiring
New `composeApp/src/commonMain/.../di/SyncModule.kt` (a common `Module`, no platform variant):
```kotlin
val syncCommonModule = module {
    single { DeviceIdentity(appSettings = get()) }
    single { SyncSnapshotCodec(crypto = get(), appVersion = APP_VERSION) }   // APP_VERSION="1.0" like RemoteBackupModule
    single { SyncRemoteStore(provider = get()) }
}
```
Add `syncCommonModule` to `appModules` in `AppModules.kt` (after `remoteBackupPlatformModule()`). `BackupCrypto` + `RemoteBackupProvider` + `AppSettings` already provided.

## Tests (commonTest)
- `SyncSnapshotCodecTest` — plaintext round-trip (`seal(null)`/`open(null)`); encrypted round-trip using a **`FakeBackupCrypto`** test double (identity or simple transform) to avoid platform crypto in commonTest; unknown-field tolerance; formatVersion preserved.
- `SyncRemoteStoreTest` — `FakeRemoteBackupProvider` (in-memory `MutableMap<String, ByteArray+ref>`): first `writeSnapshotBytes` uploads, second **updates the same file** (count stays 1); devices round-trip; read returns null when absent.
- `DeviceIdentityTest` — in-memory `AppSettings` (or `FakeAppSettings`): `deviceId()` stable across calls + persisted; `deviceName()` defaults then respects `setDeviceName`.
- Extend `GoogleDriveBackupClientTest` — `updateContents` issues PATCH to `/upload/drive/v3/files/{id}` with `uploadType=media`; returns parsed ref.

## Verification
- `./gradlew :data:sync:assembleDebug` (or `:data:sync:compileDebugKotlinAndroid`)
- `./gradlew :composeApp:assembleDebug`
- `./gradlew :composeApp:linkDebugFrameworkIosSimulatorArm64`
- `./gradlew :data:sync:testDebugUnitTest :data:remotebackup:testDebugUnitTest`

## Notes / pitfalls
- `data/sync` must NOT depend on repo modules yet (keeps the module boundary clean; Phase 3 adds them for the exporter).
- Keep sync file names off the `moneym-backup` prefix so `RemoteBackupManager.pruneOldBackups()` never deletes them — verify that filter still reads `startsWith("moneym-backup")`.
- Encryption symmetry: the engine (Phase 4) decides encrypt on/off from `REMOTE_BACKUP_ENCRYPT` + `SessionPassphrase`; codec stays passphrase-driven and dumb.
