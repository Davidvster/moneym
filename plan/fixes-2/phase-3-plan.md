# Phase 3 Plan: Transaction List Multiselect UI

## Goal

Add long-press transaction multiselect and bulk actions on top of the bulk repository methods.

## Implementation

- Store selection state in `TransactionPageViewModel` for the visible page/month.
- Long press a non-pending transaction to enter selection mode; tap toggles while active; pending recurring rows are not selectable.
- Extend row/body APIs with selected, selection mode, and long-click handling.
- Hoist selection summary to the outer transaction screen so the header becomes close, selected total/title, delete, and edit actions.
- Use confirmation sheets for bulk delete, category change, wallet move, and payment mode change.
- The edit sheet should show category, wallet only when more than one wallet exists, and payment mode only when enabled with more than one mode.
- For wallet moves across currencies, require a conversion rate before applying the move.
- Add all new user-facing strings to base plus every supported locale file.

## Tests

- ViewModel tests for selection start/toggle/clear and selected summaries.
- Tests for action eligibility rules.
- Tests proving bulk category changes transaction type, wallet currency mismatch requires a rate, and payment mode action follows settings.
- Run `./gradlew --no-configuration-cache :feature:transactions:testDebugUnitTest :data:transactions:testDebugUnitTest`.
