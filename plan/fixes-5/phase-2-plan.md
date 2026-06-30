# Phase 2 Plan: Suggested Banking App Packages

## Goal
Broaden the wallet notification app picker's suggested package list across major banking and payment apps worldwide without changing user-selected package behavior.

## Implementation
- Expand `WALLET_SYNC_SUGGESTED_PACKAGES` in `WalletSyncHomeUiState.kt`.
- Keep the list grouped by region with comments.
- Include only package IDs that are already known or can be reasonably verified from public Android package references.
- Do not add strings or other localized resources; package IDs are not user-visible copy.

## Tests
- Add a common unit test for `WalletSyncHomeUiState.suggestedApps`.
- Cover representative suggested packages from several regions and at least one non-suggested package that remains in `otherApps`.

## Verification
- `./gradlew :feature:walletsync:testDebugUnitTest --console=plain --no-configuration-cache`
- `git diff --check`

## Commit
- `fixes-5 phase 2: expand wallet sync suggested apps`
