package com.dv.moneym.feature.transactions.list.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.dv.moneym.core.designsystem.MoneyMTheme

@Composable
internal fun CloudSyncIcon(
    contentDescription: String?,
    tint: Color,
    isSyncing: Boolean,
    modifier: Modifier = Modifier,
) {
    val transition = rememberInfiniteTransition(label = "cloudSync")
    val arrowDrift by transition.animateFloat(
        initialValue = if (isSyncing) -1.1f else 0f,
        targetValue = if (isSyncing) 1.1f else 0f,
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
        val cloudStrokeWidth = 1.6.dp.toPx()
        val arrowStrokeWidth = 1.3.dp.toPx()
        fun sx(value: Float) = value * scale
        fun sy(value: Float) = value * scale
        fun point(x: Float, y: Float) = Offset(sx(x), sy(y))

        val cloud = Path().apply {
            moveTo(sx(4.5f), sy(18f))
            lineTo(sx(18.8f), sy(18f))
            cubicTo(sx(20.9f), sy(18f), sx(22f), sy(16.5f), sx(22f), sy(14.6f))
            cubicTo(sx(22f), sy(12.2f), sx(20.4f), sy(10.6f), sx(18.2f), sy(10.7f))
            cubicTo(sx(17.2f), sy(7.3f), sx(14.5f), sy(5.4f), sx(11.2f), sy(5.4f))
            cubicTo(sx(8.3f), sy(5.4f), sx(5.9f), sy(7.2f), sx(5.1f), sy(10.1f))
            cubicTo(sx(3f), sy(10.5f), sx(1.8f), sy(12.3f), sx(1.8f), sy(14.4f))
            cubicTo(sx(1.8f), sy(16.7f), sx(3.7f), sy(18f), sx(4.5f), sy(18f))
        }
        val cloudStroke = Stroke(width = cloudStrokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
        drawPath(path = cloud, color = tint, style = cloudStroke)

        val upDrift = -arrowDrift
        val downDrift = arrowDrift
        drawLine(
            color = tint,
            start = point(9f, 16.7f + upDrift),
            end = point(9f, 11.3f + upDrift),
            strokeWidth = arrowStrokeWidth,
            cap = StrokeCap.Round,
        )
        drawLine(
            color = tint,
            start = point(6.9f, 13.1f + upDrift),
            end = point(9f, 11.3f + upDrift),
            strokeWidth = arrowStrokeWidth,
            cap = StrokeCap.Round,
        )
        drawLine(
            color = tint,
            start = point(11.1f, 13.1f + upDrift),
            end = point(9f, 11.3f + upDrift),
            strokeWidth = arrowStrokeWidth,
            cap = StrokeCap.Round,
        )
        drawLine(
            color = tint,
            start = point(15f, 11.2f + downDrift),
            end = point(15f, 16.7f + downDrift),
            strokeWidth = arrowStrokeWidth,
            cap = StrokeCap.Round,
        )
        drawLine(
            color = tint,
            start = point(12.9f, 14.9f + downDrift),
            end = point(15f, 16.7f + downDrift),
            strokeWidth = arrowStrokeWidth,
            cap = StrokeCap.Round,
        )
        drawLine(
            color = tint,
            start = point(17.1f, 14.9f + downDrift),
            end = point(15f, 16.7f + downDrift),
            strokeWidth = arrowStrokeWidth,
            cap = StrokeCap.Round,
        )
    }
}

@Preview
@Composable
private fun CloudSyncIconPreviewLight() {
    MoneyMTheme(darkTheme = false) {
        CloudSyncIcon(
            contentDescription = null,
            tint = Color.Black,
            isSyncing = false,
            modifier = Modifier.size(24.dp),
        )
    }
}

@Preview
@Composable
private fun CloudSyncIconPreviewDark() {
    MoneyMTheme(darkTheme = true) {
        CloudSyncIcon(
            contentDescription = null,
            tint = Color.White,
            isSyncing = true,
            modifier = Modifier.size(24.dp),
        )
    }
}
