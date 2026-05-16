package com.dv.moneym.core.ui

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.dv.moneym.core.designsystem.MM

enum class MmIconButtonVariant { Default, Accent, Danger }

@Composable
fun MmIconButton(
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: MmIconButtonVariant = MmIconButtonVariant.Default,
    size: Dp = 40.dp,
    contentDescription: String? = null,
) {
    val colors = MM.colors
    val iconColor = when (variant) {
        MmIconButtonVariant.Default -> colors.text
        MmIconButtonVariant.Accent -> colors.accent
        MmIconButtonVariant.Danger -> colors.danger
    }

    var pressed by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .size(size)
            .alpha(if (pressed) 0.6f else 1f)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        pressed = true
                        tryAwaitRelease()
                        pressed = false
                    },
                    onTap = { onClick() },
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = iconColor,
            modifier = Modifier.size(22.dp),
        )
    }
}
