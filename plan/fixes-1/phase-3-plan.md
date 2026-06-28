# Phase 3 Plan: Prevent Last Wallet Deletion

## Goal

Prevent users from deleting their final active wallet and explain how to proceed.

## Implementation

- In `WalletManageViewModel`, intercept `DeleteRequested` when only one non-archived account exists.
- Do not open the delete confirmation for the last wallet.
- Add typed UI state for a blocked-delete dialog.
- Add localized title/body/action strings telling the user to create a new wallet first, then delete the old one.
- Keep existing delete behavior for two or more active wallets.

## Tests

- Wallet manage VM test: one active wallet blocks deletion.
- Wallet manage VM test: two active wallets still open confirmation and delete normally.
- Run `./gradlew :feature:settings:testDebugUnitTest`.
