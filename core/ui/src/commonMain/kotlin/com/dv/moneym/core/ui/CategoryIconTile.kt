package com.dv.moneym.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.dv.moneym.core.designsystem.MM
import com.dv.moneym.core.designsystem.MoneyMTheme
import com.dv.moneym.core.designsystem.onColorFor
import com.dv.moneym.core.model.Icon
import com.dv.moneym.core.model.IndicatorStyle

@Composable
fun CategoryIconTile(
    categoryName: String,
    categoryColor: Color,
    categoryIcon: ImageVector,
    size: Dp,
    variant: IndicatorStyle,
    modifier: Modifier = Modifier,
) {
    when (variant) {
        IndicatorStyle.IconTile -> {
            val cornerFraction = (size.value * 0.30f).coerceAtLeast(6f)
            val tileShape = androidx.compose.foundation.shape.RoundedCornerShape(cornerFraction.dp)
            Box(
                modifier = modifier
                    .size(size)
                    .clip(tileShape)
                    .background(categoryColor, tileShape),
                contentAlignment = Alignment.Center,
            ) {
                val iconSize = size * 0.55f
                val painter = rememberVectorPainter(categoryIcon)
                androidx.compose.foundation.Image(
                    painter = painter,
                    contentDescription = categoryName,
                    modifier = Modifier.size(iconSize),
                    colorFilter = ColorFilter.tint(onColorFor(categoryColor)),
                )
            }
        }

        IndicatorStyle.SoftIcon -> {
            Box(
                modifier = modifier
                    .size(size)
                    .clip(CircleShape)
                    .background(categoryColor.copy(alpha = 0.13f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                val iconSize = size * 0.55f
                val painter = rememberVectorPainter(categoryIcon)
                androidx.compose.foundation.Image(
                    painter = painter,
                    contentDescription = categoryName,
                    modifier = Modifier.size(iconSize),
                    colorFilter = ColorFilter.tint(categoryColor),
                )
            }
        }

        IndicatorStyle.Bar -> {
            val barShape = androidx.compose.foundation.shape.RoundedCornerShape(2.dp)
            Box(
                modifier = modifier
                    .size(width = MM.dimen.padding_0_5x, height = size)
                    .clip(barShape)
                    .background(categoryColor, barShape),
            )
        }

        IndicatorStyle.Dot -> {
            Box(
                modifier = modifier
                    .size(MM.dimen.padding_1x)
                    .clip(CircleShape)
                    .background(categoryColor, CircleShape),
            )
        }

        IndicatorStyle.Minimal -> {
            Box(modifier = modifier)
        }
    }
}

@Preview
@Composable
private fun CategoryIconTilePreview() {
    MoneyMTheme {
        Row(
            modifier = Modifier.padding(MM.dimen.padding_2x),
            horizontalArrangement = Arrangement.spacedBy(MM.dimen.padding_2x),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CategoryIconTile(
                categoryName = "Groceries",
                categoryColor = MM.colors.accent,
                categoryIcon = Icon.List.imageVector,
                size = 36.dp,
                variant = IndicatorStyle.IconTile,
            )
            CategoryIconTile(
                categoryName = "Groceries",
                categoryColor = MM.colors.danger,
                categoryIcon = Icon.List.imageVector,
                size = 36.dp,
                variant = IndicatorStyle.SoftIcon,
            )
            CategoryIconTile(
                categoryName = "Groceries",
                categoryColor = MM.colors.accent,
                categoryIcon = Icon.List.imageVector,
                size = 36.dp,
                variant = IndicatorStyle.Bar,
            )
            CategoryIconTile(
                categoryName = "Groceries",
                categoryColor = MM.colors.accent,
                categoryIcon = Icon.List.imageVector,
                size = 36.dp,
                variant = IndicatorStyle.Dot,
            )
        }
    }
}
