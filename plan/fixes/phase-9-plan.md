# Phase 9 — Transaction month/year picker: add "Now" option

## Goal
Add a "Now" button to `MonthPickerDialog` in `TransactionListScreen.kt` that jumps the selection to the current month+year.

## File to Change
`feature/transactions/src/commonMain/kotlin/com/dv/moneym/feature/transactions/ui/TransactionListScreen.kt`

## Current MonthPickerDialog (lines ~384-491)
The `MonthPickerDialog` currently has:
- Year navigation row (chevron left, year text, chevron right)
- 4x3 month grid
- Confirm button ("OK") and Cancel dismiss button

The signature is:
```kotlin
private fun MonthPickerDialog(
    currentYear: Int,
    currentMonth: Int,
    onDismiss: () -> Unit,
    onConfirm: (year: Int, month: Int) -> Unit,
)
```

## Implementation Steps

### Step 1: Add "Now" functionality
The "Now" button should jump `selectedYear` and `selectedMonth` to the actual current date.

We need access to the current date inside the composable. Since this is in commonMain, use `kotlinx.datetime`:
```kotlin
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
```

At the top of `MonthPickerDialog`, add:
```kotlin
val todayDate = remember {
    Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
}
val nowYear = todayDate.year
val nowMonth = todayDate.monthNumber
```

### Step 2: Add a "Now" button in the dialog's confirm area
Currently the `AlertDialog` has:
```kotlin
confirmButton = {
    TextButton(onClick = { onConfirm(selectedYear, selectedMonth) }) {
        Text("OK", color = colors.accent)
    }
},
dismissButton = {
    TextButton(onClick = onDismiss) {
        Text("Cancel", color = colors.text2)
    }
},
```

Change to include a "Now" button alongside the existing buttons:
```kotlin
confirmButton = {
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TextButton(onClick = {
            onConfirm(nowYear, nowMonth)
        }) {
            Text("Now", color = colors.text2)
        }
        TextButton(onClick = { onConfirm(selectedYear, selectedMonth) }) {
            Text("OK", color = colors.accent)
        }
    }
},
dismissButton = {
    TextButton(onClick = onDismiss) {
        Text("Cancel", color = colors.text2)
    }
},
```

### Step 3: Visually highlight "Now" month in the grid
When browsing in the picker, show the actual current month/year with a subtle indicator (e.g., a ring or different text style) so the user can identify it.

In the month grid loop, change to:
```kotlin
val m = row * 3 + col + 1
val isSelected = m == selectedMonth && selectedYear == selectedYear  // always true, but:
val isCurrentPeriod = m == selectedMonth
val isNow = m == nowMonth && selectedYear == nowYear

Box(
    modifier = Modifier
        .clip(RoundedCornerShape(8.dp))
        .background(
            when {
                isSelected -> colors.accent
                isNow -> colors.surface2
                else -> Color.Transparent
            }
        )
        .then(
            if (isNow && !isSelected) {
                Modifier.border(1.dp, colors.accent.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
            } else Modifier
        )
        .clickable(...) { selectedMonth = m }
        .padding(horizontal = 12.dp, vertical = 8.dp),
    contentAlignment = Alignment.Center,
) {
    Text(
        text = monthNames[m - 1],
        style = type.body,
        color = when {
            isSelected -> colors.bg
            isNow -> colors.accent
            else -> colors.text
        },
    )
}
```

Wait, there's a mistake: `isSelected` should be `m == selectedMonth` (without the redundant comparison). Fix:
```kotlin
val isSelected = m == selectedMonth
val isNow = m == nowMonth && selectedYear == nowYear
```

### Step 4: Highlight current year in the year row
When `selectedYear == nowYear`, show the year in `colors.accent` color:

```kotlin
Text(
    text = selectedYear.toString(),
    style = type.body,
    color = if (selectedYear == nowYear) colors.accent else colors.text,
    modifier = Modifier.widthIn(min = 64.dp),
    textAlign = TextAlign.Center,
)
```

### Step 5: Add necessary imports
- `kotlinx.datetime.Clock`
- `kotlinx.datetime.TimeZone`
- `kotlinx.datetime.toLocalDateTime`
- `androidx.compose.foundation.border` (if not already present)

## Acceptance Criteria
1. `MonthPickerDialog` has a "Now" text button in the confirm area
2. Tapping "Now" immediately calls `onConfirm` with the current month/year
3. When browsing to the current year, the current month cell has a dotted accent border
4. When the current year is selected, the year label appears in `colors.accent`
5. Existing "OK" and "Cancel" behavior is unchanged
6. Build compiles: `./gradlew :composeApp:assembleDebug`
