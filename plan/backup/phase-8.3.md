# Phase 8.3 — Google OAuth 2.0 + PKCE manager

Adds `core/oauth` KMP module that performs the OAuth Authorization Code + PKCE flow against Google with no Google Services / Firebase / Play-services dependency.

## Files

### commonMain
- `GoogleAuthManager.kt` — interface, `AuthState`, `GoogleAuthError` sealed hierarchy.
- `GoogleOAuthConfig.kt` — `clientId`/`redirectUri`/`scopes`. `isConfigured` drives the feature flag.
- `Pkce.kt` — PBKDF2-quality random verifier (64 bytes) + SHA-256 challenge using cryptography-kotlin.
- `OAuthEndpoints.kt` — Google auth/token/revoke URLs.
- `AuthorizationLauncher.kt` — platform-agnostic interface (`launch(request, redirectUri)`); `AuthorizationUrlBuilder`.
- `OAuthTokenClient.kt` — Ktor form-post to `oauth2.googleapis.com/token`. Exchanges `code` → `StoredTokens`, refreshes, revokes. Extracts email from `id_token` payload without verifying the signature (acceptable — Google is the only place this id_token comes from, and we only show it back to the user).
- `TokenStorage.kt` — `StoredTokens` DTO + `SecureTokenStore` interface.
- `DefaultGoogleAuthManager.kt` — orchestrator. Single in-flight refresh via `Mutex`. Wipes tokens + flips state on refresh failure.

### androidMain
- `AndroidAuthorizationLauncher.kt` — Custom Tabs `launchUrl(...)`, bridges deep-link callback into a `CompletableDeferred`.
- `OAuthRedirectActivity.kt` — translucent activity that catches `com.dv.moneym://oauth?...` and forwards to the launcher.
- `AndroidSecureTokenStore.kt` — `EncryptedSharedPreferences` (AES-256/GCM master key).
- `AndroidManifest.xml` — declares `OAuthRedirectActivity` with the `<intent-filter>` for the redirect scheme and `INTERNET` permission.

### iosMain
- `IosAuthorizationLauncher.kt` — `ASWebAuthenticationSession` with a tiny `PresentationProvider : NSObject` (instance, not object — Kotlin/Native cannot lower an `object` extending `NSObject`).
- `IosSecureTokenStore.kt` — currently `NSUserDefaults` to match the existing `IosSecureStore` pattern in `core/security`. **Follow-up note**: upgrade to true Keychain via `SecItem*` in a later patch (a clean Keychain wrapper needs more cinterop bridging than fits this phase).

## Tests

- `PkceTest` — verifier/challenge format + uniqueness.
- `AuthorizationUrlBuilderTest` — all required Google params present; absent client ID rejected.
- `OAuthTokenClientTest` — code exchange success, missing-refresh-token failure, HTTP error mapping, refresh updates fields.

## Build safety

- `GoogleOAuthConfig(clientId = null, ...)` is the **valid** default state. `DefaultGoogleAuthManager.isConfigured == false`. `signIn()` throws `NotConfigured`; `accessToken()` returns null. The UI layer (Phase 8.5) reads `isConfigured` to hide the section entirely.
- No file in the repo references a literal client ID; Phase 8.6 wires it via `BuildConfig` from `gradle.properties`.

## Verification

```
./gradlew :core:oauth:compileDebugKotlinAndroid \
          :core:oauth:linkDebugFrameworkIosSimulatorArm64 \
          :core:oauth:testDebugUnitTest
```

All green.
