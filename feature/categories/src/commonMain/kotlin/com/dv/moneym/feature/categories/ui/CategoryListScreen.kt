package com.dv.moneym.feature.categories.ui

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.dv.moneym.core.designsystem.MM
import com.dv.moneym.core.designsystem.categoryColor
import com.dv.moneym.core.model.Category
import com.dv.moneym.core.model.CategoryId
import com.dv.moneym.core.model.IndicatorStyle
import com.dv.moneym.core.navigation.ModalKey
import com.dv.moneym.core.ui.CategoryIconTile
import com.dv.moneym.core.ui.MmButton
import com.dv.moneym.core.ui.MmButtonSize
import com.dv.moneym.core.ui.MmButtonVariant
import com.dv.moneym.core.ui.MmField
import com.dv.moneym.core.ui.MmIconButton
import com.dv.moneym.core.ui.MmIcons
import com.dv.moneym.core.ui.MmRow
import com.dv.moneym.core.ui.MmSegmented
import com.dv.moneym.core.ui.ScreenHeader
import com.dv.moneym.feature.categories.presentation.CategoryListEffect
import com.dv.moneym.feature.categories.presentation.CategoryListIntent
import com.dv.moneym.feature.categories.presentation.CategoryListViewModel
import com.dv.moneym.feature.categories.presentation.CategoryTab
import kotlinx.serialization.Serializable
import moneym.feature.categories.generated.resources.Res
import moneym.feature.categories.generated.resources.categories_cancel
import moneym.feature.categories.generated.resources.categories_color_label
import moneym.feature.categories.generated.resources.categories_icon_label
import moneym.feature.categories.generated.resources.categories_name_label
import moneym.feature.categories.generated.resources.categories_title
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Serializable
data object CategoriesKey : ModalKey

fun EntryProviderScope<NavKey>.categoriesEntry(
    onEditCategory: (CategoryId?) -> Unit,
    onBack: () -> Unit,
    metadata: Map<String, Any> = emptyMap(),
) = entry<CategoriesKey>(metadata = metadata) {
    CategoryListScreen(
        onEditCategory = onEditCategory,
        onBack = onBack,
    )
}

@Composable
fun CategoryListScreen(
    onEditCategory: (CategoryId?) -> Unit,
    onBack: () -> Unit,
    viewModel: CategoryListViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is CategoryListEffect.NavigateToEdit -> onEditCategory(effect.id)
            }
        }
    }

    ManageCategoriesScreen(
        categories = state.orderedCategories,
        activeTab = state.activeTab,
        onBack = onBack,
        onSetTab = { viewModel.setTab(it) },
        onReorder = { from, to -> viewModel.reorder(from, to) },
        onCreateCategory = { name, iconKey, colorHex ->
            viewModel.createCategory(name, iconKey, colorHex)
        },
        onUpdateCategory = { id, name, iconKey, colorHex ->
            viewModel.updateCategory(id, name, iconKey, colorHex)
        },
        onDeleteCategory = { id -> viewModel.deleteCategory(id) },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ManageCategoriesScreen(
    categories: List<Category>,
    activeTab: CategoryTab,
    onBack: () -> Unit,
    onSetTab: (CategoryTab) -> Unit,
    onReorder: (Int, Int) -> Unit,
    onCreateCategory: (String, String, String) -> Unit,
    onUpdateCategory: (CategoryId, String, String, String) -> Unit,
    onDeleteCategory: (CategoryId) -> Unit,
) {
    val colors = MM.colors

    var showNewCategorySheet by remember { mutableStateOf(false) }
    var categoryToEdit by remember { mutableStateOf<Category?>(null) }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bg),
    ) {
        ScreenHeader(
            title = stringResource(Res.string.categories_title),
            onBack = onBack,
            trailingContent = {
                MmButton(
                    text = "New",
                    onClick = {
                        categoryToEdit = null
                        showNewCategorySheet = true
                    },
                    variant = MmButtonVariant.Ghost,
                    size = MmButtonSize.Sm,
                    leadingIcon = MmIcons.plus,
                )
            },
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            MmSegmented(
                options = listOf("Expense", "Income"),
                selectedIndex = if (activeTab == CategoryTab.Expense) 0 else 1,
                onOptionSelected = { onSetTab(if (it == 0) CategoryTab.Expense else CategoryTab.Income) },
                fillWidth = true,
            )
        }

        Text(
            text = "${categories.size} ${if (activeTab == CategoryTab.Expense) "expense" else "income"} categories · long-press to reorder",
            style = MM.type.caption.copy(color = colors.text3),
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 12.dp),
        )

        val listState = rememberLazyListState()
        var draggingIndex by remember { mutableIntStateOf(-1) }
        var dragOffsetY by remember { mutableFloatStateOf(0f) }

        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
        ) {
            itemsIndexed(categories, key = { _, cat -> cat.id.value }) { index, cat ->
                val isDragging = index == draggingIndex
                val elevation by animateDpAsState(
                    targetValue = if (isDragging) 6.dp else 0.dp,
                    label = "row_elevation_$index",
                )

                MmRow(
                    modifier = Modifier
                        .shadow(elevation)
                        .background(if (isDragging) colors.surface else colors.bg)
                        .pointerInput(index) {
                            detectDragGesturesAfterLongPress(
                                onDragStart = {
                                    draggingIndex = index
                                    dragOffsetY = 0f
                                },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    dragOffsetY += dragAmount.y
                                    // Use actual item height from layout info, fallback to 64dp
                                    val itemHeight = listState.layoutInfo.visibleItemsInfo
                                        .firstOrNull { it.index == draggingIndex }
                                        ?.size?.toFloat() ?: 64f
                                    val stepsFloat = dragOffsetY / itemHeight
                                    val steps = stepsFloat.toInt()
                                    if (steps != 0) {
                                        val targetIndex = (draggingIndex + steps)
                                            .coerceIn(0, categories.size - 1)
                                        if (targetIndex != draggingIndex) {
                                            onReorder(draggingIndex, targetIndex)
                                            draggingIndex = targetIndex
                                            // Subtract the amount we moved, keeping remaining offset
                                            dragOffsetY -= steps * itemHeight
                                        }
                                    }
                                },
                                onDragEnd = { draggingIndex = -1; dragOffsetY = 0f },
                                onDragCancel = { draggingIndex = -1; dragOffsetY = 0f },
                            )
                        },
                    onClick = {
                        categoryToEdit = cat
                        showNewCategorySheet = true
                    },
                    divider = index != categories.lastIndex,
                ) {
                    Icon(
                        imageVector = MmIcons.dragHandle,
                        contentDescription = "Drag to reorder",
                        tint = colors.text3,
                        modifier = Modifier.size(18.dp),
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
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        }

        Box(modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
            MmButton(
                text = "New ${if (activeTab == CategoryTab.Expense) "expense" else "income"} category",
                onClick = {
                    categoryToEdit = null
                    showNewCategorySheet = true
                },
                variant = MmButtonVariant.Secondary,
                fullWidth = true,
                leadingIcon = MmIcons.plus,
            )
        }
    }

    if (showNewCategorySheet) {
        ModalBottomSheet(
            onDismissRequest = {
                showNewCategorySheet = false
                categoryToEdit = null
            },
            sheetState = sheetState,
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
            containerColor = colors.bg,
            dragHandle = null,
        ) {
            NewCategorySheet(
                categoryToEdit = categoryToEdit,
                defaultTab = activeTab,
                onDismiss = {
                    showNewCategorySheet = false
                    categoryToEdit = null
                },
                onSave = { name, iconKey, colorHex ->
                    val editing = categoryToEdit
                    if (editing != null) {
                        onUpdateCategory(editing.id, name, iconKey, colorHex)
                    } else {
                        onCreateCategory(name, iconKey, colorHex)
                    }
                    showNewCategorySheet = false
                    categoryToEdit = null
                },
                onDelete = { id ->
                    onDeleteCategory(id)
                    showNewCategorySheet = false
                    categoryToEdit = null
                },
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun NewCategorySheet(
    categoryToEdit: Category?,
    defaultTab: CategoryTab,
    onDismiss: () -> Unit,
    onSave: (name: String, iconKey: String, colorHex: String) -> Unit,
    onDelete: (CategoryId) -> Unit,
) {
    val colors = MM.colors
    val isEditMode = categoryToEdit != null

    var name by remember(categoryToEdit?.id) { mutableStateOf(categoryToEdit?.name ?: "") }
    var selectedIconKey by remember(categoryToEdit?.id) { mutableStateOf(categoryToEdit?.iconKey ?: "basket") }
    var selectedColor by remember(categoryToEdit?.id) {
        mutableStateOf(
            if (categoryToEdit != null) categoryColor(categoryToEdit.colorHex)
            else Color(0xFF4A8E5C)
        )
    }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    val palette = listOf(
        Color(0xFFC2566B), Color(0xFF8B6FB0), Color(0xFF4A8E5C), Color(0xFF4F8694), Color(0xFFB89148),
        Color(0xFF7A9572), Color(0xFFC97A4F), Color(0xFF5A7BA8), Color(0xFFB07089), Color(0xFF8A8A8A),
        Color(0xFFD14C7A), Color(0xFF6B5BC4), Color(0xFF3F9E70), Color(0xFF3A82A5), Color(0xFFD88B33),
    )
    val iconOptions = listOf(
        "heart", "film", "car", "bolt", "basket", "utensils",
        "home", "bag", "tag", "banknote", "gift", "sun", "moon", "globe", "folder",
    )

    Column {
        // Grabber
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(width = 36.dp, height = 4.dp)
                    .clip(RoundedCornerShape(50))
                    .background(colors.borderStrong),
            )
        }

        // Header row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MmIconButton(
                icon = MmIcons.close,
                onClick = onDismiss,
                contentDescription = "Close",
            )
            Text(
                text = if (isEditMode) "Edit category" else "New category",
                style = MM.type.title3,
                color = colors.text,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.size(40.dp))
        }

        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
        ) {
            // Live preview chip
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
            ) {
                Row(
                    modifier = Modifier
                        .clip(MM.radius.pill)
                        .background(colors.surface)
                        .border(1.dp, colors.border, MM.radius.pill)
                        .padding(horizontal = 18.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(selectedColor),
                        contentAlignment = Alignment.Center,
                    ) {
                        val painter = rememberVectorPainter(resolveIconVector(selectedIconKey))
                        Image(
                            painter = painter,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            colorFilter = ColorFilter.tint(Color.White),
                        )
                    }
                    Text(
                        text = name.ifBlank { "Category name" },
                        style = MM.type.body,
                        color = if (name.isBlank()) colors.text3 else colors.text,
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            MmField(
                value = name,
                onValueChange = { name = it },
                label = stringResource(Res.string.categories_name_label),
                placeholder = "e.g. Groceries",
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = stringResource(Res.string.categories_color_label),
                style = MM.type.caption.copy(color = colors.text2),
            )
            Spacer(modifier = Modifier.height(12.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                palette.forEach { color ->
                    ColorSwatch(
                        color = color,
                        isSelected = selectedColor == color,
                        onClick = { selectedColor = color },
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = stringResource(Res.string.categories_icon_label),
                style = MM.type.caption.copy(color = colors.text2),
            )
            Spacer(modifier = Modifier.height(12.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                iconOptions.forEach { iconKey ->
                    val isSelected = selectedIconKey == iconKey
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSelected) selectedColor else colors.surface)
                            .border(
                                width = 1.dp,
                                color = if (isSelected) selectedColor else colors.border,
                                shape = RoundedCornerShape(10.dp),
                            )
                            .clickable(
                                indication = null,
                                interactionSource = remember { MutableInteractionSource() },
                            ) { selectedIconKey = iconKey },
                        contentAlignment = Alignment.Center,
                    ) {
                        val painter = rememberVectorPainter(resolveIconVector(iconKey))
                        Image(
                            painter = painter,
                            contentDescription = iconKey,
                            modifier = Modifier.size(20.dp),
                            colorFilter = ColorFilter.tint(if (isSelected) Color.White else colors.text),
                        )
                    }
                }
            }

            if (isEditMode) {
                Spacer(modifier = Modifier.height(24.dp))
                MmButton(
                    text = "Delete category",
                    onClick = { showDeleteConfirm = true },
                    variant = MmButtonVariant.Danger,
                    fullWidth = true,
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        // Pinned save button
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.bg)
                .padding(horizontal = 20.dp, vertical = 16.dp),
        ) {
            MmButton(
                text = if (isEditMode) "Save changes" else "Create category",
                onClick = {
                    val colorHex = colorToHex(selectedColor)
                    onSave(name, selectedIconKey, colorHex)
                },
                variant = MmButtonVariant.Accent,
                size = MmButtonSize.Lg,
                leadingIcon = MmIcons.check,
                fullWidth = true,
                enabled = name.isNotBlank(),
            )
        }
    }

    val catBeingDeleted = categoryToEdit
    if (showDeleteConfirm && catBeingDeleted != null) {
        DeleteConfirmSheet(
            categoryName = catBeingDeleted.name,
            onConfirm = {
                onDelete(catBeingDeleted.id)
                showDeleteConfirm = false
            },
            onCancel = { showDeleteConfirm = false },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DeleteConfirmSheet(
    categoryName: String,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
) {
    val colors = MM.colors
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onCancel,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        containerColor = colors.bg,
        dragHandle = null,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .size(width = 36.dp, height = 4.dp)
                        .clip(RoundedCornerShape(50))
                        .background(colors.borderStrong),
                )
            }
            Text(
                text = "Delete \"$categoryName\"?",
                style = MM.type.title3,
                color = colors.text,
                textAlign = TextAlign.Center,
            )
            Text(
                text = "This will remove the category. Transactions using it may be affected.",
                style = MM.type.caption,
                color = colors.text2,
                textAlign = TextAlign.Center,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                MmButton(
                    text = stringResource(Res.string.categories_cancel),
                    onClick = onCancel,
                    variant = MmButtonVariant.Secondary,
                    modifier = Modifier.weight(1f),
                )
                MmButton(
                    text = "Delete",
                    onClick = onConfirm,
                    variant = MmButtonVariant.Danger,
                    modifier = Modifier.weight(1f),
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun ColorSwatch(
    color: Color,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val colors = MM.colors

    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(color)
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
            ) { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        if (isSelected) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(2.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(colors.bg),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(6.dp))
                        .background(color),
                    contentAlignment = Alignment.Center,
                ) {
                    val painter = rememberVectorPainter(MmIcons.check)
                    Image(
                        painter = painter,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        colorFilter = ColorFilter.tint(Color.White),
                    )
                }
            }
        }
    }
}

internal fun resolveIconVector(key: String): ImageVector = when (key) {
    "heart" -> MmIcons.heart
    "film" -> MmIcons.film
    "car" -> MmIcons.car
    "bolt" -> MmIcons.bolt
    "basket" -> MmIcons.basket
    "utensils" -> MmIcons.utensils
    "home" -> MmIcons.home
    "bag" -> MmIcons.bag
    "tag" -> MmIcons.tag
    "banknote" -> MmIcons.banknote
    "gift" -> MmIcons.gift
    "sun" -> MmIcons.sun
    "moon" -> MmIcons.moon
    "globe" -> MmIcons.globe
    "folder" -> MmIcons.folder
    else -> MmIcons.tag
}

private fun colorToHex(color: Color): String {
    fun Int.hex2() = toString(16).padStart(2, '0').uppercase()
    val r = (color.red * 255).toInt()
    val g = (color.green * 255).toInt()
    val b = (color.blue * 255).toInt()
    return "#${r.hex2()}${g.hex2()}${b.hex2()}"
}
