# Phase 8.2 — Google Drive REST client

Adds `data:remotebackup` module with a provider abstraction and a Ktor-based Google Drive client that uses the `appDataFolder` space (hidden, app-scoped storage).

## Files

- `data/remotebackup/build.gradle.kts` — new KMP module, Ktor (`okhttp` engine on Android, `darwin` on iOS) + `kermit` + `core/security`.
- `data/remotebackup/.../RemoteBackupProvider.kt` — interface (`upload`/`latest`/`list`/`download`/`delete`), `RemoteFileRef`, `RemoteBackupError`.
- `data/remotebackup/.../google/GoogleDriveDtos.kt` — kotlinx-serialization DTOs for `files`, `appProperties`, `storageQuota`.
- `data/remotebackup/.../google/GoogleDriveBackupClient.kt` — Ktor client. Multipart-related upload (metadata + raw bytes) to `/upload/drive/v3/files?uploadType=multipart`, list/order by modifiedTime desc, `alt=media` download, bearer-token via `suspend () -> String?` supplier (Phase 8.3 fills it).
- `commonTest/.../GoogleDriveBackupClientTest.kt` — `MockEngine` round-trip for upload/list/download + auth/error paths.

## Dependencies added

- `io.ktor:ktor-client-core` / `ktor-client-content-negotiation` / `ktor-serialization-kotlinx-json` (common)
- `io.ktor:ktor-client-okhttp` (android)
- `io.ktor:ktor-client-darwin` (ios)
- `io.ktor:ktor-client-mock` (commonTest)

Pinned in `gradle/libs.versions.toml` under `ktor = "3.0.3"`.

## Notes

- Token provider is a plain `suspend () -> String?` — keeps this module ignorant of OAuth. Phase 8.3 wires `GoogleAuthManager::accessToken` here.
- All Drive calls scoped to `spaces=appDataFolder` so backups stay hidden from the user's main Drive UI.
- App-properties map (`schema`, `appVersion`, `createdAt`) is propagated through `upload(...)` so restore can choose the right file without downloading the envelope.
- `RemoteBackupError.NotAuthenticated` is what `RemoteBackupManager` translates into a "Sign in to Google" UI state in Phase 8.4.

## Verification

```
./gradlew :data:remotebackup:compileDebugKotlinAndroid \
          :data:remotebackup:linkDebugFrameworkIosSimulatorArm64 \
          :data:remotebackup:testDebugUnitTest
```

All green.
