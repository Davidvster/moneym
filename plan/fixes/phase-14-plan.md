# Phase 14: Delete Transaction Dialog Design

## Problem
The delete confirmation dialog in `TransactionEditScreen.kt` uses Material3's `AlertDialog` with default styling that doesn't match the app's design language. The app uses a custom design system (`MM.colors`, `MM.type`, custom buttons like `MmButton`).

The current dialog:
```kotlin
AlertDialog(
    title = { Text("Delete transaction") },
    text = { Text("This can't be undone.") },
    confirmButton = { TextButton(onClick = {...}) { Text("Delete", color = colors.danger) } },
    dismissButton = { TextButton(onClick = {...}) { Text("Cancel") } },
)
```

The app's category screen uses a `ModalBottomSheet` for delete confirmation (`DeleteConfirmSheet` in `CategoryListScreen.kt`). For transactions, use the same bottom sheet pattern.

## Files to modify
- `feature/transactionEdit/src/commonMain/kotlin/com/dv/moneym/feature/transactionedit/ui/TransactionEditScreen.kt` — replace `AlertDialog` delete confirmation with a `ModalBottomSheet` styled like `DeleteConfirmSheet` in the categories screen
- `feature/transactionEdit/src/commonMain/composeResources/values/strings.xml` — add string keys for delete dialog content if missing
- (locale variants)

## Implementation steps

1. Remove the `AlertDialog` block.

2. Add a `ModalBottomSheet` for delete confirmation, styled exactly like the category `DeleteConfirmSheet`:

```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TransactionDeleteSheet(
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
) {
    val colors = MM.colors
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onCancel,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        containerColor = colors.bg,
        dragHandle = null,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Grab handle
            Box(
                modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
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
                text = stringResource(Res.string.edit_delete_confirm_title),
                style = MM.type.title3,
                color = colors.text,
                textAlign = TextAlign.Center,
            )
            Text(
                text = stringResource(Res.string.edit_delete_confirm_body),
                style = MM.type.caption,
                color = colors.text2,
                textAlign = TextAlign.Center,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                MmButton(
                    text = stringResource(Res.string.edit_delete_confirm_cancel),
                    onClick = onCancel,
                    variant = MmButtonVariant.Secondary,
                    modifier = Modifier.weight(1f),
                )
                MmButton(
                    text = stringResource(Res.string.edit_delete_confirm_ok),
                    onClick = onConfirm,
                    variant = MmButtonVariant.Danger,
                    modifier = Modifier.weight(1f),
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}
```

3. In `TransactionEditContent`, replace the `AlertDialog` with:
```kotlin
if (showDeleteDialog) {
    TransactionDeleteSheet(
        onConfirm = {
            onIntent(TransactionEditIntent.DeleteConfirmed)
            showDeleteDialog = false
        },
        onCancel = { showDeleteDialog = false },
    )
}
```

4. String resources: the existing strings.xml already has:
   - `edit_delete_confirm_title`: "Delete transaction?"
   - `edit_delete_confirm_body`: "This cannot be undone."
   - `edit_delete_confirm_ok`: "Delete"
   - `edit_delete_confirm_cancel`: "Cancel"
   
   Use these in the sheet.

5. Import `ModalBottomSheet`, `rememberModalBottomSheetState`, `RoundedCornerShape` (already imported in the file for the date picker).

## Acceptance criteria
- [ ] Tapping the trash icon in the edit screen opens a bottom sheet (not an AlertDialog)
- [ ] The bottom sheet has the drag handle, title, body text, Cancel and Delete buttons
- [ ] Buttons use `MmButton` with Secondary and Danger variants respectively
- [ ] The sheet matches the design of the category delete confirmation sheet
- [ ] Confirming deletes the transaction and dismisses
- [ ] Cancelling dismisses without deleting
- [ ] Sheet appearance is consistent in dark and light mode
