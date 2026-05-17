# Phase 2 — Transaction date input redesign

## Goal
1. Replace the date `MmField` (read-only text input) with a `MmButton` (Ghost/Outline variant).
2. Quick-select chips ("Today" / "Yesterday") should show actual dates (e.g. "May 17" / "May 16").
3. The date button itself should still display "Today" or "Yesterday" when applicable.
4. Style the `DatePickerDialog` to match the app's design language using `MM.colors`, `MM.radius`, `MmCard` and `MmButton` style (replace the default M3 DatePickerDialog with a custom one using a `ModalBottomSheet`).

## File to Change
`feature/transactionEdit/src/commonMain/kotlin/com/dv/moneym/feature/transactionedit/ui/TransactionEditScreen.kt`

## Current Code (lines 371–388, 210–254)
The date field is an `MmField` (read-only, clickable). The date picker uses `DatePickerDialog` from Material3 with `TextButton` quick buttons for Today/Yesterday inside the confirm slot.

## Implementation Steps

### Step 1: Add a helper to format month+day for chip labels
Add below the existing `toFriendlyString` extension function:

```kotlin
private fun LocalDate.toShortMonthDay(): String {
    val monthName = month.name.lowercase().replaceFirstChar { it.uppercase() }.take(3)
    return "$monthName $dayOfMonth"
}
```

### Step 2: Replace the date MmField with a MmButton
Find the date section in `TransactionEditContent` (around line 370–388):

**Before:**
```kotlin
MmField(
    value = dateText,
    onValueChange = {},
    label = stringResource(Res.string.edit_date_label),
    singleLine = true,
    modifier = Modifier
        .fillMaxWidth()
        .clickable(...) { showDatePicker = true },
)
```

**After:**
Remove the MmField entirely and replace with:
```kotlin
// Date section label
Text(
    text = stringResource(Res.string.edit_date_label).uppercase(),
    style = type.micro,
    color = colors.text3,
)
Spacer(Modifier.height(8.dp))
// Date button + quick chips in a Row
Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.spacedBy(8.dp),
    verticalAlignment = Alignment.CenterVertically,
) {
    MmButton(
        text = dateText.ifEmpty { "Pick date" },
        onClick = { showDatePicker = true },
        variant = MmButtonVariant.Secondary,
        leadingIcon = MmIcons.calendar,
        modifier = Modifier.weight(1f),
    )
    // Today chip
    MmChip(
        selected = state.date == todayDate,
        onClick = { onIntent(TransactionEditIntent.DateChanged(todayDate)) },
    ) {
        Text(
            text = todayDate.toShortMonthDay(),
            style = type.caption,
            color = if (state.date == todayDate) colors.bg else colors.text,
            maxLines = 1,
        )
    }
    // Yesterday chip
    MmChip(
        selected = state.date == yesterdayDate,
        onClick = { onIntent(TransactionEditIntent.DateChanged(yesterdayDate)) },
    ) {
        Text(
            text = yesterdayDate.toShortMonthDay(),
            style = type.caption,
            color = if (state.date == yesterdayDate) colors.bg else colors.text,
            maxLines = 1,
        )
    }
}
```

### Step 3: Replace M3 DatePickerDialog with a custom ModalBottomSheet date picker

Replace the entire `if (showDatePicker) { ... }` block (lines 210–254) with a custom sheet:

```kotlin
if (showDatePicker) {
    val initialMillis = state.date
        ?.atStartOfDayIn(TimeZone.UTC)
        ?.toEpochMilliseconds()
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = initialMillis,
    )
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = { showDatePicker = false },
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        containerColor = colors.bg,
        dragHandle = null,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Grab handle
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .size(width = 36.dp, height = 4.dp)
                        .clip(RoundedCornerShape(50))
                        .background(colors.borderStrong),
                )
            }
            Text(
                text = stringResource(Res.string.edit_date_label),
                style = type.title3,
                color = colors.text,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            // Quick chips row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                MmChip(
                    selected = datePickerState.selectedDateMillis?.let {
                        kotlin.time.Instant.fromEpochMilliseconds(it)
                            .toLocalDateTime(TimeZone.UTC).date
                    } == todayDate,
                    onClick = {
                        onIntent(TransactionEditIntent.DateChanged(todayDate))
                        showDatePicker = false
                    },
                ) {
                    Text(
                        text = stringResource(Res.string.edit_date_today),
                        style = type.caption,
                        color = colors.text,
                        maxLines = 1,
                    )
                }
                MmChip(
                    selected = datePickerState.selectedDateMillis?.let {
                        kotlin.time.Instant.fromEpochMilliseconds(it)
                            .toLocalDateTime(TimeZone.UTC).date
                    } == yesterdayDate,
                    onClick = {
                        onIntent(TransactionEditIntent.DateChanged(yesterdayDate))
                        showDatePicker = false
                    },
                ) {
                    Text(
                        text = stringResource(Res.string.edit_date_yesterday),
                        style = type.caption,
                        color = colors.text,
                        maxLines = 1,
                    )
                }
            }
            // The actual M3 DatePicker (just the calendar widget)
            DatePicker(
                state = datePickerState,
                showModeToggle = false,
                headline = null,
                title = null,
            )
            // Confirm button
            MmButton(
                text = stringResource(Res.string.edit_date_ok),
                onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val instant = kotlin.time.Instant.fromEpochMilliseconds(millis)
                        val localDate = instant.toLocalDateTime(TimeZone.UTC).date
                        onIntent(TransactionEditIntent.DateChanged(localDate))
                    }
                    showDatePicker = false
                },
                variant = MmButtonVariant.Accent,
                size = MmButtonSize.Lg,
                fullWidth = true,
            )
            Spacer(Modifier.height(8.dp))
        }
    }
}
```

### Step 4: Remove clickable from old MmField and ensure MmIcons.calendar exists
Check if `MmIcons.calendar` exists in `core/ui/src/commonMain/kotlin/com/dv/moneym/core/ui/MmIcons.kt`. If not, use `MmIcons.info` or another available icon as a placeholder. Check by reading that file first.

If `MmIcons.calendar` does not exist, add it to `MmIcons.kt` using a vector path or use `MmIcons.folder` as fallback (without leading icon is also fine).

### Step 5: Remove unused import
Remove `MmField` import if it's no longer used elsewhere in the file (check—it may still be used for the note field). Keep if used.

### Step 6: Ensure all imports are present
Add/keep:
- `androidx.compose.material3.ModalBottomSheet`
- `androidx.compose.material3.rememberModalBottomSheetState`
- `com.dv.moneym.core.ui.MmChip`
- `com.dv.moneym.core.ui.MmButton`
- `com.dv.moneym.core.ui.MmButtonVariant`
- `com.dv.moneym.core.ui.MmButtonSize`

## Acceptance Criteria
1. Date input shows as a button (not a text field) labeled with "Today", "Yesterday", or the formatted date
2. Two quick chips show actual calendar dates (e.g. "May 17" and "May 16"), not "Today"/"Yesterday" labels
3. Clicking either chip selects that date immediately (no need to open picker)
4. Clicking the date button opens the date picker (bottom sheet, dark/light-themed)
5. The date picker bottom sheet has grab handle, title, quick chips for Today/Yesterday, calendar widget, and a confirm button
6. Confirm button uses `MmButton` with `Accent` variant
7. Build compiles: `./gradlew :composeApp:assembleDebug`
