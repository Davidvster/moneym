package com.dv.moneym.core.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import com.dv.moneym.core.designsystem.MM
import com.dv.moneym.core.designsystem.MoneyMTheme
import com.dv.moneym.core.model.Icon
import androidx.compose.ui.tooling.preview.Preview

enum class MmIconButtonVariant { Default, Accent, Danger }

@Composable
fun MmIconButton(
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: MmIconButtonVariant = MmIconButtonVariant.Default,
    size: Dp = MM.dimen.padding_5x,
    contentDescription: String? = null,
) {
    val colors = MM.colors
    val iconColor = when (variant) {
        MmIconButtonVariant.Default -> colors.text
        MmIconButtonVariant.Accent -> colors.accent
        MmIconButtonVariant.Danger -> colors.danger
    }

    Box(
        modifier = modifier
            .size(size)
            .mmClickable(
                bounded = false,
                radius = size / 2,
                rippleColor = iconColor,
                role = Role.Button,
                onClick = onClick,
            ),
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

@Preview
@Composable
private fun MmIconButtonPreview() {
    MoneyMTheme {
        Row(
            modifier = Modifier.padding(MM.dimen.padding_2x),
            horizontalArrangement = Arrangement.spacedBy(MM.dimen.padding_2x),
        ) {
            MmIconButton(icon = Icon.Plus.imageVector, onClick = {})
            MmIconButton(icon = Icon.Trash.imageVector, onClick = {}, variant = MmIconButtonVariant.Danger)
            MmIconButton(icon = Icon.Check.imageVector, onClick = {}, variant = MmIconButtonVariant.Accent)
        }
    }
}
