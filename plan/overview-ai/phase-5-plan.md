# Phase 5 Plan: Transaction Bulk Edit + List Interaction Fixes

## Goal

Fix transaction multi-select editing and list interactions from user items 2 and 8:

- Bulk edit bottom sheet actions must actually progress when tapping "Assign to category" and "Move to wallet".
- Category assignment must update transaction type when the target category type differs.
- Moving to a wallet with a different currency must ask for a conversion ratio, defaulting to editable `1`.
- Moving to a wallet with the same currency must apply without requiring a rate.
- Selection header must not flicker on each selected transaction change.
- Horizontal month paging must be disabled while multi-select is active and restored when selection clears.
- Switching transaction type filters between Income and Expense must scroll the current list back to the top.

This phase covers user items 2 and 8 only. Commit after verification.

## Expected Files/Modules

- `feature/transactions/src/commonMain/kotlin/com/dv/moneym/feature/transactions/list/TransactionListScreen.kt`
- `feature/transactions/src/commonMain/kotlin/com/dv/moneym/feature/transactions/list/TransactionListViewModel.kt`
- `feature/transactions/src/commonMain/kotlin/com/dv/moneym/feature/transactions/list/TransactionListUiState.kt`
- `feature/transactions/src/commonMain/kotlin/com/dv/moneym/feature/transactions/list/page/TransactionPageScreen.kt`
- `feature/transactions/src/commonMain/kotlin/com/dv/moneym/feature/transactions/list/page/TransactionPageViewModel.kt`
- `feature/transactions/src/commonMain/kotlin/com/dv/moneym/feature/transactions/list/page/TransactionPageUiState.kt`
- `feature/transactions/src/commonTest/kotlin/com/dv/moneym/feature/transactions/TransactionPageViewModelTest.kt`
- `feature/transactions/src/commonTest/kotlin/com/dv/moneym/feature/transactions/TransactionListViewModelTest.kt`
- Transaction string resources under `feature/transactions/src/commonMain/composeResources/values*/strings.xml` only if new user-visible text is needed.
- `plan/overview-ai/status.md`

## Implementation Notes

- Existing `TransactionPageViewModel` already has repository-backed flows for bulk category/wallet/payment-mode updates. Preserve the intent-only API.
- The likely bug is sheet replacement inside an already-open `ModalBottomSheet`: tapping an action sets a new sheet state while the current sheet is still composed. Make the action sheet dismiss/transition cleanly before showing picker sheets, or render the active bulk sheet through a single modal host that can swap content reliably.
- Preserve the tested behavior that `transactionRepository.updateCategory(ids, category.id, category.type)` updates both category and transaction type.
- When opening the wallet conversion confirmation for mismatched currencies, initialize the editable rate text to `1` instead of blank, and still validate that the final value is positive.
- Keep same-currency wallet moves one confirm away without rate entry.
- Stabilize the selection header:
  - Do not set `visibleSelection = null` and then a new non-null value for every selected-count/totals change.
  - Prefer a stable selection-mode boolean for header mode and pass changing selection details into the already-visible header.
- Disable `HorizontalPager` user swipes while any visible page is in selection mode. Arrow/month picker navigation can remain controlled by the header when not in selection mode.
- In the page list, scroll to top when the active transaction type filter changes between income/expense/all. Use a VM-owned event/effect or a composable `LaunchedEffect` based on a stable filter revision; do not put persistent scroll state in the VM unless the existing pattern already does.

## Tests

- Existing `TransactionPageViewModelTest` cases for category type changes and wallet conversion should continue to pass.
- Add/update tests for:
  - mismatched wallet picker initializes conversion rate text to `1`.
  - wallet conversion accepts default `1` without requiring user typing.
  - type filter change exposes a scroll-to-top signal/revision if implemented in VM.
- UI behavior that is hard to unit-test should be made structurally obvious and covered by previews where reasonable.

## Verification

Run:

```bash
./gradlew --no-configuration-cache :feature:transactions:testDebugUnitTest
```

Then run:

```bash
git diff --check
```

## Status Update Required

After implementation and verification, update `plan/overview-ai/status.md` with:

- Phase 5 status and verification command results.
- The commit hash after committing.

Commit only Phase 5 changes.
