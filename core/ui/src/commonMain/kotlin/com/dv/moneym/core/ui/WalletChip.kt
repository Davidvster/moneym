package com.dv.moneym.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.dv.moneym.core.designsystem.MM
import com.dv.moneym.core.designsystem.MoneyMTheme
import com.dv.moneym.core.designsystem.categoryColor
import com.dv.moneym.core.designsystem.onColorFor
import com.dv.moneym.core.model.Icon

@Composable
fun WalletChip(
    name: String,
    colorHex: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MM.colors
    val radius = MM.dimen

    val accentBg = colorHex?.let { categoryColor(it) }
    val bgColor = accentBg ?: Color.Transparent
    val borderColor = accentBg ?: colors.border
    val contentColor = accentBg?.let { onColorFor(it) } ?: colors.text
    val iconTint = accentBg?.let { onColorFor(it) } ?: colors.text2

    Row(
        modifier = modifier
            .height(34.dp)
            .clip(radius.pill)
            .background(bgColor, radius.pill)
            .border(1.dp, borderColor, radius.pill)
            .mmClickable(rippleColor = contentColor, onClick = onClick)
            .padding(horizontal = MM.dimen.padding_1_5x),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        androidx.compose.material3.Icon(
            imageVector = Icon.Wallet.imageVector,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(MM.dimen.padding_1_5x),
        )
        Text(
            text = name,
            style = MM.type.caption,
            color = contentColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
fun WalletColorDot(
    colorHex: String?,
    modifier: Modifier = Modifier,
    size: androidx.compose.ui.unit.Dp = MM.dimen.padding_2x,
) {
    val colors = MM.colors
    val fill = colorHex?.let { categoryColor(it) } ?: Color.Transparent
    Box(
        modifier = modifier
            .size(size)
            .clip(androidx.compose.foundation.shape.CircleShape)
            .background(fill, androidx.compose.foundation.shape.CircleShape)
            .border(1.dp, colors.border, androidx.compose.foundation.shape.CircleShape),
    )
}

@Preview
@Composable
private fun WalletChipPreview() {
    MoneyMTheme {
        Row(
            modifier = Modifier.padding(MM.dimen.padding_2x),
            horizontalArrangement = Arrangement.spacedBy(MM.dimen.padding_1x),
        ) {
            WalletChip(name = "Cash", colorHex = null, onClick = {})
            WalletChip(name = "Bank", colorHex = "#3B82F6", onClick = {})
            WalletChip(name = "Light", colorHex = "#EAEAEA", onClick = {})
        }
    }
}
