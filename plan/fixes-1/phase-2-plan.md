# Phase 2 — TX list display settings cleanup

File: `feature/settings/src/commonMain/kotlin/com/dv/moneym/feature/settings/overview/transactiondisplay/TxListDisplayScreen.kt`

Three fixes.

## 2a. Remove duplicate "Daily totals" toggle
In `ItemDetailsShowSection` (the SHOW section, ~lines 336–395) delete the `MmRow` block for `settings_txdisplay_daily_sums` (~lines 379–393). It is a duplicate — the canonical one stays in `ExtraShowOptions`.
After deletion, the "Note / description" `MmRow` (~lines 364–378) is the last row in that card → change its `divider = true` to `divider = false`.

## 2b. Distinct section header + larger separator
`ExtraShowOptions` (~lines 460–508) currently reuses `Res.string.settings_txdisplay_show` ("SHOW") for its `SectionLabel`. This section actually controls transaction-list behavior.
- Change that `SectionLabel`'s text to a NEW resource `Res.string.settings_txdisplay_list_section` ("TRANSACTION LIST"). Add the import line `import moneym.feature.settings.generated.resources.settings_txdisplay_list_section` (alphabetical with the other generated imports).
- Increase visual separation before this section: bump the `SectionLabel` modifier `top` padding from `MM.dimen.padding_2x` to `MM.dimen.padding_3x` (matches the bottom spacer the screen already uses).
- Keep `ItemDetailsShowSection` header as `settings_txdisplay_show`.

Add the string to all 4 locale files under `feature/settings/src/commonMain/composeResources/`. Place it next to the existing `settings_txdisplay_show` entry.
- `values/strings.xml`: `<string name="settings_txdisplay_list_section">TRANSACTION LIST</string>`
- `values-de/strings.xml`: `TRANSAKTIONSLISTE`
- `values-es/strings.xml`: `LISTA DE TRANSACCIONES`
- `values-it/strings.xml`: `ELENCO TRANSAZIONI`

## 2c. Fixed-height preview (stop screen jump)
`ItemPreviewPanel` (~lines 178–220): the preview `MmCard` of `TxRow`s changes height as `showNote`/`density`/`indicatorStyle` toggle, jumping the screen. Reserve the maximum possible height once and top-align the live content.

Use a `SubcomposeLayout` that measures a hidden "ghost" preview built with maximal prefs, then lays out the real preview at the top within that fixed height:

```kotlin
@Composable
private fun MaxHeightPreview(
    currentPrefs: TxDisplayPrefs,
    modifier: Modifier = Modifier,
) {
    val maxedPrefs = TxDisplayPrefs(
        indicatorStyle = IndicatorStyle.IconTile,
        showCategoryName = true,
        showNote = true,
        density = Density.Comfortable,
        showDailySums = true,
    )
    SubcomposeLayout(modifier) { constraints ->
        val ghost = subcompose("ghost") { PreviewCard(maxedPrefs) }
            .map { it.measure(constraints) }
        val maxH = ghost.maxOfOrNull { it.height } ?: 0
        val real = subcompose("real") { PreviewCard(currentPrefs) }
            .map { it.measure(constraints) }
        val width = real.maxOfOrNull { it.width } ?: constraints.minWidth
        layout(width, maxH) {
            real.forEach { it.place(0, 0) } // top-aligned; empty space below
        }
    }
}

@Composable
private fun PreviewCard(prefs: TxDisplayPrefs) {
    MmCard {
        sampleTransactions.forEachIndexed { i, tx ->
            TxRow(
                categoryName = tx.categoryName,
                categoryColor = tx.categoryColor,
                categoryIcon = Icon.Basket.imageVector,
                note = tx.note,
                isExpense = tx.isExpense,
                amountValue = tx.amount,
                currency = tx.currency,
                prefs = prefs,
                divider = i < sampleTransactions.size - 1,
            )
        }
    }
}
```

In `ItemPreviewPanel`, replace the inline `MmCard { sampleTransactions.forEach... }` (~lines 203–217) with `MaxHeightPreview(currentPrefs = currentPrefs)`. Keep the `SectionLabel` "Preview" header above it (it is NOT part of the reserved height). Add imports `androidx.compose.ui.layout.SubcomposeLayout`. Density/IndicatorStyle are already imported.

Note: the ghost subcompose is laid out (so it composes) but never placed → not drawn. Acceptable cost (3 sample rows).

## Constraints
- Import classes, no FQNs. No comments unless why is non-obvious.

## Verify
`./gradlew :feature:settings:compileDebugKotlinAndroid` then `:composeApp:assembleDebug`.
