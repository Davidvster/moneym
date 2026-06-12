# Phase 5 — Google Drive restore hangs on iOS (investigation)

## Symptoms
- iOS: "restore from Google Drive" hangs forever (iPhone 16e simulator, signed-in Google account).
- Android: faster, but fails to find backups for some Google accounts.

## Suspects
1. `core/oauth/src/iosMain/.../IosGoogleAuthManager.kt:49-57` — `accessToken()` suspends on `GoogleSignInBridge.currentAccessToken`; if the Swift bridge never calls back (expired token, main-thread deadlock), the coroutine hangs forever.
2. No `HttpTimeout` on the common Ktor client (`GoogleDriveBackupClient`); Darwin engine hung request = infinite wait. Add timeout regardless of root cause.
3. `list(limit=1000)` + full in-memory download — slow but should finish.
4. Android per-account: appDataFolder is scoped per OAuth-client+account; backups created under a different client are invisible.

## Steps
1. Read Swift `GoogleSignInBridge` in iosApp/ — token refresh, dispatch queue.
2. Build + install on iPhone 16e simulator (xcodebuildmcp), trigger restore, capture logs.
3. Fix where it stalls; add HttpTimeout.
4. Re-verify restore completes (or errors visibly) on the simulator.
