# Phase 2 — `core/platform`: app files directory for model storage

**Status:** Not started
**Depends on:** none

## Goal
Expose a writable app-files directory for large model blobs.

## Tasks
1. `core/platform/src/commonMain/.../DbPlatform.kt` (expect class): add `val appFilesDirectory: String`.
2. `core/platform/src/androidMain/.../DbPlatform.android.kt`: `actual val appFilesDirectory get() = context.filesDir.absolutePath`.
3. `core/platform/src/iosMain/.../DbPlatform.ios.kt`: `actual val appFilesDirectory` resolved via
   `NSFileManager.URLForDirectory(NSApplicationSupportDirectory, ...)?.path` (reuse the existing
   helper already used for `dbDirectory`).

Models will live under `$appFilesDirectory/models/<fileName>` (subdir created by Phase 3 downloader).

## Tests
No unit test (platform-only expect/actual, not constructible from commonTest). Covered indirectly by
Phase 3 with a fake path.

## Verify
`./gradlew :core:platform:compileDebugKotlinAndroid :core:platform:compileKotlinIosSimulatorArm64`

## Commit
`feat(core-platform): add appFilesDirectory to DbPlatform`
