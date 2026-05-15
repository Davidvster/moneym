# Phase 4 — Add / Edit Transaction Screen

Migrate `TransactionEditScreen` to the new design. Covers both "New" and "Edit" modes.

---

## Files to modify

- `feature/transactionEdit/src/commonMain/…/ui/TransactionEditScreen.kt` — full rewrite

No ViewModel changes needed (existing state already has: amount, type, category, note, date, isEditing).

---

## Layout (from `screens.jsx` → `AddTxScreen`)

```
Column {
  // Modal header (52dp)
  Row(4dp 12dp 12dp padding) {
    MmIconButton(MmIcons.close)
    Text(title, style=MM.type.title3, modifier=weight(1f), textAlign=center)
    if (isEditing) MmIconButton(MmIcons.trash, variant=danger)
    else           Spacer(40dp)
  }

  // Scrollable body
  Column(scroll, 8dp 20dp 100dp padding) {
    // Expense / Income segmented
    MmSegmented(["Expense","Income"], txType, fullWidth)

    Spacer(24dp)

    // Big amount display
    Column(align=center, 12 0 24dp padding) {
      Text("AMOUNT", style=MM.type.micro, color=text3)
      Spacer(8dp)
      Row(align=baseline, gap=10dp) {
        Text(currencyCode, style=MM.type.bodyMono.copy(color=text3))
        Text(
          text = if (amount == 0.0) "0.00" else formattedAmount,
          style = MM.type.display.copy(52.sp, color = if(amount==0.0) text3 else text)
        )
      }
    }

    // Fields
    MmField(label="Date", value=formattedDate)
    Spacer(12dp)
    MmField(label="Note (optional)", value=note, placeholder="Add a note…")

    Spacer(24dp)

    // Category picker
    Text("CATEGORY", style=MM.type.micro, color=text3)
    Spacer(12dp)
    FlowRow(gap=8dp) {
      categories.forEach { cat ->
        MmChip(
          selected = cat.id == selectedCategoryId,
          leading = { CategoryIconTile(cat, 20dp, IndicatorStyle.IconTile) },
          onClick = { onCategorySelected(cat.id) }
        ) { Text(cat.name) }
      }
    }
  }

  // Pinned save bar
  Box(1dp divider top, bg=MM.colors.bg, 12 16 16 padding) {
    MmButton(
      text = if (isEditing) "Save changes" else "Add transaction",
      variant = accent,
      size = lg,
      fullWidth = true,
      leading = MmIcons.check,
    )
  }
}
```

---

## Amount input

The amount field is a **display** — tapping it opens a numeric keyboard (system keyboard, `keyboardType = KeyboardType.Decimal`). The value in the `display` composable updates as the user types.

- When amount is 0.00: text color = `text3`
- When amount > 0: text color = `text`
- Format: always 2 decimal places.

Alternatively, re-use the existing keypad approach if one exists — check `TransactionEditViewModel` for the current input method before deciding.

---

## Delete flow

- Trash icon only visible in edit mode.
- Tap → `AlertDialog` (M3 is acceptable here — one component that's not worth custom-building): "Delete this transaction? This can't be undone." → accent/danger buttons.
- On confirm: `onDeleteConfirmed()` → ViewModel calls repo.delete, pops back.

---

## Category chips vs sheet

The design shows inline chips (wrapping row) for category selection. If the category list is long, the wrapping FlowRow scrolls as part of the main scroll. There is also a `CategoryPickerScreen` sheet (Phase 3 artboard 08) — use the chip approach as primary, the sheet is a fallback for many categories. Implement chips for now; sheet can be added in Phase 7.

---

## Key implementation notes

- `FlowRow` from `androidx.compose.foundation.layout` (stable in Compose 1.7+). If not available in the KMP target version, use a custom `Layout` that wraps chips.
- Date field: tapping opens a date picker. Use `DatePickerDialog` from Material3 (acceptable single usage). Format with `LocalDate` formatting helper from `core/common`.
- Currency code shown next to amount comes from `AppSettingsRepository.observeDefaultCurrency()` — pass it into the screen via ViewModel state.

---

## Verification
1. "New transaction" screen opens with 0.00 in text3 color.
2. Typing an amount updates display in real time.
3. Selecting a category fills the chip and marks it selected.
4. "Add transaction" saves and pops back; transaction appears in the list.
5. "Edit transaction" pre-fills all fields; trash icon triggers delete dialog.
6. Dark mode: amount display, chips, save button all correct colors.
