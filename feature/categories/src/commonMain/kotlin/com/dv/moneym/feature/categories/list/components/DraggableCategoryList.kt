package com.dv.moneym.feature.categories.list.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.dv.moneym.core.designsystem.MM
import com.dv.moneym.core.designsystem.categoryColor
import com.dv.moneym.core.model.Category
import com.dv.moneym.core.model.IndicatorStyle
import com.dv.moneym.core.ui.CategoryIconTile
import com.dv.moneym.core.ui.MmIcons
import com.dv.moneym.core.ui.MmRow
import com.dv.moneym.feature.categories.list.resolveIconVector

@Composable
internal fun DraggableCategoryList(
    categories: List<Category>,
    onReorder: (Int, Int) -> Unit,
    onCategoryClick: (Category) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MM.colors
    val listState = rememberLazyListState()

    // Local shadow list for immediate visual reordering during drag
    var localCategories by remember(categories) { mutableStateOf(categories) }

    var draggingIndex by remember { mutableIntStateOf(-1) }
    var draggingFromIndex by remember { mutableIntStateOf(-1) }
    var dragOffsetY by remember { mutableFloatStateOf(0f) }

    LazyColumn(
        state = listState,
        modifier = modifier
            .fillMaxWidth(),
    ) {
        itemsIndexed(localCategories, key = { _, cat -> cat.id.value }) { index, cat ->
            val isDragging = index == draggingIndex
            val elevation by animateDpAsState(
                targetValue = if (isDragging) 6.dp else 0.dp,
                label = "row_elevation_$index",
            )

            MmRow(
                modifier = Modifier
                    .shadow(elevation)
                    .background(if (isDragging) colors.surface else colors.bg)
                    .pointerInput(Unit) {
                        detectDragGesturesAfterLongPress(
                            onDragStart = {
                                draggingIndex = index
                                draggingFromIndex = index
                                dragOffsetY = 0f
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                dragOffsetY += dragAmount.y
                                val itemHeight = listState.layoutInfo.visibleItemsInfo
                                    .firstOrNull { it.index == draggingIndex }
                                    ?.size?.toFloat() ?: 64f
                                val stepsFloat = dragOffsetY / itemHeight
                                val steps = stepsFloat.toInt()
                                if (steps != 0) {
                                    val targetIndex = (draggingIndex + steps)
                                        .coerceIn(0, localCategories.size - 1)
                                    if (targetIndex != draggingIndex) {
                                        // Update local list immediately for smooth UI
                                        val mutable = localCategories.toMutableList()
                                        val moved = mutable.removeAt(draggingIndex)
                                        mutable.add(targetIndex, moved)
                                        localCategories = mutable
                                        draggingIndex = targetIndex
                                        dragOffsetY -= steps * itemHeight
                                    }
                                }
                            },
                            onDragEnd = {
                                // Persist final order to repository
                                if (draggingFromIndex != -1 && draggingIndex != -1 &&
                                    draggingFromIndex != draggingIndex
                                ) {
                                    onReorder(draggingFromIndex, draggingIndex)
                                }
                                draggingIndex = -1
                                draggingFromIndex = -1
                                dragOffsetY = 0f
                            },
                            onDragCancel = {
                                // Revert local list to original categories on cancel
                                localCategories = categories
                                draggingIndex = -1
                                draggingFromIndex = -1
                                dragOffsetY = 0f
                            },
                        )
                    },
                onClick = { onCategoryClick(cat) },
                divider = index != localCategories.lastIndex,
            ) {
                Icon(
                    imageVector = MmIcons.dragHandle,
                    contentDescription = "Drag to reorder",
                    tint = colors.text3,
                    modifier = Modifier.size(MM.dimen.icon_1x),
                )
                CategoryIconTile(
                    categoryName = cat.name,
                    categoryColor = categoryColor(cat.colorHex),
                    categoryIcon = resolveIconVector(cat.iconKey),
                    size = 36.dp,
                    variant = IndicatorStyle.IconTile,
                )
                Text(
                    text = cat.name,
                    style = MM.type.body,
                    color = colors.text,
                    modifier = Modifier.weight(1f),
                )
                Icon(
                    imageVector = MmIcons.chevronRight,
                    contentDescription = null,
                    tint = colors.text3,
                    modifier = Modifier.size(MM.dimen.padding_2x),
                )
            }
        }
    }
}
