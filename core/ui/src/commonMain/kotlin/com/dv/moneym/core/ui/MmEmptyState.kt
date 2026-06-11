package com.dv.moneym.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import com.dv.moneym.core.designsystem.MM
import com.dv.moneym.core.designsystem.MoneyMTheme
import com.dv.moneym.core.model.Icon
import androidx.compose.ui.tooling.preview.Preview

@Composable
fun MmEmptyState(
    message: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    fillSize: Boolean = true,
) {
    Box(
        modifier = modifier
            .then(if (fillSize) Modifier.fillMaxSize() else Modifier.fillMaxWidth())
            .padding(MM.dimen.padding_3x),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(MM.dimen.padding_1_5x),
        ) {
            if (icon != null) {
                Box(
                    modifier = Modifier
                        .size(MM.dimen.padding_8x)
                        .clip(CircleShape)
                        .background(MM.colors.surface2),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MM.colors.text3,
                        modifier = Modifier.size(MM.dimen.iconXl),
                    )
                }
            }
            Text(
                text = message,
                style = MM.type.body,
                color = MM.colors.text2,
                textAlign = TextAlign.Center,
            )
            if (actionLabel != null && onAction != null) {
                MmButton(
                    text = actionLabel,
                    onClick = onAction,
                    variant = MmButtonVariant.Secondary,
                    size = MmButtonSize.Md,
                    modifier = Modifier.padding(top = MM.dimen.padding_0_5x),
                )
            }
        }
    }
}

@Preview
@Composable
private fun MmEmptyStatePreview() {
    MoneyMTheme {
        MmEmptyState(
            message = "No transactions yet",
            icon = Icon.List.imageVector,
            actionLabel = "Add your first transaction",
            onAction = {},
        )
    }
}
