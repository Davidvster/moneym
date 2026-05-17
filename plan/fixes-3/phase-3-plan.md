# Phase 3 — Transaction List: Daily Sum + Persist User Settings

## Items covered
- Item 5: Transaction list view — daily sum for current date's transactions
- Item 9: Persist user last selected settings (filter + overview tab)

## Problem analysis

### Item 5: Daily sum in transaction list
The transaction list shows `DayGroup` headers with a label (day name). Need to show the total expense sum for that day's transactions at the end of the sticky header row. The sum should be smaller font and more muted color than main amounts.

The `DayGroup` already has `transactions: List<TransactionUiModel>` and each `TransactionUiModel` has `amountMinorUnits` and `isExpense`.

Changes needed:
1. Compute `dailyNetAmount` in each `DayGroup` or compute it in the UI from existing data
2. In `TransactionListBody`, in the `stickyHeader`, show the daily total alongside the label
3. Style: smaller font (caption size), muted color (text2 or text3)

The daily sum should be the net for that day (income positive, expenses negative) or just total expenses depending on context. Looking at the existing "NET" display in the header, show daily expenses total (negative sign for expenses).

Computing in UI from `group.transactions`: 
```kotlin
val dailyExpenses = group.transactions.filter { it.isExpense }.sumOf { it.amountMinorUnits }
val dailyIncome = group.transactions.filter { !it.isExpense }.sumOf { it.amountMinorUnits }
val dailyNet = dailyIncome - dailyExpenses
```

Show it as a compact number in the sticky header row.

### Item 9: Persist user last selected settings
Need to persist:
1. Last selected filter in transaction list (All/Income/Expense) — currently `TransactionFilter` is ephemeral in `_filter: MutableStateFlow`
2. Last selected tab in overview (Month/Year) — currently `_period` in OverviewViewModel is ephemeral

The project already has `AppSettings` (backed by `russhwolf/multiplatform-settings`) and `AppSettingsRepository`. We can add new pref keys for filter and overview tab.

**Strategy**: 
- Add new pref keys to `PrefKeys` object in `AppSettings.kt`
- Add methods to `AppSettingsRepository` interface and `DefaultAppSettingsRepository` implementation
- Load persisted values in `TransactionListViewModel` init and persist on changes
- Load persisted values in `OverviewViewModel` init and persist on changes

However, to minimize scope: we can use `AppSettings` directly (not the repository interface since it would require interface changes) in the ViewModels. Actually, to keep it clean, add to `AppSettingsRepository`.

**Simpler approach**: Use `AppSettings` directly in both ViewModels (it's already injected in `TransactionListViewModel` via `appSettingsRepository`). Actually `TransactionListViewModel` already has `appSettingsRepository: AppSettingsRepository`. 

Add to `AppSettingsRepository` interface:
```kotlin
fun observeLastTransactionFilter(): Flow<TransactionFilter>
suspend fun setLastTransactionFilter(filter: TransactionFilter)
fun observeLastOverviewTab(): Flow<String> // "month" or "year"
suspend fun setLastOverviewTab(tab: String)
```

But `OverviewViewModel` doesn't currently have `AppSettingsRepository` injected. Check `FeatureModules.kt` to understand DI.

Alternative simpler approach: Use `AppSettings` directly (not through the repository) in both ViewModels. TransactionListViewModel already gets `appSettingsRepository`, so extend that interface. For OverviewViewModel, inject `AppSettings` directly.

**Simplest approach that avoids DI changes**: 
- Add `TX_LAST_FILTER` and `OVERVIEW_LAST_TAB` to `PrefKeys`
- Add two methods to `AppSettingsRepository` interface
- Implement in `DefaultAppSettingsRepository`
- In `TransactionListViewModel`: on init, read last filter; on filter change, persist it
- In `OverviewViewModel`: inject `AppSettingsRepository` and on init read last period; on period change, persist it
- Update `FeatureModules.kt` to inject `AppSettingsRepository` into `OverviewViewModel`

## Files to modify

### Item 5 — Daily sum

1. `/Users/davidvalic/Developer/MoneyM2/feature/transactions/src/commonMain/kotlin/com/dv/moneym/feature/transactions/ui/TransactionListScreen.kt`
   - In `TransactionListBody`, in the `stickyHeader` lambda, add a `Row` inside `SectionLabel` area showing label on left and daily total on right
   - Actually `SectionLabel` is a simple Text composable. Need to change the stickyHeader to use a custom Row: label text on left, daily total on right
   - Compute daily net from `group.transactions` within the stickyHeader

### Item 9 — Persist settings

2. `/Users/davidvalic/Developer/MoneyM2/core/datastore/src/commonMain/kotlin/com/dv/moneym/core/datastore/AppSettings.kt`
   - Add `TX_LAST_FILTER = "pref.tx_last_filter"` and `OVERVIEW_LAST_TAB = "pref.overview_last_tab"` to PrefKeys

3. `/Users/davidvalic/Developer/MoneyM2/core/datastore/src/commonMain/kotlin/com/dv/moneym/core/datastore/AppSettingsRepository.kt`
   - Add `fun observeLastTransactionFilter(): Flow<String>` and `suspend fun setLastTransactionFilter(encoded: String)`
   - Add `fun observeLastOverviewPeriod(): Flow<String>` and `suspend fun setLastOverviewPeriod(encoded: String)`

4. `/Users/davidvalic/Developer/MoneyM2/core/datastore/src/commonMain/kotlin/com/dv/moneym/core/datastore/DefaultAppSettingsRepository.kt`
   - Implement the new methods: just store/observe a string (e.g. "all", "expense", "income" for filter; "month", "year" for tab)

5. `/Users/davidvalic/Developer/MoneyM2/feature/transactions/src/commonMain/kotlin/com/dv/moneym/feature/transactions/presentation/TransactionListViewModel.kt`
   - On init: read `appSettingsRepository.observeLastTransactionFilter()` and set `_filter` accordingly
   - On `FilterChanged`: also call `viewModelScope.launch { appSettingsRepository.setLastTransactionFilter(encoded) }`

6. `/Users/davidvalic/Developer/MoneyM2/feature/overview/src/commonMain/kotlin/com/dv/moneym/feature/overview/presentation/OverviewViewModel.kt`
   - Add `appSettingsRepository: AppSettingsRepository` parameter to constructor
   - On init: read `appSettingsRepository.observeLastOverviewPeriod()` and initialize `_period` accordingly
   - On `TogglePeriod` and `PeriodSelected`: persist the current period type

7. `/Users/davidvalic/Developer/MoneyM2/composeApp/src/commonMain/kotlin/com/dv/moneym/di/FeatureModules.kt`
   - Update `OverviewViewModel` factory to inject `AppSettingsRepository`

## Detailed implementation

### Daily sum in stickyHeader
```kotlin
stickyHeader(key = "header_${group.date}") {
    val dailyExpenses = group.transactions.filter { it.isExpense }.sumOf { it.amountMinorUnits }
    val dailyIncome = group.transactions.filter { !it.isExpense }.sumOf { it.amountMinorUnits }
    val dailyNet = dailyIncome - dailyExpenses
    val currency = group.transactions.firstOrNull()?.currency ?: "EUR"
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.bg)
            .padding(horizontal = 20.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SectionLabel(
            text = group.label,
            modifier = Modifier.weight(1f),
        )
        // Daily total: show net (income - expenses)
        val sign = if (dailyNet >= 0) "+" else "−"
        val absValue = kotlin.math.abs(dailyNet) / 100.0
        val dailyColor = if (dailyNet >= 0) colors.text2 else colors.text2
        Text(
            text = "$sign $currency ${formatDailyAmount(absValue)}",
            style = type.caption.copy(fontSize = 11.sp),
            color = colors.text3,
        )
    }
}
```

Add helper function `formatDailyAmount` in the file or reuse the existing pattern.

### Filter encoding for persistence
- `TransactionFilter.None` → "all"
- `TransactionFilter.ByType(EXPENSE)` → "expense"
- `TransactionFilter.ByType(INCOME)` → "income"

On load: decode string to filter. Other filter types (ByCategory etc.) just default to None.

### Overview period persistence
- `OverviewPeriod.Month(...)` → "month"
- `OverviewPeriod.Year(...)` → "year"

On load: only restore the mode (month vs year), not the specific month/year (which defaults to current period anyway). So just restore whether it's month or year mode.

Actually we should restore the full period. Store as: "month:2026:5" or "year:2026". On load parse it.

But simpler: just store the mode ("month" or "year") since the period starts as current anyway. When toggling is persisted, next session restores the correct mode (month or year) starting at current date.

## Acceptance criteria
- Each day group header in transaction list shows a compact daily total (net) on the right side, in smaller/muted style
- After closing and reopening the app, the transaction list shows the same filter (All/Expenses/Income) as last selected
- After closing and reopening the app, the overview tab (Month/Year) is the same as last selected
- Android build compiles without errors
