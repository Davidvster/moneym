# Phase 3 — Google Drive restore error surfacing (issue #3)

Problem: Drive restore dialog opens, shows loading, user enters password, then the dialog closes abruptly with no error. Causes:
- `BackupRestoreViewModel.handleRemoteRestoreConfirmed` hides the dialog + raises full-screen `isLoading` on confirm, then only handles `.onFailure` by emitting `BackupRestoreEffect.RemoteError`, which `BackupRestoreScreen` discards (`is BackupRestoreEffect.RemoteError -> Unit`). Errors are invisible.
- `RemoteBackupManager.restoreLatest` does `if (!uploadLock.tryLock()) return@runCatching` — when an auto-upload holds the lock it returns *success* without restoring, leaving `isLoading=true` forever (no `.onSuccess`).
- The restore `catch` never logs.

## Files & changes

### `data/remotebackup/src/commonMain/kotlin/com/dv/moneym/data/remotebackup/RemoteBackupManager.kt`
- In `restoreLatest`, replace `if (!uploadLock.tryLock()) return@runCatching` + manual `finally { if (uploadLock.isLocked) uploadLock.unlock() }` with `uploadLock.withLock { ... }` (already imports `withLock`) so a busy lock is awaited instead of silently no-op'd. Keep the inner `try/catch` that sets `_runtime`.
- In that inner `catch (t: Throwable)`, add `logger.e(t) { "Remote restore failed" }` before `throw t` (parity with `runUpload`).

### `feature/settings/src/commonMain/kotlin/com/dv/moneym/feature/settings/overview/backuprestore/BackupRestoreViewModel.kt`
- Add to `BackupRestoreUiState`: `val remoteRestoreInProgress: Boolean = false` and `val remoteRestoreError: String? = null`. (UiState is `@Serializable`; `String?`/`Boolean` are fine. No Fake references it.)
- Rewrite `handleRemoteRestoreConfirmed`:
  - On confirm: keep `showRemoteRestoreDialog = true`; set `remoteRestoreInProgress = true`, `remoteRestoreError = null`; do NOT set `isLoading = true`; do NOT hide the dialog.
  - `manager.restoreLatest(passphrase)`:
    - `.onFailure { t -> _base.update { it.copy(remoteRestoreInProgress = false, remoteRestoreError = t.message ?: "Restore failed") } }` (keep dialog open so the user can retry the passphrase).
    - `.onSuccess { _base.update { it.copy(remoteRestoreInProgress = false) } }` — real success terminates the app via `DbBackupManager.restore → terminateApp()`; this reset only matters for the busy/no-op path.
  - Still `passphrase.fill(' ')` after.
- Ensure dismissing the dialog (`RemoteRestoreDismissed`) clears `remoteRestoreError` and `remoteRestoreInProgress`.

### `feature/settings/src/commonMain/kotlin/com/dv/moneym/feature/settings/overview/backuprestore/BackupRestoreScreen.kt`
- At the `RemoteRestoreDialog(...)` call site, pass the new state: an `inProgress = state.remoteRestoreInProgress` and `errorMessage = state.remoteRestoreError`.
- In `RemoteRestoreDialog`:
  - Add params `inProgress: Boolean` and `errorMessage: String?`.
  - `confirmEnabled = input.isNotEmpty() && !loading && !tooNew && !inProgress`.
  - While `inProgress`, show a loading row (reuse the existing `CircularProgressIndicator` + label pattern already in the dialog) and disable the password field edits (`onValueChange = { if (!loading && !tooNew && !inProgress) input = it }`).
  - If `errorMessage != null`, render it (`color = MM.colors.danger`, `style = MM.type.caption`) near the field.
- The `RemoteError` effect (`is BackupRestoreEffect.RemoteError -> Unit`) can stay a no-op for restore now that the error renders in-dialog; do not remove the effect type.

## Verify
```
./gradlew :feature:settings:testDebugUnitTest :data:remotebackup:compileDebugKotlinAndroid
```
Existing tests must pass; modules compile. Report any test changes needed.
