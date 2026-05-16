# Phase 11: New Transaction Auto-Focus Amount

## Problem
When opening the new transaction screen, the amount input field should automatically receive keyboard focus so the user can immediately start typing the amount without tapping the field first.

The current implementation has a `FocusRequester` and a hidden `BasicTextField` for the amount, but there is no `LaunchedEffect` to request focus on composition.

## Files to modify
- `feature/transactionEdit/src/commonMain/kotlin/com/dv/moneym/feature/transactionedit/ui/TransactionEditScreen.kt` — add `LaunchedEffect` to request focus on the amount field when entering a new transaction

## Implementation steps

1. In `TransactionEditContent`, the `focusRequester` is already created:
   ```kotlin
   val focusRequester = remember { FocusRequester() }
   ```

2. Add a `LaunchedEffect` that requests focus when the screen is for a new transaction (not edit mode):
   ```kotlin
   LaunchedEffect(state.isEditMode) {
       if (!state.isEditMode) {
           // Small delay to let the composition settle before requesting focus
           // This is needed for the IME to show up correctly
           delay(100)
           runCatching { focusRequester.requestFocus() }
       }
   }
   ```
   Use `delay(100)` to allow the layout to settle. Wrap in `runCatching` to handle `IllegalStateException` if the node isn't attached yet.

3. Import `kotlinx.coroutines.delay` (already available via coroutines).

4. Make the `LaunchedEffect` key `Unit` for new transactions, or key it on `state.isEditMode`:
   ```kotlin
   LaunchedEffect(Unit) {
       if (!state.isEditMode) {
           delay(150)
           runCatching { focusRequester.requestFocus() }
       }
   }
   ```

Note: `state.isEditMode` might be `false` initially and then updated. Use `LaunchedEffect(state.isEditMode)` to run only once it's known to be false (i.e., new transaction):
```kotlin
if (!state.isLoading && !state.isEditMode) {
    LaunchedEffect(Unit) {
        delay(150)
        runCatching { focusRequester.requestFocus() }
    }
}
```

The `TransactionEditUiState` has `isEditMode: Boolean` and `isSaving: Boolean`. Check if there's an `isLoading` field. If not, use `!state.isEditMode` directly.

## Acceptance criteria
- [ ] When the "New transaction" button is tapped, the amount input field immediately receives focus and the numeric keyboard appears
- [ ] When editing an existing transaction, the amount field does NOT auto-focus (user may want to scroll to different fields first)
- [ ] Auto-focus does not cause crashes or log errors on any supported platform
