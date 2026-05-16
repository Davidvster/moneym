# Phase 12: Settings Transaction View Style Actually Changes Display

## Problem
The `TxListDisplayScreen` allows configuring the indicator style, category name visibility, note visibility, and density. Changes are persisted via `viewModel.setTxDisplayPrefs(...)` which calls `appSettingsRepository.setTxDisplayPrefs(...)` → `appSettings.putString/putBoolean(...)`. However, because `AppSettings.observeBoolean/observeString` are non-reactive (fixed by Phase 4), the `TransactionListViewModel` would not see these changes live.

**Note**: Phase 4 fixes the reactive prefs issue — this phase depends on Phase 4 being done first.

Additionally, there may be a display bug: `TxRow` in `TransactionListScreen.kt` already receives `prefs = state.txDisplayPrefs`, and `TransactionListViewModel` already combines `appSettingsRepository.observeTxDisplayPrefs()`. So once Phase 4 makes the observables reactive, the fix may be automatic.

This phase verifies the full flow works end-to-end after Phase 4, and adds any missing wiring.

## Files to modify
- `feature/transactions/src/commonMain/kotlin/com/dv/moneym/feature/transactions/ui/TransactionListScreen.kt` — verify `prefs = state.txDisplayPrefs` is passed to `TxRow` (it already is)
- `feature/transactions/src/commonMain/kotlin/com/dv/moneym/feature/transactions/presentation/TransactionListViewModel.kt` — verify `observeTxDisplayPrefs()` is in the combine chain (it already is)
- `feature/settings/src/commonMain/kotlin/com/dv/moneym/feature/settings/ui/TxListDisplayScreen.kt` — verify `onPrefsChanged` calls `viewModel.setTxDisplayPrefs` (it does)
- `core/datastore/src/commonMain/kotlin/com/dv/moneym/core/datastore/DefaultAppSettings.kt` — **THIS IS THE FIX** (done in Phase 4, just verify)

## Implementation steps

1. **Verify** after Phase 4 that the reactive `AppSettings` makes `observeTxDisplayPrefs()` emit new values when any of the 4 pref keys change.

2. **Test the flow**: 
   - `TxListDisplayScreen` changes density → calls `viewModel.setTxDisplayPrefs(prefs.copy(density = ...))`
   - `SettingsViewModel.setTxDisplayPrefs` calls `appSettingsRepository.setTxDisplayPrefs(prefs)`
   - `DefaultAppSettingsRepository.setTxDisplayPrefs` calls `appSettings.putString(TX_DENSITY, ...)`
   - `DefaultAppSettings.putString` emits on the changes flow (Phase 4 fix)
   - `DefaultAppSettingsRepository.observeTxDisplayPrefs()` sees the change and emits new `TxDisplayPrefs`
   - `TransactionListViewModel.state` is combined with `observeTxDisplayPrefs()`, emits new state
   - `TransactionListScreen` re-renders with new prefs
   - `TxRow` re-renders with new indicator style, density, etc.

3. If Phase 4 is implemented correctly, **no additional code changes are needed** for this phase. The phase is a verification + any remaining wiring.

4. However: check that `TransactionListViewModel` uses `appSettingsRepository.observeTxDisplayPrefs()` (it does — line 44: `appSettingsRepository.observeTxDisplayPrefs()`).

5. Also ensure the `SettingsViewModel`'s state reflects the latest prefs (it does via `combine(...) { ... _state.update { it.copy(txDisplayPrefs = txPrefs, ...) } }`).

6. **Additional check**: the `TxListDisplayScreen` uses `koinViewModel<SettingsViewModel>()`. Ensure both `TxListDisplayScreen` and `SettingsScreen` share the **same ViewModel instance**. Since `koinViewModel()` with the same key should return the same instance within the same nav scope — verify this is working.

## Acceptance criteria
- [ ] Changing the indicator style in Settings → Transaction List immediately updates the transaction list on the Transactions screen
- [ ] Changing density (Compact/Comfortable) immediately updates row heights in the transaction list
- [ ] Toggling "Show category name" or "Show note" immediately reflects in the transaction list
- [ ] Changes persist across app restarts
