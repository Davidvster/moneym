# Phase 4 — Add full color picker to new category

## Goal
In the `NewCategorySheet` composable, add an "any color" option after the existing 15 preset color swatches. This opens an HSV color picker (hue slider + saturation/brightness box, or a simple hue + hex input approach) so users can pick any color.

## File to Change
`feature/categories/src/commonMain/kotlin/com/dv/moneym/feature/categories/ui/CategoryListScreen.kt`

## Current Code (lines ~350-468)
The `NewCategorySheet` composable has a `FlowRow` of 15 preset `ColorSwatch` boxes. There's no way to enter a custom color.

## Implementation Steps

### Step 1: Add a "custom color" swatch to the FlowRow
After the `palette.forEach { ... }` block in the color section, add a special "+" swatch:

```kotlin
// "Any color" swatch
var showColorPicker by remember { mutableStateOf(false) }
var customColor by remember { mutableStateOf<Color?>(null) }

// In the FlowRow, after palette swatches:
// Add a custom color swatch item
val isCustomSelected = selectedColor !in palette
Box(
    modifier = Modifier
        .size(36.dp)
        .clip(RoundedCornerShape(10.dp))
        .background(if (isCustomSelected && customColor != null) customColor!! else colors.surface)
        .border(1.dp, if (isCustomSelected) colors.accent else colors.borderStrong, RoundedCornerShape(10.dp))
        .clickable(
            indication = null,
            interactionSource = remember { MutableInteractionSource() },
        ) { showColorPicker = true },
    contentAlignment = Alignment.Center,
) {
    if (isCustomSelected && customColor != null) {
        // Show check if this custom color is selected
        val painter = rememberVectorPainter(MmIcons.check)
        Image(
            painter = painter,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            colorFilter = ColorFilter.tint(Color.White),
        )
    } else {
        // Show a "+" icon to indicate custom color
        val painter = rememberVectorPainter(MmIcons.plus)
        Image(
            painter = painter,
            contentDescription = "Custom color",
            modifier = Modifier.size(16.dp),
            colorFilter = ColorFilter.tint(colors.text2),
        )
    }
}
```

### Step 2: Add a HsvColorPickerDialog composable

This is a pure-Compose HSV picker with:
1. A hue horizontal slider (rainbow gradient)
2. A saturation/brightness 2D box
3. A hex text input
4. Preview of the current color
5. Cancel + Confirm buttons using `MmButton`

Add this private composable to the file:

```kotlin
@Composable
private fun HsvColorPickerDialog(
    initialColor: Color,
    onDismiss: () -> Unit,
    onColorSelected: (Color) -> Unit,
) {
    val colors = MM.colors
    val type = MM.type

    // HSV state — convert initial color to HSV
    fun Color.toHsv(): Triple<Float, Float, Float> {
        val r = red; val g = green; val b = blue
        val max = maxOf(r, g, b); val min = minOf(r, g, b)
        val delta = max - min
        val h = when {
            delta == 0f -> 0f
            max == r -> 60f * (((g - b) / delta) % 6f)
            max == g -> 60f * ((b - r) / delta + 2f)
            else -> 60f * ((r - g) / delta + 4f)
        }.let { if (it < 0f) it + 360f else it }
        val s = if (max == 0f) 0f else delta / max
        val v = max
        return Triple(h, s, v)
    }

    val (ih, is_, iv) = remember(initialColor) { initialColor.toHsv() }
    var hue by remember { mutableStateOf(ih) }
    var saturation by remember { mutableStateOf(is_) }
    var brightness by remember { mutableStateOf(iv) }

    // Compute current color from HSV
    fun hsvToColor(h: Float, s: Float, v: Float): Color {
        val c = v * s
        val x = c * (1f - kotlin.math.abs((h / 60f) % 2f - 1f))
        val m = v - c
        val (r1, g1, b1) = when {
            h < 60f  -> Triple(c, x, 0f)
            h < 120f -> Triple(x, c, 0f)
            h < 180f -> Triple(0f, c, x)
            h < 240f -> Triple(0f, x, c)
            h < 300f -> Triple(x, 0f, c)
            else     -> Triple(c, 0f, x)
        }
        return Color(r1 + m, g1 + m, b1 + m)
    }

    val currentColor = hsvToColor(hue, saturation, brightness)

    // Hex input state
    var hexInput by remember(hue, saturation, brightness) {
        val r = (currentColor.red * 255).toInt()
        val g = (currentColor.green * 255).toInt()
        val b = (currentColor.blue * 255).toInt()
        mutableStateOf("#${r.toString(16).padStart(2,'0')}${g.toString(16).padStart(2,'0')}${b.toString(16).padStart(2,'0')}".uppercase())
    }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        containerColor = colors.bg,
        dragHandle = null,
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Grab handle
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Box(Modifier.size(width = 36.dp, height = 4.dp).clip(RoundedCornerShape(50)).background(colors.borderStrong))
            }

            Text("Pick color", style = type.title3, color = colors.text, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())

            // Color preview strip
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(currentColor),
            )

            // Hue slider label
            Text("Hue", style = type.caption.copy(color = colors.text2))

            // Hue slider — a Canvas with rainbow gradient + draggable thumb
            var sliderWidth by remember { mutableStateOf(0f) }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(28.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .drawBehind {
                        sliderWidth = size.width
                        // Draw rainbow gradient
                        val rainbowBrush = Brush.horizontalGradient(
                            colors = (0..6).map { i ->
                                Color.hsv(i * 60f, 1f, 1f)
                            }
                        )
                        drawRect(rainbowBrush)
                    }
                    .pointerInput(Unit) {
                        detectDragGestures { change, _ ->
                            change.consume()
                            val fraction = (change.position.x / sliderWidth).coerceIn(0f, 1f)
                            hue = fraction * 360f
                        }
                    }
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() },
                    ) { },
            ) {
                // Thumb indicator
                val thumbX = (hue / 360f) * sliderWidth
                Box(
                    modifier = Modifier
                        .offset(x = thumbX.dp - 12.dp)
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                        .border(2.dp, colors.borderStrong, CircleShape),
                )
            }

            // Saturation slider
            Text("Saturation", style = type.caption.copy(color = colors.text2))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(28.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .drawBehind {
                        sliderWidth = size.width
                        val satBrush = Brush.horizontalGradient(
                            colors = listOf(Color.White, hsvToColor(hue, 1f, brightness))
                        )
                        drawRect(satBrush)
                    }
                    .pointerInput(saturation) {
                        detectDragGestures { change, _ ->
                            change.consume()
                            saturation = (change.position.x / sliderWidth).coerceIn(0f, 1f)
                        }
                    },
            ) {
                val thumbX = saturation * sliderWidth
                Box(
                    modifier = Modifier
                        .offset(x = thumbX.dp - 12.dp)
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                        .border(2.dp, colors.borderStrong, CircleShape),
                )
            }

            // Brightness slider
            Text("Brightness", style = type.caption.copy(color = colors.text2))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(28.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .drawBehind {
                        sliderWidth = size.width
                        val valBrush = Brush.horizontalGradient(
                            colors = listOf(Color.Black, hsvToColor(hue, saturation, 1f))
                        )
                        drawRect(valBrush)
                    }
                    .pointerInput(brightness) {
                        detectDragGestures { change, _ ->
                            change.consume()
                            brightness = (change.position.x / sliderWidth).coerceIn(0f, 1f)
                        }
                    },
            ) {
                val thumbX = brightness * sliderWidth
                Box(
                    modifier = Modifier
                        .offset(x = thumbX.dp - 12.dp)
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                        .border(2.dp, colors.borderStrong, CircleShape),
                )
            }

            // Hex input
            MmField(
                value = hexInput,
                onValueChange = { input ->
                    hexInput = input
                    // Try to parse hex
                    val clean = input.trimStart('#')
                    if (clean.length == 6) {
                        try {
                            val r = clean.substring(0,2).toInt(16) / 255f
                            val g = clean.substring(2,4).toInt(16) / 255f
                            val b = clean.substring(4,6).toInt(16) / 255f
                            val parsed = Color(r, g, b)
                            val (h, s, v) = parsed.toHsv()
                            hue = h; saturation = s; brightness = v
                        } catch (_: Exception) {}
                    }
                },
                label = "Hex",
                placeholder = "#FF0000",
                modifier = Modifier.fillMaxWidth(),
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                MmButton(
                    text = "Cancel",
                    onClick = onDismiss,
                    variant = MmButtonVariant.Secondary,
                    modifier = Modifier.weight(1f),
                )
                MmButton(
                    text = "Select color",
                    onClick = { onColorSelected(currentColor) },
                    variant = MmButtonVariant.Accent,
                    modifier = Modifier.weight(1f),
                )
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}
```

### Step 3: Show the color picker dialog when `showColorPicker` is true
Inside `NewCategorySheet`, add after the FlowRow for colors:
```kotlin
if (showColorPicker) {
    HsvColorPickerDialog(
        initialColor = if (selectedColor !in palette) selectedColor else Color(0xFF4A8E5C),
        onDismiss = { showColorPicker = false },
        onColorSelected = { color ->
            selectedColor = color
            customColor = color
            showColorPicker = false
        },
    )
}
```

### Step 4: Update `showColorPicker` and `customColor` state declarations
Add at the top of `NewCategorySheet`, after the existing state variables:
```kotlin
var showColorPicker by remember { mutableStateOf(false) }
var customColor by remember(categoryToEdit?.id) {
    mutableStateOf(
        if (categoryToEdit != null && categoryColor(categoryToEdit.colorHex) !in palette) {
            categoryColor(categoryToEdit.colorHex)
        } else null
    )
}
```

### Step 5: Add necessary imports
- `androidx.compose.foundation.gestures.detectDragGestures`
- `androidx.compose.ui.graphics.Brush`
- `androidx.compose.foundation.shape.CircleShape`
- `androidx.compose.ui.input.pointer.pointerInput`
- `androidx.compose.ui.draw.drawBehind`
- `androidx.compose.material3.ModalBottomSheet`
- `androidx.compose.material3.rememberModalBottomSheetState`
- `androidx.compose.foundation.layout.Arrangement`

## Important Notes
- The `Color.hsv(hue, saturation, value)` function is available in Compose: `import androidx.compose.ui.graphics.Color` with `.hsv` extension. Use `Color(hue=h, saturation=s, value=v)` if `.hsv()` isn't available; alternatively compute manually using the `hsvToColor` helper.
- The thumb offset approach using `offset(x = thumbX.dp - 12.dp)` approximates pixel position; the builder must ensure slider width is measured in pixels not dp. Alternatively use `BoxWithConstraints` to get the width in dp directly.
- Keep implementation simple and functional; polish is secondary.

## Acceptance Criteria
1. In `NewCategorySheet`, after the 15 palette swatches, a "+" swatch appears
2. Tapping it opens a color picker modal sheet
3. The picker has three sliders: Hue, Saturation, Brightness
4. There's a hex input field that updates the sliders
5. A preview strip shows the current color
6. Tapping "Select color" sets the category color and closes the picker
7. Tapping "Cancel" closes without change
8. The custom color swatch in the palette FlowRow shows the picked color with a check mark when selected
9. Build compiles: `./gradlew :composeApp:assembleDebug`
