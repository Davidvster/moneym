package com.dv.moneym.core.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.gestures.horizontalDrag
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.dv.moneym.core.designsystem.MM
import com.dv.moneym.core.designsystem.MoneyMColors
import com.dv.moneym.core.designsystem.MoneyMTheme
import moneym.core.ui.generated.resources.Res
import moneym.core.ui.generated.resources.colorpicker_brightness
import moneym.core.ui.generated.resources.colorpicker_cancel
import moneym.core.ui.generated.resources.colorpicker_hex
import moneym.core.ui.generated.resources.colorpicker_select
import moneym.core.ui.generated.resources.colorpicker_title
import org.jetbrains.compose.resources.stringResource
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

// Using Dialog instead of ModalBottomSheet to avoid gesture conflicts with parent sheet

@Composable
fun HsvColorPickerDialog(
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
        mutableStateOf(colorToHex(currentColor))
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

@Preview
@Composable
private fun HsvColorPickerDialogPreview() {
    MoneyMTheme {
        HsvColorPickerDialog(
            initialColor = Color(0.29f, 0.56f, 0.36f),
            onDismiss = {},
            onColorSelected = {},
        )
    }
}

@Composable
private fun HsvColorPickerContent(
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
        MmSheetHeader(
            title = stringResource(Res.string.colorpicker_title),
            onClose = onDismiss,
        )

        Box(
            Modifier
                .fillMaxWidth()
                .height(MM.dimen.padding_6x)
                .clip(RoundedCornerShape(MM.dimen.padding_1_5x))
                .background(currentColor),
        )

        HueSaturationWheel(
            hue = hue,
            saturation = saturation,
            brightness = brightness,
            onHueChange = onHueChange,
            onSaturationChange = onSaturationChange,
            colors = colors,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(vertical = MM.dimen.padding_1x)
                .size(240.dp),
        )

        Text(
            stringResource(Res.string.colorpicker_brightness),
            style = type.caption.copy(color = colors.text2),
        )
        HsvSlider(
            gradient = Brush.horizontalGradient(
                listOf(
                    Color.Black,
                    hsvToColor(hue, saturation.coerceAtLeast(0.3f), 1f),
                )
            ),
            thumbPosition = brightness,
            onPositionChanged = onBrightnessChange,
            colors = colors,
        )

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
            label = stringResource(Res.string.colorpicker_hex),
            placeholder = "#4A8E5C",
            modifier = Modifier.fillMaxWidth(),
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(MM.dimen.padding_1_5x),
        ) {
            MmButton(
                text = stringResource(Res.string.colorpicker_cancel),
                onClick = onDismiss,
                variant = MmButtonVariant.Secondary,
                modifier = Modifier.weight(1f),
            )
            MmButton(
                text = stringResource(Res.string.colorpicker_select),
                onClick = { onColorSelected(currentColor) },
                variant = MmButtonVariant.Accent,
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(Modifier.height(MM.dimen.padding_1x))
    }
}

@Composable
private fun HueSaturationWheel(
    hue: Float,
    saturation: Float,
    brightness: Float,
    onHueChange: (Float) -> Unit,
    onSaturationChange: (Float) -> Unit,
    colors: MoneyMColors,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val latestOnHueChange by rememberUpdatedState(onHueChange)
    val latestOnSaturationChange by rememberUpdatedState(onSaturationChange)
    val sweepColors = remember { (0..6).map { i -> hsvToColor(i * 60f, 1f, 1f) } }
    val thumbRadiusPx = with(density) { 10.dp.toPx() }
    val thumbStrokePx = with(density) { 2.dp.toPx() }

    Canvas(
        modifier = modifier.pointerInput(Unit) {
            fun emit(pos: Offset) {
                val r = min(size.width, size.height) / 2f
                val dx = pos.x - size.width / 2f
                val dy = pos.y - size.height / 2f
                val sat = (hypot(dx, dy) / r).coerceIn(0f, 1f)
                var deg = atan2(dy, dx) * 180f / PI.toFloat()
                if (deg < 0f) deg += 360f
                latestOnHueChange(deg)
                latestOnSaturationChange(sat)
            }
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false)
                emit(down.position)
                drag(down.id) { change ->
                    change.consume()
                    emit(change.position)
                }
            }
        },
    ) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val r = size.minDimension / 2f
        drawCircle(Brush.sweepGradient(sweepColors, center), radius = r, center = center)
        drawCircle(
            Brush.radialGradient(
                listOf(Color.White, Color.White.copy(alpha = 0f)),
                center = center,
                radius = r,
            ),
            radius = r,
            center = center,
        )
        if (brightness < 1f) {
            drawCircle(Color.Black.copy(alpha = 1f - brightness), radius = r, center = center)
        }
        val angleRad = hue * PI.toFloat() / 180f
        val thumb = Offset(
            center.x + cos(angleRad) * saturation * r,
            center.y + sin(angleRad) * saturation * r,
        )
        drawCircle(Color.White, radius = thumbRadiusPx, center = thumb)
        drawCircle(
            colors.borderStrong,
            radius = thumbRadiusPx,
            center = thumb,
            style = Stroke(width = thumbStrokePx),
        )
    }
}

@Composable
private fun HsvSlider(
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
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .padding(start = thumbStartDp)
                    .fillMaxHeight()
                    .width(thumbSizeDp),
            ) {
                Box(
                    modifier = Modifier
                        .size(thumbSizeDp)
                        .clip(CircleShape)
                        .background(Color.White)
                        .border(2.dp, colors.borderStrong, CircleShape),
                )
            }
        }
    }
}

fun colorToHex(color: Color): String {
    fun Int.hex2() = toString(16).padStart(2, '0').uppercase()
    val r = (color.red * 255).roundToInt()
    val g = (color.green * 255).roundToInt()
    val b = (color.blue * 255).roundToInt()
    return "#${r.hex2()}${g.hex2()}${b.hex2()}"
}

internal fun colorToHsv(color: Color): Triple<Float, Float, Float> {
    val r = color.red
    val g = color.green
    val b = color.blue
    val max = maxOf(r, g, b)
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
