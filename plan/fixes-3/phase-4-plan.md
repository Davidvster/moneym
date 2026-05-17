# Phase 4 — Overview: Category Trend Hover Interaction + Average Spend Per Category

## Items covered
- Item 6: Overview — cumulative per category chart hover/tap interaction
- Item 7: Overview — average spend stats (avg per day total, avg per day per category, year: avg by month AND by day)

## Problem analysis

### Item 6: Category cumulative chart hover
The `CategoryTrendsCard` composable shows per-category mini cumulative line charts using `MiniCumulativeLine`. Unlike `CumulativeChart` (used in `CumulativeSpendCard`) which supports drag-to-seek interaction, `MiniCumulativeLine` has no interaction at all.

The request is to add the same hover/tap interaction as the monthly cumulative spend chart (`CumulativeChart`). When user taps or drags on a `MiniCumulativeLine`, show the value at that point.

Implementation approach:
1. Add `seekIndex: Int?` state to `CategoryTrendsCard` (one per chart, or one shared across all charts)
2. Add `onSeekChanged: (Int?) -> Unit` callback to `MiniCumulativeLine`, or handle interaction via `pointerInput` wrapping the chart
3. Show a floating label with value when seeking
4. On drag end, clear the seek index

Simpler approach: Wrap `MiniCumulativeLine` in a Box with `pointerInput` that detects tap/drag and derives seek index. Show a value overlay Box above the chart.

Actually, we should modify `MiniCumulativeLine` to support an optional seek interaction, similar to `CumulativeChart`. Add parameters:
- `interactive: Boolean = false`
- `onSeekValue: ((dayIndex: Int, value: Double)?) -> Unit` (optional callback)

But since `MiniCumulativeLine` is in `core/ui` and is used elsewhere, a non-breaking approach is best: add optional `seekIndex: Int? = null` parameter to show a seek line and dot, plus a companion state in `CategoryTrendsCard`.

The cleanest approach: 
1. Update `MiniCumulativeLine` to support optional `seekIndex` parameter that draws a vertical line + dot at that position (no input handling, just rendering)  
2. In `CategoryTrendsCard`, add per-trend seek state and wrap each chart with `pointerInput` to detect drag/tap
3. Show a small label overlay in the chart area

### Item 7: Average spend stats
Looking at current state:
- Month mode: shows `avgDailyExpense` (avg expense per elapsed day) — already implemented in ViewModel and shown in `AvgStatsCard`
- Year mode: shows `avgMonthlyExpense` and `avgDailyExpenseYear` — already computed in ViewModel, shown in `AvgStatsCard`

The request also asks for average spend per day per category. This is new — need to:
1. Add `avgPerCategory: List<CategoryAvgDay>` to `OverviewUiState` (month mode: avg daily spend per category; year mode: avg monthly spend per category)
2. Compute it in `OverviewViewModel`
3. Show it in `OverviewScreen` below `AvgStatsCard` or expand `AvgStatsCard`

Data class for per-category averages:
```kotlin
data class CategoryAvgSpend(
    val categoryName: String,
    val categoryColor: Long,
    val categoryIcon: String,
    val avgAmount: Double, // avg per day (month mode) or avg per month (year mode)
)
```

Computation (month mode):
- For each category in `categoryDailyTrend`: `totalAmount / elapsedDays`

Computation (year mode):
- For each category in `categoryMonthlyTrend`: `totalAmount / elapsedMonths`

Display: Expand `AvgStatsCard` or add a new card below it showing a list of category averages (small icon + name + avg amount). Keep it compact.

## Files to modify

### Item 6 — Category chart hover

1. `/Users/davidvalic/Developer/MoneyM2/core/ui/src/commonMain/kotlin/com/dv/moneym/core/ui/MiniCumulativeLine.kt`
   - Add `seekIndex: Int? = null` parameter
   - When `seekIndex != null`, draw a vertical line and dot at that position (like `CumulativeChart` does for its seek line)

2. `/Users/davidvalic/Developer/MoneyM2/feature/overview/src/commonMain/kotlin/com/dv/moneym/feature/overview/ui/OverviewScreen.kt`
   - In `CategoryTrendsCard`, add `var seekInfo: Pair<Int, Int>? by remember { mutableStateOf(null) }` (trendIndex, dayIndex)
   - Wrap `MiniCumulativeLine` in a Box with `pointerInput` for drag/tap detection
   - Pass `seekIndex` to `MiniCumulativeLine`
   - Show a value label overlay when seeking (similar to CumulativeChart overlay)

### Item 7 — Per-category averages

3. `/Users/davidvalic/Developer/MoneyM2/feature/overview/src/commonMain/kotlin/com/dv/moneym/feature/overview/presentation/OverviewUiState.kt`
   - Add `data class CategoryAvgSpend(val categoryName: String, val categoryColor: Long, val categoryIcon: String, val avgAmount: Double)`
   - Add `val categoryAvgSpend: List<CategoryAvgSpend> = emptyList()` to `OverviewUiState`

4. `/Users/davidvalic/Developer/MoneyM2/feature/overview/src/commonMain/kotlin/com/dv/moneym/feature/overview/presentation/OverviewViewModel.kt`
   - In month mode: after computing `categoryDailyTrend`, compute `categoryAvgSpend`:
     ```kotlin
     val categoryAvgSpend = categoryDailyTrend.map { trend ->
         CategoryAvgSpend(
             categoryName = trend.categoryName,
             categoryColor = trend.categoryColor,
             categoryIcon = trend.categoryIcon,
             avgAmount = if (elapsedDays > 0) trend.totalAmount / elapsedDays else 0.0,
         )
     }
     ```
   - In year mode: compute from `categoryMonthlyTrend` with `elapsedMonths`:
     ```kotlin
     val categoryAvgSpend = categoryMonthlyTrend.map { trend ->
         CategoryAvgSpend(
             categoryName = trend.categoryName,
             categoryColor = trend.categoryColor,
             categoryIcon = trend.categoryIcon,
             avgAmount = if (elapsedMonths > 0) trend.totalAmount / elapsedMonths else 0.0,
         )
     }
     ```
   - Add `categoryAvgSpend = categoryAvgSpend` to `OverviewUiState(...)` constructor call

5. `/Users/davidvalic/Developer/MoneyM2/feature/overview/src/commonMain/kotlin/com/dv/moneym/feature/overview/ui/OverviewScreen.kt`
   - Update `OverviewPeriodBody` to pass `state.categoryAvgSpend` to `AvgStatsCard`
   - Expand `AvgStatsCard` to show per-category averages when the list is non-empty
   - Show each category row: small color dot + icon + name + avg amount (compact, small font)

## Detailed implementation

### MiniCumulativeLine with seekIndex
```kotlin
@Composable
fun MiniCumulativeLine(
    data: List<Double>,
    color: Color,
    modifier: Modifier = Modifier,
    upToIndex: Int = -1,
    seekIndex: Int? = null,  // NEW
) {
    Canvas(modifier = modifier) {
        // ... existing drawing code ...
        
        // NEW: Draw seek vertical line and dot
        if (seekIndex != null && seekIndex < cumulative.size) {
            val seekX = xAt(seekIndex)
            val seekVal = cumulative.getOrElse(seekIndex) { 0.0 }
            val seekY = yAt(seekVal)
            drawLine(
                color = color.copy(alpha = 0.7f),
                start = Offset(seekX, 0f),
                end = Offset(seekX, size.height),
                strokeWidth = 1.dp.toPx(),
            )
            drawCircle(
                color = color,
                radius = 4.dp.toPx() / 2f,
                center = Offset(seekX, seekY),
            )
        }
    }
}
```

### CategoryTrendsCard interaction
In `CategoryTrendsCard`, for each trend:
```kotlin
var seekTrendIndex by remember { mutableStateOf<Int?>(null) }
var seekDataIndex by remember { mutableStateOf<Int?>(null) }

// For each trend row, wrap MiniCumulativeLine in:
Box(modifier = Modifier.fillMaxWidth().height(32.dp)) {
    MiniCumulativeLine(
        data = trend.series,
        color = Color(trend.categoryColor),
        upToIndex = if (highlightIndex >= 0) highlightIndex else -1,
        seekIndex = if (seekTrendIndex == index) seekDataIndex else null,
        modifier = Modifier.fillMaxSize(),
    )
    // pointerInput for seek interaction
    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(trend.series) {
                detectDragGestures(
                    onDragStart = { offset ->
                        seekTrendIndex = index
                        val total = trend.series.size.coerceAtLeast(2)
                        seekDataIndex = ((offset.x / size.width) * (total - 1)).toInt().coerceIn(0, total - 1)
                    },
                    onDrag = { change, _ ->
                        change.consume()
                        val total = trend.series.size.coerceAtLeast(2)
                        seekDataIndex = ((change.position.x / size.width) * (total - 1)).toInt().coerceIn(0, total - 1)
                    },
                    onDragEnd = { seekTrendIndex = null; seekDataIndex = null },
                    onDragCancel = { seekTrendIndex = null; seekDataIndex = null },
                )
            }
    )
    // Value label overlay
    if (seekTrendIndex == index && seekDataIndex != null) {
        val cumData = buildList {
            var r = 0.0
            trend.series.forEach { v -> r += v; add(r) }
        }
        val val_ = cumData.getOrElse(seekDataIndex!!) { 0.0 }
        val labelText = formatSeekValue(val_)
        Box(
            modifier = Modifier
                .align(if ((seekDataIndex ?: 0) > trend.series.size / 2) Alignment.TopStart else Alignment.TopEnd)
                .clip(RoundedCornerShape(4.dp))
                .background(MM.colors.surface.copy(alpha = 0.92f))
                .border(1.dp, MM.colors.border, RoundedCornerShape(4.dp))
                .padding(horizontal = 6.dp, vertical = 2.dp),
        ) {
            Text(labelText, style = MM.type.captionMono.copy(fontSize = 10.sp), color = MM.colors.text)
        }
    }
}
```

Need to add `detectDragGestures`, `RoundedCornerShape`, `border` imports.

### AvgStatsCard with per-category
```kotlin
@Composable
private fun AvgStatsCard(
    inMonthMode: Boolean,
    avgDailyExpense: Double,
    avgMonthlyExpense: Double,
    avgDailyExpenseYear: Double,
    avgDayLabel: String,
    avgMonthLabel: String,
    currencyCode: String,
    categoryAvgSpend: List<CategoryAvgSpend>,  // NEW
) {
    // ... existing display code ...
    
    // After the main avg card, if categoryAvgSpend non-empty, show a second card
    if (categoryAvgSpend.isNotEmpty()) {
        MmCard(...) {
            Column {
                SectionLabel("AVG PER CATEGORY", ...)
                categoryAvgSpend.forEach { cat ->
                    Row(...) {
                        Box(Modifier.size(8.dp).clip(CircleShape).background(Color(cat.categoryColor)))
                        Text(cat.categoryName, ...)
                        MmMoney(cat.avgAmount, ...)
                    }
                }
            }
        }
    }
}
```

Add string resource `overview_avg_per_category` = "AVG PER CATEGORY" to overview strings.xml.

## Acceptance criteria
- Dragging on any mini cumulative line in "Daily trend by category" or "Monthly trend by category" shows a seek indicator (vertical line + dot) and a value label overlay
- Releasing drag hides the seek indicator
- Month mode shows "AVG/DAY" for overall AND per-category avg daily spend
- Year mode shows "AVG/MONTH", "AVG/DAY" for overall AND per-category avg monthly spend
- Android build compiles without errors
