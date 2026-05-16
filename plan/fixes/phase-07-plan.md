# Phase 7: Overview Amount Overflow

## Problem
In the Overview screen, the income and expense cards are side-by-side in a Row with `weight(1f)` each. When the total amount is large (e.g., `12,345.67`), the `MmMoney` text can overflow or wrap, making the layout look broken.

## Files to modify
- `feature/overview/src/commonMain/kotlin/com/dv/moneym/feature/overview/ui/OverviewScreen.kt` — fix the income and expenses cards layout

## Implementation steps

### Option chosen: Switch income/expenses from two-column to two-row layout

This is the simplest and most readable approach. Instead of two cards side-by-side, stack them vertically as a single card with two rows:

```kotlin
// Replace the two-card Row with a single card containing two rows:
MmCard(
    modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp),
    padded = true,
    shape = MM.radius.md,
) {
    // Income row
    Row(
        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(MmIcons.arrowDown, contentDescription = null, tint = colors.accent, modifier = Modifier.size(14.dp))
        SectionLabel(stringResource(Res.string.overview_label_income), modifier = Modifier.weight(1f))
        MmMoney(
            value = state.income,
            size = 17.sp,
            weight = FontWeight.SemiBold,
            color = colors.accent,
        )
    }
    // Divider
    HorizontalDivider(color = colors.divider, thickness = 1.dp)
    Spacer(Modifier.height(12.dp))
    // Expenses row
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(MmIcons.arrowUp, contentDescription = null, tint = colors.text, modifier = Modifier.size(14.dp))
        SectionLabel(stringResource(Res.string.overview_label_expenses), modifier = Modifier.weight(1f))
        MmMoney(
            value = state.expenses,
            size = 17.sp,
            weight = FontWeight.SemiBold,
        )
    }
}
```

This gives each amount the full remaining width after the label, preventing overflow on any reasonable amount.

Alternative (auto-size text): Could use `basicMarquee` or auto-size, but the two-row layout is cleaner and more consistent with the design system.

Note: The `MmMoney` composable accepts `currency` — check if it needs to be passed here. Looking at the OverviewScreen, the cards already determine currency from `state.totalIncome/totalExpense`. For the new layout, use the `currencyCode` variable already computed in `OverviewContent`.

Update `MmMoney` calls to include `currency = currencyCode` if the existing `MmMoney` signature requires it.

## Acceptance criteria
- [ ] Income and expenses amounts never overflow or clip on any reasonable amount (up to 7 figures)
- [ ] The layout is clean and consistent with the app design language
- [ ] Income shows in accent color, expenses in text color
- [ ] Dark and light mode both look correct
