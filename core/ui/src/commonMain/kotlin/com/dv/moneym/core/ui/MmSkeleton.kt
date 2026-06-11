package com.dv.moneym.core.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import com.dv.moneym.core.designsystem.MM
import com.dv.moneym.core.designsystem.MoneyMTheme

@Composable
fun MmSkeleton(
    modifier: Modifier = Modifier,
    shape: Shape = MM.dimen.radius_1x,
) {
    val base = MM.colors.surface2
    val highlight = MM.colors.text3.copy(alpha = 0.08f)
    val transition = rememberInfiniteTransition(label = "skeleton")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "shimmer",
    )
    Box(
        modifier
            .clip(shape)
            .drawBehind {
                drawRect(base)
                val band = size.width.coerceAtLeast(1f)
                val center = progress * (size.width + 2 * band) - band
                drawRect(
                    Brush.linearGradient(
                        colors = listOf(Color.Transparent, highlight, Color.Transparent),
                        start = Offset(center - band, 0f),
                        end = Offset(center + band, size.height),
                    ),
                )
            },
    )
}

@Composable
fun MmSkeletonCircle(
    size: Dp,
    modifier: Modifier = Modifier,
) {
    MmSkeleton(modifier = modifier.size(size), shape = CircleShape)
}

@Preview
@Composable
private fun MmSkeletonPreview() {
    MoneyMTheme {
        Column(Modifier.padding(MM.dimen.padding_2x)) {
            MmSkeletonCircle(size = 38.dp)
            MmSkeleton(
                modifier = Modifier
                    .padding(top = MM.dimen.padding_1x)
                    .fillMaxWidth(0.55f)
                    .height(12.dp),
            )
            MmSkeleton(
                modifier = Modifier
                    .padding(top = MM.dimen.padding_1x)
                    .fillMaxWidth(0.35f)
                    .height(12.dp),
            )
        }
    }
}
