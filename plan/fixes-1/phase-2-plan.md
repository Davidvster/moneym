# Phase 2 Plan: Edit Transaction From Suggestions

## Goal

Allow pending wallet notification suggestions to open the transaction edit modal prefilled as a draft.

## Implementation

- Add serializable `TransactionEditDraft` containing primitive fields for amount, currency, type, date, note, account, category, suggestion source, suggestion ID, and external ID.
- Extend `TransactionEditKey` with optional draft data while keeping edit-by-ID behavior unchanged.
- Prefill `TransactionEditViewModel` new-transaction state from draft after settings, accounts, categories, and payment modes have loaded.
- On save from a suggestion draft, upsert the transaction, set the external ID, and call `SuggestionSource.accept(...)`.
- Add a wallet-suggestion-only edit action to pending suggestion cards and wire navigation through `walletSuggestionsEntry` and `MainNav`.

## Tests

- Transaction edit VM test for draft prefill.
- Transaction edit save test proving the suggestion is accepted and the transaction external ID is stored.
- Suggestions test proving the edit action is available only for wallet suggestions.
- Run `./gradlew :feature:transactionEdit:testDebugUnitTest :feature:banksync:testDebugUnitTest`.
