package com.dv.moneym.feature.transactionedit.ui.components

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.style.TextOverflow
import com.dv.moneym.core.designsystem.MM
import com.dv.moneym.core.designsystem.categoryColor
import com.dv.moneym.core.designsystem.iconForKey
import com.dv.moneym.core.model.Category
import com.dv.moneym.core.model.IndicatorStyle
import com.dv.moneym.core.ui.CategoryIconTile
import com.dv.moneym.core.ui.MmChip
import androidx.compose.ui.unit.dp

@Composable
internal fun CategoryChip(
    category: Category,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val colors = MM.colors
    val type = MM.type

    val catColor = categoryColor(category.colorHex)
    val catIcon = iconForKey(category.iconKey)

    MmChip(
        selected = isSelected,
        onClick = onClick,
        leadingContent = {
            CategoryIconTile(
                categoryName = category.name,
                categoryColor = catColor,
                categoryIcon = catIcon,
                size = MM.space.padding_2_5x,
                variant = IndicatorStyle.IconTile,
            )
        },
    ) {
        Text(
            text = category.name,
            style = type.caption,
            color = if (isSelected) colors.bg else colors.text,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
