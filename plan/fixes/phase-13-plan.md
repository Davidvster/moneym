# Phase 13: Transaction Delete List Refresh

## Problem
Deleting a transaction in the edit screen doesn't remove it from the transaction list. The SqlDelight data source already uses `.asFlow().mapToList()` which is reactive. The `TransactionRepositoryImpl.delete()` correctly delegates to `dataSource.delete(id.value)`. However, the issue is that the transaction list view has already navigated away (the `TransactionEditScreen` dismisses after delete), but the `TransactionListViewModel` state should still update because its `observeByMonth` flow is active.

The actual bug may be:
1. The `TransactionListViewModel` uses `SharingStarted.WhileSubscribed(5_000)` — the state flow may be paused and miss the delete emission, then resume with stale data.
2. Alternatively, the `DeleteTransactionUseCase` dismisses the screen (via `TransactionEditEffect.Deleted`) BEFORE the database delete completes, so the list screen might re-attach to the flow before the delete is committed.

More likely root cause to investigate: the `stateIn(SharingStarted.WhileSubscribed(5_000))` — when the transaction edit screen is shown (modal overlay), the background `TransactionListViewModel` state flow may stop collecting (5 second timeout). When the modal dismisses, the flow resumes and re-subscribes to `observeByMonth` — at this point SqlDelight's reactive query should emit the latest state including the deletion. This should work correctly.

**Check DeleteTransactionUseCase**: it might not be calling the delete at all, or the effect might fire before delete completes.

## Files to modify
- `feature/transactionEdit/src/commonMain/kotlin/com/dv/moneym/feature/transactionedit/domain/DeleteTransactionUseCase.kt` — verify it's called and awaited
- `feature/transactionEdit/src/commonMain/kotlin/com/dv/moneym/feature/transactionedit/presentation/TransactionEditViewModel.kt` — verify delete flow (effect fires after delete)
- `feature/transactions/src/commonMain/kotlin/com/dv/moneym/feature/transactions/presentation/TransactionListViewModel.kt` — increase `WhileSubscribed` timeout or change to `Eagerly` to avoid missing updates

## Implementation steps

1. **Read `DeleteTransactionUseCase.kt`** and `TransactionEditViewModel.kt` to trace the delete flow.

2. **In `TransactionEditViewModel`**: The `DeleteConfirmed` intent should:
   - Call `withContext(dispatchers.io) { deleteTransaction(state.id) }` (suspend, awaited)
   - THEN send `TransactionEditEffect.Deleted`
   
   If the effect fires first (before the suspend completes), the screen dismisses before the DB is updated.

3. Fix the ordering:
   ```kotlin
   is TransactionEditIntent.DeleteConfirmed -> viewModelScope.launch {
       withContext(dispatchers.io) { deleteTransaction(currentState.transactionId!!) }
       _effects.send(TransactionEditEffect.Deleted)  // Send AFTER delete completes
   }
   ```

4. **In `TransactionListViewModel`**: Change `SharingStarted.WhileSubscribed(5_000)` to `SharingStarted.WhileSubscribed(10_000)` or `SharingStarted.Eagerly` so the flow stays active while the edit modal is shown.
   
   Actually `Eagerly` is better here — the list is always visible (behind the modal) and the ViewModel is always active:
   ```kotlin
   .stateIn(
       scope = viewModelScope,
       started = SharingStarted.Eagerly,
       initialValue = TransactionListUiState(),
   )
   ```
   
   This ensures the flow never stops and always receives DB updates.

5. Same fix for `OverviewViewModel` if needed.

## Acceptance criteria
- [ ] Deleting a transaction from the edit screen immediately removes it from the transaction list when returning
- [ ] The transaction list updates reactively — no manual refresh or navigation required
- [ ] Deleting the last transaction in a month shows the empty state
- [ ] Overview screen also updates after deletion
