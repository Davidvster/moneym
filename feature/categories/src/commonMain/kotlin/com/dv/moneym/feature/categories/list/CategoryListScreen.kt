package com.dv.moneym.feature.categories.list

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.ui.unit.dp
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
import com.dv.moneym.core.designsystem.MoneyMTheme
import com.dv.moneym.core.designsystem.categoryColor
import com.dv.moneym.core.model.Category
import com.dv.moneym.core.model.CategoryId
import com.dv.moneym.core.model.Icon
import com.dv.moneym.core.model.IndicatorStyle
import com.dv.moneym.core.navigation.ModalKey
import com.dv.moneym.core.ui.CategoryIconTile
import com.dv.moneym.core.ui.HsvColorPickerDialog
import com.dv.moneym.core.ui.MmButton
import com.dv.moneym.core.ui.MmButtonVariant
import com.dv.moneym.core.ui.MmLoadingOverlay
import com.dv.moneym.core.ui.MmRow
import com.dv.moneym.core.ui.colorToHex
import com.dv.moneym.core.ui.imageVector
import com.dv.moneym.feature.categories.list.components.CategoryListHeader
import com.dv.moneym.feature.categories.list.components.DeleteAllTransactionsConfirmSheet
import com.dv.moneym.feature.categories.list.components.DeleteCategoryOptionsSheet
import com.dv.moneym.feature.categories.list.components.DeleteConfirmSheet
import com.dv.moneym.feature.categories.list.components.DraggableCategoryList
import com.dv.moneym.feature.categories.list.components.MigratePickerSheet
import com.dv.moneym.feature.categories.list.components.NewCategorySaveButton
import com.dv.moneym.feature.categories.list.components.NewCategorySheetBody
import com.dv.moneym.feature.categories.list.components.NewCategorySheetHeader
import kotlinx.serialization.Serializable
import moneym.feature.categories.generated.resources.Res
import moneym.feature.categories.generated.resources.categories_cancel
import moneym.feature.categories.generated.resources.categories_delete_all_confirm
import moneym.feature.categories.generated.resources.categories_delete_all_input_label
import moneym.feature.categories.generated.resources.categories_delete_all_title
import moneym.feature.categories.generated.resources.categories_delete_all_warning
import moneym.feature.categories.generated.resources.categories_delete_confirm_title
import moneym.feature.categories.generated.resources.categories_edit_sheet_title
import moneym.feature.categories.generated.resources.categories_migrate_title
import moneym.feature.categories.generated.resources.categories_new_expense
import moneym.feature.categories.generated.resources.categories_new_income
import moneym.feature.categories.generated.resources.categories_new_sheet_title
import moneym.feature.categories.generated.resources.categories_option_archive
import moneym.feature.categories.generated.resources.categories_option_delete_all
import moneym.feature.categories.generated.resources.categories_option_migrate
import moneym.feature.categories.generated.resources.categories_option_unarchive
import moneym.feature.categories.generated.resources.categories_options_subtitle
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
    val showArchived = state.showArchived

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    Box(modifier = Modifier.fillMaxSize().background(colors.bg)) {
        Column(modifier = Modifier.fillMaxSize()) {
            CategoryListHeader(
                activeTab = activeTab,
                categoryCount = if (showArchived) state.archived.size else categories.size,
                archivedCount = state.archived.size,
                showArchived = showArchived,
                onBack = onBack,
                onSetTab = { onIntent(CategoryListIntent.SetTab(it)) },
                onToggleArchived = { onIntent(CategoryListIntent.ToggleShowArchived) },
            )

            if (showArchived) {
                ArchivedCategoryList(
                    categories = state.archived,
                    onCategoryClick = { cat -> onIntent(CategoryListIntent.OpenDeleteOptions(cat)) },
                    modifier = Modifier.weight(1f),
                )
            } else {
                DraggableCategoryList(
                    categories = categories,
                    onReorder = { ordered ->
                        onIntent(CategoryListIntent.Reorder(ordered.map { it.id }))
                    },
                    onCategoryClick = { cat -> onIntent(CategoryListIntent.StartEditCategory(cat)) },
                    modifier = Modifier.weight(1f).fillMaxWidth(),
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
                        variant = MmButtonVariant.Primary,
                        fullWidth = true,
                        leadingIcon = Icon.Plus.imageVector,
                    )
                }
            }
        }

        MmLoadingOverlay(visible = state.isSaving)
    }

    state.deleteOptionsFor?.let { category ->
        DeleteCategoryOptionsSheet(
            category = category,
            transactionCount = state.deleteTxCount,
            title = stringResource(Res.string.categories_delete_confirm_title, category.name),
            subtitle = stringResource(Res.string.categories_options_subtitle, state.deleteTxCount),
            migrateLabel = stringResource(Res.string.categories_option_migrate),
            archiveLabel = stringResource(Res.string.categories_option_archive),
            unarchiveLabel = stringResource(Res.string.categories_option_unarchive),
            deleteAllLabel = stringResource(Res.string.categories_option_delete_all),
            onMigrate = { onIntent(CategoryListIntent.OpenMigratePicker) },
            onArchive = { onIntent(CategoryListIntent.DeleteArchive) },
            onUnarchive = { onIntent(CategoryListIntent.UnarchiveRequested(category.id)) },
            onDeleteAll = { onIntent(CategoryListIntent.OpenDeleteAllConfirm) },
            onDismiss = { onIntent(CategoryListIntent.DismissDeleteOptions) },
        )
    }

    if (state.showMigratePicker) {
        MigratePickerSheet(
            title = stringResource(Res.string.categories_migrate_title),
            targets = state.migrateTargets,
            onSelect = { onIntent(CategoryListIntent.MigrateTo(it)) },
            onDismiss = { onIntent(CategoryListIntent.DismissMigratePicker) },
        )
    }

    state.typeConfirmFor?.let { category ->
        DeleteAllTransactionsConfirmSheet(
            title = stringResource(Res.string.categories_delete_all_title, category.name),
            warning = stringResource(Res.string.categories_delete_all_warning, state.deleteTxCount),
            inputLabel = stringResource(Res.string.categories_delete_all_input_label),
            confirmLabel = stringResource(Res.string.categories_delete_all_confirm),
            cancelLabel = stringResource(Res.string.categories_cancel),
            categoryName = category.name,
            input = state.typeConfirmInput,
            onInputChange = { onIntent(CategoryListIntent.TypeConfirmChanged(it)) },
            onConfirm = { onIntent(CategoryListIntent.ConfirmDeleteAll) },
            onCancel = { onIntent(CategoryListIntent.DismissDeleteAllConfirm) },
        )
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
            onDeleteClick = { categoryToEdit?.let { onDelete(it.id) } },
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
            onConfirm = { onIntent(CategoryListIntent.ConfirmSimpleDelete) },
            onCancel = { onIntent(CategoryListIntent.ShowDeleteConfirm(false)) },
        )
    }
}

@Composable
private fun ArchivedCategoryList(
    categories: List<Category>,
    onCategoryClick: (Category) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MM.colors
    LazyColumn(modifier = modifier.fillMaxWidth()) {
        items(categories, key = { it.id.value }) { cat ->
            MmRow(
                onClick = { onCategoryClick(cat) },
                divider = cat.id != categories.lastOrNull()?.id,
            ) {
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
                    color = colors.text2,
                    modifier = Modifier.weight(1f),
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
        Category(
            id = CategoryId(1L),
            name = "Groceries",
            iconKey = "basket",
            colorHex = "#4A8E5C",
            isUserCreated = false,
            archived = false,
            createdAt = epoch,
            updatedAt = epoch,
        ),
        Category(
            id = CategoryId(2L),
            name = "Eating Out",
            iconKey = "pizza",
            colorHex = "#E07A5F",
            isUserCreated = true,
            archived = false,
            createdAt = epoch,
            updatedAt = epoch,
        ),
    )
    MoneyMTheme {
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
