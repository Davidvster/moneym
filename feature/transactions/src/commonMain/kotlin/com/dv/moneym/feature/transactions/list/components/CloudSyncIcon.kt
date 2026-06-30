package com.dv.moneym.feature.transactions.list.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

@Composable
internal fun CloudSyncIcon(
    contentDescription: String?,
    tint: Color,
    isSyncing: Boolean,
    modifier: Modifier = Modifier,
) {
    val transition = rememberInfiniteTransition(label = "cloudSync")
    val arrowDrift by transition.animateFloat(
        initialValue = if (isSyncing) -1.2f else 0f,
        targetValue = if (isSyncing) 1.2f else 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 520),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "cloudSyncArrowDrift",
    )

    val semanticsModifier = if (contentDescription == null) {
        modifier
    } else {
        modifier.semantics { this.contentDescription = contentDescription }
    }
    Canvas(modifier = semanticsModifier) {
        val scale = size.minDimension / 24f
        val strokeWidth = 1.8.dp.toPx()
        fun sx(value: Float) = value * scale
        fun sy(value: Float) = value * scale
        fun point(x: Float, y: Float) = Offset(sx(x), sy(y))

        val cloud = Path().apply {
            moveTo(sx(6.8f), sy(18.5f))
            lineTo(sx(17.4f), sy(18.5f))
            cubicTo(sx(20.2f), sy(18.5f), sx(22f), sy(16.8f), sx(22f), sy(14.3f))
            cubicTo(sx(22f), sy(12.1f), sx(20.4f), sy(10.4f), sx(18f), sy(10.6f))
            cubicTo(sx(16.8f), sy(7.5f), sx(14.4f), sy(5.5f), sx(11.4f), sy(5.5f))
            cubicTo(sx(8.8f), sy(5.5f), sx(6.8f), sy(7.1f), sx(6.2f), sy(9.6f))
            cubicTo(sx(3.6f), sy(10.1f), sx(2f), sy(11.9f), sx(2f), sy(14.2f))
            cubicTo(sx(2f), sy(16.6f), sx(3.9f), sy(18.5f), sx(6.8f), sy(18.5f))
        }
        val stroke = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
        drawPath(path = cloud, color = tint, style = stroke)

        val upDrift = -arrowDrift
        val downDrift = arrowDrift
        drawLine(
            color = tint,
            start = point(9f, 17f + upDrift),
            end = point(9f, 11f + upDrift),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round,
        )
        drawLine(
            color = tint,
            start = point(6.8f, 13.2f + upDrift),
            end = point(9f, 11f + upDrift),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round,
        )
        drawLine(
            color = tint,
            start = point(11.2f, 13.2f + upDrift),
            end = point(9f, 11f + upDrift),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round,
        )
        drawLine(
            color = tint,
            start = point(15f, 11f + downDrift),
            end = point(15f, 17f + downDrift),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round,
        )
        drawLine(
            color = tint,
            start = point(12.8f, 14.8f + downDrift),
            end = point(15f, 17f + downDrift),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round,
        )
        drawLine(
            color = tint,
            start = point(17.2f, 14.8f + downDrift),
            end = point(15f, 17f + downDrift),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round,
        )
    }
}