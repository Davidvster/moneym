package com.dv.moneym.feature.categories.list

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.dv.moneym.core.designsystem.MM
import com.dv.moneym.core.designsystem.categoryColor
import com.dv.moneym.core.model.CategoryId
import com.dv.moneym.core.model.Icon
import com.dv.moneym.core.navigation.ModalKey
import com.dv.moneym.core.ui.HsvColorPickerDialog
import com.dv.moneym.core.ui.MmButton
import com.dv.moneym.core.ui.MmButtonVariant
import com.dv.moneym.core.ui.colorToHex
import com.dv.moneym.core.ui.imageVector
import com.dv.moneym.feature.categories.list.components.CategoryListHeader
import com.dv.moneym.feature.categories.list.components.DeleteConfirmSheet
import com.dv.moneym.feature.categories.list.components.DraggableCategoryList
import com.dv.moneym.feature.categories.list.components.NewCategorySaveButton
import com.dv.moneym.feature.categories.list.components.NewCategorySheetBody
import com.dv.moneym.feature.categories.list.components.NewCategorySheetHeader
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
    onBack: () -> Unit,
    metadata: Map<String, Any> = emptyMap(),
) = entry<CategoriesKey>(metadata = metadata) {
    CategoryListScreen(
        onBack = onBack,
    )
}

@Composable
fun CategoryListScreen(
    onBack: () -> Unit,
    viewModel: CategoryListViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    ManageCategoriesScreen(
        state = state,
        onBack = onBack,
        onIntent = viewModel::onIntent,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ManageCategoriesScreen(
    state: CategoryListUiState,
    onBack: () -> Unit,
    onIntent: (CategoryListIntent) -> Unit,
) {
    val colors = MM.colors
    val categories = state.orderedCategories
    val activeTab = state.activeTab

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
            onSetTab = { onIntent(CategoryListIntent.SetTab(it)) },
        )

        DraggableCategoryList(
            categories = categories,
            onReorder = { from, to -> onIntent(CategoryListIntent.Reorder(from, to)) },
            onCategoryClick = { cat -> onIntent(CategoryListIntent.StartEditCategory(cat)) },
            modifier = Modifier.weight(1f),
        )

        val newCategoryButtonText = if (activeTab == CategoryTab.Expense)
            stringResource(Res.string.categories_new_expense)
        else
            stringResource(Res.string.categories_new_income)

        Box(
            modifier = Modifier
                .padding(
                    horizontal = MM.dimen.padding_2_5x,
                    vertical = MM.dimen.padding_2x
                )
                .navigationBarsPadding()
        ) {
            MmButton(
                text = newCategoryButtonText,
                onClick = { onIntent(CategoryListIntent.ShowCategoryEditSheet(true)) },
                variant = MmButtonVariant.Secondary,
                fullWidth = true,
                leadingIcon = Icon.Plus.imageVector,
            )
        }
    }

    if (state.showCategoryEditSheet) {
        ModalBottomSheet(
            onDismissRequest = { onIntent(CategoryListIntent.ShowCategoryEditSheet(false)) },
            sheetState = sheetState,
            shape = RoundedCornerShape(
                topStart = MM.dimen.padding_2_5x,
                topEnd = MM.dimen.padding_2_5x
            ),
            containerColor = colors.bg,
            dragHandle = null,
        ) {
            NewCategorySheet(
                state = state,
                onIntent = onIntent,
                onDismiss = { onIntent(CategoryListIntent.ShowCategoryEditSheet(false)) },
                onSave = {
                    val editing = state.editingCategory
                    if (editing != null) {
                        onIntent(
                            CategoryListIntent.UpdateCategory(
                                editing.id,
                                state.editingName,
                                state.editingIcon,
                                state.editingColorHex,
                            )
                        )
                    } else {
                        onIntent(
                            CategoryListIntent.CreateCategory(
                                state.editingName,
                                state.editingIcon,
                                state.editingColorHex,
                            )
                        )
                    }
                },
                onDelete = { id -> onIntent(CategoryListIntent.DeleteCategory(id)) },
            )
        }
    }
}

// TODO this sheet could be a new screen with a new viewmodel
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun NewCategorySheet(
    state: CategoryListUiState,
    onIntent: (CategoryListIntent) -> Unit,
    onDismiss: () -> Unit,
    onSave: () -> Unit,
    onDelete: (CategoryId) -> Unit,
) {
    val colors = MM.colors
    val categoryToEdit = state.editingCategory
    val isEditMode = categoryToEdit != null

    val selectedIcon = state.editingIcon
    val selectedColor = categoryColor(state.editingColorHex)
    // List of custom colors generated via the HSV picker — appends on each new color
    var customColors by remember(categoryToEdit?.id) { mutableStateOf(listOf<Color>()) }

    val sheetTitle = if (isEditMode)
        stringResource(Res.string.categories_edit_sheet_title)
    else
        stringResource(Res.string.categories_new_sheet_title)

    Column {
        NewCategorySheetHeader(sheetTitle = sheetTitle, onDismiss = onDismiss)
        NewCategorySheetBody(
            modifier = Modifier.weight(1f),
            name = state.editingName,
            palette = CATEGORY_PALETTE,
            iconOptions = CATEGORY_ICON_OPTIONS,
            selectedColor = selectedColor,
            selectedIcon = selectedIcon,
            customColors = customColors,
            isEditMode = isEditMode,
            nameError = state.nameError,
            onNameChange = { onIntent(CategoryListIntent.EditingNameChanged(it)) },
            onColorSelected = { onIntent(CategoryListIntent.EditingColorChanged(colorToHex(it))) },
            onCustomColorClick = { onIntent(CategoryListIntent.ShowColorPicker(true)) },
            onIconSelected = { onIntent(CategoryListIntent.EditingIconChanged(it)) },
            onDeleteClick = { onIntent(CategoryListIntent.ShowDeleteConfirm(true)) },
        )
        NewCategorySaveButton(
            isEditMode = isEditMode,
            nameIsBlank = state.editingName.isBlank(),
            colors = colors,
            onSave = { onSave() },
        )
    }

    // Color picker dialog — extracted to HsvColorPickerDialog.kt
    if (state.showColorPicker) {
        HsvColorPickerDialog(
            initialColor = selectedColor,
            onDismiss = { onIntent(CategoryListIntent.ShowColorPicker(false)) },
            onColorSelected = { color ->
                onIntent(CategoryListIntent.EditingColorChanged(colorToHex(color)))
                if (color !in CATEGORY_PALETTE) {
                    customColors = customColors + color
                }
                onIntent(CategoryListIntent.ShowColorPicker(false))
            },
        )
    }

    if (state.showDeleteConfirm && categoryToEdit != null) {
        DeleteConfirmSheet(
            categoryName = categoryToEdit.name,
            onConfirm = {
                onDelete(categoryToEdit.id)
                onIntent(CategoryListIntent.ShowDeleteConfirm(false))
            },
            onCancel = { onIntent(CategoryListIntent.ShowDeleteConfirm(false)) },
        )
    }
}

private val CATEGORY_PALETTE = listOf(
    Color(0xFFF4743B),
    Color(0xFFC97A4F),
    Color(0xFFD88B33),
    Color(0xFFFF9F1C),
    Color(0xFFB89148),
    Color(0xFFF4B400),
    Color(0xFFA8C63A),
    Color(0xFF7A9572),
    Color(0xFF2EA84F),
    Color(0xFF4A8E5C),
    Color(0xFF3F9E70),
    Color(0xFF12B5A5),
    Color(0xFF1CA7C9),
    Color(0xFF4F8694),
    Color(0xFF3A82A5),
    Color(0xFF5A7BA8),
    Color(0xFF2D6CDF),
    Color(0xFF6B5BC4),
    Color(0xFF5A3FC0),
    Color(0xFF8B6FB0),
    Color(0xFF9B51E0),
    Color(0xFFB07089),
    Color(0xFFE84B8A),
    Color(0xFFD14C7A),
    Color(0xFFC2566B),
    Color(0xFFE63946),
    Color(0xFF8A8A8A),
)

private val CATEGORY_ICON_OPTIONS = listOf(
    Icon.Heart,
    Icon.Film,
    Icon.Car,
    Icon.Bolt,
    Icon.Basket,
    Icon.Utensils,
    Icon.Home,
    Icon.Bag,
    Icon.Tag,
    Icon.Banknote,
    Icon.Gift,
    Icon.Sun,
    Icon.Moon,
    Icon.Globe,
    Icon.Folder,
)

@androidx.compose.ui.tooling.preview.Preview
@Composable
private fun ManageCategoriesScreenPreview() {
    val epoch = kotlin.time.Instant.fromEpochSeconds(0)
    val sample = listOf(
        com.dv.moneym.core.model.Category(
            id = com.dv.moneym.core.model.CategoryId(1L),
            name = "Groceries",
            iconKey = "basket",
            colorHex = "#4A8E5C",
            isUserCreated = false,
            archived = false,
            createdAt = epoch,
            updatedAt = epoch,
        ),
        com.dv.moneym.core.model.Category(
            id = com.dv.moneym.core.model.CategoryId(2L),
            name = "Eating Out",
            iconKey = "pizza",
            colorHex = "#E07A5F",
            isUserCreated = true,
            archived = false,
            createdAt = epoch,
            updatedAt = epoch,
        ),
    )
    com.dv.moneym.core.designsystem.MoneyMTheme {
        ManageCategoriesScreen(
            state = CategoryListUiState(
                isLoading = false,
                active = sample,
                orderedCategories = sample,
            ),
            onBack = {},
            onIntent = {},
        )
    }
}
