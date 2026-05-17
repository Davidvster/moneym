# Phase 6 — Overview cumulative graph: drag-to-seek

## Goal
Add press-and-drag interaction to the `CumulativeChart` composable: when the user presses and drags horizontally, show a vertical seek line and display the cumulative value nearest to the drag X position.

## Files to Change
1. `core/ui/src/commonMain/kotlin/com/dv/moneym/core/ui/CumulativeChart.kt` — add drag interaction + seek line + tooltip
2. `feature/overview/src/commonMain/kotlin/com/dv/moneym/feature/overview/ui/OverviewScreen.kt` — update the `CumulativeChart` call to handle the seek state (optional; can be self-contained in the chart)

## Current CumulativeChart Signature
```kotlin
fun CumulativeChart(
    values: List<Double>,
    todayIndex: Int,
    modifier: Modifier = Modifier,
)
```

## Implementation Plan

### Approach: Self-contained seek state in CumulativeChart
The seek state (which index is being hovered) is best kept inside `CumulativeChart` as local state with a `remember`. This avoids changing the OverviewScreen or ViewModel.

### Step 1: Add seek state to CumulativeChart
```kotlin
var seekIndex by remember { mutableStateOf<Int?>(null) }
```

### Step 2: Replace `Canvas` with a `Box` containing a `Canvas` + overlay `Canvas`, and add `pointerInput` modifier

The approach:
- Wrap the existing Canvas with a Box that captures drag gestures
- Use `detectDragGestures` + `detectTapGestures` to update `seekIndex`
- When `seekIndex != null`, draw a seek line at that X position and show the value

```kotlin
@Composable
fun CumulativeChart(
    values: List<Double>,
    todayIndex: Int,
    modifier: Modifier = Modifier,
) {
    val colors = MM.colors
    val lineColor = colors.text
    val gridColor = colors.text3
    val todayLineColor = colors.text3
    val seekLineColor = colors.accent

    var seekIndex by remember { mutableStateOf<Int?>(null) }

    Box(modifier = modifier) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(values) {
                    // Detect press + drag to set seekIndex
                    detectDragGestures(
                        onDragStart = { offset ->
                            if (values.size >= 2) {
                                val totalPoints = values.size
                                val idx = ((offset.x / size.width) * (totalPoints - 1)).toInt()
                                    .coerceIn(0, totalPoints - 1)
                                seekIndex = idx
                            }
                        },
                        onDrag = { change, _ ->
                            change.consume()
                            if (values.size >= 2) {
                                val totalPoints = values.size
                                val idx = ((change.position.x / size.width) * (totalPoints - 1)).toInt()
                                    .coerceIn(0, totalPoints - 1)
                                seekIndex = idx
                            }
                        },
                        onDragEnd = { seekIndex = null },
                        onDragCancel = { seekIndex = null },
                    )
                },
        ) {
            // ... existing drawing code (grid, area, line, today line, today dot) ...
            // Plus: if seekIndex != null, draw seek line + dot
            
            seekIndex?.let { si ->
                val displayCount = (todayIndex + 1).coerceIn(1, values.size)
                if (si <= displayCount - 1 && values.isNotEmpty()) {
                    val totalPoints = values.size.coerceAtLeast(2)
                    val minVal = 0.0
                    val maxVal = values.take(displayCount).maxOrNull()?.coerceAtLeast(1.0) ?: 1.0

                    fun xAt(index: Int): Float = (index.toFloat() / (totalPoints - 1)) * size.width
                    fun yAt(value: Double): Float = size.height - ((value - minVal) / (maxVal - minVal)).toFloat() * size.height

                    val seekX = xAt(si)
                    val seekValue = values.getOrElse(si) { 0.0 }
                    val seekY = yAt(seekValue)

                    // Vertical seek line (solid, accent color)
                    drawLine(
                        color = seekLineColor,
                        start = Offset(seekX, 0f),
                        end = Offset(seekX, size.height),
                        strokeWidth = 1.5.dp.toPx(),
                    )
                    // Dot at seek position
                    drawCircle(
                        color = seekLineColor,
                        radius = 5.dp.toPx() / 2f,
                        center = Offset(seekX, seekY),
                    )
                }
            }
        }

        // Seek value label overlay
        seekIndex?.let { si ->
            val displayCount = (todayIndex + 1).coerceIn(1, values.size)
            if (si <= displayCount - 1) {
                val seekValue = values.getOrElse(si) { 0.0 }
                val dayLabel = si + 1
                val valueText = "Day $dayLabel: ${formatSeekValue(seekValue)}"
                // Position: top-right if seekIndex is in the first half, top-left otherwise
                val isLeftHalf = si < values.size / 2
                Box(
                    modifier = Modifier
                        .align(if (isLeftHalf) Alignment.TopEnd else Alignment.TopStart)
                        .clip(RoundedCornerShape(6.dp))
                        .background(MM.colors.surface.copy(alpha = 0.9f))
                        .border(1.dp, MM.colors.border, RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                ) {
                    Text(
                        text = valueText,
                        style = MM.type.captionMono.copy(fontSize = 11.sp),
                        color = MM.colors.text,
                    )
                }
            }
        }
    }
}
```

### Step 3: Add `formatSeekValue` private function in CumulativeChart.kt
```kotlin
private fun formatSeekValue(value: Double): String {
    val intPart = value.toLong()
    val decPart = kotlin.math.round((value - intPart) * 100).toInt()
    return "$intPart.${decPart.toString().padStart(2, '0')}"
}
```

### Step 4: Keep the existing drawing code intact
The existing Canvas drawing code (grid lines, area path, line path, today line + dot) should remain unchanged. Only add the seek overlay drawing at the end of the canvas block.

### Step 5: Add necessary imports to CumulativeChart.kt
- `androidx.compose.foundation.gestures.detectDragGestures`
- `androidx.compose.foundation.layout.Box`
- `androidx.compose.foundation.layout.fillMaxSize`
- `androidx.compose.foundation.background`
- `androidx.compose.foundation.border`
- `androidx.compose.foundation.layout.padding`
import `androidx.compose.foundation.shape.RoundedCornerShape`
- `androidx.compose.runtime.mutableStateOf`
- `androidx.compose.runtime.remember`
- `androidx.compose.runtime.getValue`
- `androidx.compose.runtime.setValue`
- `androidx.compose.ui.Alignment`
- `androidx.compose.ui.input.pointer.pointerInput`
- `androidx.compose.ui.unit.sp`
- `androidx.compose.material3.Text`
- `androidx.compose.ui.geometry.Offset`

## Acceptance Criteria
1. When the user presses and drags horizontally over the cumulative chart, a vertical line (accent color) follows the finger/pointer
2. A small label overlay shows the day number and cumulative value at the drag position
3. When the user lifts their finger (drag ends), the seek line disappears and the chart returns to normal
4. The seek line only seeks within the "filled" area (up to `todayIndex`), not in the future area
5. The existing today line (dashed) and dot remain visible when not seeking
6. Build compiles: `./gradlew :composeApp:assembleDebug`
