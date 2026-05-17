# Phase 5 — Split Large Composables & Extract Reusable Components

## Goal
1. Split `OverviewContent` (>500 lines) into semantically named sub-composables, each under 100 lines.
2. Split `SettingsContent` into sub-composables.
3. Extract reusable components that appear in multiple screens: `MonthPickerDialog` (identical in TransactionListScreen and OverviewScreen), `YearPickerDialog` (OverviewScreen), `AppLockup` (PinSetupScreen + PinUnlockScreen), `PinDots` (PinSetupScreen + PinUnlockScreen).

## Files to Edit

- `/Users/davidvalic/Developer/MoneyM2/feature/overview/src/commonMain/kotlin/com/dv/moneym/feature/overview/ui/OverviewScreen.kt`
- `/Users/davidvalic/Developer/MoneyM2/feature/settings/src/commonMain/kotlin/com/dv/moneym/feature/settings/ui/SettingsScreen.kt`
- `/Users/davidvalic/Developer/MoneyM2/feature/security/src/commonMain/kotlin/com/dv/moneym/feature/security/ui/PinSetupScreen.kt`
- `/Users/davidvalic/Developer/MoneyM2/feature/security/src/commonMain/kotlin/com/dv/moneym/feature/security/ui/PinUnlockScreen.kt`
- `/Users/davidvalic/Developer/MoneyM2/feature/transactions/src/commonMain/kotlin/com/dv/moneym/feature/transactions/ui/TransactionListScreen.kt`

## Detailed Changes

### OverviewScreen.kt — split OverviewContent

`OverviewContent` currently is 400+ lines with inline LazyColumn item content. Extract these sub-composables (all `private`):

**`OverviewHeader`** — the title row + period navigation row (lines 190–246 approx):
```kotlin
@Composable
private fun OverviewHeader(
    periodLabel: String,
    isMonthMode: Boolean,
    onTogglePeriod: () -> Unit,
    onPreviousPeriod: () -> Unit,
    onNextPeriod: () -> Unit,
    onPeriodLabelClick: () -> Unit,
)
```

**`IncomeExpensesCard`** — the stacked income+expenses card (lines 265–323 approx):
```kotlin
@Composable
private fun IncomeExpensesCard(
    income: Double,
    expenses: Double,
    currencyCode: String,
    modifier: Modifier = Modifier,
)
```

**`AvgStatsCard`** — the average stats card (2 variants: month and year) — already has 2 `if (inMonthMode)` blocks. Extract both into one composable with a mode param:
```kotlin
@Composable
private fun AvgStatsCard(
    inMonthMode: Boolean,
    avgDailyExpense: Double,
    avgMonthlyExpense: Double,
    avgDailyExpenseYear: Double,
    currencyCode: String,
    modifier: Modifier = Modifier,
)
```

**`CumulativeSpendCard`** — the cumulative spend + CumulativeChart + axis labels (lines ~395–449):
```kotlin
@Composable
private fun CumulativeSpendCard(
    cumulativeTotals: List<Double>,
    todayIndex: Int,
    currencyCode: String,
    modifier: Modifier = Modifier,
)
```

**`MonthlySpendingBarChart`** — the monthly bar chart shown in year mode (lines ~461–521):
```kotlin
@Composable
private fun MonthlySpendingBarChart(
    monthlyTotals: List<Double>,
    currentMonthIndex: Int,
    monthNames: List<String>,
    modifier: Modifier = Modifier,
)
```

`OverviewContent` itself becomes a coordinator that calls these sub-composables in sequence, staying under 100 lines.

### SettingsScreen.kt — split SettingsContent

Extract these private sub-composables:

**`AppearanceSection`**:
```kotlin
@Composable
private fun AppearanceSection(
    isDark: Boolean,
    themeIndex: Int,
    themeModes: List<ThemeMode>,
    txDisplaySummary: String,
    onThemeChanged: (ThemeMode) -> Unit,
    onNavigateToTxDisplay: () -> Unit,
)
```

**`SecuritySection`**:
```kotlin
@Composable
private fun SecuritySection(
    state: SettingsUiState,
    lockAfterLabel: String,
    onIntent: (SettingsIntent) -> Unit,
    onShowLockPicker: () -> Unit,
)
```

**`PreferencesSection`**:
```kotlin
@Composable
private fun PreferencesSection(
    currencySubtitle: String,
    languageSubtitle: String,
    onNavigateToCurrency: () -> Unit,
    onNavigateToLanguage: () -> Unit,
    onNavigateToCategories: () -> Unit,
)
```

**`DataSection`**:
```kotlin
@Composable
private fun DataSection(
    onIntent: (SettingsIntent) -> Unit,
)
```

`SettingsContent` becomes the coordinator that calls these sections.

### PinSetupScreen + PinUnlockScreen — shared AppLockup and PinDots

Both screens have identical "app lockup" box (56dp square with "M" letter) and "MoneyM" text. Extract:

**In `PinSetupScreen.kt`** — add private sub-composables:
```kotlin
@Composable
private fun AppLockup(colors: MoneyMColors, type: MoneyMType)

@Composable
private fun PinDots(filledCount: Int, shakeOffsetPx: Int, colors: MoneyMColors)
```

Refactor `PinSetupContent` to call `AppLockup` and `PinDots` instead of inline code.
Refactor `PinUnlockContent` to do the same. 

Note: Since these two screens are in the same module (`feature/security`), the shared composables can go in `PinSetupScreen.kt` as internal or in a new file `PinShared.kt` in the same package. Use a new file `PinShared.kt`.

New file: `/Users/davidvalic/Developer/MoneyM2/feature/security/src/commonMain/kotlin/com/dv/moneym/feature/security/ui/PinShared.kt`

Contents:
```kotlin
package com.dv.moneym.feature.security.ui

// AppLockup and PinDots composables
// Used by both PinSetupScreen and PinUnlockScreen
```

### TransactionListScreen.kt — split TransactionListContent

Extract:

**`TransactionListHeader`**:
```kotlin
@Composable
private fun TransactionListHeader(
    isSearchActive: Boolean,
    searchQuery: String,
    monthLabel: String,
    netDouble: Double,
    netAmount: Long,
    netCurrency: String,
    selectedFilterIndex: Int,
    onSearchActivated: () -> Unit,
    onSearchClosed: () -> Unit,
    onSearchQueryChanged: (String) -> Unit,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onMonthLabelClick: () -> Unit,
    onFilterChanged: (Int) -> Unit,
)
```

**`TransactionListBody`**:
```kotlin
@Composable
private fun TransactionListBody(
    state: TransactionListUiState,
    onEditTransaction: (TransactionId) -> Unit,
    modifier: Modifier = Modifier,
)
```

**`TransactionListFooter`**:
```kotlin
@Composable
private fun TransactionListFooter(
    onAddTransaction: () -> Unit,
    onTabSelected: (TabRoute) -> Unit,
)
```

`TransactionListContent` becomes a coordinator calling these.

## Important Constraints
- Do NOT change any public API (composable function signatures visible from outside the file/module).
- All extracted composables must be `private`.
- Preserve all existing behavior — this is pure structural refactoring.
- After refactoring, verify NO composable function body exceeds 100 lines.

## Acceptance Criteria
1. `OverviewContent` function body is under 100 lines.
2. `SettingsContent` function body is under 100 lines.
3. `TransactionListContent` function body is under 100 lines.
4. `PinSetupContent` function body is under 100 lines.
5. `PinUnlockContent` function body is under 100 lines.
6. `PinShared.kt` exists with `AppLockup` and `PinDots` composables.
7. `./gradlew :composeApp:assembleDebug` passes.
