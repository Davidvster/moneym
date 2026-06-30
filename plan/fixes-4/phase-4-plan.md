# Phase 4 Plan: Rejected Suggestions Delete Flow

## Goal
Rejected sync suggestions can be deleted from the database, including bulk deletion from multiselect, and every delete must confirm through `MmDeleteSheet`.

## Scope
- Likely modules:
  - `feature/banksync` suggestion UI/ViewModel
  - `data/banksync`
  - `data/walletsync`
  - `core/testing` or module-local fakes
- Do not touch transaction multiselect, AI analysis, or bank-sync copy.

## Data Layer
1. Extend the shared suggestion contract:
   - Add `deleteRejected(ids: Set<Long>)` or the existing strongly typed equivalent to `SuggestionSource`.
   - Implement it for bank-sync suggestions and wallet-sync suggestions.
   - Preserve existing pending accept/reject behavior.
2. DAO/data source:
   - Add delete-by-ids queries for rejected bank suggestions and rejected wallet suggestions.
   - Delete rows from the database; do not status-change them.
   - No schema migration is needed for delete-only queries.
3. Fakes/tests:
   - Update all fakes that implement `SuggestionSource`.
   - Add repository tests proving delete rejected IDs removes only rejected rows and leaves pending rows unaffected.

## UI / ViewModel
1. Rejected tab:
   - Allow selection/multiselect of rejected suggestions.
   - Show a delete action for one or many selected rejected suggestions.
2. Confirmation:
   - Always show `MmDeleteSheet` before deleting one or many rejected suggestions.
   - Confirm calls `deleteRejected(...)` and then clears selection/sheet state.
3. Pending tab:
   - Existing accept/reject flows remain unchanged.
   - Pending suggestions should not show the rejected-delete action.

## Tests
- Add/repair `SuggestionsViewModelTest` coverage for:
  - rejected row selection
  - delete sheet state before delete
  - confirmed delete calls through source/repository and clears selection
  - pending tab accept/reject behavior unaffected
- Add repository tests for bank-sync and wallet-sync rejected delete behavior.

## Verification
- Run `./gradlew :feature:banksync:testDebugUnitTest :data:banksync:testDebugUnitTest :data:walletsync:testDebugUnitTest --console=plain`.
- If configuration cache blocks test execution, rerun with `--no-configuration-cache` and record that.

## Constraints
- Follow data-layer conventions and fake parity.
- Follow ViewModel convention: one public `onIntent(...)`; selection/sheet state belongs in UiState/ViewModel.
- Every new user-facing string must be added to base and all 27 locales.
- Do not commit; the main agent will review, update status, verify, and commit.
