package com.dv.moneym.feature.transactions.list.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.dv.moneym.core.designsystem.MM
import com.dv.moneym.core.designsystem.categoryColor
import com.dv.moneym.core.model.Category
import com.dv.moneym.core.model.CategoryId
import com.dv.moneym.core.model.Icon
import com.dv.moneym.core.model.IndicatorStyle
import com.dv.moneym.core.ui.CategoryIconTile
import com.dv.moneym.core.ui.MmButton
import com.dv.moneym.core.ui.MmButtonSize
import com.dv.moneym.core.ui.MmButtonVariant
import com.dv.moneym.core.ui.MmChip
import com.dv.moneym.core.ui.imageVector
import moneym.feature.transactions.generated.resources.Res
import moneym.feature.transactions.generated.resources.transactions_filter_categories
import moneym.feature.transactions.generated.resources.transactions_filter_clear
import moneym.feature.transactions.generated.resources.transactions_ok
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
internal fun CategoryFilterSheet(
    categories: List<Category>,
    selectedCategoryIds: Set<CategoryId>,
    onToggle: (CategoryId) -> Unit,
    onClearAll: () -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = MM.colors
    val type = MM.type
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(
            topStart = MM.dimen.padding_2_5x,
            topEnd = MM.dimen.padding_2_5x
        ),
        containerColor = colors.bg,
        dragHandle = null,
    ) {
        Column(
            modifier = Modifier.padding(
                horizontal = MM.dimen.padding_2_5x,
                vertical = MM.dimen.padding_3x
            ),
            verticalArrangement = Arrangement.spacedBy(MM.dimen.padding_2x),
        ) {
            // Grab handle
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .size(width = 36.dp, height = 4.dp)
                        .clip(RoundedCornerShape(50))
                        .background(colors.borderStrong),
                )
            }

            // Title row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(Res.string.transactions_filter_categories),
                    style = type.title3,
                    color = colors.text,
                    modifier = Modifier.weight(1f),
                )
                if (selectedCategoryIds.isNotEmpty()) {
                    TextButton(onClick = onClearAll) {
                        Text(
                            text = stringResource(Res.string.transactions_filter_clear),
                            color = colors.text2,
                            style = type.caption,
                        )
                    }
                }
            }

            // Category chips — multi-select
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(MM.dimen.padding_1x),
                verticalArrangement = Arrangement.spacedBy(MM.dimen.padding_1x),
            ) {
                categories.forEach { cat ->
                    val isSelected = cat.id in selectedCategoryIds
                    val catColor = categoryColor(cat.colorHex)
                    val catIcon = Icon.fromKeyOrDefault(cat.iconKey).imageVector
                    MmChip(
                        selected = isSelected,
                        onClick = { onToggle(cat.id) },
                        leadingContent = {
                            CategoryIconTile(
                                categoryName = cat.name,
                                categoryColor = catColor,
                                categoryIcon = catIcon,
                                size = MM.dimen.padding_2_5x,
                                variant = IndicatorStyle.IconTile,
                            )
                        },
                    ) {
                        Text(
                            text = cat.name,
                            style = type.caption,
                            color = if (isSelected) colors.bg else colors.text,
                            maxLines = 1,
                        )
                    }
                }
            }

            // Done button
            MmButton(
                text = stringResource(Res.string.transactions_ok),
                onClick = onDismiss,
                variant = MmButtonVariant.Primary,
                size = MmButtonSize.Lg,
                fullWidth = true,
            )

            Spacer(Modifier.height(MM.dimen.padding_1x))
        }
    }
}
