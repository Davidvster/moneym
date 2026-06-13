# Phase 6 — Top-right X close on ALL bottom sheets (task 8)

## Goal
Every `ModalBottomSheet` in the app gets a top-right **X** close affordance so it's obvious it can be dismissed. Introduce a shared `MmSheetHeader` and retrofit all 15 sheets.

## Step 1 — new string `mm_close` = "Close" (core/ui, all 28 locales)
`core/ui/src/commonMain/composeResources/values{,-<loc>}/strings.xml` (insert near other generic keys):

| loc|val|loc|val |
|---|---|---|---|
|values|Close|nb|Lukk|
|ar|إغلاق|nl|Sluiten|
|cs|Zavřít|pl|Zamknij|
|da|Luk|pt|Fechar|
|de|Schließen|ru|Закрыть|
|es|Cerrar|sk|Zavrieť|
|et|Sulge|sl|Zapri|
|fi|Sulje|sv|Stäng|
|fr|Fermer|tr|Kapat|
|hi|बंद करें|vi|Đóng|
|hr|Zatvori|zh|关闭|
|hu|Bezárás|is|Loka|
|it|Chiudi|ja|閉じる|
|lt|Uždaryti|lv|Aizvērt|
|mk|Затвори|||

## Step 2 — new component `core/ui/.../MmSheetHeader.kt`
(Add the `mm_close` strings FIRST so codegen resolves the accessor.)
```kotlin
package com.dv.moneym.core.ui

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.dv.moneym.core.designsystem.MM
import com.dv.moneym.core.model.Icon
import moneym.core.ui.generated.resources.Res
import moneym.core.ui.generated.resources.mm_close
import org.jetbrains.compose.resources.stringResource

@Composable
fun MmSheetHeader(
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    title: String? = null,
) {
    Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        if (title != null) {
            Text(text = title, style = MM.type.title3, color = MM.colors.text, modifier = Modifier.weight(1f))
        } else {
            Spacer(Modifier.weight(1f))
        }
        MmIconButton(
            icon = Icon.Close.imageVector,
            onClick = onClose,
            contentDescription = stringResource(Res.string.mm_close),
        )
    }
}
```
(`imageVector` is `com.dv.moneym.core.ui.imageVector`, same module — no import needed.)

## Step 3 — retrofit the 15 sheets
For each `ModalBottomSheet`, find the dismiss lambda passed to `onDismissRequest` (call it `dismiss`). Then, **at the top of the sheet's content Column**:
- **Default**: replace the existing title `Text` with `MmSheetHeader(title = <that title string>, onClose = dismiss)`. Keep any existing drag-pill `Box` above the header.
- **If there is no title**: insert `MmSheetHeader(onClose = dismiss)` (title = null) as the first child.
- **If a title row already hosts other actions** (e.g. `MmCategoryPickerSheet`'s "Clear" button): keep that row, and **append** a close button `MmIconButton(icon = Icon.Close.imageVector, onClick = dismiss, contentDescription = stringResource(Res.string.mm_close))` at the row's end (do not also add `MmSheetHeader`).

Files (verify each by reading it; titles below are hints):
- `core/ui/.../MmCategoryPickerSheet.kt` — title row already has optional Clear → append X to that row. (`Res` already imported.)
- `core/ui/.../MmWalletPickerSheet.kt` — has optional title param; add `MmSheetHeader(title = title, onClose = onDismiss)`.
- `core/ui/.../MmDeleteSheet.kt` — title present; `MmSheetHeader(title = title, onClose = onCancel)`.
- `core/ui/.../HsvColorPickerDialog.kt` — add header with its dismiss.
- `feature/banksync/.../suggestions/BankSuggestionsScreen.kt` — `FilterSheet`: title "Filter" present; `MmSheetHeader(title = ..., onClose = { onIntent(BankSuggestionsIntent.ShowFilterSheet(false)) })`.
- `feature/transactions/.../list/TransactionListScreen.kt` — `SyncStatusSheet`: now has section headers (Cloud/Bank). Add `MmSheetHeader(onClose = onDismiss)` as the first child (title = null) above the sections.
- `feature/categories/.../list/CategoryListScreen.kt`
- `feature/categories/.../list/components/DeleteAllTransactionsConfirmSheet.kt`
- `feature/categories/.../list/components/DeleteCategoryOptionsSheet.kt`
- `feature/categories/.../list/components/MigratePickerSheet.kt`
- `feature/settings/.../overview/importdata/ImportDataScreen.kt`
- `feature/settings/.../overview/importdata/ImportSourceSheet.kt`
- `feature/settings/.../wallet/EditWalletCurrencyScreen.kt`
- `feature/transactionEdit/.../components/CalculatorBottomSheet.kt`
- `feature/aianalysis/.../AnalyzeScreen.kt`

Each feature-module file needs `import com.dv.moneym.core.ui.MmSheetHeader` (already depends on core/ui). Where you append a raw close `MmIconButton` instead, import `MmIconButton`, `com.dv.moneym.core.model.Icon`, `com.dv.moneym.core.ui.imageVector`, and the module's `Res` + `mm_close`… — but `mm_close` lives in **core/ui** resources, NOT each feature module's `Res`. So the close content-description must come from core/ui. **Therefore prefer `MmSheetHeader`** (which already wires the core/ui `mm_close`) for all feature-module sheets, and only use the raw-append variant inside `core/ui` itself (MmCategoryPickerSheet), where `Res.string.mm_close` is accessible. For the category picker in core/ui, append `MmIconButton(... contentDescription = stringResource(Res.string.mm_close))`.

## Verify
- `grep -rL mm_close core/ui/src/commonMain/composeResources/*/strings.xml` → nothing
- Build every touched module + the app:
  `./gradlew :core:ui:compileDebugKotlinAndroid :feature:banksync:compileDebugKotlinAndroid :feature:transactions:compileDebugKotlinAndroid :feature:categories:compileDebugKotlinAndroid :feature:settings:compileDebugKotlinAndroid :feature:transactionEdit:compileDebugKotlinAndroid :feature:aianalysis:compileDebugKotlinAndroid`
