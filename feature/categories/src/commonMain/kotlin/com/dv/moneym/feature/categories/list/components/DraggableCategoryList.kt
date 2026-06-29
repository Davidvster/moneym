package com.dv.moneym.feature.categories.list.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.dv.moneym.core.designsystem.MM
import com.dv.moneym.core.designsystem.MoneyMTheme
import com.dv.moneym.core.designsystem.categoryColor
import com.dv.moneym.core.model.Category
import com.dv.moneym.core.model.CategoryId
import com.dv.moneym.core.model.Icon
import com.dv.moneym.core.model.IndicatorStyle
import com.dv.moneym.core.ui.CategoryIconTile
import com.dv.moneym.core.ui.MmRow
import com.dv.moneym.core.ui.imageVector
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

@Composable
internal fun DraggableCategoryList(
    categories: List<Category>,
    transactionCountsByCategoryId: Map<CategoryId, Int>,
    onReorder: (List<Category>) -> Unit,
    onCategoryClick: (Category) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MM.colors
    val haptic = LocalHapticFeedback.current
    val listState = rememberLazyListState()

    // Local shadow list for immediate visual reordering during drag; resets when source changes
    var localCategories by remember(categories) { mutableStateOf(categories) }

    val reorderableState = rememberReorderableLazyListState(listState) { from, to ->
        localCategories = localCategories.toMutableList().apply {
            add(to.index, removeAt(from.index))
        }
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
    }

    LazyColumn(
        state = listState,
        modifier = modifier,
    ) {
        itemsIndexed(localCategories, key = { _, cat -> cat.id.value }) { index, cat ->
            ReorderableItem(reorderableState, key = cat.id.value) { isDragging ->
                val elevation by animateDpAsState(
                    targetValue = if (isDragging) 6.dp else 0.dp,
                    label = "row_elevation_${cat.id.value}",
                )

                MmRow(
                    modifier = Modifier
                        .shadow(elevation)
                        .background(if (isDragging) colors.surface else colors.bg),
                    onClick = { onCategoryClick(cat) },
                    divider = index != localCategories.lastIndex,
                ) {
                    Icon(
                        imageVector = Icon.DragHandle.imageVector,
                        contentDescription = "Drag to reorder",
                        tint = colors.text3,
                        modifier = Modifier
                            .size(MM.dimen.icon_1x)
                            .draggableHandle(
                                onDragStarted = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                },
                                onDragStopped = { onReorder(localCategories) },
                            ),
                    )
                    CategoryIconTile(
                        categoryName = cat.name,
                        categoryColor = categoryColor(cat.colorHex),
                        categoryIcon = Icon.fromKeyOrDefault(cat.iconKey).imageVector,
                        size = 36.dp,
                        variant = IndicatorStyle.IconTile,
                    )
                    Text(
                        text = cat.name,
                        style = MM.type.body,
                        color = colors.text,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = (transactionCountsByCategoryId[cat.id] ?: 0).toString(),
                        style = MM.type.caption.copy(color = colors.text3),
                        modifier = Modifier.padding(end = MM.dimen.padding_1x),
                    )
                    Icon(
                        imageVector = Icon.ChevronRight.imageVector,
                        contentDescription = null,
                        tint = colors.text3,
                        modifier = Modifier.size(MM.dimen.padding_2x),
                    )
                }
            }
        }
    }
}

@Preview
@Composable
private fun DraggableCategoryListPreview() {
    val epoch = kotlin.time.Instant.fromEpochSeconds(0)
    val categories = listOf(
        Category(
            id = CategoryId(1),
            name = "Groceries",
            iconKey = "basket",
            colorHex = "#4A8E5C",
            isUserCreated = false,
            archived = false,
            createdAt = epoch,
            updatedAt = epoch,
        ),
        Category(
            id = CategoryId(2),
            name = "Transport",
            iconKey = "car",
            colorHex = "#3A82A5",
            isUserCreated = false,
            archived = false,
            createdAt = epoch,
            updatedAt = epoch,
        ),
        Category(
            id = CategoryId(3),
            name = "Eating Out",
            iconKey = "utensils",
            colorHex = "#E07A5F",
            isUserCreated = true,
            archived = false,
            createdAt = epoch,
            updatedAt = epoch,
        ),
    )
    MoneyMTheme {
        DraggableCategoryList(
            categories = categories,
            transactionCountsByCategoryId = mapOf(CategoryId(1) to 3, CategoryId(2) to 0, CategoryId(3) to 8),
            onReorder = {},
            onCategoryClick = {},
            modifier = Modifier.fillMaxWidth().height(300.dp),
        )
    }
}
