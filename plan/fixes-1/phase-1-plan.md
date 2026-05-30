# Phase 1: Cloud Backup Fixes

## Context
Three bugs in the remote backup UI/logic:
1. "Tap to retry" text is not clickable — missing `.clickable` modifier
2. "Back up now" silently no-ops when auto backup is disabled — `RemoteBackupManager.flushNow()` checks `isEnabled()` (the auto-backup flag), so a manual one-time backup does nothing unless auto backup is on
3. Need to verify that enabling auto backup triggers immediate backup (code path exists but should be confirmed)

## Critical Files
- `feature/settings/src/commonMain/kotlin/com/dv/moneym/feature/settings/overview/backuprestore/BackupRestoreScreen.kt`
- `data/remotebackup/src/commonMain/kotlin/com/dv/moneym/data/remotebackup/RemoteBackupManager.kt`
- `feature/settings/src/commonMain/kotlin/com/dv/moneym/feature/settings/overview/backuprestore/BackupRestoreViewModel.kt`

## Changes

### Fix 1: Retry button clickable
`BackupRestoreScreen.kt` — `RuntimeStatusLine` composable (lines ~428–434):
```kotlin
// Current — Text has NO clickable:
if (runtime is RemoteBackupRuntimeState.Error) {
    Text(
        text = stringResource(Res.string.settings_remote_status_retry),
        style = type.caption.copy(color = colors.accent),
        modifier = Modifier.padding(top = space.padding_0_5x),
    )
}

// Fix — add .clickable { onRetry() }:
if (runtime is RemoteBackupRuntimeState.Error) {
    Text(
        text = stringResource(Res.string.settings_remote_status_retry),
        style = type.caption.copy(color = colors.accent),
        modifier = Modifier
            .padding(top = space.padding_0_5x)
            .clickable { onRetry() },
    )
}
```

### Fix 2: flushNow() ignores isEnabled check
`RemoteBackupManager.kt` — `flushNow()` (line ~68):
```kotlin
// Current: returns early if auto backup disabled
suspend fun flushNow(): Result<Unit> = runCatching {
    if (!isEnabled()) return@runCatching
    if (encryptEnabled() && !sessionPassphrase.isSet.value) return@runCatching
    runUpload()
}

// Fix: remove isEnabled check so manual one-time backup always works
suspend fun flushNow(): Result<Unit> = runCatching {
    if (encryptEnabled() && !sessionPassphrase.isSet.value) return@runCatching
    runUpload()
}
```
Note: `enqueueUpload()` still checks `isEnabled()` — that is correct behavior for auto-triggered uploads.

### Fix 3: Enable auto backup → immediate backup
`BackupRestoreViewModel.kt` — `handleRemoteAutoToggled(true)` already calls `flushRemoteNow()` after setting the flag. With Fix 2 applied, this will now always upload. No additional change needed unless passphrase flow blocks it — which is correct behavior.

## Verification
1. Put remote backup in Error state, confirm retry tap triggers a new upload attempt
2. Disable auto backup, press "Back up now" — should upload
3. Enable auto backup toggle — should immediately start upload (runtime shows Encrypting/Uploading)
