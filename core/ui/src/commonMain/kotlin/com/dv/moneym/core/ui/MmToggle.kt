package com.dv.moneym.core.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.dv.moneym.core.designsystem.MM

@Composable
fun MmToggle(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val colors = MM.colors
    val radius = MM.radius

    val trackWidth = 44.dp
    val trackHeight = 26.dp
    val thumbSize = MM.space.padding_2_5x
    val thumbPadding = 3.dp
    val thumbTravel = trackWidth - thumbSize - thumbPadding * 2

    val thumbX by animateFloatAsState(
        targetValue = if (checked) thumbTravel.value else 0f,
        animationSpec = spring(stiffness = 400f),
        label = "toggle_thumb",
    )

    val trackBg = if (checked) colors.text else colors.surface2
    val trackBorder = if (checked) colors.text else colors.borderStrong

    Box(
        modifier = modifier
            .size(trackWidth, trackHeight)
            .clip(radius.pill)
            .background(trackBg, radius.pill)
            .border(1.dp, trackBorder, radius.pill)
            .alpha(if (enabled) 1f else 0.45f)
            .pointerInput(enabled) {
                if (enabled) {
                    detectTapGestures(onTap = { onCheckedChange(!checked) })
                }
            },
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(
            modifier = Modifier
                .offset(x = thumbPadding + thumbX.dp)
                .size(thumbSize)
                .shadow(1.dp, CircleShape)
                .clip(CircleShape)
                .background(colors.bg, CircleShape),
        )
    }
}
