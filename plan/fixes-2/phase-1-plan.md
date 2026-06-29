# Phase 1 Plan: Small UI And Sync Fixes

## Goal

Ship the smaller transaction edit, category count, transaction display preference, and sync feedback fixes before the larger bulk-edit work.

## Implementation

- Add an edit-mode "Save as new" action to transaction edit that inserts a new transaction from the current form while preserving the original transaction.
- Move transaction edit category filtering from the composable into ViewModel-derived state.
- Show transaction counts for active and archived categories in category management by deriving counts from `transactionRepository.observeAll()`.
- Add `TxDisplayPrefs.showSyncSuggestionBanner`, persist it through `AppSettingsRepository`, and expose it as a toggle in transaction view settings.
- Render the full-width sync banner only when `showSyncSuggestionBanner` is true; keep the header sync action visible whenever any sync source is enabled.
- Add immediate busy state for sync rename save and keep remove-device busy cleanup intact.
- Show a warning triangle sync action with a numeric badge when wallet suggestions, pending deletions, or backup sync conflicts require attention.

## Tests

- Transaction edit ViewModel test for save-as-new preserving the original and inserting a second transaction.
- Category list ViewModel test for visible transaction counts.
- Datastore/settings tests for the new transaction display preference.
- Sync settings ViewModel test for immediate rename busy state.
- Transaction list ViewModel test for banner preference and attention count state.
- Run `./gradlew --no-configuration-cache :feature:transactionEdit:testDebugUnitTest :feature:categories:testDebugUnitTest :feature:settings:testDebugUnitTest :feature:sync:testDebugUnitTest :feature:transactions:testDebugUnitTest`.
