# Phase 5 — Overview: Year View Monthly Spending Graph Fixes

## Items covered
- Item 8: Overview — year view monthly spending graph fixes
  - Bar labels going over month name text: fix layout
  - Month names should be localized (use system locale string resources)
  - Y-axis should show EUR amounts
  - Tapping a month bar shows a number above it with that month's total
  - Show a dotted line at the average monthly spend

## Problem analysis

Current implementation in `MonthlySpendingBarChart` in `OverviewScreen.kt` (lines ~595-661):
- Uses a `Row` of `Column` items, each with a `Box` bar and a `Text` month name
- No tap interaction
- No Y-axis
- No average line
- Bar heights are computed with `barFraction = (value / maxVal).toFloat()`
- The layout uses `Modifier.fillMaxHeight()` on the outer Row which is 140.dp tall — the bar fills from bottom as a fraction, and the month name is at the bottom

### Issue 1: Bar labels going over month names
The current layout puts the month name at the bottom of a `fillMaxHeight` Column. When bars are tall, they can overlap the month name because the Spacer + bar + space + text all share the 140.dp height. Fix: give the chart area a fixed height separate from the labels, so labels always sit below the chart.

### Issue 2: Month names localization
Already uses `localizedMonthNames().map { it.take(3) }` which pulls from string resources. This is already localized. No change needed unless the abbreviation behavior needs updating.

### Issue 3: Y-axis EUR amounts
Add a Y-axis on the left side of the bar chart showing 2-3 amounts (e.g. 0, max/2, max) with "€" prefix. This means restructuring the chart to have a Row with a Y-axis Column on the left and the bar chart area on the right.

### Issue 4: Tap to show value above bar
Add tap interaction. When user taps a bar, show the month's total as a Text above the bar. Use a `remember { mutableStateOf<Int?>(null) }` for selectedBarIndex. When tapped, show label; tapping again or tapping elsewhere clears it.

### Issue 5: Dotted average line
Calculate average monthly spend from `monthlyTotals`. Draw a dashed horizontal line at the average height using Canvas overlay, or compute via padding/offset.

Better: Convert the entire chart to a Canvas-based drawing (like `CumulativeChart`) for full control, OR keep the Compose layout but add a Canvas overlay just for the dashed average line.

**Recommended approach**: Refactor `MonthlySpendingBarChart` to use a custom Canvas drawing that handles:
- Y-axis labels on the left
- Bars with proper spacing
- Dashed average line
- Tap to show value

## Implementation plan

### Refactored MonthlySpendingBarChart

The current implementation is a Row of Columns. Refactor to:

```kotlin
@Composable
private fun MonthlySpendingBarChart(
    monthlyTotals: List<Double>,
    currentMonthIndex: Int,
    currencyCode: String,
) {
    val colors = MM.colors
    val type = MM.type
    val space = MM.space
    val monthNames = localizedMonthNames().map { it.take(3) }
    
    var selectedBarIndex by remember { mutableStateOf<Int?>(null) }
    val avgMonthly = if (monthlyTotals.any { it > 0 }) monthlyTotals.filter { it > 0 }.average() else 0.0
    val maxVal = monthlyTotals.maxOrNull()?.takeIf { it > 0 } ?: 1.0
    
    MmCard(...) {
        Column {
            // Header row (title + currency)
            Row(...) {
                Text(overview_monthly_spending, ...)
                Text(currencyCode, ...)
            }
            Spacer(height)
            
            // Chart area: Y-axis + bars
            Row(modifier = Modifier.fillMaxWidth()) {
                // Y-axis column
                Column(
                    modifier = Modifier.width(48.dp).height(CHART_HEIGHT),
                    verticalArrangement = Arrangement.SpaceBetween,
                ) {
                    // top: max value
                    Text("${formatAxisAmount(maxVal)}", style = type.captionMono.copy(fontSize=9.sp), color=colors.text3, textAlign=TextAlign.End, modifier=Modifier.fillMaxWidth())
                    // middle
                    Text("${formatAxisAmount(maxVal/2)}", style = type.captionMono.copy(fontSize=9.sp), color=colors.text3, textAlign=TextAlign.End, modifier=Modifier.fillMaxWidth())
                    // bottom: 0
                    Text("0", style = type.captionMono.copy(fontSize=9.sp), color=colors.text3, textAlign=TextAlign.End, modifier=Modifier.fillMaxWidth())
                }
                Spacer(Modifier.width(4.dp))
                
                // Bar chart area
                Box(modifier = Modifier.weight(1f)) {
                    // Canvas for avg dashed line
                    Canvas(modifier = Modifier.fillMaxWidth().height(CHART_HEIGHT)) {
                        val avgFraction = (avgMonthly / maxVal).toFloat().coerceIn(0f, 1f)
                        val avgY = size.height * (1f - avgFraction)
                        drawLine(
                            color = accentColor.copy(alpha = 0.5f),
                            start = Offset(0f, avgY),
                            end = Offset(size.width, avgY),
                            strokeWidth = 1.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 4f), 0f),
                        )
                    }
                    
                    // Bars
                    Row(
                        modifier = Modifier.fillMaxWidth().height(CHART_HEIGHT),
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.SpaceEvenly,
                    ) {
                        monthlyTotals.forEachIndexed { i, value ->
                            val isCurrent = i == currentMonthIndex
                            val isSelected = i == selectedBarIndex
                            val barFraction = (value / maxVal).toFloat().coerceIn(0f, 1f)
                            
                            Box(
                                modifier = Modifier.weight(1f).fillMaxHeight(),
                                contentAlignment = Alignment.BottomCenter,
                            ) {
                                // Value label above bar when selected
                                if (isSelected && value > 0) {
                                    Text(
                                        text = formatBarAmount(value),
                                        style = type.captionMono.copy(fontSize = 9.sp),
                                        color = colors.text,
                                        modifier = Modifier
                                            .align(Alignment.TopCenter)
                                            .padding(bottom = 2.dp),
                                    )
                                }
                                // The bar
                                Box(
                                    modifier = Modifier
                                        .width(14.dp)
                                        .fillMaxHeight(barFraction.coerceAtLeast(0.01f))
                                        .clip(RoundedCornerShape(2.dp))
                                        .background(
                                            when {
                                                isSelected -> colors.accent
                                                isCurrent -> colors.text
                                                else -> colors.borderStrong
                                            }
                                        )
                                        .alpha(if (value == 0.0) 0.3f else 1f)
                                        .clickable(
                                            indication = null,
                                            interactionSource = remember { MutableInteractionSource() },
                                        ) { selectedBarIndex = if (isSelected) null else i },
                                )
                            }
                        }
                    }
                }
            }
            
            // Month name labels below chart (separate from chart, so no overlap)
            Spacer(Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 52.dp), // offset for Y-axis
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                monthNames.forEachIndexed { i, name ->
                    val isCurrent = i == currentMonthIndex
                    Text(
                        text = name,
                        style = type.captionMono.copy(
                            fontSize = 10.sp,
                            color = if (isCurrent) colors.text else colors.text3,
                        ),
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}
```

Key fixes:
1. Month labels are in a separate `Row` below the chart area, not inside the bar columns — no overlap possible
2. Y-axis column on the left shows 3 amount labels
3. Canvas draws a dashed average line
4. Tapping a bar sets `selectedBarIndex` and shows value label above

## Files to modify

1. `/Users/davidvalic/Developer/MoneyM2/feature/overview/src/commonMain/kotlin/com/dv/moneym/feature/overview/ui/OverviewScreen.kt`
   - Rewrite `MonthlySpendingBarChart` composable with:
     - Fixed-height chart area (e.g. `val CHART_HEIGHT = 140.dp`)
     - Y-axis Column on the left (48.dp wide) with top/mid/bottom EUR labels
     - Bar area as `Box` with `Canvas` overlay for dashed average line
     - Bars as clickable with tap-to-show-value behavior
     - Month name labels in a separate `Row` below the chart area (not inside bar columns)
   - Add imports: `androidx.compose.foundation.gestures.detectTapGestures`, `androidx.compose.ui.graphics.PathEffect`, `androidx.compose.foundation.Canvas`

2. String resources may need a new key for the average line label (optional — can skip if avg is communicated via the visual line only):
   - Could add `overview_avg_line` = "AVG" as a small label near the dashed line

## Helper functions to add to OverviewScreen.kt
```kotlin
private fun formatAxisAmount(value: Double): String {
    return if (value >= 1000) "${(value / 1000).toInt()}k" else value.toInt().toString()
}

private fun formatBarAmount(value: Double): String {
    return if (value >= 1000) "${(value / 1000 * 10).toInt() / 10.0}k" else value.toInt().toString()
}
```

## Acceptance criteria
- Month names in bar chart are always below bars with no overlap (separate row)
- Y-axis on left side of bar chart shows 3 EUR amount labels (0, mid, max)
- A dotted horizontal line shows average monthly spend  
- Tapping a bar shows that month's total above the bar (tapping again clears it)
- Month names come from localized string resources (already the case, verify)
- Android build compiles without errors
