# Phase 2 — Payment types + budgets delete/add UX

Repo: /Users/davidvalic/Developer/MoneyM. Touches `feature/settings` and
`feature/budgets`. Reuse existing components — do NOT invent new ones.

Shared danger color token: **`MM.colors.danger`** (already used in this file at
line 114 and by `MmButtonVariant.Danger` / `MmIconButtonVariant.Danger`).

---

## Fix 1 — Payment-mode delete button = red trash icon + confirm bottom sheet

File: `feature/settings/src/commonMain/kotlin/com/dv/moneym/feature/settings/paymentmodes/PaymentModeListScreen.kt`

**1a. Replace the hardcoded "Delete" TextButton (lines 110-116)** inside the
`MmRow` of each item with a red trash icon button using the existing
`MmIconButton`:
```kotlin
MmIconButton(
    icon = Icon.Trash.imageVector,
    onClick = { onDeleteClick(mode) },
    variant = MmIconButtonVariant.Danger,
    contentDescription = stringResource(Res.string.settings_payment_mode_delete_title),
)
```
Imports to add: `com.dv.moneym.core.ui.MmIconButton`,
`com.dv.moneym.core.ui.MmIconButtonVariant`.
(`MmIconButton` is in `core/ui/.../MmIconButton.kt`; `Danger` variant tints with
`colors.danger`.)

**1b. Replace the delete `AlertDialog` (lines 157-185)** in the `when (val dialog
= state.dialogState)` block, branch `is PaymentModeDialogState.DeleteConfirm`,
with the shared `MmDeleteSheet` (in `core/ui/.../MmDeleteSheet.kt`):
```kotlin
is PaymentModeDialogState.DeleteConfirm -> {
    MmDeleteSheet(
        title = stringResource(Res.string.settings_payment_mode_delete_title),
        body = stringResource(Res.string.settings_payment_mode_delete_message, dialog.name),
        cancelText = stringResource(Res.string.settings_payment_mode_cancel),
        confirmText = stringResource(Res.string.settings_payment_mode_delete_confirm),
        onConfirm = { onConfirmDelete(dialog.id) },
        onCancel = onDismissDialog,
    )
}
```
Import: `com.dv.moneym.core.ui.MmDeleteSheet`. `MmDeleteSheet` is
`@OptIn(ExperimentalMaterial3Api::class)` internally — the call site does not
need the annotation.

---

## Fix 2 — Add/Rename payment type = bottom sheet (not AlertDialog)

Same file. Convert `NameInputDialog` (lines 209-257) from an `AlertDialog` into a
bottom sheet that matches the app's other sheets. Model the structure on
`ImportSourceSheet.kt` (drag-handle box + `MmSheetHeader` + content + bottom
spacer) and use `MmField` for the input.

Rewrite `NameInputDialog` as:
```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NameInputSheet(
    title: String,
    initialName: String,
    placeholder: String,
    confirmText: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    val colors = MM.colors
    var name by remember(initialName) { mutableStateOf(initialName) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = MM.dimen.padding_2_5x, topEnd = MM.dimen.padding_2_5x),
        containerColor = colors.bg,
        dragHandle = null,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .padding(horizontal = MM.dimen.padding_2_5x, vertical = MM.dimen.padding_3x),
            verticalArrangement = Arrangement.spacedBy(MM.dimen.padding_2x),
        ) {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Box(
                    Modifier.size(width = 36.dp, height = MM.dimen.padding_0_5x)
                        .clip(RoundedCornerShape(50))
                        .background(colors.borderStrong),
                )
            }
            MmSheetHeader(title = title, onClose = onDismiss)
            MmField(
                value = name,
                onValueChange = { name = it },
                placeholder = placeholder,
            )
            MmButton(
                text = confirmText,
                onClick = { if (name.isNotBlank()) onConfirm(name) },
                variant = MmButtonVariant.Primary,
                size = MmButtonSize.Lg,
                fullWidth = true,
            )
            Spacer(Modifier.height(MM.dimen.padding_1x))
        }
    }
}
```
Update the two call sites (`PaymentModeDialogState.Add` ~line 137 and
`.Rename` ~line 147) to call `NameInputSheet` instead of `NameInputDialog`,
passing `confirmText = stringResource(Res.string.settings_payment_mode_save)`.

Imports to add: `MmField`, `MmSheetHeader`, `MmButtonSize`, `MmButtonVariant`,
`ModalBottomSheet`, `rememberModalBottomSheetState`, `ExperimentalMaterial3Api`,
`RoundedCornerShape`, `Box`, `Spacer`, `Alignment`, `clip`, `background`,
`imePadding`, `size`, `height`, `Arrangement`, `dp`. (All from the same packages
used in `ImportSourceSheet.kt` / `MmDeleteSheet.kt` — copy those import lines.)

Remove now-unused imports: `AlertDialog`, `TextButton`, `TextField`,
`TextFieldDefaults` (verify none remain used after the edits).

**Dumb-UI note:** dialog visibility stays VM-driven via `PaymentModeDialogState`
(unchanged). The transient text value may remain a local `remember` (as it is
today) — that's an acceptable in-sheet form transient.

---

## Fix 3 — Budgets trash icon red

File: `feature/budgets/src/commonMain/kotlin/com/dv/moneym/feature/budgets/list/components/BudgetRow.kt`

Replace the M3 `IconButton` + `M3Icon` (lines 53-59) with the shared red icon
button for consistency:
```kotlin
MmIconButton(
    icon = Icon.Trash.imageVector,
    onClick = onDelete,
    variant = MmIconButtonVariant.Danger,
    contentDescription = stringResource(Res.string.budgets_delete),
)
```
- Add imports: `com.dv.moneym.core.ui.MmIconButton`,
  `com.dv.moneym.core.ui.MmIconButtonVariant`,
  `moneym.feature.budgets.generated.resources.Res`,
  `moneym.feature.budgets.generated.resources.budgets_delete`,
  `org.jetbrains.compose.resources.stringResource`.
- Remove the hardcoded `contentDescription = "Delete"` (now localized).
- Remove now-unused `IconButton` and `M3Icon` imports if nothing else in the
  file uses them (check first).
- `budgets_delete` key already exists in all budgets locales (it's the delete
  label). If for some reason it's missing in a locale, that's out of scope here.

---

## Verify
```bash
./gradlew :feature:settings:compileDebugKotlinAndroid :feature:budgets:compileDebugKotlinAndroid
```
Both must compile. Report files changed, the compile result, and any deviation
from this plan. Do NOT commit — the main thread commits.
