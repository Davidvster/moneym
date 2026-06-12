# Phase 3 — Overview: filter-aware avg stats, categories, monthly charts

Filter enum: `SpendingFilter { All, Expenses, Income }` (`core/model/.../SpendingFilter.kt`). It lives in `OverviewViewModel._spendingFilter` and is already passed down to page composables (`OverviewPeriodBody` uses it in `resolveAnimCats`). Strategy: **use cases always compute expense + income + net variants; the UI picks what to render based on the filter** (same pattern as the existing category breakdown).

## A) Avg stats respect filter (user task 4)

`feature/overview/.../usecase/BuildOverviewPageStateUseCase.kt`:
- Today only `expensesDouble` is computed (lines ~44-47) feeding `avgDailyExpense` (month mode, line ~147), `avgMonthlyExpense` + `avgDailyExpenseYear` (year mode, lines ~173-174).
- Add `incomeDouble` (sum of `TransactionType.INCOME`) and `netDouble = incomeDouble - expensesDouble` and populate new UiState fields.

`feature/overview/.../page/OverviewPageUiState.kt` — add:
```kotlin
val avgDailyIncome: Double = 0.0
val avgMonthlyIncome: Double = 0.0
val avgDailyIncomeYear: Double = 0.0
val avgDailyNet: Double = 0.0
val avgMonthlyNet: Double = 0.0
val avgDailyNetYear: Double = 0.0
```

`feature/overview/.../components/AvgStatsCard.kt` — add `spendingFilter: SpendingFilter` param:
- `Expenses` → exactly current rendering (expense avgs).
- `Income` → same layout but income avgs (labels: avg income / day, avg income / month).
- `All` → expense, income, and net rows (net can be negative — format with sign).
- Caller is in `OverviewPeriodBody.kt` (which already has the active filter — pass it through).

## B) Categories respect filter (user task 5a)

1. `OverviewPeriodBody.kt` `resolveAnimCats` (lines ~264-277) already branches on filter for the donut/legend. **Trace why the user still sees only expense categories**: verify the `activeFilter` parameter passed into `SpendingByCategoryCard` is the live `spendingFilter` from `OverviewUiState` and not a hardcoded/stale value, and that `categoryIncomeBreakdown` is actually populated (check `BuildOverviewPageStateUseCase` lines ~61-76 and `BuildCategoryBreakdownUseCase`). Fix whatever breaks the chain.
2. `BuildCategoryTrendsUseCase.kt` — `daily()`, `monthly()`, `range()` all hardcode `it.type == TransactionType.EXPENSE`. Change each to take a `types: Set<TransactionType>` parameter and filter with `it.type in types`. Compute trends for the expense set and income set in `BuildOverviewPageStateUseCase` and expose both in UiState (e.g. `categoryDailyTrend` stays expense, add `categoryIncomeDailyTrend`, `categoryIncomeMonthlyTrend`) — OR compute combined per current filter only if that's how state is built; prefer always-compute-both + UI-picks, consistent with breakdowns. `CategoryTrendsCard` (`OverviewPeriodBody.kt:568-630`) then selects by filter: Expenses → expense trends, Income → income trends, All → expense + income merged (sorted by totalAmount desc).

## C) Monthly chart variants (user task 5b, year mode)

1. `BuildCumulativeSeriesUseCase.monthlyTotals()` (lines ~47-59) hardcodes EXPENSE. Add income and net variants (net = income − expense per month). New UiState fields:
```kotlin
val monthlyIncomeTotals: List<Double> = List(12) { 0.0 }
val monthlyNetTotals: List<Double> = List(12) { 0.0 }
```
2. `MonthlySpendingBarChart.kt` — parameterize title (it currently shows a fixed "monthly spending" string) and accept values; or add a generic title param.
3. `OverviewPeriodBody.kt` year-mode section (lines ~165-170) renders by filter:
   - `Expenses` → current single chart (unchanged).
   - `Income` → single chart with `monthlyIncomeTotals`, title "Total income".
   - `All` → three charts stacked: expenses ("Monthly spending" — existing title), income ("Monthly income"), net ("Monthly net").
4. **BarChart negative support** — `core/ui-graphs/.../BarChart.kt` clamps fraction to `[0,1]`; negative values render as nothing. Extend it: compute `minVal = min(values.min(), 0.0)`, `maxVal = max(values.max(), epsilon)`, baseline at `size.height * maxVal/(maxVal-minVal)`; bars above baseline grow up (rounded top corners, as now), below grow down (rounded bottom corners). **When all values >= 0 behavior must be pixel-identical to today** (baseline = bottom). Keep avg-line logic working for the all-positive case.

## D) Strings

New keys in `feature/overview/src/commonMain/composeResources/values/strings.xml` AND all 27 locales (`ar cs da de es et fi fr hi hr hu is it ja lt lv mk nb nl pl pt ru sk sl sv tr vi zh`), translated per locale. Expected keys (reuse existing keys where suitable — check first):
- monthly income chart title, monthly net chart title
- avg income / day, avg income / month, avg net / day, avg net / month labels (match phrasing style of existing avg labels)

Compose resources need per-key imports.

## Conventions

- Intent-only VMs; no new public VM methods. Pure logic in use cases.
- No UiState literal date defaults.
- No code comments.
- Use cases are plain classes, constructor-injected; if a NEW use case class is added, register it in `shared/.../di/FeatureModules.kt` and keep it `public`. (Prefer extending existing use cases — then no DI change.)

## Verification

```bash
./gradlew :feature:overview:compileDebugKotlinAndroid --no-configuration-cache
./gradlew :feature:overview:testDebugUnitTest --no-configuration-cache
./gradlew :shared:compileDebugKotlinAndroid --no-configuration-cache
```
Update existing use case tests for new params/fields; add coverage for income/net monthly totals and negative-value BarChart input handling at the use case level. Do NOT commit.
