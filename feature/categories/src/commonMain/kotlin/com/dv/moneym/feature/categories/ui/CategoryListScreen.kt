package com.dv.moneym.feature.categories.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.dv.moneym.core.designsystem.MM
import com.dv.moneym.core.designsystem.categoryColor
import com.dv.moneym.core.model.Category
import com.dv.moneym.core.model.CategoryId
import com.dv.moneym.core.navigation.ModalKey
import com.dv.moneym.core.ui.MmButton
import com.dv.moneym.core.ui.MmButtonVariant
import com.dv.moneym.core.ui.MmIcons
import com.dv.moneym.feature.categories.presentation.CategoryListEffect
import com.dv.moneym.feature.categories.presentation.CategoryListViewModel
import com.dv.moneym.feature.categories.presentation.CategoryTab
import com.dv.moneym.feature.categories.ui.components.CategoryListHeader
import com.dv.moneym.feature.categories.ui.components.DeleteConfirmSheet
import com.dv.moneym.feature.categories.ui.components.DraggableCategoryList
import com.dv.moneym.feature.categories.ui.components.NewCategorySaveButton
import com.dv.moneym.feature.categories.ui.components.NewCategorySheetBody
import com.dv.moneym.feature.categories.ui.components.NewCategorySheetHeader
import kotlinx.serialization.Serializable
import moneym.feature.categories.generated.resources.Res
import moneym.feature.categories.generated.resources.categories_edit_sheet_title
import moneym.feature.categories.generated.resources.categories_new_expense
import moneym.feature.categories.generated.resources.categories_new_income
import moneym.feature.categories.generated.resources.categories_new_sheet_title
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

        Box(
            modifier = Modifier.padding(
                horizontal = MM.dimen.padding_2_5x,
                vertical = MM.dimen.padding_2x
            )
        ) {
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
            shape = RoundedCornerShape(
                topStart = MM.dimen.padding_2_5x,
                topEnd = MM.dimen.padding_2_5x
            ),
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
    var selectedIconKey by remember(categoryToEdit?.id) {
        mutableStateOf(
            categoryToEdit?.iconKey ?: "basket"
        )
    }
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
        Color(0xFFC2566B),
        Color(0xFF8B6FB0),
        Color(0xFF4A8E5C),
        Color(0xFF4F8694),
        Color(0xFFB89148),
        Color(0xFF7A9572),
        Color(0xFFC97A4F),
        Color(0xFF5A7BA8),
        Color(0xFFB07089),
        Color(0xFF8A8A8A),
        Color(0xFFD14C7A),
        Color(0xFF6B5BC4),
        Color(0xFF3F9E70),
        Color(0xFF3A82A5),
        Color(0xFFD88B33),
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

    // Color picker dialog — extracted to HsvColorPickerDialog.kt
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
