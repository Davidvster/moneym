package com.dv.moneym.feature.budgets.list

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.dv.moneym.core.designsystem.MM
import com.dv.moneym.core.model.BudgetId
import com.dv.moneym.core.model.Icon
import com.dv.moneym.core.navigation.ModalKey
import com.dv.moneym.core.ui.MmButton
import com.dv.moneym.core.ui.MmButtonSize
import com.dv.moneym.core.ui.MmButtonVariant
import com.dv.moneym.core.ui.ScreenHeader
import com.dv.moneym.core.ui.imageVector
import com.dv.moneym.feature.budgets.list.components.BudgetRow
import kotlinx.serialization.Serializable
import moneym.feature.budgets.generated.resources.Res
import moneym.feature.budgets.generated.resources.budgets_all_categories
import moneym.feature.budgets.generated.resources.budgets_cancel
import moneym.feature.budgets.generated.resources.budgets_delete
import moneym.feature.budgets.generated.resources.budgets_delete_confirm_body
import moneym.feature.budgets.generated.resources.budgets_delete_confirm_title
import moneym.feature.budgets.generated.resources.budgets_empty
import moneym.feature.budgets.generated.resources.budgets_new
import moneym.feature.budgets.generated.resources.budgets_recurring_badge_n_months
import moneym.feature.budgets.generated.resources.budgets_recurring_badge_unlimited
import moneym.feature.budgets.generated.resources.budgets_title
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Serializable
data object BudgetListKey : ModalKey

fun EntryProviderScope<NavKey>.budgetListEntry(
    onBack: () -> Unit,
    onCreate: () -> Unit,
    onEdit: (BudgetId) -> Unit,
    metadata: Map<String, Any> = emptyMap(),
) = entry<BudgetListKey>(metadata = metadata) {
    BudgetListScreen(onBack = onBack, onCreate = onCreate, onEdit = onEdit)
}

@Composable
fun BudgetListScreen(
    onBack: () -> Unit,
    onCreate: () -> Unit,
    onEdit: (BudgetId) -> Unit,
    viewModel: BudgetListViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    BudgetListContent(
        state = state,
        onBack = onBack,
        onCreate = onCreate,
        onEdit = onEdit,
        onIntent = viewModel::onIntent,
    )
}

@Composable
private fun BudgetListContent(
    state: BudgetListUiState,
    onBack: () -> Unit,
    onCreate: () -> Unit,
    onEdit: (BudgetId) -> Unit,
    onIntent: (BudgetListIntent) -> Unit,
) {
    val colors = MM.colors
    val type = MM.type
    val allCategoriesLabel = stringResource(Res.string.budgets_all_categories)
    val unlimitedLabel = stringResource(Res.string.budgets_recurring_badge_unlimited)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bg),
    ) {
        ScreenHeader(title = stringResource(Res.string.budgets_title), onBack = onBack)

        if (state.rows.isEmpty() && !state.isLoading) {
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                Text(
                    stringResource(Res.string.budgets_empty),
                    style = type.body,
                    color = colors.text3,
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    vertical = MM.dimen.padding_1x,
                ),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                items(state.rows, key = { it.id.value }) { row ->
                    val scope = when (row.scopeLabel) {
                        BudgetListViewModel.ALL_CATEGORIES_SENTINEL -> allCategoriesLabel
                        else -> row.scopeLabel
                    }
                    val recurring = when (row.recurringLabel) {
                        null -> null
                        BudgetListViewModel.RECURRING_UNLIMITED_SENTINEL -> unlimitedLabel
                        else -> {
                            val n = row.recurringLabel.toIntOrNull() ?: 0
                            recurringNMonthsString(n)
                        }
                    }
                    BudgetRow(
                        row = row,
                        scopeLabel = scope,
                        recurringLabel = recurring,
                        onClick = { onEdit(row.id) },
                        onDelete = { onIntent(BudgetListIntent.DeleteRequested(row.id)) },
                    )
                }
            }
        }

        Box(
            modifier = Modifier.padding(
                horizontal = MM.dimen.padding_2_5x,
                vertical = MM.dimen.padding_2x,
            ),
        ) {
            MmButton(
                text = stringResource(Res.string.budgets_new),
                onClick = onCreate,
                variant = MmButtonVariant.Secondary,
                fullWidth = true,
                leadingIcon = Icon.Plus.imageVector,
            )
        }
    }

    if (state.deleteRequestId != null) {
        AlertDialog(
            onDismissRequest = { onIntent(BudgetListIntent.DismissDelete) },
            containerColor = colors.surface,
            title = {
                Text(
                    text = stringResource(
                        Res.string.budgets_delete_confirm_title,
                        state.deleteRequestName.orEmpty(),
                    ),
                    style = type.title2,
                    color = colors.text,
                )
            },
            text = {
                Text(
                    text = stringResource(Res.string.budgets_delete_confirm_body),
                    style = type.body.copy(color = colors.text2),
                )
            },
            confirmButton = {
                MmButton(
                    text = stringResource(Res.string.budgets_delete),
                    onClick = { onIntent(BudgetListIntent.ConfirmDelete) },
                    variant = MmButtonVariant.Primary,
                    size = MmButtonSize.Md,
                )
            },
            dismissButton = {
                MmButton(
                    text = stringResource(Res.string.budgets_cancel),
                    onClick = { onIntent(BudgetListIntent.DismissDelete) },
                    variant = MmButtonVariant.Ghost,
                    size = MmButtonSize.Md,
                )
            },
        )
    }
}

@Composable
private fun recurringNMonthsString(n: Int): String =
    stringResource(Res.string.budgets_recurring_badge_n_months, n)
