# Phase 3 — onboarding Google Drive restore (task 2, full parity)

Today onboarding "Restore from Backup" (in `OnboardingCurrencyStep`) is local-file-only + unencrypted. Replace it with a dedicated onboarding **Restore** screen offering: (A) Restore from local file (prompting passphrase if the file is encrypted), and (B) Connect Google + restore latest from Drive. Reuse the same managers the settings `BackupRestoreViewModel` uses. The Drive section is hidden when `googleAuthManager` is null / not configured (e.g. iOS without OAuth config).

Reference implementation to mirror: `feature/settings/.../overview/backuprestore/BackupRestoreViewModel.kt` + `BackupRestoreScreen.kt` (read both). Reuse their logic patterns; the dialogs there are `private` and use settings strings, so MIRROR them in onboarding with new strings rather than importing.

## Managers/APIs to reuse (already DI singletons)
- `DbBackupManager.restore(plain: ByteArray)` — restores + terminates app (data/backup).
- `BackupCodec` (data/backup): `isEncrypted(bytes): Boolean`, `suspend open(bytes, passphrase: CharArray): ByteArray`. Wrong password throws `com.dv.moneym.core.security.BackupCryptoError`.
- `GoogleAuthManager` (core/oauth): `isConfigured: Boolean`, `state: StateFlow<AuthState>` (`AuthState.SignedOut` / `SignedIn(email)`), `suspend signIn(): Result<AuthState.SignedIn>`.
- `RemoteBackupManager` (data/remotebackup): `suspend peekLatestMetadata(): Result<RemoteBackupMetadata?>`, `suspend restoreLatest(passphrase: CharArray): Result<Unit>` (ignores passphrase if backup unencrypted, restores + terminates).
- `RemoteBackupMetadata` fields: `createdAtMs, appVersion, schema, envelopeVersion, sizeBytes, fileName`.
- `SessionPassphrase` (data/remotebackup) — not strictly needed for restore; restoreLatest takes the passphrase directly. Inject as `getOrNull` only if convenient; otherwise omit.

## 1. Gradle deps
`feature/onboarding/build.gradle.kts` commonMain: add
```
implementation(projects.data.remotebackup)
implementation(projects.core.oauth)
```
(data.backup, core.security already present.)

## 2. New ViewModel — `feature/onboarding/.../restore/OnboardingRestoreViewModel.kt`
Intent-only public VM (registered in Koin → must be public per CLAUDE.md). Pattern mirrors `OnboardingCurrencyViewModel` (savedStateHandle.saved StateFlow, intent fun, viewModelScope + dispatchers.io).

Constructor:
```kotlin
class OnboardingRestoreViewModel(
    private val dbBackupManager: DbBackupManager,
    private val backupCodec: BackupCodec,
    private val appSettings: AppSettings,
    private val dispatchers: DispatcherProvider,
    private val googleAuthManager: GoogleAuthManager? = null,
    private val remoteBackupManager: RemoteBackupManager? = null,
    savedStateHandle: SavedStateHandle,
) : ViewModel()
```

UiState (`@Serializable data class`): `remoteAvailable: Boolean` (= `googleAuthManager?.isConfigured == true`), `remoteSignedIn: Boolean`, `remoteAccountEmail: String?`, `showLocalRestoreDialog: Boolean`, `localNeedsPassphrase: Boolean`, `localError: String?`, `showRemoteRestoreDialog: Boolean`, `remotePreview: RemoteBackupMetadata?`, `remotePreviewLoading: Boolean`, `remoteError: String?`, `isLoading: Boolean`. Combine `googleAuthManager?.state` into the StateFlow (like settings VM, lines 125–150) to surface `remoteSignedIn`/`remoteAccountEmail`; if manager null use `flowOf(AuthState.SignedOut)`.

Intents (sealed interface):
- `LocalFileSelected(bytes: ByteArray)` → stash bytes, set `showLocalRestoreDialog=true`, `localNeedsPassphrase = backupCodec.isEncrypted(bytes)`.
- `LocalRestoreConfirmed(passphrase: CharArray?)` → mirror settings `handleRestoreConfirmed` (lines 203–225): on io decode if encrypted via `backupCodec.open`, set `ONBOARDING_COMPLETED=true` (`appSettings.putBoolean(PrefKeys.ONBOARDING_COMPLETED, true)`) then `dbBackupManager.restore(plain)`. Catch `BackupCryptoError` → reshow dialog with `localError`; catch generic → `localError`.
- `LocalRestoreDismissed` → clear dialog + stashed bytes.
- `ConnectGoogleTapped` → mirror settings `handleConnectGoogle` (lines 314–325): `googleAuthManager?.signIn()`, on success store email in `PrefKeys.REMOTE_BACKUP_ACCOUNT_EMAIL`; on failure set `remoteError`.
- `RemoteRestoreTapped` → mirror `handleRemoteRestoreTapped` (403–432): set dialog visible + loading, `peekLatestMetadata()` → preview or error.
- `RemoteRestoreConfirmed(passphrase: CharArray)` → set `ONBOARDING_COMPLETED=true`, then `remoteBackupManager?.restoreLatest(passphrase)`; on failure set `remoteError`, `isLoading=false`. (restore terminates app.)
- `RemoteRestoreDismissed` → clear remote dialog/preview.

Set `ONBOARDING_COMPLETED=true` before restore (as current onboarding does) so a re-launch after the app terminates lands in the main app with restored data.

## 3. New Screen — `feature/onboarding/.../restore/OnboardingRestoreScreen.kt`
- `@Serializable data object OnboardingRestoreKey : NavKey`.
- `fun EntryProviderScope<NavKey>.onboardingRestoreEntry(onBack: () -> Unit, viewModel: OnboardingRestoreViewModel? = null)` with `entry<OnboardingRestoreKey> { ... }` (pattern from `onboardingCurrencyEntry`).
- Screen composable: `ScreenHeader`/back button (reuse `core/ui` `ScreenHeader` like other settings screens, with `onBack`), a `rememberBinaryFilePicker { bytes -> if (bytes != null) vm.onIntent(LocalFileSelected(bytes)) }` (`com.dv.moneym.platform.rememberBinaryFilePicker`).
- Body: two sections built from `MmButton`/`MmCard`/`MmRow` (core/ui):
  - "Restore from file" primary button → invokes the file picker.
  - Google section, only if `state.remoteAvailable`: if `!remoteSignedIn` show "Connect Google account" button → `ConnectGoogleTapped`; if `remoteSignedIn` show account email + "Restore from Google Drive" button → `RemoteRestoreTapped`.
- Dialogs (mirror settings, use `MmDialog`/`MmField`/`MmIconButton` from core/ui + new strings):
  - Local restore warning dialog (like `RestoreWarningDialog`, lines 452–503): app-close notice + passphrase field shown only when `localNeedsPassphrase`; confirm → `LocalRestoreConfirmed(passphrase?)`.
  - Remote restore dialog (like `RemoteRestoreDialog`, 595–696): loading spinner, preview (created time/app version/size), passphrase field, confirm → `RemoteRestoreConfirmed`. Keep it simpler than settings (skip tooNew/conflict logic unless trivial).
- Reuse `MM` theme tokens, no hardcoded colors.

## 4. Wire navigation — `composeApp/.../OnboardingNav.kt`
- Import `OnboardingRestoreKey`, `onboardingRestoreEntry`.
- Pass a new `onOpenRestore = { backStack.add(OnboardingRestoreKey) }` into `onboardingCurrencyEntry` (add that param).
- Register `onboardingRestoreEntry(onBack = { backStack.removeLastOrNull() })` in the `entryProvider` block. The screen resolves its VM via `koinViewModel()`.

## 5. Trim OnboardingCurrency restore code
- `OnboardingCurrencyStep.kt`: replace `onRestoreFromBackup = restorePicker` wiring with `onOpenRestore` navigation callback. Remove the inline `rememberBinaryFilePicker`, the `showRestoreWarning` `AlertDialog`, and the restore-related params of `CurrencyStep`/`OnboardingCurrencyScreen`. Keep the "Restore from Backup" `MmButton`, now calling `onOpenRestore`. `onboardingCurrencyEntry` gains `onOpenRestore: () -> Unit = {}`.
- `OnboardingCurrencyViewModel.kt`: remove `RestoreFileSelected`/`RestoreConfirmed`/`RestoreDismissed` handling, `pendingRestoreContent`, and the now-unused `dbBackupManager` constructor param + import.
- `OnboardingCurrencyUiState.kt`: remove `showRestoreWarning` from UiState and the 3 restore intents from the Intent sealed interface.
- `FeatureModules.kt`: drop `dbBackupManager = get(),` from `OnboardingCurrencyViewModel { }`; add registration:
```kotlin
viewModel {
    OnboardingRestoreViewModel(
        dbBackupManager = get(),
        backupCodec = get(),
        appSettings = get(),
        dispatchers = get(),
        googleAuthManager = getOrNull(),
        remoteBackupManager = getOrNull(),
        savedStateHandle = get(),
    )
}
```

## 6. Strings — `feature/onboarding/src/commonMain/composeResources/values{,-de,-es,-it}/strings.xml`
Add keys (English below; translate to de/es/it). Reuse existing `onboarding_restore_*` where present.
- `onboarding_restore_screen_title` = "Restore Backup"
- `onboarding_restore_local_button` = "Restore from file"
- `onboarding_restore_google_section` = "Google Drive"
- `onboarding_restore_google_connect` = "Connect Google account"
- `onboarding_restore_google_button` = "Restore from Google Drive"
- `onboarding_restore_passphrase_label` = "Backup password"
- `onboarding_restore_app_close_notice` = "The app will close after restoring. Reopen it to continue."
- `onboarding_restore_cancel` = "Cancel"
- `onboarding_restore_remote_loading` = "Fetching backup info…"
- `onboarding_restore_wrong_password` = "Wrong password or corrupt backup."
(plus reuse `onboarding_restore_confirm`, `onboarding_restore_warning_title/body` already in the module.)

## Verify
```
cd /Users/davidvalic/Developer/MoneyM && ./gradlew \
  :feature:onboarding:compileDebugKotlinAndroid \
  :composeApp:compileDebugKotlinAndroid
```
Fix compile errors. Orchestrator runs full Android + iOS builds after. Report changed/new files, the VM/screen/intent surface, DI + nav edits, and compile result.
