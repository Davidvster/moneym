# Phase 8: Transaction Search

## Problem
The search icon button in the Transactions screen header currently has `onClick = { /* TODO search */ }` — it does nothing. When tapped, it should reveal a search field that filters the displayed transaction list by note/category text.

## Files to modify
- `feature/transactions/src/commonMain/kotlin/com/dv/moneym/feature/transactions/ui/TransactionListScreen.kt` — add search UI state, animated search field, and wire up to filter logic
- `feature/transactions/src/commonMain/kotlin/com/dv/moneym/feature/transactions/presentation/TransactionListIntent.kt` — add `SearchQueryChanged(query: String)` intent
- `feature/transactions/src/commonMain/kotlin/com/dv/moneym/feature/transactions/presentation/TransactionListUiState.kt` — add `searchQuery: String` field
- `feature/transactions/src/commonMain/kotlin/com/dv/moneym/feature/transactions/presentation/TransactionListViewModel.kt` — handle search query; filter `dayGroups` by it
- `feature/transactions/src/commonMain/composeResources/values/strings.xml` — add `transactions_search_placeholder`
- (all locale variants)

## Implementation steps

### ViewModel changes
1. Add `_searchQuery = MutableStateFlow("")` to the ViewModel.
2. Add `SearchQueryChanged` to `TransactionListIntent`.
3. Add `searchQuery` to `TransactionListUiState` (default `""`).
4. In the state combine flow, after building `dayGroups`, filter by `searchQuery`:
   ```kotlin
   val filteredGroups = if (searchQuery.isBlank()) dayGroups
   else {
       val q = searchQuery.trim().lowercase()
       dayGroups.mapNotNull { group ->
           val matchingTxns = group.transactions.filter { tx ->
               tx.categoryName.lowercase().contains(q) ||
               (tx.note?.lowercase()?.contains(q) == true)
           }
           if (matchingTxns.isEmpty()) null
           else group.copy(transactions = matchingTxns)
       }
   }
   ```
5. Include `searchQuery` in the combine or as a separate `.map` step. Add `_searchQuery` to the combine pipeline or use `flatMapLatest`.
6. Handle intent: `is TransactionListIntent.SearchQueryChanged -> _searchQuery.update { intent.query }`.

### UI changes
1. Add `var isSearchActive by remember { mutableStateOf(false) }` in `TransactionListContent`.
2. When `isSearchActive` is false: show the title row with the search icon button.
3. When `isSearchActive` is true: replace the title row with an animated `MmField` (search bar) with a close button to return to normal mode. Auto-focus the field.
4. Use `AnimatedContent` or `AnimatedVisibility` for the transition between title and search bar.
5. Tapping the X button clears the search query and hides the search bar.
6. When search bar closes, send `SearchQueryChanged("")`.

### Search field UI
```kotlin
// In the title row area:
if (isSearchActive) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        MmField(
            value = state.searchQuery,
            onValueChange = { onIntent(TransactionListIntent.SearchQueryChanged(it)) },
            placeholder = stringResource(Res.string.transactions_search_placeholder),
            prefix = { Icon(MmIcons.search, null, tint = colors.text3, modifier = Modifier.size(18.dp)) },
            modifier = Modifier.weight(1f),
            // auto-focus
        )
        MmIconButton(
            icon = MmIcons.close,
            onClick = {
                isSearchActive = false
                onIntent(TransactionListIntent.SearchQueryChanged(""))
            },
            contentDescription = "Close search",
        )
    }
} else {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("Transactions" / stringResource, ...)
        MmIconButton(
            icon = MmIcons.search,
            onClick = { isSearchActive = true },
            contentDescription = "Search",
        )
    }
}
```

## Acceptance criteria
- [ ] Tapping the search icon reveals an inline search field in the header
- [ ] Typing in the search field filters transaction rows in real-time by category name or note text
- [ ] Empty search shows all transactions for the month
- [ ] Tapping X clears the search and hides the search bar
- [ ] The segmented filter (All/Expenses/Income) and month navigation still work alongside search
