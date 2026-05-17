# Phase 10 — Overview month/year selector: clickable header + "Now" option

## Goal
Make the Overview period selector match the Transactions screen:
1. The period label (e.g. "May 2026" or "2026") should be clickable and open a month/year picker dialog.
2. For month mode: show a MonthPicker dialog with a "Now" option like in transactions.
3. For year mode: show a simpler year picker dialog.
4. Keep the existing chevron left/right navigation working.

## Files to Change
1. `feature/overview/src/commonMain/kotlin/com/dv/moneym/feature/overview/presentation/OverviewUiState.kt` — add new intent
2. `feature/overview/src/commonMain/kotlin/com/dv/moneym/feature/overview/presentation/OverviewViewModel.kt` — handle new intent
3. `feature/overview/src/commonMain/kotlin/com/dv/moneym/feature/overview/ui/OverviewScreen.kt` — clickable label + dialogs

## Implementation Steps

### Step 1: Add PeriodSelected intent to OverviewUiState.kt
In `OverviewUiState.kt`, inside `sealed interface OverviewIntent`, add:
```kotlin
data class PeriodSelected(val period: OverviewPeriod) : OverviewIntent
```

### Step 2: Handle PeriodSelected in OverviewViewModel.kt
In `OverviewViewModel.kt`, inside `fun onIntent(intent: OverviewIntent)` when block, add:
```kotlin
is OverviewIntent.PeriodSelected -> {
    _periodOffset.value = 0
    _period.value = intent.period
}
```

### Step 3: Add local dialog state in OverviewContent composable
In `OverviewScreen.kt`, inside `OverviewContent`, after `val isMonthMode = ...`, add:
```kotlin
var showPeriodPicker by remember { mutableStateOf(false) }
```

### Step 4: Make the period label clickable
In the header item block, replace the plain `Text(text = periodLabel, ...)` with a clickable Box:
```kotlin
Box(
    modifier = Modifier
        .widthIn(min = 96.dp)
        .clip(RoundedCornerShape(8.dp))
        .clickable(
            indication = null,
            interactionSource = remember { MutableInteractionSource() },
        ) { showPeriodPicker = true }
        .padding(horizontal = 4.dp, vertical = 2.dp),
    contentAlignment = Alignment.Center,
) {
    Text(
        text = periodLabel,
        style = type.body,
        color = colors.text,
        textAlign = TextAlign.Center,
    )
}
```

### Step 5: Show period picker dialog in OverviewContent Column
Place the dialog display logic inside `OverviewContent`, at the end of the Column body (after `MmTabBar`):
```kotlin
if (showPeriodPicker) {
    if (isMonthMode) {
        val currentPeriod = state.period as? OverviewPeriod.Month
        if (currentPeriod != null) {
            OverviewMonthPickerDialog(
                currentYear = currentPeriod.yearMonth.year,
                currentMonth = currentPeriod.yearMonth.monthNumber,
                onDismiss = { showPeriodPicker = false },
                onConfirm = { year, month ->
                    onIntent(OverviewIntent.PeriodSelected(OverviewPeriod.Month(YearMonth(year, month))))
                    showPeriodPicker = false
                },
            )
        }
    } else {
        val currentPeriod = state.period as? OverviewPeriod.Year
        if (currentPeriod != null) {
            OverviewYearPickerDialog(
                currentYear = currentPeriod.year,
                onDismiss = { showPeriodPicker = false },
                onConfirm = { year ->
                    onIntent(OverviewIntent.PeriodSelected(OverviewPeriod.Year(year)))
                    showPeriodPicker = false
                },
            )
        }
    }
}
```

### Step 6: Add OverviewMonthPickerDialog private composable
Add at the bottom of OverviewScreen.kt:
```kotlin
@Composable
private fun OverviewMonthPickerDialog(
    currentYear: Int,
    currentMonth: Int,
    onDismiss: () -> Unit,
    onConfirm: (year: Int, month: Int) -> Unit,
) {
    val colors = MM.colors
    val type = MM.type
    var selectedYear by remember { mutableIntStateOf(currentYear) }
    var selectedMonth by remember { mutableIntStateOf(currentMonth) }
    val todayDate = remember {
        kotlinx.datetime.Clock.System.now()
            .toLocalDateTime(kotlinx.datetime.TimeZone.currentSystemDefault()).date
    }
    val nowYear = todayDate.year
    val nowMonth = todayDate.monthNumber
    val monthNames = listOf("Jan","Feb","Mar","Apr","May","Jun","Jul","Aug","Sep","Oct","Nov","Dec")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Month", style = type.title3, color = colors.text) },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
                    MmIconButton(icon = MmIcons.chevronLeft, onClick = { selectedYear-- }, size = 32.dp, contentDescription = "Prev year")
                    Text(
                        text = selectedYear.toString(),
                        style = type.body,
                        color = if (selectedYear == nowYear) colors.accent else colors.text,
                        modifier = Modifier.widthIn(min = 64.dp),
                        textAlign = TextAlign.Center,
                    )
                    MmIconButton(icon = MmIcons.chevronRight, onClick = { selectedYear++ }, size = 32.dp, contentDescription = "Next year")
                }
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (row in 0..3) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                            for (col in 0..2) {
                                val m = row * 3 + col + 1
                                val isSelected = m == selectedMonth
                                val isNow = m == nowMonth && selectedYear == nowYear
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) colors.accent else Color.Transparent)
                                        .then(if (isNow && !isSelected) Modifier.border(1.dp, colors.accent.copy(alpha = 0.5f), RoundedCornerShape(8.dp)) else Modifier)
                                        .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) { selectedMonth = m }
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        text = monthNames[m - 1],
                                        style = type.body,
                                        color = when { isSelected -> colors.bg; isNow -> colors.accent; else -> colors.text },
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = { onConfirm(nowYear, nowMonth) }) { Text("Now", color = colors.text2) }
                TextButton(onClick = { onConfirm(selectedYear, selectedMonth) }) { Text("OK", color = colors.accent) }
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = colors.text2) } },
        containerColor = colors.surface,
        titleContentColor = colors.text,
    )
}
```

### Step 7: Add OverviewYearPickerDialog private composable
```kotlin
@Composable
private fun OverviewYearPickerDialog(
    currentYear: Int,
    onDismiss: () -> Unit,
    onConfirm: (year: Int) -> Unit,
) {
    val colors = MM.colors
    val type = MM.type
    var selectedYear by remember { mutableIntStateOf(currentYear) }
    val nowYear = remember {
        kotlinx.datetime.Clock.System.now()
            .toLocalDateTime(kotlinx.datetime.TimeZone.currentSystemDefault()).date.year
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Year", style = type.title3, color = colors.text) },
        text = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
                MmIconButton(icon = MmIcons.chevronLeft, onClick = { selectedYear-- }, size = 32.dp, contentDescription = "Prev year")
                Text(
                    text = selectedYear.toString(),
                    style = type.body,
                    color = if (selectedYear == nowYear) colors.accent else colors.text,
                    modifier = Modifier.widthIn(min = 80.dp),
                    textAlign = TextAlign.Center,
                )
                MmIconButton(icon = MmIcons.chevronRight, onClick = { selectedYear++ }, size = 32.dp, contentDescription = "Next year")
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = { onConfirm(nowYear) }) { Text("Now", color = colors.text2) }
                TextButton(onClick = { onConfirm(selectedYear) }) { Text("OK", color = colors.accent) }
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = colors.text2) } },
        containerColor = colors.surface,
        titleContentColor = colors.text,
    )
}
```

### Step 8: Add necessary imports to OverviewScreen.kt
- `androidx.compose.foundation.clickable`
- `androidx.compose.foundation.interaction.MutableInteractionSource`
- `androidx.compose.foundation.layout.Arrangement`
- `androidx.compose.foundation.shape.RoundedCornerShape`
- `androidx.compose.foundation.border`
- `androidx.compose.material3.AlertDialog`
- `androidx.compose.material3.TextButton`
- `androidx.compose.runtime.mutableIntStateOf`
- `androidx.compose.runtime.mutableStateOf`
- `androidx.compose.ui.draw.clip`
- `com.dv.moneym.core.model.YearMonth`

## Acceptance Criteria
1. Tapping period label in Overview opens a picker dialog
2. Month mode: dialog shows month grid + year chevrons + "Now" button + "OK" + "Cancel"
3. Year mode: dialog shows year with chevrons + "Now" button + "OK" + "Cancel"
4. "Now" button jumps to current month/year or year
5. "OK" applies the selection via `OverviewIntent.PeriodSelected`
6. Current month highlighted with accent border in month picker when browsing current year
7. Chevron left/right on header still work
8. Build compiles: `./gradlew :composeApp:assembleDebug`
