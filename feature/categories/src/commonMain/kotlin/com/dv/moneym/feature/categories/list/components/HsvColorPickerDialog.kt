package com.dv.moneym.feature.categories.list.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.horizontalDrag
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.dv.moneym.core.designsystem.MM
import com.dv.moneym.core.designsystem.MoneyMColors
import com.dv.moneym.core.ui.MmButton
import com.dv.moneym.core.ui.MmButtonVariant
import com.dv.moneym.core.ui.MmField
import moneym.feature.categories.generated.resources.Res
import moneym.feature.categories.generated.resources.categories_cancel
import moneym.feature.categories.generated.resources.categories_color_brightness
import moneym.feature.categories.generated.resources.categories_color_hex
import moneym.feature.categories.generated.resources.categories_color_hue
import moneym.feature.categories.generated.resources.categories_color_picker_title
import moneym.feature.categories.generated.resources.categories_color_saturation
import moneym.feature.categories.generated.resources.categories_color_select
import org.jetbrains.compose.resources.stringResource
import kotlin.math.abs
import kotlin.math.roundToInt

// ─── HSV Color Picker Dialog ──────────────────────────────────────────────────
// Using Dialog instead of ModalBottomSheet to avoid gesture conflicts with parent sheet

@Composable
internal fun HsvColorPickerDialog(
    initialColor: Color,
    onDismiss: () -> Unit,
    onColorSelected: (Color) -> Unit,
) {
    val colors = MM.colors

    val (ih, is_, iv) = remember(initialColor) { colorToHsv(initialColor) }
    var hue by remember { mutableStateOf(ih) }
    var saturation by remember { mutableStateOf(is_) }
    var brightness by remember { mutableStateOf(iv) }

    val currentColor = hsvToColor(hue, saturation, brightness)

    var hexText by remember(hue, saturation, brightness) {
        val r = (currentColor.red * 255).roundToInt()
        val g = (currentColor.green * 255).roundToInt()
        val b = (currentColor.blue * 255).roundToInt()
        mutableStateOf(
            "#${r.toString(16).padStart(2, '0')}${g.toString(16).padStart(2, '0')}${
                b.toString(16).padStart(2, '0')
            }".uppercase()
        )
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = MM.dimen.padding_2x)
                .clip(RoundedCornerShape(MM.dimen.padding_2_5x))
                .background(colors.bg),
        ) {
            HsvColorPickerContent(
                colors = colors,
                currentColor = currentColor,
                hue = hue,
                saturation = saturation,
                brightness = brightness,
                hexText = hexText,
                onHueChange = { hue = it },
                onSaturationChange = { saturation = it },
                onBrightnessChange = { brightness = it },
                onHexTextChange = { hexText = it },
                onHsvChange = { h, s, v -> hue = h; saturation = s; brightness = v },
                onDismiss = onDismiss,
                onColorSelected = onColorSelected,
            )
        }
    }
}

// ─── HSV Color Picker content ─────────────────────────────────────────────────

@Composable
internal fun HsvColorPickerContent(
    colors: MoneyMColors,
    currentColor: Color,
    hue: Float,
    saturation: Float,
    brightness: Float,
    hexText: String,
    onHueChange: (Float) -> Unit,
    onSaturationChange: (Float) -> Unit,
    onBrightnessChange: (Float) -> Unit,
    onHexTextChange: (String) -> Unit,
    onHsvChange: (Float, Float, Float) -> Unit,
    onDismiss: () -> Unit,
    onColorSelected: (Color) -> Unit,
) {
    val type = MM.type

    Column(
        modifier = Modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = MM.dimen.padding_2_5x, vertical = MM.dimen.padding_2x),
        verticalArrangement = Arrangement.spacedBy(MM.dimen.padding_1_5x),
    ) {
        Text(
            stringResource(Res.string.categories_color_picker_title),
            style = type.title3,
            color = colors.text,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )

        // Color preview strip
        Box(
            Modifier
                .fillMaxWidth()
                .height(MM.dimen.padding_6x)
                .clip(RoundedCornerShape(MM.dimen.padding_1_5x))
                .background(currentColor),
        )

        HsvSlidersSection(
            hue = hue,
            saturation = saturation,
            brightness = brightness,
            onHueChange = onHueChange,
            onSaturationChange = onSaturationChange,
            onBrightnessChange = onBrightnessChange,
            colors = colors,
        )

        // Hex input
        MmField(
            value = hexText,
            onValueChange = { input ->
                onHexTextChange(input)
                val clean = input.trimStart('#')
                if (clean.length == 6) {
                    try {
                        val r = clean.substring(0, 2).toInt(16) / 255f
                        val g = clean.substring(2, 4).toInt(16) / 255f
                        val b = clean.substring(4, 6).toInt(16) / 255f
                        val (h, s, v) = colorToHsv(Color(r, g, b))
                        onHsvChange(h, s, v)
                    } catch (_: Exception) {
                    }
                }
            },
            label = stringResource(Res.string.categories_color_hex),
            placeholder = "#4A8E5C",
            modifier = Modifier.fillMaxWidth(),
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(MM.dimen.padding_1_5x),
        ) {
            MmButton(
                text = stringResource(Res.string.categories_cancel),
                onClick = onDismiss,
                variant = MmButtonVariant.Secondary,
                modifier = Modifier.weight(1f),
            )
            MmButton(
                text = stringResource(Res.string.categories_color_select),
                onClick = { onColorSelected(currentColor) },
                variant = MmButtonVariant.Accent,
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(Modifier.height(MM.dimen.padding_1x))
    }
}

// ─── HSV Sliders Section ──────────────────────────────────────────────────────

@Composable
internal fun HsvSlidersSection(
    hue: Float,
    saturation: Float,
    brightness: Float,
    onHueChange: (Float) -> Unit,
    onSaturationChange: (Float) -> Unit,
    onBrightnessChange: (Float) -> Unit,
    colors: MoneyMColors,
) {
    val type = MM.type
    // Hue slider
    Text(
        stringResource(Res.string.categories_color_hue),
        style = type.caption.copy(color = colors.text2)
    )
    HsvSlider(
        gradient = Brush.horizontalGradient((0..6).map { i -> hsvToColor(i * 60f, 1f, 1f) }),
        thumbPosition = hue / 360f,
        onPositionChanged = { pos -> onHueChange(pos * 360f) },
        colors = colors,
    )
    // Saturation slider
    Text(
        stringResource(Res.string.categories_color_saturation),
        style = type.caption.copy(color = colors.text2)
    )
    HsvSlider(
        gradient = Brush.horizontalGradient(
            listOf(
                hsvToColor(hue, 0f, brightness.coerceAtLeast(0.3f)),
                hsvToColor(hue, 1f, brightness.coerceAtLeast(0.3f)),
            )
        ),
        thumbPosition = saturation,
        onPositionChanged = { pos -> onSaturationChange(pos) },
        colors = colors,
    )
    // Brightness slider
    Text(
        stringResource(Res.string.categories_color_brightness),
        style = type.caption.copy(color = colors.text2)
    )
    HsvSlider(
        gradient = Brush.horizontalGradient(
            listOf(
                Color.Black,
                hsvToColor(hue, saturation.coerceAtLeast(0.3f), 1f)
            )
        ),
        thumbPosition = brightness,
        onPositionChanged = { pos -> onBrightnessChange(pos) },
        colors = colors,
    )
}

// ─── HSV Slider ───────────────────────────────────────────────────────────────

@Composable
internal fun HsvSlider(
    gradient: Brush,
    thumbPosition: Float,
    onPositionChanged: (Float) -> Unit,
    colors: MoneyMColors,
) {
    val density = LocalDensity.current
    val latestOnPositionChanged by rememberUpdatedState(onPositionChanged)
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(MM.dimen.padding_4x)
            .clip(RoundedCornerShape(MM.dimen.padding_2x)),
    ) {
        val widthPx = constraints.maxWidth.toFloat()
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(gradient)
                .pointerInput(Unit) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        latestOnPositionChanged((down.position.x / widthPx).coerceIn(0f, 1f))
                        horizontalDrag(down.id) { change ->
                            change.consume()
                            latestOnPositionChanged((change.position.x / widthPx).coerceIn(0f, 1f))
                        }
                    }
                },
        ) {
            val thumbSizeDp = MM.dimen.padding_3x
            val thumbSizePx = with(density) { thumbSizeDp.toPx() }
            val thumbCenterPx = thumbPosition * widthPx
            val thumbStartPx = (thumbCenterPx - thumbSizePx / 2f).coerceAtLeast(0f)
            val thumbStartDp = with(density) { thumbStartPx.toDp() }
            Box(
                modifier = Modifier
                    .padding(start = thumbStartDp)
                    .size(thumbSizeDp)
                    .clip(CircleShape)
                    .background(Color.White)
                    .border(2.dp, colors.borderStrong, CircleShape),
            )
        }
    }
}

// ─── HSV math helpers ─────────────────────────────────────────────────────────

internal fun colorToHsv(color: Color): Triple<Float, Float, Float> {
    val r = color.red;
    val g = color.green;
    val b = color.blue
    val max = maxOf(r, g, b);
    val min = minOf(r, g, b)
    val delta = max - min
    val h = when {
        delta == 0f -> 0f
        max == r -> (60f * (((g - b) / delta) % 6f)).let { if (it < 0f) it + 360f else it }
        max == g -> 60f * ((b - r) / delta + 2f)
        else -> 60f * ((r - g) / delta + 4f)
    }
    val s = if (max == 0f) 0f else delta / max
    return Triple(h, s, max)
}

internal fun hsvToColor(h: Float, s: Float, v: Float): Color {
    val c = v * s
    val x = c * (1f - abs((h / 60f) % 2f - 1f))
    val m = v - c
    val (r1, g1, b1) = when {
        h < 60f -> Triple(c, x, 0f)
        h < 120f -> Triple(x, c, 0f)
        h < 180f -> Triple(0f, c, x)
        h < 240f -> Triple(0f, x, c)
        h < 300f -> Triple(x, 0f, c)
        else -> Triple(c, 0f, x)
    }
    return Color(r1 + m, g1 + m, b1 + m)
}
