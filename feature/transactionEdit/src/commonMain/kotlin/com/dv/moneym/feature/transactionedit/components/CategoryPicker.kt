package com.dv.moneym.feature.transactionedit.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.dv.moneym.core.designsystem.MM
import com.dv.moneym.core.model.Category
import com.dv.moneym.core.model.CategoryId
import com.dv.moneym.core.ui.CategoryChip
import moneym.feature.transactionedit.generated.resources.Res
import moneym.feature.transactionedit.generated.resources.edit_category_error
import moneym.feature.transactionedit.generated.resources.edit_category_label
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun CategoryPicker(
    categories: List<Category>,
    selectedCategoryId: CategoryId?,
    categoryError: Boolean,
    onCategorySelected: (CategoryId) -> Unit,
) {
    val colors = MM.colors
    val type = MM.type

    Text(
        text = stringResource(Res.string.edit_category_label).uppercase(),
        style = type.micro,
        color = if (categoryError) colors.danger else colors.text3,
    )
    Spacer(Modifier.height(MM.dimen.padding_1_5x))

    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(MM.dimen.padding_1x),
        verticalArrangement = Arrangement.spacedBy(MM.dimen.padding_1x),
    ) {
        categories.forEach { cat ->
            CategoryChip(
                category = cat,
                isSelected = cat.id == selectedCategoryId,
                onClick = { onCategorySelected(cat.id) },
            )
        }
    }

    // Category error message
    if (categoryError) {
        Spacer(Modifier.height(MM.dimen.padding_1x))
        Text(
            text = stringResource(Res.string.edit_category_error),
            style = type.caption,
            color = colors.danger,
        )
    }
}
