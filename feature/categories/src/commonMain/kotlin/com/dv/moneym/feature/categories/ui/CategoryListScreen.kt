package com.dv.moneym.feature.categories.ui

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
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
import moneym.feature.categories.generated.resources.categories_color_brightness
import moneym.feature.categories.generated.resources.categories_color_hex
import moneym.feature.categories.generated.resources.categories_color_hue
import moneym.feature.categories.generated.resources.categories_color_label
import moneym.feature.categories.generated.resources.categories_color_picker_title
import moneym.feature.categories.generated.resources.categories_color_saturation
import moneym.feature.categories.generated.resources.categories_color_select
import moneym.feature.categories.generated.resources.categories_create
import moneym.feature.categories.generated.resources.categories_delete
import moneym.feature.categories.generated.resources.categories_delete_button
import moneym.feature.categories.generated.resources.categories_delete_confirm_body
import moneym.feature.categories.generated.resources.categories_delete_confirm_title
import moneym.feature.categories.generated.resources.categories_edit_sheet_title
import moneym.feature.categories.generated.resources.categories_hint
import moneym.feature.categories.generated.resources.categories_icon_label
import moneym.feature.categories.generated.resources.categories_name_label
import moneym.feature.categories.generated.resources.categories_name_placeholder
import moneym.feature.categories.generated.resources.categories_name_preview_placeholder
import moneym.feature.categories.generated.resources.categories_new_button
import moneym.feature.categories.generated.resources.categories_new_expense
import moneym.feature.categories.generated.resources.categories_new_income
import moneym.feature.categories.generated.resources.categories_new_sheet_title
import moneym.feature.categories.generated.resources.categories_save_changes
import moneym.feature.categories.generated.resources.categories_tab_expense
import moneym.feature.categories.generated.resources.categories_tab_income
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
        CategoryListHeader(
            activeTab = activeTab,
            categoryCount = categories.size,
            onBack = onBack,
            onSetTab = onSetTab,
            onAddClick = {
                categoryToEdit = null
                showNewCategorySheet = true
            },
        )

        DraggableCategoryList(
            categories = categories,
            onReorder = onReorder,
            onCategoryClick = { cat ->
                categoryToEdit = cat
                showNewCategorySheet = true
            },
            modifier = Modifier.weight(1f),
        )

        val newCategoryButtonText = if (activeTab == CategoryTab.Expense)
            stringResource(Res.string.categories_new_expense)
        else
            stringResource(Res.string.categories_new_income)

        Box(modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
            MmButton(
                text = newCategoryButtonText,
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

// ─── Category list header ─────────────────────────────────────────────────────

@Composable
private fun CategoryListHeader(
    activeTab: CategoryTab,
    categoryCount: Int,
    onBack: () -> Unit,
    onSetTab: (CategoryTab) -> Unit,
    onAddClick: () -> Unit,
) {
    val colors = MM.colors

    ScreenHeader(
        title = stringResource(Res.string.categories_title),
        onBack = onBack,
        trailingContent = {
            MmButton(
                text = stringResource(Res.string.categories_new_button),
                onClick = onAddClick,
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
            options = listOf(
                stringResource(Res.string.categories_tab_expense),
                stringResource(Res.string.categories_tab_income),
            ),
            selectedIndex = if (activeTab == CategoryTab.Expense) 0 else 1,
            onOptionSelected = { onSetTab(if (it == 0) CategoryTab.Expense else CategoryTab.Income) },
            fillWidth = true,
        )
    }

    val tabLabel = if (activeTab == CategoryTab.Expense)
        stringResource(Res.string.categories_tab_expense).lowercase()
    else
        stringResource(Res.string.categories_tab_income).lowercase()

    Text(
        text = "$categoryCount $tabLabel ${stringResource(Res.string.categories_hint)}",
        style = MM.type.caption.copy(color = colors.text3),
        modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 12.dp),
    )
}

// ─── Draggable category list ──────────────────────────────────────────────────

@Composable
private fun DraggableCategoryList(
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
                                    draggingFromIndex != draggingIndex) {
                                    // The localCategories already reflects the final order
                                    // Call onReorder with the indices in the original list
                                    // We need to tell the VM the new order by finding where the
                                    // originally-from item ended up
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
    var showColorPicker by remember { mutableStateOf(false) }
    // List of custom colors generated via the HSV picker — appends on each new color
    var customColors by remember(categoryToEdit?.id) { mutableStateOf(listOf<Color>()) }

    val palette = listOf(
        Color(0xFFC2566B), Color(0xFF8B6FB0), Color(0xFF4A8E5C), Color(0xFF4F8694), Color(0xFFB89148),
        Color(0xFF7A9572), Color(0xFFC97A4F), Color(0xFF5A7BA8), Color(0xFFB07089), Color(0xFF8A8A8A),
        Color(0xFFD14C7A), Color(0xFF6B5BC4), Color(0xFF3F9E70), Color(0xFF3A82A5), Color(0xFFD88B33),
    )
    val iconOptions = listOf(
        "heart", "film", "car", "bolt", "basket", "utensils",
        "home", "bag", "tag", "banknote", "gift", "sun", "moon", "globe", "folder",
    )

    val sheetTitle = if (isEditMode)
        stringResource(Res.string.categories_edit_sheet_title)
    else
        stringResource(Res.string.categories_new_sheet_title)

    Column {
        NewCategorySheetHeader(sheetTitle = sheetTitle, onDismiss = onDismiss)
        NewCategorySheetBody(
            name = name,
            palette = palette,
            iconOptions = iconOptions,
            selectedColor = selectedColor,
            selectedIconKey = selectedIconKey,
            customColors = customColors,
            isEditMode = isEditMode,
            onNameChange = { name = it },
            onColorSelected = { selectedColor = it },
            onCustomColorClick = { showColorPicker = true },
            onIconSelected = { selectedIconKey = it },
            onDeleteClick = { showDeleteConfirm = true },
        )
        NewCategorySaveButton(
            isEditMode = isEditMode,
            nameIsBlank = name.isBlank(),
            colors = colors,
            onSave = { onSave(name, selectedIconKey, colorToHex(selectedColor)) },
        )
    }

    // Color picker dialog — uses Dialog instead of ModalBottomSheet to avoid gesture conflicts
    if (showColorPicker) {
        HsvColorPickerDialog(
            initialColor = selectedColor,
            onDismiss = { showColorPicker = false },
            onColorSelected = { color ->
                selectedColor = color
                // Append the new color to the custom list (don't replace)
                if (color !in palette) {
                    customColors = customColors + color
                }
                showColorPicker = false
            },
        )
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

// ─── New category sheet header ────────────────────────────────────────────────

@Composable
private fun NewCategorySheetHeader(
    sheetTitle: String,
    onDismiss: () -> Unit,
) {
    val colors = MM.colors

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
            contentDescription = stringResource(Res.string.categories_cancel),
        )
        Text(
            text = sheetTitle,
            style = MM.type.title3,
            color = colors.text,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.size(40.dp))
    }
}

// ─── New category save button ─────────────────────────────────────────────────

@Composable
private fun NewCategorySaveButton(
    isEditMode: Boolean,
    nameIsBlank: Boolean,
    colors: com.dv.moneym.core.designsystem.MoneyMColors,
    onSave: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.bg)
            .padding(horizontal = 20.dp, vertical = 16.dp),
    ) {
        MmButton(
            text = if (isEditMode) stringResource(Res.string.categories_save_changes) else stringResource(Res.string.categories_create),
            onClick = onSave,
            variant = MmButtonVariant.Accent,
            size = MmButtonSize.Lg,
            leadingIcon = MmIcons.check,
            fullWidth = true,
            enabled = !nameIsBlank,
        )
    }
}

// ─── New category sheet body ──────────────────────────────────────────────────

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun NewCategorySheetBody(
    name: String,
    palette: List<Color>,
    iconOptions: List<String>,
    selectedColor: Color,
    selectedIconKey: String,
    customColors: List<Color>,
    isEditMode: Boolean,
    onNameChange: (String) -> Unit,
    onColorSelected: (Color) -> Unit,
    onCustomColorClick: () -> Unit,
    onIconSelected: (String) -> Unit,
    onDeleteClick: () -> Unit,
) {
    val colors = MM.colors

    Column(
        modifier = Modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
    ) {
        // Live preview chip
        CategoryPreviewChip(
            name = name,
            selectedColor = selectedColor,
            selectedIconKey = selectedIconKey,
            placeholderText = stringResource(Res.string.categories_name_preview_placeholder),
            colors = colors,
        )

        Spacer(modifier = Modifier.height(24.dp))

        MmField(
            value = name,
            onValueChange = onNameChange,
            label = stringResource(Res.string.categories_name_label),
            placeholder = stringResource(Res.string.categories_name_placeholder),
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = stringResource(Res.string.categories_color_label),
            style = MM.type.caption.copy(color = colors.text2),
        )
        Spacer(modifier = Modifier.height(12.dp))

        ColorPickerSection(
            palette = palette,
            selectedColor = selectedColor,
            customColors = customColors,
            onColorSelected = onColorSelected,
            onCustomColorClick = onCustomColorClick,
            colors = colors,
        )

        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = stringResource(Res.string.categories_icon_label),
            style = MM.type.caption.copy(color = colors.text2),
        )
        Spacer(modifier = Modifier.height(12.dp))

        IconPickerSection(
            iconOptions = iconOptions,
            selectedIconKey = selectedIconKey,
            selectedColor = selectedColor,
            onIconSelected = onIconSelected,
            colors = colors,
        )

        if (isEditMode) {
            Spacer(modifier = Modifier.height(24.dp))
            MmButton(
                text = stringResource(Res.string.categories_delete),
                onClick = onDeleteClick,
                variant = MmButtonVariant.Danger,
                fullWidth = true,
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

// ─── Category Preview Chip ────────────────────────────────────────────────────

@Composable
private fun CategoryPreviewChip(
    name: String,
    selectedColor: Color,
    selectedIconKey: String,
    placeholderText: String,
    colors: com.dv.moneym.core.designsystem.MoneyMColors,
) {
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
                text = name.ifBlank { placeholderText },
                style = MM.type.body,
                color = if (name.isBlank()) colors.text3 else colors.text,
            )
        }
    }
}

// ─── Color Picker Section ─────────────────────────────────────────────────────

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ColorPickerSection(
    palette: List<Color>,
    selectedColor: Color,
    customColors: List<Color>,
    onColorSelected: (Color) -> Unit,
    onCustomColorClick: () -> Unit,
    colors: com.dv.moneym.core.designsystem.MoneyMColors,
) {
    val isCustomSelected = selectedColor !in palette
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        // Preset palette swatches
        palette.forEach { color ->
            ColorSwatch(
                color = color,
                isSelected = selectedColor == color,
                onClick = { onColorSelected(color) },
            )
        }
        // Custom colors generated via HSV picker — each appears as its own swatch
        customColors.forEach { color ->
            ColorSwatch(
                color = color,
                isSelected = selectedColor == color,
                onClick = { onColorSelected(color) },
            )
        }
        // "+" button to open HSV color picker — always visible
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(colors.surface)
                .border(
                    1.dp,
                    colors.borderStrong,
                    RoundedCornerShape(10.dp),
                )
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() },
                ) { onCustomColorClick() },
            contentAlignment = Alignment.Center,
        ) {
            val painter = rememberVectorPainter(MmIcons.plus)
            Image(
                painter = painter,
                contentDescription = "Custom color",
                modifier = Modifier.size(16.dp),
                colorFilter = ColorFilter.tint(colors.text2),
            )
        }
    }
}

// ─── Icon Picker Section ──────────────────────────────────────────────────────

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun IconPickerSection(
    iconOptions: List<String>,
    selectedIconKey: String,
    selectedColor: Color,
    onIconSelected: (String) -> Unit,
    colors: com.dv.moneym.core.designsystem.MoneyMColors,
) {
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
                    ) { onIconSelected(iconKey) },
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
}

// ─── HSV Color Picker Dialog ──────────────────────────────────────────────────
// Using Dialog instead of ModalBottomSheet to avoid gesture conflicts with parent sheet

@Composable
private fun HsvColorPickerDialog(
    initialColor: Color,
    onDismiss: () -> Unit,
    onColorSelected: (Color) -> Unit,
) {
    val colors = MM.colors

    fun colorToHsv(color: Color): Triple<Float, Float, Float> {
        val r = color.red; val g = color.green; val b = color.blue
        val max = maxOf(r, g, b); val min = minOf(r, g, b)
        val delta = max - min
        val h = when {
            delta == 0f -> 0f
            max == r -> (60f * (((g - b) / delta) % 6f)).let { if (it < 0f) it + 360f else it }
            max == g -> 60f * ((b - r) / delta + 2f)
            else -> 60f * ((r - g) / delta + 4f)
        }
        val s = if (max == 0f) 0f else delta / max
        return Triple(h, s, max)
    }

    fun hsvToColor(h: Float, s: Float, v: Float): Color {
        val c = v * s
        val x = c * (1f - kotlin.math.abs((h / 60f) % 2f - 1f))
        val m = v - c
        val (r1, g1, b1) = when {
            h < 60f -> Triple(c, x, 0f)
            h < 120f -> Triple(x, c, 0f)
            h < 180f -> Triple(0f, c, x)
            h < 240f -> Triple(0f, x, c)
            h < 300f -> Triple(x, 0f, c)
            else -> Triple(c, 0f, x)
        }
        return Color(r1 + m, g1 + m, b1 + m)
    }

    val (ih, is_, iv) = remember(initialColor) { colorToHsv(initialColor) }
    var hue by remember { mutableStateOf(ih) }
    var saturation by remember { mutableStateOf(is_) }
    var brightness by remember { mutableStateOf(iv) }

    val currentColor = hsvToColor(hue, saturation, brightness)

    var hexText by remember(hue, saturation, brightness) {
        val r = (currentColor.red * 255).toInt()
        val g = (currentColor.green * 255).toInt()
        val b = (currentColor.blue * 255).toInt()
        mutableStateOf(
            "#${r.toString(16).padStart(2, '0')}${g.toString(16).padStart(2, '0')}${b.toString(16).padStart(2, '0')}".uppercase()
        )
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(colors.bg),
        ) {
            HsvColorPickerContent(
                colors = colors,
                currentColor = currentColor,
                hue = hue,
                saturation = saturation,
                brightness = brightness,
                hexText = hexText,
                hsvToColor = ::hsvToColor,
                colorToHsv = ::colorToHsv,
                onHueChange = { hue = it },
                onSaturationChange = { saturation = it },
                onBrightnessChange = { brightness = it },
                onHexTextChange = { hexText = it },
                onHsvChange = { h, s, v -> hue = h; saturation = s; brightness = v },
                onDismiss = onDismiss,
                onColorSelected = onColorSelected,
            )
        }
    }
}

// ─── HSV Color Picker content ─────────────────────────────────────────────────

@Composable
private fun HsvColorPickerContent(
    colors: com.dv.moneym.core.designsystem.MoneyMColors,
    currentColor: Color,
    hue: Float,
    saturation: Float,
    brightness: Float,
    hexText: String,
    hsvToColor: (Float, Float, Float) -> Color,
    colorToHsv: (Color) -> Triple<Float, Float, Float>,
    onHueChange: (Float) -> Unit,
    onSaturationChange: (Float) -> Unit,
    onBrightnessChange: (Float) -> Unit,
    onHexTextChange: (String) -> Unit,
    onHsvChange: (Float, Float, Float) -> Unit,
    onDismiss: () -> Unit,
    onColorSelected: (Color) -> Unit,
) {
    val type = MM.type

    Column(
        modifier = Modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            stringResource(Res.string.categories_color_picker_title),
            style = type.title3,
            color = colors.text,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )

        // Color preview strip
        Box(
            Modifier
                .fillMaxWidth()
                .height(48.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(currentColor),
        )

        HsvSlidersSection(
            hue = hue,
            saturation = saturation,
            brightness = brightness,
            hsvToColor = hsvToColor,
            onHueChange = onHueChange,
            onSaturationChange = onSaturationChange,
            onBrightnessChange = onBrightnessChange,
            colors = colors,
        )

        // Hex input
        MmField(
            value = hexText,
            onValueChange = { input ->
                onHexTextChange(input)
                val clean = input.trimStart('#')
                if (clean.length == 6) {
                    try {
                        val r = clean.substring(0, 2).toInt(16) / 255f
                        val g = clean.substring(2, 4).toInt(16) / 255f
                        val b = clean.substring(4, 6).toInt(16) / 255f
                        val (h, s, v) = colorToHsv(Color(r, g, b))
                        onHsvChange(h, s, v)
                    } catch (_: Exception) {}
                }
            },
            label = stringResource(Res.string.categories_color_hex),
            placeholder = "#4A8E5C",
            modifier = Modifier.fillMaxWidth(),
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            MmButton(
                text = stringResource(Res.string.categories_cancel),
                onClick = onDismiss,
                variant = MmButtonVariant.Secondary,
                modifier = Modifier.weight(1f),
            )
            MmButton(
                text = stringResource(Res.string.categories_color_select),
                onClick = { onColorSelected(currentColor) },
                variant = MmButtonVariant.Accent,
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(Modifier.height(8.dp))
    }
}

// ─── HSV Sliders Section ──────────────────────────────────────────────────────

@Composable
private fun HsvSlidersSection(
    hue: Float,
    saturation: Float,
    brightness: Float,
    hsvToColor: (Float, Float, Float) -> Color,
    onHueChange: (Float) -> Unit,
    onSaturationChange: (Float) -> Unit,
    onBrightnessChange: (Float) -> Unit,
    colors: com.dv.moneym.core.designsystem.MoneyMColors,
) {
    val type = MM.type
    // Hue slider
    Text(stringResource(Res.string.categories_color_hue), style = type.caption.copy(color = colors.text2))
    HsvSlider(
        gradient = Brush.horizontalGradient((0..6).map { i -> hsvToColor(i * 60f, 1f, 1f) }),
        thumbPosition = hue / 360f,
        onPositionChanged = { pos -> onHueChange(pos * 360f) },
        colors = colors,
    )
    // Saturation slider
    Text(stringResource(Res.string.categories_color_saturation), style = type.caption.copy(color = colors.text2))
    HsvSlider(
        gradient = Brush.horizontalGradient(listOf(
            hsvToColor(hue, 0f, brightness.coerceAtLeast(0.3f)),
            hsvToColor(hue, 1f, brightness.coerceAtLeast(0.3f)),
        )),
        thumbPosition = saturation,
        onPositionChanged = { pos -> onSaturationChange(pos) },
        colors = colors,
    )
    // Brightness slider
    Text(stringResource(Res.string.categories_color_brightness), style = type.caption.copy(color = colors.text2))
    HsvSlider(
        gradient = Brush.horizontalGradient(listOf(Color.Black, hsvToColor(hue, saturation.coerceAtLeast(0.3f), 1f))),
        thumbPosition = brightness,
        onPositionChanged = { pos -> onBrightnessChange(pos) },
        colors = colors,
    )
}

// ─── HSV Slider ───────────────────────────────────────────────────────────────

@Composable
private fun HsvSlider(
    gradient: Brush,
    thumbPosition: Float,
    onPositionChanged: (Float) -> Unit,
    colors: com.dv.moneym.core.designsystem.MoneyMColors,
) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(32.dp)
            .clip(RoundedCornerShape(16.dp)),
    ) {
        val widthPx = constraints.maxWidth.toFloat()
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(gradient)
                .pointerInput(thumbPosition) {
                    detectDragGestures { change, _ ->
                        change.consume()
                        onPositionChanged((change.position.x / widthPx).coerceIn(0f, 1f))
                    }
                }
                .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) {},
        ) {
            val thumbXDp = (thumbPosition * widthPx).toInt()
            Box(
                modifier = Modifier
                    .padding(start = (thumbXDp - 12).coerceAtLeast(0).dp)
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(Color.White)
                    .border(2.dp, colors.borderStrong, CircleShape),
            )
        }
    }
}

// ─── Delete Confirm Sheet ─────────────────────────────────────────────────────

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
                text = stringResource(Res.string.categories_delete_confirm_title, categoryName),
                style = MM.type.title3,
                color = colors.text,
                textAlign = TextAlign.Center,
            )
            Text(
                text = stringResource(Res.string.categories_delete_confirm_body),
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
                    text = stringResource(Res.string.categories_delete_button),
                    onClick = onConfirm,
                    variant = MmButtonVariant.Danger,
                    modifier = Modifier.weight(1f),
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

// ─── Color Swatch ─────────────────────────────────────────────────────────────

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

// ─── Icon resolution ──────────────────────────────────────────────────────────

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
