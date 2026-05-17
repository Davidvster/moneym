# Phase 8 — Overview: average spend per day (month view) and average by month/day (year view)

## Goal
- **Month view**: Add a stat row showing "Avg/day" — the average daily expense for the month (total expenses / days elapsed in month, or total / days in month for past months).
- **Year view**: Add two stat rows showing "Avg/month" (total expenses / months elapsed) and "Avg/day" (total expenses / days elapsed in year).

## Files to Change
1. `feature/overview/src/commonMain/kotlin/com/dv/moneym/feature/overview/presentation/OverviewUiState.kt` — add fields to `OverviewUiState`
2. `feature/overview/src/commonMain/kotlin/com/dv/moneym/feature/overview/presentation/OverviewViewModel.kt` — compute and populate new fields
3. `feature/overview/src/commonMain/kotlin/com/dv/moneym/feature/overview/ui/OverviewScreen.kt` — render the new stat rows/cards

## Implementation Steps

### Step 1: Add new fields to OverviewUiState
In `OverviewUiState.kt`, add to the `OverviewUiState` data class:
```kotlin
// ── Average stats ────────────────────────────────────────────
val avgDailyExpense: Double = 0.0,       // month view: avg expense per day (elapsed days)
val avgMonthlyExpense: Double = 0.0,     // year view: avg expense per month (elapsed months)
val avgDailyExpenseYear: Double = 0.0,   // year view: avg expense per day (elapsed days in year)
```

### Step 2: Compute average stats in OverviewViewModel
In `OverviewViewModel.kt`, inside the `combine` lambda, add computation after existing data:

```kotlin
// ── Average stats ────────────────────────────────────────────
val avgDailyExpense: Double
val avgMonthlyExpense: Double
val avgDailyExpenseYear: Double

when (period) {
    is OverviewPeriod.Month -> {
        val month = period.yearMonth
        val isCurrentMonth = month.year == today.year && month.monthNumber == today.monthNumber
        val elapsedDays = if (isCurrentMonth) today.dayOfMonth else daysInMonth(month.year, month.monthNumber)
        avgDailyExpense = if (elapsedDays > 0) expensesDouble / elapsedDays else 0.0
        avgMonthlyExpense = 0.0
        avgDailyExpenseYear = 0.0
    }
    is OverviewPeriod.Year -> {
        val isCurrentYear = period.year == today.year
        val elapsedMonths = if (isCurrentYear) today.monthNumber else 12
        val elapsedDaysInYear = if (isCurrentYear) {
            // Days from Jan 1 to today
            val jan1 = LocalDate(period.year, 1, 1)
            (today.toEpochDays() - jan1.toEpochDays()).toInt() + 1
        } else {
            // Full year
            val jan1 = LocalDate(period.year, 1, 1)
            val jan1Next = LocalDate(period.year + 1, 1, 1)
            (jan1Next.toEpochDays() - jan1.toEpochDays()).toInt()
        }
        avgDailyExpense = 0.0
        avgMonthlyExpense = if (elapsedMonths > 0) expensesDouble / elapsedMonths else 0.0
        avgDailyExpenseYear = if (elapsedDaysInYear > 0) expensesDouble / elapsedDaysInYear else 0.0
    }
}
```

Then include in `OverviewUiState(...)` constructor call:
```kotlin
avgDailyExpense = avgDailyExpense,
avgMonthlyExpense = avgMonthlyExpense,
avgDailyExpenseYear = avgDailyExpenseYear,
```

### Step 3: Render stat rows in OverviewScreen

#### Month view: Add "Avg/day" below the Income + Expenses card
In `OverviewScreen.kt`, inside the `AnimatedContent` block, after the Income/Expenses `MmCard` and before `SpendingByCategoryCard`, add:

```kotlin
// Average stats card (month mode)
if (inMonthMode && state.avgDailyExpense > 0) {
    MmCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        padded = true,
        shape = MM.radius.md,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            SectionLabel(
                text = "AVG / DAY",
                modifier = Modifier.weight(1f),
            )
            MmMoney(
                value = state.avgDailyExpense,
                size = 15.sp,
                weight = FontWeight.SemiBold,
                currency = currencyCode,
            )
        }
    }
}
```

#### Year view: Add "Avg/month" and "Avg/day" rows
In the year view section, after the monthly spending bar chart `MmCard` and before `CategoryTrendsCard`, add:

```kotlin
// Average stats card (year mode)
if (!inMonthMode && (state.avgMonthlyExpense > 0 || state.avgDailyExpenseYear > 0)) {
    MmCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        padded = true,
        shape = MM.radius.md,
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                SectionLabel(
                    text = "AVG / MONTH",
                    modifier = Modifier.weight(1f),
                )
                MmMoney(
                    value = state.avgMonthlyExpense,
                    size = 15.sp,
                    weight = FontWeight.SemiBold,
                    currency = currencyCode,
                )
            }
            Spacer(Modifier.height(8.dp))
            HorizontalDivider(color = MM.colors.divider, thickness = 1.dp)
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                SectionLabel(
                    text = "AVG / DAY",
                    modifier = Modifier.weight(1f),
                )
                MmMoney(
                    value = state.avgDailyExpenseYear,
                    size = 15.sp,
                    weight = FontWeight.SemiBold,
                    currency = currencyCode,
                )
            }
        }
    }
}
```

### Step 4: Ensure `state` variable usage in AnimatedContent
Note: Inside `AnimatedContent`, the `targetState` is `state.period` and the lambda parameter is `period`. But `state.avgDailyExpense` etc. need to come from the outer `state`. This is fine since `state` is read from `collectAsState()` in the parent scope and is accessible inside the lambda (it's a closure).

However, `state` in `AnimatedContent` context refers to the outer `state by viewModel.state.collectAsState()`. The `inMonthMode` variable should use `period` from the `AnimatedContent` lambda (already done in existing code as `val inMonthMode = period is OverviewPeriod.Month`).

## Acceptance Criteria
1. Month view shows a small card with "AVG / DAY" and the computed average daily expense
2. Year view shows a small card with both "AVG / MONTH" and "AVG / DAY" rows
3. Cards only appear when there are expenses (value > 0)
4. Values are formatted correctly using MmMoney
5. Values are computed correctly:
   - Month avg/day = total month expenses / elapsed days in month
   - Year avg/month = total year expenses / elapsed months
   - Year avg/day = total year expenses / elapsed days in year
6. Build compiles: `./gradlew :composeApp:assembleDebug`
