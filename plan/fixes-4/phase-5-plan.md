# Phase 5 Plan: Final Verification and Cleanup

## Goal
Verify the full Fixes 4 stack after all implementation phases, record commands/results and commit hashes, and make a final status-only verification commit.

## Verification Commands
1. Android build:
   - `./gradlew :androidApp:assembleDebug --console=plain`
2. iOS framework links:
   - `./gradlew :shared:linkDebugFrameworkIosArm64 :shared:linkDebugFrameworkIosSimulatorArm64 --console=plain`
3. Focused changed-module tests:
   - `./gradlew :feature:aianalysis:testDebugUnitTest :core:ai:testDebugUnitTest --console=plain --no-configuration-cache`
   - `./gradlew :feature:transactions:testDebugUnitTest :data:transactions:testDebugUnitTest --console=plain --no-configuration-cache`
   - `./gradlew :feature:banksync:testDebugUnitTest :data:banksync:testDebugUnitTest :data:walletsync:testDebugUnitTest --console=plain`
4. Full Android unit tests:
   - `./gradlew testDebugUnitTest --console=plain`
5. String parity:
   - Audit every touched resource module against the 27 supported locales:
     - `feature/settings`
     - `feature/banksync`
     - `feature/infopage`
     - `feature/transactions`

## Cleanup
- Check `git status --short` and `git diff --check`.
- Update `plan/fixes-4/status.md`:
  - Mark Phase 5 complete.
  - Record all verification commands.
  - Record commit hashes for Phases 1-4 and the final Phase 5 commit as pending until committed.
  - Record known residual risks.

## Commit
- Commit message: `fixes-4 phase 5: verify android and ios builds`.
