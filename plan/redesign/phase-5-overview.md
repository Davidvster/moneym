# Phase 5 — Overview Screen (Month + Year)

Migrate `OverviewScreen`. Adds three new chart components: `DonutChart`, `CumulativeChart`, `MiniBars`. Depends on Phase 0–1.

---

## Files to modify

- `feature/overview/src/commonMain/…/ui/OverviewScreen.kt` — full rewrite
- `feature/overview/src/commonMain/…/presentation/OverviewUiState.kt` — extend with cumulative/trend data
- `feature/overview/src/commonMain/…/presentation/OverviewViewModel.kt` — compute new data shapes

Existing chart files (`DonutChart.kt`, `BarChart.kt`) are **replaced** by the new ones from `core/ui`.

---

## ViewModel / UiState changes

### `OverviewUiState`

```kotlin
data class OverviewUiState(
    val mode: OverviewMode = OverviewMode.Month,        // Month | Year
    val period: YearMonth,                              // or Year: Int for year mode
    val income: Money,
    val expenses: Money,

    // Spending by category
    val categoryBreakdown: List<CategorySpend>,

    // Month view only
    val dailyTotals: List<Double>,       // 31 values (or 28/29/30 for the month)
    val cumulativeTotals: List<Double>,  // running sum
    val todayIndex: Int,
    val categoryDailyTrend: List<CategoryDailyTrend>,

    // Year view only
    val monthlyTotals: List<Double>,     // 12 values
    val categoryMonthlyTrend: List<CategoryMonthlyTrend>,
)

data class CategorySpend(val category: Category, val amount: Money, val percent: Int)
data class CategoryDailyTrend(val category: Category, val amount: Money, val txCount: Int, val dailySeries: List<Double>)
data class CategoryMonthlyTrend(val category: Category, val amount: Money, val txCount: Int, val monthlySeries: List<Double>)
```

### ViewModel computation

`dailyTotals`: group transactions by day-of-month, sum amounts.
`cumulativeTotals`: running prefix sum of `dailyTotals`.
`categoryDailyTrend`: for each category, produce a 31-element list of daily spend.
`monthlyTotals`: for year mode, group transactions by month, sum.
`categoryMonthlyTrend`: for each category, 12-element monthly list.

---

## Layout — Month view

```
Column(scroll) {
  // Header
  Row(4 16 16 padding) {
    Text("Overview", style=MM.type.title1, weight=1f)
    MmSegmented(["Month","Year"], mode)
  }
  Row(gap=4dp, margin=14dp top) {
    MmIconButton(chevronLeft, 32dp)
    Text(monthLabel, style=MM.type.body, minWidth=96dp, center)
    MmIconButton(chevronRight, 32dp)
  }

  // Summary cards
  Row(0 16dp padding, gap=10dp) {
    MmCard(padded) {  // Income
      Row(gap=6dp) { Icon(arrowDown, 12dp, accent) + SectionLabel("Income") }
      MmMoney(income, size=20, color=accent)
    }
    MmCard(padded) {  // Expenses
      Row(gap=6dp) { Icon(arrowUp, 12dp, text) + SectionLabel("Expenses") }
      MmMoney(expenses, size=20)
    }
  }

  // Spending by category
  SpendingByCategoryCard(categoryBreakdown)

  // Cumulative spend
  MmCard(padded) {
    Row { Text("Cumulative spend", title3) + Text("EUR", captionMono, text3) }
    Row(align=baseline, gap=8dp) {
      MmMoney(cumulativeTotals[todayIndex], size=22, weight=600)
      Text("through day ${todayIndex+1}", caption, text2)
    }
    Spacer(16dp)
    CumulativeChart(cumulativeTotals, todayIndex, max, height=120dp)
    Row(spaceEvenly) { ["1","8","15","22","31"].forEach { Text(it, 10sp mono text3) } }
  }

  // Daily trend by category
  CategoryTrendsCard(categoryDailyTrend, highlight=todayIndex, xLabels=["1","8","15","22","31"])

  MmTabBar(active=TabRoute.Overview)
}
```

## Layout — Year view

Same header with Year switcher. Different cards:

```
  // Monthly spending bar chart
  MmCard(padded) {
    Row { Text("Monthly spending", title3) + Text("EUR", captionMono, text3) }
    Spacer(16dp)
    // 12 bars
    Row(align=bottom, height=140dp) {
      months.forEachIndexed { i, month ->
        Column(align=center) {
          Box(
            width=fillMaxWidth(0.7f),
            height=(value/max)*100dp clamped to 2dp min,
            bg = if(i == currentMonthIdx) text else borderStrong,
            radius=2dp,
            alpha = if(value==0.0) 0.4f else 1f
          )
          Text(month, 10sp mono, color = if(i==currentMonthIdx) text else text3)
        }
      }
    }
  }

  // Monthly trend by category
  CategoryTrendsCard(categoryMonthlyTrend, highlight=currentMonthIdx, xLabels=["Jan","Apr","Jul","Oct","Dec"])
```

## SpendingByCategoryCard composable

Lives in `feature/overview` (too data-specific for core/ui):

```kotlin
@Composable fun SpendingByCategoryCard(
    categories: List<CategorySpend>,
    total: Money,
)
```
- Internal state: `var showPercent by remember { mutableStateOf(true) }`
- `%` / `EUR` MmSegmented top-right.
- `DonutChart(categories)` left side, 130dp, 18dp stroke.
- Right side legend: TOTAL row (divider beneath) + one row per category (6dp dot + name + value).

## CategoryTrendsCard composable

Also in `feature/overview`. Per-category rows with `MiniBars` at bottom.

---

## Key implementation notes

- `CumulativeChart` draws only up to `todayIndex` — future days are blank (not dashed projection).
- `DonutChart` gap between slices: 2dp `strokeGap` parameter (renders as a tiny gap by offsetting dashArray).
- Year mode: `currentMonthIdx` = current calendar month index if viewing the current year; else -1 (no highlight).
- The overview screen is a single `LazyColumn` — the chart cards are `item { … }` entries. Don't use a nested `Column` with scroll.
- `MiniBars` zero-height bars: draw at 1.5dp to keep the time axis visible.

---

## Verification
1. Month view: cumulative chart line stops at today (day 15 in test data).
2. `%` / `EUR` toggle switches legend values without re-fetching.
3. Year view: current month bar is `text` color; others are `borderStrong`.
4. Category trend mini-bars: today's bar is 100% opacity.
5. Dark mode: area fill is barely visible, line is `text` (near-white).
6. Switching Month↔Year via segmented preserves the selected period.
