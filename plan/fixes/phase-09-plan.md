# Phase 9: Month Popup (Month Picker Dialog)

## Problem
Clicking the month label in the Transactions screen (and Overview screen) should open a popup/dialog to pick any month, not just navigate one month at a time. Currently the month label is just a static `Text` with no click handler.

## Files to modify
- `feature/transactions/src/commonMain/kotlin/com/dv/moneym/feature/transactions/ui/TransactionListScreen.kt` — make month label clickable; show month picker dialog
- `feature/transactions/src/commonMain/kotlin/com/dv/moneym/feature/transactions/presentation/TransactionListIntent.kt` — add `MonthSelected(yearMonth: YearMonth)` intent
- `feature/transactions/src/commonMain/kotlin/com/dv/moneym/feature/transactions/presentation/TransactionListViewModel.kt` — handle `MonthSelected` intent
- `feature/overview/src/commonMain/kotlin/com/dv/moneym/feature/overview/ui/OverviewScreen.kt` — same treatment for period label in month mode
- `feature/overview/src/commonMain/kotlin/com/dv/moneym/feature/overview/presentation/OverviewIntent.kt` — add `MonthSelected(yearMonth: YearMonth)` intent
- `feature/overview/src/commonMain/kotlin/com/dv/moneym/feature/overview/presentation/OverviewViewModel.kt` — handle `MonthSelected` intent

## Implementation steps

### Month Picker Dialog composable

Create a reusable `MonthPickerDialog` composable (can be placed in `TransactionListScreen.kt` or a shared utility file):

```kotlin
@Composable
fun MonthPickerDialog(
    currentYear: Int,
    currentMonth: Int,
    onDismiss: () -> Unit,
    onConfirm: (year: Int, month: Int) -> Unit,
) {
    var selectedYear by remember { mutableIntStateOf(currentYear) }
    var selectedMonth by remember { mutableIntStateOf(currentMonth) }
    
    val monthNames = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun",
                            "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Month", style = MM.type.title3, color = MM.colors.text) },
        text = {
            Column {
                // Year row with chevrons
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                    MmIconButton(MmIcons.chevronLeft, onClick = { selectedYear-- }, ...)
                    Text("$selectedYear", style = MM.type.body, color = MM.colors.text, modifier = Modifier.widthIn(min = 64.dp), textAlign = TextAlign.Center)
                    MmIconButton(MmIcons.chevronRight, onClick = { selectedYear++ }, ...)
                }
                Spacer(Modifier.height(16.dp))
                // 4x3 grid of month buttons
                // Use FlowRow or LazyVerticalGrid
                val months = (1..12).toList()
                // 3 columns x 4 rows
                for (row in 0..3) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        for (col in 0..2) {
                            val m = row * 3 + col + 1
                            val isSelected = m == selectedMonth && selectedYear == currentYear
                            // Or: isSelected = m == selectedMonth (picker selection state)
                            Box(
                                modifier = Modifier
                                    .clip(MM.radius.md)
                                    .background(if (isSelected) MM.colors.accent else Color.Transparent)
                                    .clickable { selectedMonth = m }
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    monthNames[m - 1],
                                    style = MM.type.body,
                                    color = if (isSelected) MM.colors.bg else MM.colors.text,
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(selectedYear, selectedMonth) }) {
                Text("OK", color = MM.colors.accent)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = MM.colors.text2) }
        },
        containerColor = MM.colors.surface,
        titleContentColor = MM.colors.text,
    )
}
```

Use the existing `AlertDialog` from Material3 (it's already imported in `TransactionEditScreen`).

### TransactionListScreen changes
1. Add `var showMonthPicker by remember { mutableStateOf(false) }`.
2. Wrap the month label `Text` in a `Box` with `Modifier.clickable { showMonthPicker = true }`.
3. Show `MonthPickerDialog` when `showMonthPicker == true`.
4. `onConfirm` → `onIntent(TransactionListIntent.MonthSelected(YearMonth(year, month)))`; dismiss.

### ViewModel changes
Handle `MonthSelected`: `_currentMonth.update { YearMonth(intent.yearMonth.year, intent.yearMonth.monthNumber) }`.

### Overview Screen
Same pattern: make period label clickable only when in month mode; show month picker; on confirm, emit `OverviewIntent.MonthSelected(...)` and handle it in the ViewModel (jump to `OverviewPeriod.Month(selectedYearMonth)`).

## Acceptance criteria
- [ ] Tapping the month label in the Transactions screen opens a month picker dialog
- [ ] The dialog shows a year selector (chevrons) and a 4x3 grid of month abbreviations
- [ ] Selecting a month and confirming navigates to that month
- [ ] Cancel closes dialog without changing month
- [ ] Same works in the Overview screen (month mode only)
