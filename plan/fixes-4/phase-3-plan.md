# Phase 3 Plan: Transaction Multiselect and Bulk Edit Repair

## Goal
Make transaction multiselect feel intentional and make all bulk edit actions work: category, wallet, payment mode, and delete.

## Scope
- Primary module: `feature/transactions`
- Likely related data/model modules:
  - `data/transactions`
  - `data/accounts`
  - `data/categories`
  - `core/model`
  - `core/testing` fakes if repository contracts change
- Do not touch AI analysis or rejected suggestions.

## UX Implementation
1. Haptic feedback:
   - On the first long press that enters selection mode, trigger `LocalHapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)`.
2. Back behavior:
   - Add a `BackHandler(enabled = selectionMode)` in the transaction page/screen level that clears selection.
3. Normal controls during selection:
   - Hide the new transaction/footer action while selection mode is active.
   - Animate normal/selection header and footer changes with lightweight `AnimatedVisibility` or `AnimatedContent`.
4. Selected-row clarity:
   - In selection mode, show `MmCheckbox` in rows.
   - Selected rows need a visibly stronger selected background/accent edge.
   - Non-selected rows should be subtly dimmed while selection mode is active.

## Bulk Edit Implementation
1. Preserve the existing selection-header trash and pen actions.
2. Pen opens a bottom sheet with actions:
   - Bulk edit category.
   - Bulk move to another wallet, only when more than one wallet exists.
   - Bulk move to a different payment option, only when payment modes are enabled and more than one option exists.
3. Category:
   - Picker allows any active category.
   - Confirmation explains all selected transactions will switch to the chosen category and to that category's transaction type.
   - Confirm action must call repository/update code for every selected transaction.
4. Wallet:
   - Confirmation required.
   - If target wallet currency differs from any selected transaction currency, request an exchange-rate input consistent with the app's existing wallet-currency edit behavior.
   - Validate the rate and show an error instead of applying invalid rates.
5. Payment mode:
   - Confirmation required.
   - Confirm action must call repository/update code for every selected transaction.
6. Existing delete behavior should remain intact and still confirm through the existing delete flow.

## Tests
- Add or repair `TransactionPageViewModelTest` coverage for:
  - start and clear selection
  - bulk delete
  - bulk category changes transaction type
  - bulk wallet move with same currency
  - bulk wallet move with exchange rate
  - invalid exchange rate
  - bulk payment mode
- Add/repair UI preview or screenshot coverage for selected and non-selected rows if the module already has screenshot test patterns for transaction rows.

## Verification
- Run `./gradlew :feature:transactions:testDebugUnitTest :data:transactions:testDebugUnitTest --console=plain`.
- If configuration cache blocks Paparazzi/test execution, rerun with `--no-configuration-cache` and record that.

## Constraints
- Follow ViewModel convention: one public `onIntent(...)`; no extra public VM methods.
- Keep composables dumb: selection/dialog/sheet state belongs in UiState/ViewModel.
- All new user-facing strings must be added to base and all 27 locales.
- Repository interface changes require fake parity.
- Do not commit; the main agent will review, update status, verify, and commit.
