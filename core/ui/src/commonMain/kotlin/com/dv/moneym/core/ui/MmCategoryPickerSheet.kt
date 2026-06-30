package com.dv.moneym.core.ui

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
import com.dv.moneym.core.model.TransactionType
import com.dv.moneym.core.ui.SectionLabel
import moneym.core.ui.generated.resources.Res
import moneym.core.ui.generated.resources.category_picker_clear
import moneym.core.ui.generated.resources.category_picker_select_title
import moneym.core.ui.generated.resources.category_picker_title
import moneym.core.ui.generated.resources.mm_close
import moneym.core.ui.generated.resources.picker_ok
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun MmCategoryPickerSheet(
    categories: List<Category>,
    selectedIds: Set<CategoryId>,
    onToggle: (CategoryId) -> Unit,
    onClearAll: (() -> Unit)?,
    onDismiss: () -> Unit,
    groupedByType: Boolean = false,
    groupLabelForType: ((TransactionType) -> StringResource)? = null,
) {
    CategoryPickerSheetImpl(
        categories = categories,
        selectedIds = selectedIds,
        onCategoryClick = onToggle,
        onClearAll = onClearAll,
        onDismiss = onDismiss,
        titleRes = Res.string.category_picker_title,
        showConfirmButton = true,
        groupedByType = groupedByType,
        groupLabelForType = groupLabelForType,
    )
}

@Composable
fun MmCategoryPickerSheet(
    categories: List<Category>,
    selectedId: CategoryId?,
    onPick: (CategoryId) -> Unit,
    onDismiss: () -> Unit,
    groupedByType: Boolean = false,
    groupLabelForType: ((TransactionType) -> StringResource)? = null,
) {
    CategoryPickerSheetImpl(
        categories = categories,
        selectedIds = selectedId?.let { setOf(it) } ?: emptySet(),
        onCategoryClick = {
            onPick(it)
            onDismiss()
        },
        onClearAll = null,
        onDismiss = onDismiss,
        titleRes = Res.string.category_picker_select_title,
        showConfirmButton = false,
        groupedByType = groupedByType,
        groupLabelForType = groupLabelForType,
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun CategoryPickerSheetImpl(
    categories: List<Category>,
    selectedIds: Set<CategoryId>,
    onCategoryClick: (CategoryId) -> Unit,
    onClearAll: (() -> Unit)?,
    onDismiss: () -> Unit,
    titleRes: StringResource,
    showConfirmButton: Boolean,
    groupedByType: Boolean,
    groupLabelForType: ((TransactionType) -> StringResource)?,
) {
    val colors = MM.colors
    val type = MM.type
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(
            topStart = MM.dimen.padding_2_5x,
            topEnd = MM.dimen.padding_2_5x,
        ),
        containerColor = colors.bg,
        dragHandle = null,
    ) {
        Column(
            modifier = Modifier.padding(
                horizontal = MM.dimen.padding_2_5x,
                vertical = MM.dimen.padding_3x,
            ),
            verticalArrangement = Arrangement.spacedBy(MM.dimen.padding_2x),
        ) {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .size(width = 36.dp, height = MM.dimen.padding_0_5x)
                        .clip(RoundedCornerShape(50))
                        .background(colors.borderStrong),
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(titleRes),
                    style = type.title3,
                    color = colors.text,
                    modifier = Modifier.weight(1f),
                )
                if (onClearAll != null && selectedIds.isNotEmpty()) {
                    TextButton(onClick = onClearAll) {
                        Text(
                            text = stringResource(Res.string.category_picker_clear),
                            color = colors.text2,
                            style = type.caption,
                        )
                    }
                }
                MmIconButton(
                    icon = Icon.Close.imageVector,
                    onClick = onDismiss,
                    contentDescription = stringResource(Res.string.mm_close),
                )
            }

            if (groupedByType && groupLabelForType != null) {
                Column(verticalArrangement = Arrangement.spacedBy(MM.dimen.padding_2x)) {
                    TransactionType.entries.forEach { categoryType ->
                        val typedCategories = categories.filter { it.type == categoryType }
                        if (typedCategories.isNotEmpty()) {
                            Column(verticalArrangement = Arrangement.spacedBy(MM.dimen.padding_1x)) {
                                SectionLabel(
                                    text = stringResource(groupLabelForType(categoryType)),
                                    modifier = Modifier.padding(top = MM.dimen.padding_0_5x),
                                )
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(MM.dimen.padding_1x),
                                    verticalArrangement = Arrangement.spacedBy(MM.dimen.padding_1x),
                                ) {
                                    typedCategories.forEach { cat ->
                                        CategoryChip(
                                            cat = cat,
                                            selected = cat.id in selectedIds,
                                            onCategoryClick = onCategoryClick,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(MM.dimen.padding_1x),
                    verticalArrangement = Arrangement.spacedBy(MM.dimen.padding_1x),
                ) {
                    categories.forEach { cat ->
                        CategoryChip(
                            cat = cat,
                            selected = cat.id in selectedIds,
                            onCategoryClick = onCategoryClick,
                        )
                    }
                }
            }

            if (showConfirmButton) {
                MmButton(
                    text = stringResource(Res.string.picker_ok),
                    onClick = onDismiss,
                    variant = MmButtonVariant.Primary,
                    size = MmButtonSize.Lg,
                    fullWidth = true,
                )
            }

            Spacer(Modifier.height(MM.dimen.padding_1x))
        }
    }
}

@Composable
private fun CategoryChip(
    cat: Category,
    selected: Boolean,
    onCategoryClick: (CategoryId) -> Unit,
) {
    val catColor = categoryColor(cat.colorHex)
    val catIcon = Icon.fromKeyOrDefault(cat.iconKey).imageVector
    val type = MM.type
    val colors = MM.colors
    MmChip(
        selected = selected,
        onClick = { onCategoryClick(cat.id) },
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
            color = if (selected) colors.bg else colors.text,
            maxLines = 1,
        )
    }
}
