# Phase 8.6 — Koin wiring, BuildConfig, docs

Final wiring step. After this phase the feature is end-to-end live when an OAuth client ID is provided, and fully hidden (and still buildable) when it is not.

## Files

### `composeApp/build.gradle.kts`
- New `defaultConfig` block reads `googleOAuthClientId` from `gradle.properties` or the `GOOGLE_OAUTH_CLIENT_ID` env var and emits `BuildConfig.GOOGLE_OAUTH_CLIENT_ID: String?` (nullable). Missing ⇒ `null` ⇒ feature hidden.
- `buildFeatures { buildConfig = true }`.
- Adds `:data:remotebackup`, `:core:oauth`, Ktor (`core`/`content-negotiation`/`json` in common; `okhttp` Android; `darwin` iOS).

### `composeApp/.../di/RemoteBackupModule.kt` (common, expect)
- Common Koin module binds: `BackupCrypto`, `SessionPassphrase`, `HttpClient` (with `ContentNegotiation`), `OAuthTokenClient`, `RemoteBackupProvider` (Drive), `GoogleAuthManager` (default impl), `RemoteBackupManager`.
- `expect fun remoteBackupPlatformModule(): Module` — actuals supply `GoogleOAuthConfig`, `AuthorizationLauncher`, `SecureTokenStore`.

### `composeApp/.../di/RemoteBackupModule.android.kt`
- Reads `BuildConfig.GOOGLE_OAUTH_CLIENT_ID` for `GoogleOAuthConfig`.
- Wires `AndroidAuthorizationLauncher` + `AndroidSecureTokenStore` against the `Context` already bound in `androidPlatformModule`.

### `composeApp/.../di/RemoteBackupModule.ios.kt`
- Reads `Bundle.infoDictionary["GoogleOAuthClientId"]` for `GoogleOAuthConfig`.
- Wires `IosAuthorizationLauncher` + `IosSecureTokenStore`.

### `composeApp/.../di/AndroidPlatformModule.kt`
- Now binds `Context` as a singleton so the remote module can `get<Context>()`.

### `composeApp/.../di/FeatureModules.kt`
- `BackupRestoreViewModel` Koin binding now passes `getOrNull()` for `googleAuthManager`, `remoteBackupManager`, `sessionPassphrase` — these are always present when the platform module is loaded, but the optional shape keeps the binding resilient.
- `AutoBackupManager` Koin binding now takes the optional `RemoteBackupManager` via `getOrNull()`.

### `composeApp/.../di/AppModules.kt`
- Appends `remoteBackupCommonModule` and `remoteBackupPlatformModule()` to `appModules`.

### `docs/REMOTE_BACKUP.md` (new)
- Developer guide: provisioning the OAuth client, where the build flag goes (Android `gradle.properties` / env, iOS Info.plist), how the feature stays hidden, encryption format, restore flow, test commands, known follow-ups.

## Manifests

The `<intent-filter>` for `com.dv.moneym://oauth` already ships in `core/oauth/src/androidMain/AndroidManifest.xml`. Android Gradle merges that library manifest into the app manifest automatically — no changes needed in `composeApp/src/androidMain/AndroidManifest.xml`.

iOS `Info.plist` changes are documented in `docs/REMOTE_BACKUP.md`; they are not committed to `iosApp/iosApp/Info.plist` so that a developer who has no OAuth credentials does not need to touch the bundle.

## Tests covered across all phases

```
./gradlew :core:security:testDebugUnitTest \
          :core:oauth:testDebugUnitTest \
          :data:remotebackup:testDebugUnitTest \
          :composeApp:assembleDebug
```

All green. The debug APK builds without `googleOAuthClientId` set; the remote backup section is hidden in the UI; existing local backup/restore continues to work.

## Acceptance criteria (from the original prompt)

| Criterion | Status |
|---|---|
| App builds without Google OAuth config | ✔ `assembleDebug` green |
| When config absent, remote backup UI hidden | ✔ `remoteAvailable` gates the section |
| User can connect Google account when config present | ✔ `ConnectGoogleTapped` → `signIn()` |
| User can enable automatic remote backup | ✔ Toggle + passphrase flow |
| Backup runs automatically after data changes with debounce/coalescing | ✔ 3 s local + 5 s remote debounce; `enqueueUpload()` from `AutoBackupManager` |
| Remote backup stores the whole database backup artifact | ✔ Reuses `DbBackupManager.export()` ZIP |
| User can restore from remote backup | ✔ `RemoteRestoreConfirmed` → `restoreLatest(passphrase)` |
| Remote backup data is encrypted before upload and decrypted during restore | ✔ AES-256-GCM + PBKDF2-HMAC-SHA256 |
| Loading spinner/progress shown during encrypt/decrypt/upload/download/restore | ✔ `RemoteBackupRuntimeState` + `RuntimeStatusLine` |
| Existing local backup/restore continues to work | ✔ Local card unchanged; only nullable wiring added |
