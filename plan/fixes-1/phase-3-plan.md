# Phase 3 — Category bar chart selected bar turns green

File: `feature/overview/src/commonMain/kotlin/com/dv/moneym/feature/overview/components/MonthlySpendingBarChart.kt`

In `CategoryBarChart`, the `BarColors(...)` passed to `BarChart` (~lines 165–170) uses the category color for the selected bar. Total-spending mode (`MonthlySpendingBarChart`) uses `colors.accent` (green) for selected. Make category mode match.

Change only the `selected` line:
- from: `selected = barColor,`
- to:   `selected = colors.accent,`

Leave `current = barColor.copy(alpha = 0.85f)`, `other = barColor.copy(alpha = 0.35f)`, `avg = barColor.copy(alpha = 0.50f)` unchanged — non-selected bars keep the category color. `colors` (= `MM.colors`) is already in scope (~line 138).

## Verify
`./gradlew :feature:overview:compileDebugKotlinAndroid`
