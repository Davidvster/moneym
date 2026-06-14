package com.dv.moneym.core.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.dv.moneym.core.designsystem.MM
import com.dv.moneym.core.designsystem.MoneyMTheme

@Composable
fun MmCheckbox(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val colors = MM.colors

    val fillColor by animateColorAsState(
        targetValue = if (checked) colors.accent else Color.Transparent,
    )
    val borderColor by animateColorAsState(
        targetValue = if (checked) colors.accent else colors.borderStrong,
    )
    val checkProgress by animateFloatAsState(
        targetValue = if (checked) 1f else 0f,
    )

    val toggleModifier = if (onCheckedChange != null) {
        Modifier.toggleable(
            value = checked,
            enabled = enabled,
            role = Role.Checkbox,
            onValueChange = onCheckedChange,
        )
    } else {
        Modifier
    }

    Box(
        modifier = modifier
            .then(toggleModifier)
            .padding(2.dp)
            .size(20.dp)
            .alpha(if (enabled) 1f else 0.4f)
            .clip(CircleShape)
            .background(fillColor)
            .border(MM.dimen.strokeThin, borderColor, CircleShape)
            .drawWithCache {
                // Checkmark drawn from three points, trimmed by progress 0..1.
                val w = size.width
                val h = size.height
                val start = Offset(w * 0.28f, h * 0.52f)
                val mid = Offset(w * 0.44f, h * 0.68f)
                val end = Offset(w * 0.74f, h * 0.34f)
                val stroke = Stroke(
                    width = w * 0.10f,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round,
                )
                onDrawWithContent {
                    drawContent()
                    val p = checkProgress
                    if (p <= 0f) return@onDrawWithContent
                    // First leg length is shorter than the second; split the trim ~40/60.
                    val firstLegEnd = 0.4f
                    val path = Path().apply {
                        moveTo(start.x, start.y)
                        if (p <= firstLegEnd) {
                            val t = p / firstLegEnd
                            lineTo(
                                start.x + (mid.x - start.x) * t,
                                start.y + (mid.y - start.y) * t,
                            )
                        } else {
                            lineTo(mid.x, mid.y)
                            val t = (p - firstLegEnd) / (1f - firstLegEnd)
                            lineTo(
                                mid.x + (end.x - mid.x) * t,
                                mid.y + (end.y - mid.y) * t,
                            )
                        }
                    }
                    drawPath(path = path, color = Color.White, style = stroke)
                }
            },
    )
}

@Preview
@Composable
private fun MmCheckboxPreview() {
    MoneyMTheme {
        MmCheckbox(checked = true, onCheckedChange = {})
    }
}

@Preview
@Composable
private fun MmCheckboxUncheckedPreview() {
    MoneyMTheme {
        MmCheckbox(checked = false, onCheckedChange = {})
    }
}
