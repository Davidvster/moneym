# Phase 8.5 — Backup & Restore UI for remote

Extends the existing `BackupRestoreScreen` / `BackupRestoreViewModel` with a Google Drive section that renders **only** when `googleAuthManager.isConfigured`. Everything else on the screen behaves exactly as before.

## Files

- `feature/settings/.../BackupRestoreViewModel.kt` — accepts three new optional collaborators (`GoogleAuthManager?`, `RemoteBackupManager?`, `SessionPassphrase?`). All three default to `null`, so the existing Koin binding in `composeApp` continues to compile until Phase 8.6 supplies them. New intents:
  - `ConnectGoogleTapped`, `DisconnectGoogleTapped`
  - `RemoteAutoBackupToggled(enabled)`
  - `PassphrasePromptOpened`, `PassphrasePromptDismissed`, `PassphraseSubmitted(value)`
  - `RemoteBackupNowTapped`
  - `RemoteRestoreTapped`, `RemoteRestoreConfirmed(passphrase)`, `RemoteRestoreDismissed`
  - New effects: `RemoteError(message)`, `RemoteSignedIn`
- `feature/settings/.../BackupRestoreScreen.kt` — adds a "Google Drive backup" `MmCard` section under the existing card; passphrase dialog; remote restore dialog; inline `RuntimeStatusLine` that maps `RemoteBackupRuntimeState` → localized message and uses `colors.danger` on error.
- Strings: 24 new keys added to `values/`, `values-de/`, `values-es/`, `values-it/`.
- `feature/settings/build.gradle.kts` — `:data:remotebackup` + `:core:oauth` deps.

## UX notes

- Toggle "Automatic remote backup" while no passphrase is set immediately surfaces the passphrase dialog; once submitted the toggle is enabled and an upload is enqueued.
- "Back up now" button bypasses the debounce window — useful before known offline periods.
- Restore-from-remote forces a passphrase re-entry (the in-memory `SessionPassphrase` may have been cleared and the user must prove they own it).
- Runtime states (`Encrypting | Uploading | Downloading | Decrypting | Restoring`) disable the auto-backup toggle and replace the chevron with a circular progress indicator; `Idle` shows the last-backup timestamp; `Error` shows the failure message in `colors.danger`.

## Build safety

When `googleAuthManager` is `null` (Phase 8.6 not yet bound or config missing), `state.remoteAvailable == false` and the entire section is skipped by the lazy column.

## Verification

```
./gradlew :feature:settings:compileDebugKotlinAndroid \
          :composeApp:assembleDebug
```

Both green. The debug APK builds with no `googleOAuthClientId` in `gradle.properties` (the remote section will be hidden at runtime once Koin wires the manager in Phase 8.6).
