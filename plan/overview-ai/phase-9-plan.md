# Phase 9 Plan: Final Integration, QA, iOS/Android Build

## Goal

Finish the overview customization, AI widget, and follow-up bugfix rollout with final cross-platform verification and a clean status record.

## Scope

1. Status cleanup
   - Record the Phase 8 commit hash in `/plan/overview-ai/status.md`.
   - Keep all prior phase hashes visible.
   - Mark Phase 9 complete only after verification and commit.

2. String and resource parity
   - Confirm every new overview/settings string key exists in `values/` and all 27 locale folders.
   - Fix any missing or mismatched keys in the same phase if found.

3. Persistence/backup sanity
   - Re-check that overview layout preferences and saved AI widgets remain covered by backup export/import/restore paths.
   - Re-run targeted backup and overview persistence tests.

4. Cross-platform build verification
   - Run Android debug app build:
     - `./gradlew --no-configuration-cache :androidApp:assembleDebug`
   - Run iOS framework links:
     - `./gradlew --no-configuration-cache :shared:linkDebugFrameworkIosArm64 :shared:linkDebugFrameworkIosSimulatorArm64`

5. Targeted test verification
   - Run the touched-module tests:
     - `./gradlew --no-configuration-cache :data:overview:testDebugUnitTest :data:backup:testDebugUnitTest :feature:overview:testDebugUnitTest :feature:settings:testDebugUnitTest :feature:transactions:testDebugUnitTest :core:ai:testDebugUnitTest :feature:aianalysis:testDebugUnitTest`
   - Run `git diff --check`.
   - If time and environment allow, run `./gradlew --no-configuration-cache allTests`; if not, document that it was skipped.

## Builder Handoff

Use a cheaper builder for this phase unless build failures become complex.

The builder owns:
- `/plan/overview-ai/status.md`
- Any string/resource parity fixes required by verification
- No feature rewrites unless directly required to fix failing verification

Instructions:
- Do not revert previous phase work.
- Prefer small, targeted fixes only.
- Update `/plan/overview-ai/status.md` with exact commands and pass/fail results.
- Commit only Phase 9 changes after verification.

## Acceptance Criteria

- Android debug build passes.
- iOS arm64 and simulator arm64 framework links pass.
- Targeted tests from all touched modules pass, or any unavoidable environment blocker is clearly recorded.
- String parity is checked for all base + 27 locale resource folders.
- `/plan/overview-ai/status.md` records final phase status and phase commit hashes.
- Phase 9 is committed.
