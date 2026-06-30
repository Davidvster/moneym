# Phase 3 Plan: Final Build Verification

## Goal
Verify the complete Fixes 5 stack on Android and iOS after parser and suggested-app changes are committed.

## Verification
- `./gradlew :data:walletsync:testDebugUnitTest :feature:walletsync:testDebugUnitTest --console=plain --no-configuration-cache`
- `./gradlew :androidApp:assembleDebug --console=plain`
- `./gradlew :shared:linkDebugFrameworkIosArm64 :shared:linkDebugFrameworkIosSimulatorArm64 --console=plain`
- `git diff --check`

## Status
- Update `plan/fixes-5/status.md` with each phase's status, commit hash, verification commands, and known residual risks.
- Record any known configuration-cache reruns explicitly.

## Commit
- `fixes-5 phase 3: verify wallet sync fixes`
