# Phase 4: Transaction Edit UseCase Refactor And Suggestion Mapping

## Goal

Move transaction edit business logic out of the ViewModel and move suggestion-to-draft mapping out of `MainNav.kt`.

## Changes

- Replace `MainNav.kt`'s private `SuggestionRow.toTransactionEditDraft()` extension with a mapper owned by the transaction edit/suggestion boundary.
- Extract focused use cases for:
  - Loading edit reference data and defaults.
  - Applying suggestion drafts.
  - Selecting note suggestions and matching categories.
  - Saving normal and recurring transactions.
  - Accepting suggestion drafts.
  - Computing budget projection input/results.
- Keep the route contract and ViewModel public API unchanged.

## Verification

- `./gradlew :feature:transactionEdit:testDebugUnitTest :shared:compileDebugKotlinAndroid`

## Commit

- `Refactor transaction edit business logic into use cases`
