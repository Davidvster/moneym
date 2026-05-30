# Phase 5 — Immediate local backup on enable + loading overlay (task 6)

## Goal
When local auto-backup is turned ON, run a backup **now** (save to the configured
location), show a full-screen spinner overlay while running, then show the existing
"Backup saved" success message.

## Background (verified)
- `BackupRestoreViewModel` (`feature/settings/.../backuprestore/`) does **not** currently
  inject `FilePlatform`. Constructor: `dbBackupManager, appSettings, dispatchers,
  googleAuthManager?, remoteBackupManager?, sessionPassphrase?, savedStateHandle`.
- The manual `handleBackupTapped()` flow uses a **file-saver effect** (`LaunchFileSaver`)
  — that opens a save dialog, WRONG for silent auto-backup. Do **not** reuse it.
- The correct save-to-configured-location logic lives in `AutoBackupManager.start()`
  (`composeApp/.../AutoBackupManager.kt`):
  ```kotlin
  val bytes = withContext(dispatchers.io) { dbBackupManager.export() }
  val dirUri = appSettings.getString(PrefKeys.AUTO_BACKUP_DIR_URI)
  val path = if (dirUri != null && dirUri != "default")
      filePlatform.saveFileToDirBinary(dirUri, "moneym-backup.zip", bytes)
  else
      filePlatform.saveFileLocallyBinary("moneym-backup.zip", bytes)
  if (path != null) recordBackup(path)   // sets LAST_BACKUP_TIME_MS + LAST_BACKUP_PATH
  ```
- `FilePlatform` (`core/platform/.../FilePlatform.kt`) is an `expect class` with
  `suspend fun saveFileToDirBinary(dirUri, name, bytes): String?` and
  `suspend fun saveFileLocallyBinary(name, bytes): String?`.
- **Enable trigger points** (both end at the same place):
  - Android: `handleAutoBackupToggled(true)` with no dir → emits `LaunchFolderPicker`;
    after the user picks → `handleAutoBackupLocationSelected(uri)` (real SAF uri).
  - iOS: `rememberFolderPicker` (FileSaver.ios.kt:42) immediately returns `"default"`,
    so the same `handleAutoBackupLocationSelected("default")` fires.
  - Re-enable when a dir is already stored: `handleAutoBackupToggled(true)` hits the
    `hasDirUri` branch (sets enabled, no picker).
  So the two places that must trigger an immediate backup are:
  **`handleAutoBackupLocationSelected` (uri != null)** and the
  **`hasDirUri` branch of `handleAutoBackupToggled`**.
- State already has `isLoading: Boolean` and `showBackupSuccess: Boolean`.
  `handleBackupSaveCompleted` already flips `isLoading=false, showBackupSuccess=true`.
- The Screen (`BackupRestoreScreen.kt`) collects `state` via
  `collectAsStateWithLifecycle()`. It has inline `CircularProgressIndicator`s
  (lines ~357, ~500) for *remote* sub-states — those are NOT a full-screen overlay.

## Steps

### 1. New `core/ui` composable — `MmLoadingOverlay.kt`
- New file `core/ui/.../MmLoadingOverlay.kt`:
  ```kotlin
  @Composable
  fun MmLoadingOverlay(visible: Boolean, modifier: Modifier = Modifier) {
      if (!visible) return
      Box(
          modifier = modifier.fillMaxSize()
              .background(MM.colors.??.copy(alpha = 0.5f))   // scrim
              .clickable(enabled = false) {},                 // swallow taps
          contentAlignment = Alignment.Center,
      ) { CircularProgressIndicator(color = MM.colors.??) }
  }
  ```
  - Use the project's design-system accessor (`MM.colors`) — check `MmCard.kt` /
    `MmButton.kt` in core:ui for the exact scrim/overlay colour token already in use
    (e.g. a `scrim`, `overlay`, or `bg`/`onBg` token). Pick the existing token; do **not**
    invent a new colour or hardcode hex.
  - Block input under the scrim (a no-op `clickable` or `pointerInput {}`), so the user
    can't tap through while backing up.
  - Imports: `androidx.compose.foundation.background`, `.layout.Box`,
    `.layout.fillMaxSize`, `androidx.compose.material3.CircularProgressIndicator`,
    `androidx.compose.ui.Alignment`, `androidx.compose.ui.Modifier`,
    `com.dv.moneym.core.designsystem.MM`, and the input-blocker import.

### 2. Inject `FilePlatform` into the VM
- `BackupRestoreViewModel`: add `private val filePlatform: FilePlatform` constructor param
  (place it before `savedStateHandle`). Import `com.dv.moneym.platform.FilePlatform`.
- `composeApp/.../di/FeatureModules.kt` (the `BackupRestoreViewModel(...)` block, ~line
  221): add `filePlatform = get(),`. (Confirm `FilePlatform` is already provided in a Koin
  module — `AutoBackupManager` gets it via `get()`, so it is. Just add the arg.)

### 3. Immediate-backup helper in the VM
- Add a private fun:
  ```kotlin
  private fun runImmediateLocalBackup() {
      _base.update { it.copy(isLoading = true, showBackupSuccess = false) }
      viewModelScope.launch {
          val bytes = withContext(dispatchers.io) { dbBackupManager.export() }
          val dirUri = appSettings.getString(PrefKeys.AUTO_BACKUP_DIR_URI)
          val path = withContext(dispatchers.io) {
              if (dirUri != null && dirUri != "default")
                  filePlatform.saveFileToDirBinary(dirUri, "moneym-backup.zip", bytes)
              else
                  filePlatform.saveFileLocallyBinary("moneym-backup.zip", bytes)
          }
          handleBackupSaveCompleted(path)   // reuse existing: records path + flips state
      }
  }
  ```
  - Reuse `handleBackupSaveCompleted(path)` so LAST_BACKUP_TIME_MS / LAST_BACKUP_PATH
    persistence and the `isLoading=false, showBackupSuccess=true` transition stay in one
    place. (It already does exactly that.)
- Wire it into the two enable paths:
  - `handleAutoBackupLocationSelected(uri)`: after `putString(DIR_URI)` +
    `putBoolean(AUTO_BACKUP_ENABLED, true)` + state update → call
    `runImmediateLocalBackup()`.
  - `handleAutoBackupToggled` `hasDirUri == true` branch: after enabling → call
    `runImmediateLocalBackup()`.
  - Do **NOT** trigger on disable.

### 4. Render the overlay in the Screen
- `BackupRestoreScreen.kt`: at the end of the screen's root container (so it draws on top
  of everything), render `MmLoadingOverlay(visible = state.isLoading)`. Import it from
  core:ui. Confirm the root is a `Box` (or wrap so the overlay overlays the content);
  if the root is a `Column`/`Scaffold`, wrap the whole thing in a `Box { … ;
  MmLoadingOverlay(...) }`.
- The existing "Backup saved" message already keys off `state.showBackupSuccess` — no
  change there.

## Conventions
- No new strings expected (overlay has no text; success message already exists). If you
  add any user-visible text, add it ×4 langs.
- VM stays single-`onIntent`; helper is private. Import classes (CLAUDE.md).
- `FilePlatform` is `expect class` — fine to inject into a VM constructed via Koin
  (it's a platform single, not constructed from commonTest). No Fake needed: this VM
  isn't unit-tested (depends on `DbBackupManager`/`DbPlatform` — see CLAUDE.md
  testability note).

## Build / verify
- `./gradlew :core:ui:compileDebugKotlinAndroid`
- `./gradlew :feature:settings:compileDebugKotlinAndroid`
- `./gradlew :composeApp:compileDebugKotlinAndroid`
- Report: files changed, which `MM.colors` token you used for the scrim, where you put
  the overlay in the Screen tree, and build result.
