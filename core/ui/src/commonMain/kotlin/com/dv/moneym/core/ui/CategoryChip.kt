package com.dv.moneym.core.ui

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.style.TextOverflow
import com.dv.moneym.core.designsystem.MM
import com.dv.moneym.core.designsystem.categoryColor
import com.dv.moneym.core.model.Category
import com.dv.moneym.core.model.Icon
import com.dv.moneym.core.model.IndicatorStyle
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import com.dv.moneym.core.designsystem.MoneyMTheme
import com.dv.moneym.core.model.CategoryId
import com.dv.moneym.core.model.TransactionType
import androidx.compose.ui.tooling.preview.Preview
import kotlin.time.Instant

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

@Preview
@Composable
private fun CategoryChipPreview() {
    val sample = Category(
        id = CategoryId(1L),
        name = "Groceries",
        iconKey = "cart",
        colorHex = "#3B82F6",
        isUserCreated = false,
        archived = false,
        createdAt = Instant.fromEpochSeconds(0),
        updatedAt = Instant.fromEpochSeconds(0),
        type = TransactionType.EXPENSE,
    )
    MoneyMTheme {
        Row(
            modifier = Modifier.padding(MM.dimen.padding_2x),
            horizontalArrangement = Arrangement.spacedBy(MM.dimen.padding_1x),
        ) {
            CategoryChip(category = sample, isSelected = false, onClick = {})
            CategoryChip(category = sample.copy(name = "Eating Out"), isSelected = true, onClick = {})
        }
    }
}
