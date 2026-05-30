# Phase 4: Recurring Transactions Improvements

## Context
Two sub-tasks:
1. Empty state text is too bare ("No recurring transactions yet") — needs explanation that user must create a transaction and mark it as recurring first, OR can directly create a recurring template
2. Add ability to create a recurring rule directly from the list (without first creating an actual transaction)

Architecture note: `RecurringTransaction` is a standalone entity. `RecurringEditViewModel` can create one without a linked transaction. Currently `RecurringEditKey(val ruleId: Long)` requires an existing ID. Extend to support `ruleId = 0L` for "new" mode.

## Critical Files
- `feature/settings/src/commonMain/kotlin/com/dv/moneym/feature/settings/recurring/RecurringListScreen.kt`
- `feature/transactionEdit/src/commonMain/kotlin/com/dv/moneym/feature/transactionedit/RecurringEditViewModel.kt`
- `feature/transactionEdit/src/commonMain/kotlin/com/dv/moneym/feature/transactionedit/RecurringEditScreen.kt`
- `feature/settings/src/commonMain/composeResources/values/strings.xml` (+ de, es, it)
- `composeApp/src/commonMain/kotlin/com/dv/moneym/MainNav.kt`
- `composeApp/src/commonMain/kotlin/com/dv/moneym/di/FeatureModules.kt`

## Changes

### Change 1: Update empty state string
`feature/settings/strings.xml`:
```xml
<!-- Old -->
<string name="settings_recurring_empty">No recurring transactions yet</string>

<!-- New (split into title + body or single multi-line) -->
<string name="settings_recurring_empty_title">No recurring transactions yet</string>
<string name="settings_recurring_empty_body">Create a transaction and enable the recurring option when saving, or tap + to create a recurring template directly.</string>
```
Add translations to de, es, it.

`RecurringListScreen.kt` — update `EmptyView()` to show both title (body style) and explanation (caption style, text3 color).

### Change 2: Add "+" FAB to RecurringListScreen
`RecurringListScreen.kt`:
- Add `onCreateNew: () -> Unit` param to `recurringListEntry`, `RecurringListScreen`, `RecurringListContent`
- Add `Box` with `Scaffold` or a `FloatingActionButton` overlaid:
  ```kotlin
  Box(Modifier.fillMaxSize()) {
      // existing content
      FloatingActionButton(
          onClick = onCreateNew,
          modifier = Modifier.align(Alignment.BottomEnd).padding(MM.dimen.padding_2x),
          containerColor = MM.colors.accent,
      ) {
          Icon(Icon.Plus.imageVector, contentDescription = null, tint = MM.colors.bg)
      }
  }
  ```

### Change 3: RecurringEditViewModel — new mode support
`RecurringEditViewModel.kt`:
- Change `ruleId: RecurringTransactionId` to `ruleId: RecurringTransactionId` (keep as-is)
- In `init()`, check `if (ruleId.value == 0L)` → initialize with defaults instead of loading:
  ```kotlin
  private suspend fun init() {
      if (ruleId.value == 0L) {
          initNewRule()
          return
      }
      val rule = recurringRepo.getById(ruleId) ?: run {
          _effects.send(TransactionEditEffect.Deleted)
          return
      }
      // ... existing logic
  }
  
  private fun initNewRule() {
      viewModelScope.launch {
          combine(
              categoryRepository.observeActive(),
              accountRepository.observeAll(),
              paymentModeRepository.observeAll(),
              appSettingsRepository.observePaymentModeEnabled(),
          ) { cats, accs, modes, pmEnabled -> Quad(cats, accs, modes, pmEnabled) }
          .collect { (cats, accs, modes, pmEnabled) ->
              val defaultCat = cats.firstOrNull { it.type == TransactionType.EXPENSE }
              val defaultAcc = accs.firstOrNull()
              _state.update { s ->
                  s.copy(
                      isLoading = false,
                      isEditMode = false,  // new, not edit
                      isRecurring = true,
                      availableCategories = cats,
                      availableAccounts = accs,
                      paymentModes = modes,
                      showPaymentMode = pmEnabled,
                      selectedCategoryId = s.selectedCategoryId ?: defaultCat?.id,
                      selectedAccountId = s.selectedAccountId ?: defaultAcc?.id,
                      date = s.date ?: clock.today(),
                      type = TransactionType.EXPENSE,
                  )
              }
          }
      }
  }
  ```
- `save()` in new mode (`ruleId.value == 0L`): create new `RecurringTransaction` with `id = RecurringTransactionId(0)` instead of loading existing:
  ```kotlin
  private fun save() {
      // ... validation same as before ...
      if (ruleId.value == 0L) {
          // create new
          val newRule = RecurringTransaction(
              id = RecurringTransactionId(0),
              type = s.type,
              amount = Money(minorUnits, currency),
              note = s.note.trim().ifEmpty { null },
              categoryId = catId,
              accountId = accId,
              paymentModeId = if (s.showPaymentMode) s.selectedPaymentModeId else null,
              startDate = date,
              rule = rule,
              endCondition = end,
              lastMaterializedDate = null,
              createdAt = now,
              updatedAt = now,
          )
          withContext(dispatchers.io) { recurringRepo.upsert(newRule) }
      } else {
          // existing edit — same as before
      }
  }
  ```

### Change 4: RecurringEditScreen nav key
`RecurringEditScreen.kt` — `RecurringEditKey(val ruleId: Long)` already supports 0. No change needed to key itself.

`recurringEditEntry(...)` in `RecurringEditScreen.kt` — already creates VM with `ruleId = RecurringTransactionId(key.ruleId)`. Works for 0L.

### Change 5: MainNav.kt
`recurringListEntry(onEdit = ..., onBack = ...)` — add `onCreateNew = { navController.navigate(RecurringEditKey(0L)) }` callback.

Update `recurringListEntry` call in `MainNav.kt` to pass `onCreateNew`.

## Verification
1. Open recurring list empty → see explanation text + "+" FAB
2. Tap "+" → opens edit screen with no pre-filled data, is in "new" mode
3. Fill in amount/category/recurrence → save → appears in recurring list
4. Tap existing recurring rule → edit works as before
