# Phase 2 — Transaction Edit: Date Picker Fix + Category Required Validation

## Items covered
- Item 3: Transaction new/edit — date picker (clickable field, always shows date, calendar dialog)
- Item 4: Transaction new/edit — category required validation error message

## Problem analysis

### Item 3: Date picker
Currently the date field uses `MmField` (a text input) with a clickable overlay. The field is still an input field visually, and it doesn't prevent text entry. Need to:
- Make it a read-only styled "clickable display field" (not a text input, just a styled Box/Row that looks like a field)
- Always display a date (even when "Today" or "Yesterday" is selected via shortcuts)
- The date displayed should always be the currently selected date formatted

The `state.date` is set to `clock.today()` at init in `TransactionEditViewModel`, so it is never null for new transactions. The date picker dialog already works. The main fix: replace `MmField` for date with a non-editable clickable field that always shows the date text and always opens the picker.

Also, the displayed text should always show the friendly date (Today/Yesterday or the formatted date) — currently this already works but can be confirmed by checking `dateText` derivation which already has that logic.

The calendar dialog should use design system colors. Currently it uses the default Material3 `DatePickerDialog` which uses M3 color scheme. To apply design system colors, wrap with custom `DatePickerDefaults.colors(...)` or theme the dialog container.

### Item 4: Category validation error  
`categoryError` flag already exists in `TransactionEditUiState` and is set in `TransactionEditViewModel.save()`. It just needs to be visually shown in `TransactionEditScreen.kt`. Currently there is no error display code for `categoryError`.

The `edit_category_error` string already exists in strings.xml: "Select a category".

Need to: after the `CategoryPicker` composable, show an error text if `state.categoryError == true`. Style it like a red/danger text.

## Files to modify

1. `/Users/davidvalic/Developer/MoneyM2/feature/transactionEdit/src/commonMain/kotlin/com/dv/moneym/feature/transactionedit/ui/TransactionEditScreen.kt`

### Change 1: Replace MmField date with clickable display field
In `TransactionEditScrollBody`, replace the `MmField` for date (around line 271-280) with a custom clickable Box that looks like a field but is not editable. The box should:
- Show label "DATE" (uppercase micro style) above
- Show the `dateText` value
- Open picker on tap anywhere
- Style matching MmField (border, background, corner radius, padding from MM design system)

### Change 2: Add category error display
In `CategoryPicker` composable (called from `TransactionEditScrollBody`), pass `categoryError: Boolean` param.
After the `FlowRow` of chips, if `categoryError == true`, show:
```kotlin
if (categoryError) {
    Spacer(Modifier.height(6.dp))
    Text(
        text = stringResource(Res.string.edit_category_error),
        style = type.caption,
        color = colors.danger,
    )
}
```

Update the call site in `TransactionEditScrollBody` to pass `categoryError = state.categoryError`.

### Change 3: Date picker dialog theming
The existing `DatePickerDialog` can stay largely as-is. To apply design system colors, provide `colors = DatePickerDefaults.colors(...)` with MM colors mapped:
- `containerColor = colors.bg`  
- `titleContentColor = colors.text`
- `headlineContentColor = colors.text`
- `weekdayContentColor = colors.text2`
- `subheadContentColor = colors.text2`
- `yearContentColor = colors.text`
- `currentYearContentColor = colors.accent`
- `selectedYearContentColor = colors.bg`
- `selectedYearContainerColor = colors.accent`
- `selectedDayContentColor = colors.bg`
- `selectedDayContainerColor = colors.accent`
- `todayContentColor = colors.accent`
- `todayDateBorderColor = colors.accent`
- `dayContentColor = colors.text`
- `navigationContentColor = colors.text`

Pass these to `DatePicker(state = datePickerState, colors = themedColors)`.

## Detailed implementation

### Clickable date display field

```kotlin
// Replace MmField for date with:
val dateColors = MM.colors
val dateType = MM.type
val dateRadius = MM.radius

Column(modifier = Modifier.fillMaxWidth()) {
    Text(
        text = stringResource(Res.string.edit_date_label).uppercase(),
        style = dateType.micro,
        color = dateColors.text3,
        modifier = Modifier.padding(bottom = 4.dp),
    )
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(dateRadius.sm)
            .background(dateColors.surface2)
            .border(1.dp, dateColors.border, dateRadius.sm)
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
            ) { onDatePickerOpen() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Text(
            text = dateText,
            style = dateType.body,
            color = if (dateText.isEmpty()) dateColors.text3 else dateColors.text,
        )
    }
}
Spacer(Modifier.height(12.dp))
```

Note: We need to check what MM.radius.sm looks like. Based on the design system pattern, use `MM.radius.sm` or `RoundedCornerShape(8.dp)` consistently.

Also ensure `dateText` is always non-empty — since `state.date` is initialized to `clock.today()`, it should always have a value. If null somehow, show placeholder text.

## Acceptance criteria
- Date field is a styled, non-editable clickable box (not a BasicTextField/MmField text input)
- Tapping the date field always opens the calendar dialog
- The displayed date always shows even when Today or Yesterday shortcuts are used
- Calendar dialog uses design system accent color for selected day and today indicator
- When user tries to save without a category, an error message "Select a category" appears below the category chips in danger/red color
- Error clears when user selects a category
- Android build compiles without errors
