# Phase 7 — Monthly overview daily trend by category: show cumulative line chart instead of bars

## Goal
In `CategoryTrendsCard` composable (in `OverviewScreen.kt`), replace the `MiniBars` widget with a `MiniCumulativeLine` — a small cumulative line chart (similar to `CumulativeChart` but without the today line or drag interaction, just the line and fill).

## File to Change
`feature/overview/src/commonMain/kotlin/com/dv/moneym/feature/overview/ui/OverviewScreen.kt`

## Also Needed
Add a new `MiniCumulativeLine` composable to:
`core/ui/src/commonMain/kotlin/com/dv/moneym/core/ui/MiniCumulativeLine.kt` (new file)

## Current Code (CategoryTrendsCard, lines ~652-659)
```kotlin
Spacer(Modifier.height(8.dp))
MiniBars(
    data = trend.series,
    color = Color(trend.categoryColor),
    highlightIndex = if (highlightIndex >= 0) highlightIndex else -1,
    modifier = Modifier
        .fillMaxWidth()
        .height(26.dp),
)
```

## Implementation Steps

### Step 1: Create MiniCumulativeLine.kt in core/ui

```kotlin
package com.dv.moneym.core.ui

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.dv.moneym.core.designsystem.MM

/**
 * A compact cumulative line chart used in category trend rows.
 * Shows running total as a filled area + line.
 * No today marker, no drag interaction.
 *
 * @param data       Raw daily/monthly values (will be converted to cumulative internally)
 * @param color      Line and fill color
 * @param upToIndex  How many values to show (inclusive). -1 = show all.
 */
@Composable
fun MiniCumulativeLine(
    data: List<Double>,
    color: Color,
    modifier: Modifier = Modifier,
    upToIndex: Int = -1,
) {
    Canvas(modifier = modifier) {
        if (data.isEmpty() || data.all { it == 0.0 }) return@Canvas

        // Build cumulative values
        val cumulative = mutableListOf<Double>()
        var running = 0.0
        data.forEach { v -> running += v; cumulative.add(running) }

        val displayCount = if (upToIndex >= 0) (upToIndex + 1).coerceIn(1, cumulative.size) else cumulative.size
        val displayValues = cumulative.take(displayCount)

        if (displayValues.size < 2) return@Canvas

        val minVal = 0.0
        val maxVal = displayValues.maxOrNull()?.coerceAtLeast(0.001) ?: 0.001
        val totalPoints = cumulative.size.coerceAtLeast(2)

        fun xAt(index: Int): Float = (index.toFloat() / (totalPoints - 1)) * size.width
        fun yAt(value: Double): Float = size.height - ((value - minVal) / (maxVal - minVal)).toFloat() * size.height

        // Area path
        val areaPath = Path()
        areaPath.moveTo(xAt(0), size.height)
        areaPath.lineTo(xAt(0), yAt(displayValues[0]))
        displayValues.forEachIndexed { i, v -> areaPath.lineTo(xAt(i), yAt(v)) }
        areaPath.lineTo(xAt(displayValues.size - 1), size.height)
        areaPath.close()

        drawPath(path = areaPath, color = color.copy(alpha = 0.12f))

        // Line path
        val linePath = Path()
        displayValues.forEachIndexed { i, v ->
            if (i == 0) linePath.moveTo(xAt(i), yAt(v))
            else linePath.lineTo(xAt(i), yAt(v))
        }
        drawPath(path = linePath, color = color, style = Stroke(width = 1.5.dp.toPx()))

        // End dot
        val lastX = xAt(displayValues.size - 1)
        val lastY = yAt(displayValues.last())
        drawCircle(color = color, radius = 3.dp.toPx() / 2f, center = androidx.compose.ui.geometry.Offset(lastX, lastY))
    }
}
```

### Step 2: Replace MiniBars with MiniCumulativeLine in CategoryTrendsCard
In `OverviewScreen.kt`, find the `MiniBars` call inside `CategoryTrendsCard`:

**Before:**
```kotlin
MiniBars(
    data = trend.series,
    color = Color(trend.categoryColor),
    highlightIndex = if (highlightIndex >= 0) highlightIndex else -1,
    modifier = Modifier
        .fillMaxWidth()
        .height(26.dp),
)
```

**After:**
```kotlin
MiniCumulativeLine(
    data = trend.series,
    color = Color(trend.categoryColor),
    upToIndex = if (highlightIndex >= 0) highlightIndex else -1,
    modifier = Modifier
        .fillMaxWidth()
        .height(32.dp),
)
```

### Step 3: Update imports in OverviewScreen.kt
- Add: `import com.dv.moneym.core.ui.MiniCumulativeLine`
- Remove: `import com.dv.moneym.core.ui.MiniBars` (if no longer used — verify if it's used elsewhere in the file first)

### Step 4: Update the module's build.gradle or exports if needed
`MiniCumulativeLine` is in `core/ui` — no build.gradle changes needed since it's a new file in the same module.

## Acceptance Criteria
1. In the Overview month view, `CategoryTrendsCard` shows a small cumulative line chart (filled area + line) for each category instead of bar charts
2. The line chart uses the category's color
3. The chart shows cumulative spending up to the current day (`highlightIndex`)
4. Future days are not shown (data after `upToIndex` is hidden)
5. In year view, `CategoryTrendsCard` uses the same MiniCumulativeLine showing all 12 months' cumulative data
6. Build compiles: `./gradlew :composeApp:assembleDebug`
