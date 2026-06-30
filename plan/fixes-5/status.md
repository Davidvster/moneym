# Fixes 5 Status

## Phase 1: Notification Parser Accuracy
- Status: Complete
- Plan: `plan/fixes-5/phase-1-plan.md`
- Commit: `0de9ef70` (`fixes-5 phase 1: tighten notification parsing`)
- Verification:
  - `./gradlew :data:walletsync:testDebugUnitTest --console=plain`
  - `git diff --check`

## Phase 2: Suggested Banking App Packages
- Status: Complete
- Plan: `plan/fixes-5/phase-2-plan.md`
- Commit: `db8eb7e8` (`fixes-5 phase 2: expand wallet sync suggested apps`)
- Verification:
  - `./gradlew :feature:walletsync:testDebugUnitTest --console=plain --no-configuration-cache`
  - `git diff --check`

## Phase 3: Final Build Verification
- Status: Complete
- Plan: `plan/fixes-5/phase-3-plan.md`
- Commit: pending (`fixes-5 phase 3: verify wallet sync fixes`)
- Verification:
  - `./gradlew :data:walletsync:testDebugUnitTest :feature:walletsync:testDebugUnitTest --console=plain --no-configuration-cache`
  - `./gradlew :androidApp:assembleDebug --console=plain`
  - `./gradlew :shared:linkDebugFrameworkIosArm64 :shared:linkDebugFrameworkIosSimulatorArm64 --console=plain`
  - `git diff --check`
- Known residual risks:
  - iOS framework linking still emits existing non-blocking bundle ID inference warnings.
  - Kotlin compile output still includes existing non-blocking deprecation and expect/actual beta warnings.
