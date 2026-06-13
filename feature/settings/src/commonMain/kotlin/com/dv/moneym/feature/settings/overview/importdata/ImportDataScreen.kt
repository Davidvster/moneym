package com.dv.moneym.feature.settings.overview.importdata

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.clickable
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.dv.moneym.core.common.DateStyle
import com.dv.moneym.core.common.formatDate
import com.dv.moneym.core.designsystem.MM
import com.dv.moneym.core.model.Account
import com.dv.moneym.core.model.Category
import com.dv.moneym.core.model.CategoryId
import com.dv.moneym.core.model.TransactionType
import com.dv.moneym.core.ui.MmButton
import com.dv.moneym.core.ui.MmButtonSize
import com.dv.moneym.core.ui.MmCard
import com.dv.moneym.core.ui.MmCheckbox
import com.dv.moneym.core.ui.MmMoney
import com.dv.moneym.core.ui.MmRow
import com.dv.moneym.core.ui.MmSheetHeader
import com.dv.moneym.core.ui.ScreenHeader
import com.dv.moneym.core.ui.SectionLabel
import com.dv.moneym.core.ui.imageVector
import kotlinx.serialization.Serializable
import moneym.feature.settings.generated.resources.Res
import moneym.feature.settings.generated.resources.settings_import_button_one
import moneym.feature.settings.generated.resources.settings_import_button_other
import moneym.feature.settings.generated.resources.settings_import_categories
import moneym.feature.settings.generated.resources.settings_import_create_new_category
import moneym.feature.settings.generated.resources.settings_import_data_title
import moneym.feature.settings.generated.resources.settings_import_deselect_all
import moneym.feature.settings.generated.resources.settings_import_into
import moneym.feature.settings.generated.resources.settings_import_map_to
import moneym.feature.settings.generated.resources.settings_import_new_category
import moneym.feature.settings.generated.resources.settings_import_select_all
import moneym.feature.settings.generated.resources.settings_import_transactions_selected
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import com.dv.moneym.core.model.Icon as MmIcon

@Serializable
data object ImportDataKey : NavKey

fun EntryProviderScope<NavKey>.importDataEntry(
    onBack: () -> Unit,
    metadata: Map<String, Any> = emptyMap(),
) = entry<ImportDataKey>(metadata = metadata) {
    ImportDataScreen(onBack = onBack)
}

@Composable
private fun ImportDataScreen(
    onBack: () -> Unit,
    viewModel: ImportDataViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                ImportDataEffect.ImportDone -> onBack()
                is ImportDataEffect.ShowError -> Unit
            }
        }
    }

    ImportDataContent(
        state = state,
        onIntent = viewModel::onIntent,
        onBack = onBack,
    )
}

@Composable
private fun ImportDataContent(
    state: ImportDataUiState,
    onIntent: (ImportDataIntent) -> Unit,
    onBack: () -> Unit,
) {
    val colors = MM.colors
    val type = MM.type
    val allSelected = state.transactions.isNotEmpty() && state.transactions.all { it.isSelected }
    val selectedCount = state.transactions.count { it.isSelected }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bg),
    ) {
        ScreenHeader(
            title = stringResource(Res.string.settings_import_data_title),
            onBack = onBack,
        )

        when {
            state.isParsing -> {
                Box(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(color = colors.text)
                }
            }

            state.parseError != null -> {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(MM.dimen.padding_2x),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = state.parseError,
                        style = type.body,
                        color = colors.danger,
                    )
                }
            }

            else -> {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onIntent(ImportDataIntent.SelectAllToggled) }
                        .padding(horizontal = MM.dimen.padding_2x, vertical = MM.dimen.padding_1x),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(MM.dimen.padding_1x),
                ) {
                    MmCheckbox(
                        checked = allSelected,
                        onCheckedChange = { onIntent(ImportDataIntent.SelectAllToggled) },
                    )
                    Text(
                        text = stringResource(if (allSelected) Res.string.settings_import_deselect_all else Res.string.settings_import_select_all),
                        style = MM.type.body,
                        color = MM.colors.text,
                    )
                }
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(
                        horizontal = MM.dimen.padding_2x,
                        vertical = MM.dimen.padding_2x,
                    ),
                    verticalArrangement = Arrangement.spacedBy(MM.dimen.padding_2x),
                ) {
                    if (state.availableAccounts.isNotEmpty()) {
                        item {
                            SectionLabel(
                                text = stringResource(Res.string.settings_import_into),
                                modifier = Modifier.padding(bottom = MM.dimen.padding_0_5x),
                            )
                            MmCard(shape = MM.dimen.radius_1_5x) {
                                state.availableAccounts.forEachIndexed { i, account ->
                                    AccountPickerRow(
                                        account = account,
                                        isSelected = account.id == state.selectedAccountId,
                                        onClick = {
                                            onIntent(ImportDataIntent.AccountSelected(account.id))
                                        },
                                        divider = i < state.availableAccounts.lastIndex,
                                    )
                                }
                            }
                        }
                    }

                    if (state.categoryMappings.isNotEmpty()) {
                        item {
                            SectionLabel(
                                text = stringResource(Res.string.settings_import_categories),
                                modifier = Modifier.padding(bottom = MM.dimen.padding_0_5x),
                            )
                            MmCard(shape = MM.dimen.radius_1_5x) {
                                state.categoryMappings.forEachIndexed { i, mapping ->
                                    CategoryMappingRow(
                                        mapping = mapping,
                                        availableCategories = state.availableCategories,
                                        onMappingChanged = { catId ->
                                            onIntent(
                                                ImportDataIntent.CategoryMappingChanged(
                                                    csvName = mapping.csvName,
                                                    newCategoryId = catId,
                                                )
                                            )
                                        },
                                        divider = i < state.categoryMappings.lastIndex,
                                    )
                                }
                            }
                        }
                    }

                    if (state.transactions.isNotEmpty()) {
                        item {
                            SectionLabel(
                                text = stringResource(Res.string.settings_import_transactions_selected, selectedCount),
                                modifier = Modifier.padding(bottom = MM.dimen.padding_0_5x),
                            )
                        }
                        items(state.transactions, key = { it.id }) { txn ->
                            TransactionImportRow(
                                item = txn,
                                onToggle = { onIntent(ImportDataIntent.TransactionToggled(txn.id)) },
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(colors.bg)
                        .padding(MM.dimen.padding_2x),
                ) {
                    if (state.isImporting) {
                        CircularProgressIndicator(
                            modifier = Modifier.align(Alignment.Center),
                            color = colors.text,
                        )
                    } else {
                        MmButton(
                            text = if (selectedCount == 1)
                                stringResource(Res.string.settings_import_button_one, selectedCount)
                            else
                                stringResource(Res.string.settings_import_button_other, selectedCount),
                            onClick = { onIntent(ImportDataIntent.ImportConfirmed) },
                            modifier = Modifier.fillMaxWidth(),
                            size = MmButtonSize.Lg,
                            enabled = selectedCount > 0 && state.selectedAccountId != null,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AccountPickerRow(
    account: Account,
    isSelected: Boolean,
    onClick: () -> Unit,
    divider: Boolean,
) {
    val colors = MM.colors
    val type = MM.type

    MmRow(onClick = onClick, divider = divider) {
        Text(
            text = account.name,
            style = type.body,
            color = colors.text,
            modifier = Modifier.weight(1f),
        )
        if (isSelected) {
            Spacer(Modifier.width(MM.dimen.padding_1x))
            Icon(
                imageVector = MmIcon.Check.imageVector,
                contentDescription = null,
                tint = colors.accent,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryMappingRow(
    mapping: CategoryMappingUiItem,
    availableCategories: List<Category>,
    onMappingChanged: (CategoryId?) -> Unit,
    divider: Boolean,
) {
    val colors = MM.colors
    val type = MM.type
    var showPicker by remember { mutableStateOf(false) }

    MmRow(onClick = { showPicker = true }, divider = divider) {
        Text(
            text = mapping.csvName,
            style = type.body,
            color = colors.text,
            modifier = Modifier.weight(1f),
        )
        Spacer(Modifier.width(MM.dimen.padding_1x))
        Text(
            text = mapping.mappedToCategoryName ?: stringResource(Res.string.settings_import_new_category),
            style = type.caption,
            color = if (mapping.mappedToCategoryId != null) colors.accent else colors.text2,
        )
    }

    if (showPicker) {
        CategoryMappingPickerSheet(
            csvName = mapping.csvName,
            selectedId = mapping.mappedToCategoryId,
            categories = availableCategories,
            onSelected = { catId ->
                onMappingChanged(catId)
                showPicker = false
            },
            onDismiss = { showPicker = false },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryMappingPickerSheet(
    csvName: String,
    selectedId: CategoryId?,
    categories: List<Category>,
    onSelected: (CategoryId?) -> Unit,
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
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = MM.dimen.padding_2x),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .size(width = 36.dp, height = 4.dp)
                        .clip(RoundedCornerShape(50))
                        .background(colors.borderStrong),
                )
            }

            MmSheetHeader(
                title = stringResource(Res.string.settings_import_map_to, csvName),
                onClose = onDismiss,
                modifier = Modifier.padding(bottom = MM.dimen.padding_2x),
            )

            MmCard(shape = MM.dimen.radius_1_5x) {
                MmRow(
                    onClick = { onSelected(null) },
                    divider = categories.isNotEmpty(),
                ) {
                    Text(
                        text = stringResource(Res.string.settings_import_create_new_category),
                        style = type.body,
                        color = colors.text,
                        modifier = Modifier.weight(1f),
                    )
                    if (selectedId == null) {
                        Icon(
                            imageVector = MmIcon.Check.imageVector,
                            contentDescription = null,
                            tint = colors.accent,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
                categories.forEachIndexed { i, cat ->
                    MmRow(
                        onClick = { onSelected(cat.id) },
                        divider = i < categories.lastIndex,
                    ) {
                        Text(
                            text = cat.name,
                            style = type.body,
                            color = colors.text,
                            modifier = Modifier.weight(1f),
                        )
                        if (cat.id == selectedId) {
                            Icon(
                                imageVector = MmIcon.Check.imageVector,
                                contentDescription = null,
                                tint = colors.accent,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.size(MM.dimen.padding_1x))
        }
    }
}

@Composable
private fun TransactionImportRow(
    item: ImportTransactionUiItem,
    onToggle: () -> Unit,
) {
    val colors = MM.colors
    val type = MM.type
    val isExpense = item.type == TransactionType.EXPENSE
    val amountColor = if (isExpense) colors.text else colors.accent
    val sign = if (isExpense) "" else "+"

    MmRow(onClick = onToggle, divider = false) {
        MmCheckbox(
            checked = item.isSelected,
            onCheckedChange = { onToggle() },
        )
        Spacer(Modifier.width(MM.dimen.padding_2x))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.categoryName,
                style = type.body,
                color = colors.text,
                maxLines = 1,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(MM.dimen.padding_1x)) {
                Text(
                    text = formatDate(item.date, DateStyle.Medium),
                    style = type.caption,
                    color = colors.text2,
                )
                item.note?.takeIf { it.isNotEmpty() }?.let { note ->
                    Text(
                        text = "· $note",
                        style = type.caption,
                        color = colors.text2,
                        maxLines = 1,
                    )
                }
            }
        }
        MmMoney(
            value = item.amountMinorUnits / 100.0,
            currency = item.currencyCode,
            sign = sign,
            color = amountColor,
        )
    }
}

@androidx.compose.ui.tooling.preview.Preview
@Composable
private fun ImportDataContentPreview() {
    com.dv.moneym.core.designsystem.MoneyMTheme {
        ImportDataContent(
            state = ImportDataUiState(isParsing = false, parseError = null),
            onIntent = {},
            onBack = {},
        )
    }
}
