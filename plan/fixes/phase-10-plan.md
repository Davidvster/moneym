# Phase 10: New Transaction Date Picker

## Problem
The date field in the new transaction form is clickable and opens a Material3 `DatePickerDialog`. The user wants:
1. The `DatePickerDialog` to include "Today" and "Yesterday" quick-select buttons for convenience
2. The date field to show a human-readable date (not the raw `LocalDate.toString()` like `2026-05-16`)

## Files to modify
- `feature/transactionEdit/src/commonMain/kotlin/com/dv/moneym/feature/transactionedit/ui/TransactionEditScreen.kt` — enhance the `DatePickerDialog` with quick-select buttons; improve date display format

## Implementation steps

### Improve date display
The current code: `val dateText = state.date?.toString() ?: ""`
`LocalDate.toString()` returns ISO format `yyyy-MM-dd`. Replace with a friendlier format:
- Today → "Today"
- Yesterday → "Yesterday"
- Other dates → "Mon, May 16" style (day-of-week abbreviation + month abbreviation + day)

Add a helper function:
```kotlin
private fun LocalDate.toFriendlyString(today: LocalDate): String {
    val yesterday = LocalDate(today.year, today.monthNumber, today.dayOfMonth - 1)
    // Handle month rollover for yesterday
    return when (this) {
        today -> "Today"
        // yesterday calculation properly:
        else -> {
            val dayName = dayOfWeek.name.lowercase().replaceFirstChar { it.uppercase() }.take(3)
            val monthName = month.name.lowercase().replaceFirstChar { it.uppercase() }.take(3)
            "$dayName, $monthName $dayOfMonth"
        }
    }
}
```

For yesterday: use `kotlinx.datetime.LocalDate` arithmetic. The library supports `minus(DatePeriod)`:
```kotlin
val yesterday = today.minus(DatePeriod(days = 1))
```

Then display:
- `this == today` → "Today"
- `this == yesterday` → "Yesterday"  
- else → `"$dayName, $monthName $dayOfMonth $year"` (include year if different from today's year)

### Add quick-select buttons to DatePickerDialog
The existing `DatePickerDialog`:
```kotlin
DatePickerDialog(
    onDismissRequest = { showDatePicker = false },
    confirmButton = { TextButton(onClick = { ... }) { Text("OK") } },
) {
    DatePicker(state = datePickerState)
}
```

Add a `dismissButton` slot with Today/Yesterday chips:
```kotlin
DatePickerDialog(
    onDismissRequest = { showDatePicker = false },
    confirmButton = {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            // Today chip
            TextButton(onClick = {
                onIntent(TransactionEditIntent.DateChanged(todayDate))
                showDatePicker = false
            }) { Text("Today", color = colors.accent) }
            // Yesterday chip
            TextButton(onClick = {
                onIntent(TransactionEditIntent.DateChanged(yesterdayDate))
                showDatePicker = false
            }) { Text("Yesterday", color = colors.accent) }
            // OK button
            TextButton(onClick = {
                datePickerState.selectedDateMillis?.let { millis ->
                    val instant = kotlin.time.Instant.fromEpochMilliseconds(millis)
                    val localDate = instant.toLocalDateTime(TimeZone.UTC).date
                    onIntent(TransactionEditIntent.DateChanged(localDate))
                }
                showDatePicker = false
            }) { Text("OK", color = colors.accent) }
        }
    },
) {
    DatePicker(state = datePickerState)
}
```

Need to compute `todayDate` and `yesterdayDate` in the composable. Add:
```kotlin
val todayDate = remember { Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date }
val yesterdayDate = remember { todayDate.minus(DatePeriod(days = 1)) }
```

Import `kotlinx.datetime.Clock`, `TimeZone`, `toLocalDateTime`, `DatePeriod`.

### Localize button text
Use string resources for "Today", "Yesterday", "OK", "Cancel" if not already available in the `transactionEdit` module strings. Add:
- `edit_date_today`
- `edit_date_yesterday`
to `feature/transactionEdit/src/commonMain/composeResources/values/strings.xml` and locales.

## Acceptance criteria
- [ ] Date field shows "Today" when the date is today
- [ ] Date field shows "Yesterday" when the date is yesterday
- [ ] Date field shows a human-readable format for other dates (e.g., "Mon, May 14")
- [ ] Opening the date picker shows "Today" and "Yesterday" quick-select buttons
- [ ] Tapping "Today" or "Yesterday" selects that date and closes the picker
- [ ] The standard calendar still works for selecting any other date
