# Phase 1 Plan: Shared Persisted Filters

## Goal

Persist the transaction type and category filter across resume, and apply the same filter on both the transaction list and overview.

## Implementation

- Extend `TransactionFilter` so it can represent Income/Expense plus multiple selected category IDs while keeping current single-category variants for existing callers.
- Update `DefaultAppSettingsRepository` encoding/decoding with backwards compatibility for `all`, `expense`, and `income`.
- Use `AppSettingsRepository.observeLastTransactionFilter()` as the source of truth for transaction list type/category filters.
- Keep search query ephemeral.
- Update transaction list intents and UI so the type segmented control and category picker update the same persisted filter.
- Update `TransactionPageViewModel` to apply type and category filters to saved transactions and pending recurring rows.
- Update overview to observe the same stored `TransactionFilter`, expose the same type/category controls, and pass the filter into overview page state building.

## Tests

- Datastore tests for old and new filter encodings.
- Transaction list VM tests for category persistence and combined type/category state.
- Overview VM/page tests proving shared filter affects totals and survives VM recreation.
- Run `./gradlew :core:datastore:testDebugUnitTest :feature:transactions:testDebugUnitTest :feature:overview:testDebugUnitTest`.
