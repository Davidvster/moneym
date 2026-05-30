# Phase 5: Navigation Bounds (Month/Year)

## Context
The user reports that overview allows navigating to months before the first available transaction. Three navigation surfaces need to respect the earliest-transaction bound:
1. Chevron buttons (left/right)
2. Calendar/month picker dialog
3. Month/year mode picker

### Root cause analysis
**Overview chevron**: `OverviewViewModel.canGoBack` has `if (minDate == null) true` — when `_dateBounds` hasn't loaded yet (initial emit is `null to null`), the left chevron is always visible. Even if minDate loads, during the null window the user could tap. Fix: change to `false` when null (if no transactions, don't allow going back from current month). Same issue in Year mode.

**Pager swipe**: Overview pager page 0 = anchor = earliestMonth, so swipe can't go before anchor. OK.

**Month picker dialog**: `MmMonthPickerDialog` already receives `minYear`/`minMonth` from `minSelectableDateIso`. However: the year left-chevron check is `selectedYear > minYear` (strict), meaning at minYear, the user can't go further left. The month grid disables months with `selectedYear == minYear && m < minMonth`. Looks correct.

**Year mode period picker**: Verify it also gets `minYear` constraint.

**Transaction list**: `canGoBack = state.currentPage > 0` where page 0 = anchor (earliestMonth). When no transactions, anchor = today and `currentPage = 0` → no back. Correct. Month picker gets `earliestMonth?.year` and `earliestMonth?.monthNumber`. Correct.

## Critical Files
- `feature/overview/src/commonMain/kotlin/com/dv/moneym/feature/overview/OverviewViewModel.kt`
- `feature/overview/src/commonMain/kotlin/com/dv/moneym/feature/overview/OverviewScreen.kt`
- `feature/transactions/src/commonMain/kotlin/com/dv/moneym/feature/transactions/list/TransactionListScreen.kt`
- `feature/transactions/src/commonMain/kotlin/com/dv/moneym/feature/transactions/list/TransactionListUiState.kt`

## Changes

### Change 1: Fix OverviewViewModel canGoBack
`OverviewViewModel.kt` — in the `combine` block (lines ~120–134):
```kotlin
// Current:
is OverviewPeriod.Month -> {
    val minDate = minIso?.let { LocalDate.parse(it) }
    if (minDate == null) true   // BUG: allows going back when data not loaded
    else YearMonth(p.yearMonth.year, p.yearMonth.monthNumber) >
            YearMonth(minDate.year, minDate.month.number)
}
is OverviewPeriod.Year -> {
    val minYear = minIso?.let { LocalDate.parse(it).year }
    minYear == null || p.year > minYear  // BUG: same issue
}

// Fix:
is OverviewPeriod.Month -> {
    val minDate = minIso?.let { LocalDate.parse(it) }
    if (minDate == null) false
    else YearMonth(p.yearMonth.year, p.yearMonth.monthNumber) >
            YearMonth(minDate.year, minDate.month.number)
}
is OverviewPeriod.Year -> {
    val minYear = minIso?.let { LocalDate.parse(it).year }
    minYear != null && p.year > minYear
}
```

### Change 2: Verify year period picker constraint in OverviewScreen
`OverviewScreen.kt` — find where the year period picker is shown and verify `minYear = state.minSelectableDateIso?.let { LocalDate.parse(it).year }` is passed. If it uses `MmMonthPickerDialog`, it already has minYear support. If it's a separate year-only picker, add the min constraint.

Also check that pager-swipe intents `MonthPagerSwiped` / `YearPagerSwiped` don't allow navigating before anchor. Since page 0 = anchor, the pager itself prevents this — no code change needed.

### Change 3: TransactionListUiState — expose canGoBack explicitly
`TransactionListUiState.kt`: Add `val canGoBack: Boolean = false` field (so screen doesn't compute it inline).

`TransactionListViewModel.kt` — in the `combine` block that computes `currentPage`:
```kotlin
state.copy(
    earliestMonth = earliestMonth,
    currentPage = currentPage,
    pageCount = todayPage + 1 + 120,
    canGoBack = currentPage > 0,
)
```

`TransactionListScreen.kt` — replace inline `val canGoBack = state.currentPage > 0` with `val canGoBack = state.canGoBack`.

### Change 4: Verify MmMonthPickerDialog in TransactionListScreen
Lines 155–161 in `TransactionListScreen.kt`:
```kotlin
MmMonthPickerDialog(
    minYear = state.earliestMonth?.year,
    minMonth = state.earliestMonth?.monthNumber,
    ...
)
```
This is already correct. Verify it's passed and the dialog respects it (already confirmed in `MmMonthPickerDialog.kt`).

## Verification
1. App with transactions starting June 2024 → left chevron in overview hides when at June 2024
2. Tapping period label → month picker grays out Jan–May 2024 when year 2024 selected
3. Fresh app (no transactions) → left chevron hidden in both overview and transactions
4. Year mode: can't go to year before first transaction year
