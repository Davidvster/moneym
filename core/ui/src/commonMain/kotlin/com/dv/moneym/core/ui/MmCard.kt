package com.dv.moneym.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.dp
import com.dv.moneym.core.designsystem.MM

@Composable
fun MmCard(
    modifier: Modifier = Modifier,
    padded: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colors = MM.colors
    val radius = MM.radius

    Box(
        modifier = modifier
            .shadow(1.dp, radius.lg)
            .clip(radius.lg)
            .background(colors.surface, radius.lg)
            .border(1.dp, colors.border, radius.lg)
            .then(if (padded) Modifier.padding(20.dp) else Modifier),
    ) {
        content()
    }
}
