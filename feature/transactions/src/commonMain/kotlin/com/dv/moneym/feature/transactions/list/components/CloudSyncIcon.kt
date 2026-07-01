package com.dv.moneym.feature.transactions.list.components

import androidx.compose.animation.core.LinearEasing
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
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
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
    val arrowRotation by transition.animateFloat(
        initialValue = 0f,
        targetValue = if (isSyncing) 360f else 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1100, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "cloudSyncArrowSpin",
    )

    val semanticsModifier = if (contentDescription == null) {
        modifier
    } else {
        modifier.semantics { this.contentDescription = contentDescription }
    }

    Canvas(modifier = semanticsModifier) {
        val scale = size.minDimension / 24f
        val cloudStrokeWidth = 2.1.dp.toPx()
        val arrowStrokeWidth = 1.7.dp.toPx()
        val arrowScale = 0.76f

        fun sx(value: Float) = value * scale
        fun sy(value: Float) = value * scale
        fun point(x: Float, y: Float) = Offset(sx(x), sy(y))

        val center = point(12f, 12f)

        val cloud = Path().apply {
            moveTo(sx(5.5f), sy(18.4f))
            lineTo(sx(18.3f), sy(18.4f))
            cubicTo(sx(20.9f), sy(18.4f), sx(22.5f), sy(16.7f), sx(22.5f), sy(14.3f))
            cubicTo(sx(22.5f), sy(11.8f), sx(20.7f), sy(10.1f), sx(18.4f), sy(10.1f))
            cubicTo(sx(17.4f), sy(6.8f), sx(14.7f), sy(4.8f), sx(11.4f), sy(4.8f))
            cubicTo(sx(8.2f), sy(4.8f), sx(5.6f), sy(6.9f), sx(4.9f), sy(10.1f))
            cubicTo(sx(2.8f), sy(10.7f), sx(1.5f), sy(12.4f), sx(1.5f), sy(14.5f))
            cubicTo(sx(1.5f), sy(16.8f), sx(3.2f), sy(18.4f), sx(5.5f), sy(18.4f))
        }

        drawPath(
            path = cloud,
            color = tint,
            style = Stroke(
                width = cloudStrokeWidth,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round,
            ),
        )

        rotate(degrees = arrowRotation, pivot = center) {
            scale(scaleX = arrowScale, scaleY = arrowScale, pivot = center) {
                val topArrow = Path().apply {
                    moveTo(sx(9.1f), sy(9.0f))
                    cubicTo(sx(9.9f), sy(8.2f), sx(11.0f), sy(7.8f), sx(12.3f), sy(7.9f))
                    cubicTo(sx(13.7f), sy(8.0f), sx(14.8f), sy(8.7f), sx(15.5f), sy(9.8f))
                }
                drawPath(
                    path = topArrow,
                    color = tint,
                    style = Stroke(
                        width = arrowStrokeWidth,
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round,
                    ),
                )
                drawLine(
                    color = tint,
                    start = point(15.5f, 9.8f),
                    end = point(15.35f, 8.0f),
                    strokeWidth = arrowStrokeWidth,
                    cap = StrokeCap.Round,
                )
                drawLine(
                    color = tint,
                    start = point(15.5f, 9.8f),
                    end = point(13.7f, 9.7f),
                    strokeWidth = arrowStrokeWidth,
                    cap = StrokeCap.Round,
                )

                val bottomArrow = Path().apply {
                    moveTo(sx(14.9f), sy(15.0f))
                    cubicTo(sx(14.1f), sy(15.8f), sx(13.0f), sy(16.2f), sx(11.7f), sy(16.1f))
                    cubicTo(sx(10.3f), sy(16.0f), sx(9.2f), sy(15.3f), sx(8.5f), sy(14.2f))
                }
                drawPath(
                    path = bottomArrow,
                    color = tint,
                    style = Stroke(
                        width = arrowStrokeWidth,
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round,
                    ),
                )
                drawLine(
                    color = tint,
                    start = point(8.5f, 14.2f),
                    end = point(8.65f, 16.0f),
                    strokeWidth = arrowStrokeWidth,
                    cap = StrokeCap.Round,
                )
                drawLine(
                    color = tint,
                    start = point(8.5f, 14.2f),
                    end = point(10.3f, 14.3f),
                    strokeWidth = arrowStrokeWidth,
                    cap = StrokeCap.Round,
                )
            }
        }
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
