package com.dv.moneym.core.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.dv.moneym.core.designsystem.MM
import com.dv.moneym.core.designsystem.MoneyMTheme

enum class MmButtonVariant { Primary, Secondary, Ghost, Outline, Accent, Danger }
enum class MmButtonSize { Sm, Md, Lg }

@Composable
fun MmButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: MmButtonVariant = MmButtonVariant.Primary,
    size: MmButtonSize = MmButtonSize.Md,
    leadingIcon: ImageVector? = null,
    enabled: Boolean = true,
    fullWidth: Boolean = false,
) {
    val colors = MM.colors
    val type = MM.type
    val radius = MM.dimen

    val height: Dp = when (size) {
        MmButtonSize.Sm -> MM.dimen.padding_4x
        MmButtonSize.Md -> 44.dp
        MmButtonSize.Lg -> 52.dp
    }

    val horizontalPadding: Dp = when (size) {
        MmButtonSize.Sm -> MM.dimen.padding_1_5x
        MmButtonSize.Md -> MM.dimen.padding_2x
        MmButtonSize.Lg -> MM.dimen.padding_2_5x
    }

    val bgColor: Color
    val fgColor: Color
    val borderColor: Color?

    if (!enabled) {
        bgColor = colors.surface2
        fgColor = colors.text3
        borderColor = colors.border
    } else {
        when (variant) {
            MmButtonVariant.Primary -> {
                bgColor = colors.text
                fgColor = colors.bg
                borderColor = null
            }

            MmButtonVariant.Secondary -> {
                bgColor = colors.surface2
                fgColor = colors.text
                borderColor = colors.border
            }

            MmButtonVariant.Ghost -> {
                bgColor = Color.Transparent
                fgColor = colors.text
                borderColor = null
            }

            MmButtonVariant.Outline -> {
                bgColor = Color.Transparent
                fgColor = colors.text
                borderColor = colors.borderStrong
            }

            MmButtonVariant.Accent -> {
                bgColor = colors.accent
                fgColor = Color.White
                borderColor = null
            }

            MmButtonVariant.Danger -> {
                bgColor = Color.Transparent
                fgColor = colors.danger
                borderColor = colors.border
            }
        }
    }

    val shape = radius.radius_2_5x

    val sizeModifier = if (fullWidth) Modifier.fillMaxWidth() else Modifier

    Box(
        modifier = modifier
            .then(sizeModifier)
            .height(height)
            .clip(shape)
            .background(bgColor, shape)
            .then(
                if (borderColor != null) Modifier.border(1.dp, borderColor, shape) else Modifier,
            )
            .mmClickable(enabled = enabled, rippleColor = fgColor, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = horizontalPadding),
            horizontalArrangement = Arrangement.spacedBy(MM.dimen.padding_1x),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (leadingIcon != null) {
                val painter = rememberVectorPainter(leadingIcon)
                Image(
                    painter = painter,
                    contentDescription = null,
                    modifier = Modifier.size(MM.dimen.padding_2x),
                    colorFilter = ColorFilter.tint(fgColor),
                )
            }
            Text(
                text = text,
                style = type.body,
                color = fgColor,
            )
        }
    }
}

@Preview
@Composable
private fun MmButtonPreview() {
    MoneyMTheme {
        Column(
            Modifier.padding(MM.dimen.padding_2x),
            verticalArrangement = Arrangement.spacedBy(MM.dimen.padding_1x)
        ) {
            MmButton("Primary", onClick = {}, variant = MmButtonVariant.Primary)
            MmButton("Secondary", onClick = {}, variant = MmButtonVariant.Secondary)
            MmButton("Accent", onClick = {}, variant = MmButtonVariant.Accent)
            MmButton("Danger", onClick = {}, variant = MmButtonVariant.Danger)
        }
    }
}
