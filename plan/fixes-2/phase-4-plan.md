# Phase 4 Plan: Final Android And iOS Verification

## Goal

Prove the full app builds on Android and iOS after all fixes.

## Verification

- Run `./gradlew :androidApp:assembleDebug`.
- Run `./gradlew :shared:linkDebugFrameworkIosArm64 :shared:linkDebugFrameworkIosSimulatorArm64`.
- If a verification command fails because of configuration-cache serialization before executing the intended work, rerun with `--no-configuration-cache` and record both outcomes in `status.md`.

## Completion

- Update `/plan/fixes-2/status.md` with final verification commands and results.
- Commit the final status update.
