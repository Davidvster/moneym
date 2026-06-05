# Remote (Google Drive) Encrypted Backup

This document explains how the remote-backup feature is wired, how to enable it for a local build, and how the safety/encryption properties work.

> **Note:** the periodic `.db-zip` snapshot history described here is no longer a separate
> user-facing toggle. It now runs as the safety-net half of the unified **Cloud sync** feature
> (one toggle enables both cross-device sync and snapshot history), and its lifecycle is driven by
> `CROSS_DEVICE_SYNC_ENABLED` via `SyncEngine`, not by the local file-backup toggle. See
> [SYNC.md](SYNC.md) for the unified model, persistent passphrase, and password-conflict handling.

## TL;DR

- **No `google-services.json`. No Firebase. No Google Services Gradle plugin.**
- The feature is **off by default** and **completely hidden** when no OAuth client ID is configured at build time. The rest of the app builds, runs, and ships unchanged.
- When enabled, backups are encrypted on-device (AES-256-GCM, key derived from a user passphrase via PBKDF2-HMAC-SHA256 with 600 000 iterations) and uploaded to the app's hidden Google Drive folder (`appDataFolder`).

## Module map

| Module | Role |
|---|---|
| `core/security` | `BackupCrypto` interface + `DefaultBackupCrypto` impl + `EncryptedBackup` envelope DTO |
| `core/oauth` | `GoogleAuthManager`, `GoogleOAuthConfig`, PKCE, token client, platform auth launchers and secure token stores |
| `data/remotebackup` | `RemoteBackupProvider` + `GoogleDriveBackupClient` (Ktor), `RemoteBackupManager` orchestrator, `SessionPassphrase` |
| `composeApp/di/RemoteBackupModule[.android.kt,.ios.kt]` | Koin wiring, platform `BuildConfig`/`Info.plist` reads |
| `feature/settings/.../backuprestore` | UI section, gated on `googleAuthManager.isConfigured` |

## Configuring OAuth for a build

1. **Provision an OAuth 2.0 client** in Google Cloud Console:
   - Application type: **Android** (or iOS for the iOS target).
   - Authorized redirect URI: `com.dv.moneym://oauth`.
   - Scope: `https://www.googleapis.com/auth/drive.appdata`.
2. **Android**: add the client ID to `gradle.properties` (or pass via the `GOOGLE_OAUTH_CLIENT_ID` environment variable):
   ```
   googleOAuthClientId=YOUR_CLIENT_ID.apps.googleusercontent.com
   ```
   The `composeApp/build.gradle.kts` Android block converts this into a `BuildConfig.GOOGLE_OAUTH_CLIENT_ID` string field. If the property is absent, the field is `null` and the feature stays hidden.
3. **iOS**: add a `GoogleOAuthClientId` string key to `iosApp/iosApp/Info.plist`. Also add a `CFBundleURLTypes` entry registering the `com.dv.moneym` scheme so `ASWebAuthenticationSession` can deliver the callback.

## How the feature stays hidden

```
GoogleOAuthConfig.isConfigured = clientId != null && clientId.isNotBlank()
                       ↓
DefaultGoogleAuthManager.isConfigured = config.isConfigured
                       ↓
BackupRestoreUiState.remoteAvailable = googleAuthManager.isConfigured
                       ↓
BackupRestoreScreen renders no remote section when remoteAvailable == false
```

Koin always binds `GoogleAuthManager`; when unconfigured it refuses every operation (`signIn()` throws `NotConfigured`, `accessToken()` returns `null`). The UI never reaches those code paths because `remoteAvailable` is false.

## Encryption

1. User picks a passphrase in the UI (`SessionPassphrase` holds it in RAM only).
2. Local backup bytes come from the existing `DbBackupManager.export()` (a ZIP of all four Room SQLite databases).
3. `DefaultBackupCrypto.encrypt(...)`:
   - PBKDF2-HMAC-SHA256, 600 000 iterations, 16-byte random salt → 256-bit key.
   - AES-256-GCM, 12-byte random IV, 128-bit auth tag.
   - Output: an `EncryptedBackup` envelope (`version`, `schema`, `createdAt`, `appVersion`, `kdf{name,iter,saltB64}`, `cipher{name,ivB64,ctB64,tagBits}`) serialized as JSON.
4. The JSON-encoded envelope is uploaded to Drive `appDataFolder` as a single file named `moneym-backup.bin`. Drive `appProperties` carry `schema`/`appVersion`/`createdAt` for cheap listing.

Underlying crypto library: `dev.whyoleg.cryptography` (JDK provider on Android, Apple provider on iOS).

## Authentication

OAuth 2.0 Authorization Code + PKCE (S256):

- **Android**: Chrome Custom Tabs opens the consent page; a transparent `OAuthRedirectActivity` (in `core/oauth`) receives the `com.dv.moneym://oauth` deep link and forwards the result to an in-process `CompletableDeferred`.
- **iOS**: `ASWebAuthenticationSession` with the same custom URL scheme.
- Tokens are exchanged via the Google `oauth2.googleapis.com/token` endpoint using Ktor.
- Refresh tokens live in the platform secure store:
  - Android: `EncryptedSharedPreferences` (AES-256/GCM master key).
  - iOS: `NSUserDefaults` (matches the existing `core/security/IosSecureStore` pattern; **TODO**: upgrade to Keychain).

## Auto-upload cadence

The existing `AutoBackupManager` already debounces 3 s on the combined `categories ⊕ accounts ⊕ transactions` observe flow. After every local save it now calls `remoteBackupManager?.enqueueUpload()`, which pulses a separate 5 s debounce inside `RemoteBackupManager`. `AppLifecycleObserver` flushes pending uploads on `ON_PAUSE` so the latest state reaches Drive before the app suspends.

A second `enqueueUpload()` while an upload is in flight re-emits exactly one pulse — there is at most one in-flight upload and at most one pending pulse.

## Restore

`RemoteBackupManager.restoreLatest(passphrase)`:

1. `provider.latest()` → newest file in `appDataFolder`.
2. `provider.download(ref)` → ciphertext bytes.
3. `BackupEnvelopeJson.decodeBytes(...)` → envelope DTO. Newer envelope `version` than this app supports → `UnsupportedEnvelope`.
4. `crypto.decrypt(envelope, passphrase)` → plain ZIP bytes (wrong passphrase → `WrongPassphrase`).
5. `DbBackupManager.restore(plain)` → same restore path the local file picker uses; closes Room handles, replaces the four `.db` files (and clears WAL/SHM), then `terminateApp()` so the next launch sees the restored data.

## Failure handling

- Network/Drive failures never throw out of the auto-backup loop; they surface as `RemoteBackupRuntimeState.Error(message)` in the UI.
- Refresh-token expiry transitions the auth state back to `SignedOut` and clears the local secure store.
- A failed decrypt leaves the local DB untouched.

## Testing

| Test | What it covers |
|---|---|
| `:core:security:BackupCryptoRoundTripTest` | Encrypt → decrypt round trip, wrong-passphrase failure (Android JVM, real crypto). |
| `:core:security:BackupEnvelopeSerializationTest` | JSON envelope round trip. |
| `:core:oauth:PkceTest` | URL-safe verifier/challenge, uniqueness. |
| `:core:oauth:AuthorizationUrlBuilderTest` | Required Google params present, `NotConfigured` on missing client ID. |
| `:core:oauth:OAuthTokenClientTest` | Code exchange, refresh, missing-refresh-token, HTTP error. |
| `:data:remotebackup:GoogleDriveBackupClientTest` | Multipart upload, `appDataFolder` listing, `alt=media` download, auth/error paths. |
| `:data:remotebackup:SessionPassphraseTest` | Defensive copy semantics, zeroize on clear. |

Run them all with:

```bash
./gradlew :core:security:testDebugUnitTest \
          :core:oauth:testDebugUnitTest \
          :data:remotebackup:testDebugUnitTest
```

## Verification of the "hidden when absent" contract

```bash
# Without OAuth config:
unset GOOGLE_OAUTH_CLIENT_ID
./gradlew :composeApp:assembleDebug
adb install -r composeApp/build/outputs/apk/debug/composeApp-debug.apk
# Open Backup & Restore — no Google Drive section should be visible.

# With OAuth config:
export GOOGLE_OAUTH_CLIENT_ID=YOUR_CLIENT_ID.apps.googleusercontent.com
./gradlew :composeApp:assembleDebug
# Open Backup & Restore — the Google Drive section appears.
```

## Known follow-ups (out of scope for the current phases)

- **iOS Keychain** — `IosSecureTokenStore` currently uses `NSUserDefaults` (matches `IosSecureStore` in `core/security`). Upgrade to true Keychain (`SecItem*`) once cinterop helpers are in place.
- **Multi-snapshot retention** — keep N backups in Drive, choose which to restore.
- **Drive quota probe** before first upload.
- **Other providers** — `RemoteBackupProvider` is intentionally generic; only Google Drive ships today.
