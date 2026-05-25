# Remote (Google Drive) Encrypted Backup — Plan

> Mirror of the approved plan from `/Users/davidvalic/.claude/plans/ultra-idempotent-creek.md`.
> Per-phase notes live in this directory as `phase-8.N.md` and are written when each phase begins.

## Goal

Add an **optional** encrypted remote backup to the user's Google Drive (app-folder hidden storage) using native OAuth 2.0 + PKCE — no `google-services.json`, no Firebase, no Google Services Gradle plugin. The app continues to build and run when OAuth credentials are absent; in that state the remote-backup UI is fully hidden.

## Confirmed design choices

| Concern | Choice |
|---|---|
| Encryption key | User passphrase → PBKDF2-HMAC-SHA256 (600 000 iter) → AES-256-GCM. Loss of passphrase = unrecoverable backup. |
| OAuth client ID delivery | `BuildConfig` field sourced from `gradle.properties` / env. Android `BuildConfig.GOOGLE_OAUTH_CLIENT_ID`, iOS `Bundle.infoDictionary["GoogleOAuthClientId"]`. Missing ⇒ feature hidden. |
| Auto-upload cadence | Extend the existing `AutoBackupManager` combined-flow debounce (3 s) — also enqueue a remote upload. Add an `ON_PAUSE` flush via `AppLifecycleObserver`. |
| iOS OAuth redirect | `ASWebAuthenticationSession` with custom URL scheme `com.dv.moneym://oauth`. |
| Android OAuth redirect | Chrome Custom Tabs + `OAuthRedirectActivity` deep-link `com.dv.moneym://oauth`. |
| Storage location | Google Drive `appDataFolder` space (hidden, app-scoped). |
| HTTP client | Ktor (`okhttp` engine on Android, `darwin` engine on iOS). |
| Token storage | `SecureTokenStore` expect/actual — Android `EncryptedSharedPreferences`, iOS Keychain. |
| Restore pipeline | Reuses `BackupRestorer` unchanged. |

## Phases

Each phase is independently buildable and ends with a single commit.

| Phase | Title | Commit message |
|---|---|---|
| 8.1 | Backup crypto envelope (AES-256-GCM + PBKDF2) | `Phase 8.1: backup crypto envelope (AES-256-GCM + PBKDF2)` |
| 8.2 | Google Drive REST client (`appDataFolder`) | `Phase 8.2: Google Drive remote backup client (appDataFolder)` |
| 8.3 | Google OAuth 2.0 + PKCE manager | `Phase 8.3: Google OAuth 2.0 + PKCE (Android Custom Tabs, iOS ASWebAuthenticationSession)` |
| 8.4 | RemoteBackupManager + auto-upload wiring | `Phase 8.4: remote backup orchestrator + auto-upload` |
| 8.5 | Backup & Restore UI for remote | `Phase 8.5: remote backup UI in Backup & Restore` |
| 8.6 | Koin wiring + BuildConfig + docs + tests | `Phase 8.6: wire remote backup + docs + tests` |

## Reused existing code

- `data/backup/.../DbBackupManager.kt` — `export()` produces the ZIP artifact uploaded remotely.
- `data/backup/.../BackupRestorer.kt` — consumes the decrypted bytes during restore.
- `composeApp/.../AutoBackupManager.kt` — debounce/coalesce, extended to also enqueue remote upload.
- `composeApp/.../AppLifecycleObserver.kt` — extended with `ON_PAUSE` `flushNow()`.
- `core/datastore/AppSettings` + `PrefKeys` — new keys appended.
- `BackupRestoreScreen` + `BackupRestoreViewModel` Intent/UiState/Effects — extended in place.
- `MmSwitch`, `MmCard`, `MmButton`, restore-warning dialog — UI primitives reused.

## Acceptance criteria

See the parent plan for the full list. Key points:

- `./gradlew :composeApp:assembleDebug` succeeds with and without `googleOAuthClientId` set.
- iOS Xcode build succeeds with and without `GoogleOAuthClientId` set in Info.plist.
- When config absent, the remote-backup section of `BackupRestoreScreen` is **not rendered**.
- When config present, the user can connect a Google account, enable automatic remote backup with a passphrase, and restore from a backup file in their Drive `appDataFolder`.
- Failed uploads never block local writes; they surface as a UI status.

## Out of scope

- Other providers (Dropbox / iCloud / OneDrive); interface kept open but not implemented.
- Multi-snapshot retention beyond a single "latest" file (UX suggestion #6 deferred).
- Differential / incremental upload.
- Server-side schema migration of older backups.
