package com.dv.moneym.core.ui

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.style.TextOverflow
import com.dv.moneym.core.designsystem.MM
import com.dv.moneym.core.designsystem.categoryColor
import com.dv.moneym.core.model.Category
import com.dv.moneym.core.model.Icon
import com.dv.moneym.core.model.IndicatorStyle

@Composable
fun CategoryChip(
    category: Category,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val colors = MM.colors
    val type = MM.type

    val catColor = categoryColor(category.colorHex)
    val catIcon = Icon.fromKeyOrDefault(category.iconKey).imageVector

    MmChip(
        selected = isSelected,
        onClick = onClick,
        leadingContent = {
            CategoryIconTile(
                categoryName = category.name,
                categoryColor = catColor,
                categoryIcon = catIcon,
                size = MM.dimen.padding_2_5x,
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
