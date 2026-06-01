# Phase 7 — Device registry UI + cross-device sync settings

**Status: DONE** — DeviceRegistryManager (load/touchSelf/rename/remove over plaintext devices.json); SyncEngine touchSelf after each successful pull/push (runCatching-guarded); feature/sync SyncSettings screen (toggle CROSS_DEVICE_SYNC_ENABLED + pullNow on enable, rename device, device list w/ remove); settings overview row. i18n. All builds + tests green.

## Goal
User-facing controls: a `CROSS_DEVICE_SYNC_ENABLED` toggle (distinct from existing remote-backup auto toggle), rename-this-device, and a participating-device list (from `devices.json`) with remove. Each successful sync upserts this device's entry into `devices.json`.

## data/sync — device registry maintenance
### `DeviceRegistryManager.kt`
Constructor: `DeviceRegistryManager(store: SyncRemoteStore, deviceIdentity: DeviceIdentity, json: Json = Json{ignoreUnknownKeys=true; encodeDefaults=true}, nowMs: () -> Long)`.
- `suspend fun load(): List<DeviceEntry>` — `store.readDevicesBytes()` → decode `DeviceRegistry` (empty list if absent/parse-fail).
- `suspend fun touchSelf()` — load, upsert this device's `DeviceEntry(id = deviceIdentity.deviceId(), displayName = deviceIdentity.deviceName(), platform = deviceIdentity.platform(), lastSyncMs = nowMs())` (replace existing entry with same id), write back via `store.writeDevicesBytes`.
- `suspend fun rename(name: String)` — `deviceIdentity.setDeviceName(name)`; `touchSelf()`.
- `suspend fun remove(deviceId: String)` — load, drop entry with that id, write back.
- `val thisDeviceId: String = deviceIdentity.deviceId()`.
Register in `composeApp/.../di/SyncModule.kt` (`single { DeviceRegistryManager(get(), get(), nowMs = …) }`).
Devices.json is **plaintext** (not sensitive) — do NOT route through the encrypted codec.

### SyncEngine hook
Inject `DeviceRegistryManager` (nullable ok). After a successful `pull()` AND successful `push()`, call `deviceRegistryManager?.touchSelf()` (wrap in runCatching — registry upkeep must never fail the sync). This keeps each device's lastSyncMs fresh.

## feature/sync — settings screen
### Files
- `SyncSettingsUiState.kt` + `SyncSettingsIntent.kt`:
  - State: `crossDeviceSyncEnabled: Boolean`, `thisDeviceName: String`, `isRenaming: Boolean`, `renameDraft: String`, `devices: List<DeviceRow>` where `DeviceRow(id, displayName, platform, lastSyncMs, isThisDevice)`, `isLoading`.
  - Intent: `ToggleSync`, `StartRename`, `RenameDraftChanged(String)`, `SubmitRename`, `CancelRename`, `RemoveDevice(id)`, `Refresh`.
- `SyncSettingsViewModel.kt` (public, intent-only): 
  - On init + `Refresh`: load `deviceRegistryManager.load()` → map to rows (mark `isThisDevice = id == thisDeviceId`, sort this-device first then by lastSyncMs desc); read `crossDeviceSyncEnabled` + `thisDeviceName` from settings/identity.
  - `ToggleSync` → `appSettings.putBoolean(CROSS_DEVICE_SYNC_ENABLED, newValue)`; if enabling → `syncEngine.pullNow()` (kick an immediate sync) then refresh; if disabling → just update state.
  - `SubmitRename` → `deviceRegistryManager.rename(renameDraft)` → refresh.
  - `RemoveDevice` → `deviceRegistryManager.remove(id)` → refresh.
  - Depend on minimal interfaces where the concrete classes are awkward to fake (reuse the established pattern); otherwise inject `DeviceRegistryManager`, `AppSettings`, `SyncEngine`/`SyncStatusProvider`-style.
- `SyncSettingsScreen.kt` — `@Serializable data object SyncSettingsKey : NavKey` + `EntryProviderScope<NavKey>.syncSettingsEntry(onBack: () -> Unit)`. Layout: `ScreenHeader`, an `MmToggle`/switch row for cross-device sync, a rename row (`MmField` + save when `isRenaming`, else current name + edit button), a device list (`MmCard` per device showing name + platform + "last synced" relative time; a remove icon/button, hidden or disabled for `isThisDevice`). Dumb UI.
- Strings (en/de/es/it): screen title, sync toggle label + subtitle, "This device", rename label/save/cancel, device-list header, last-synced label, remove, platform labels reuse the stored platform string.

### Wiring
- `composeApp/.../di/FeatureModules.kt` — register `SyncSettingsViewModel` in `featureSyncModule` (public).
- `composeApp/.../MainNav.kt` — register `syncSettingsEntry(onBack = { tabBackStack.removeLast()/pop })`.
- Settings overview entry point: in `feature/settings` overview screen + its ViewModel, add a row "Cross-device sync" with an `onNavigateToSync` callback; in `MainNav` settings entry registration pass `onNavigateToSync = { tabBackStack.push(SyncSettingsKey) }`. Mirror the existing `onNavigateToBackupRestore` plumbing (settings VM intent/state → screen row → entry callback). Place it near the Backup/Restore row but visually distinct (it's a separate concern). Add a string for the row label (en/de/es/it).

## Reference files (read)
- data/sync/.../DeviceIdentity.kt, DeviceRegistry.kt, SyncRemoteStore.kt (devices bytes), SyncEngine.kt (pull/push success points), di/SyncModule.kt.
- feature/sync/.../PendingDeletions* (module conventions, NavKey+entry, strings layout).
- feature/settings overview screen + ViewModel + the `onNavigateToBackupRestore` plumbing, and MainNav settings entry registration (mirror for `onNavigateToSync`).
- core/ui MmToggle, MmField, MmCard, MmSettingsRow, ScreenHeader.

## Tests
- `SyncSettingsViewModelTest` (Turbine): toggle persists to settings (+ triggers pullNow when enabling); rename calls registry.rename and refreshes; remove drops the device; this-device row flagged + not removable.
- `DeviceRegistryManagerTest` (data/sync, fake store): touchSelf upserts (no dup on repeat); rename updates name; remove drops entry; load tolerates absent/garbage.

## Verification
- `./gradlew :data:sync:compileDebugKotlinAndroid :feature:sync:compileDebugKotlinAndroid :feature:settings:compileDebugKotlinAndroid :composeApp:assembleDebug :composeApp:linkDebugFrameworkIosSimulatorArm64`
- `./gradlew :data:sync:testDebugUnitTest :feature:sync:testDebugUnitTest :feature:settings:testDebugUnitTest`

## Notes
- `touchSelf()` after sync must never throw into the sync flow (runCatching).
- Removing a still-active device is advisory — it re-adds itself on its next sync. Acceptable; the screen copy can hint this.
- Known-unrelated `:data:budgets` FakeBudgetRepositoryTest failure — ignore.
