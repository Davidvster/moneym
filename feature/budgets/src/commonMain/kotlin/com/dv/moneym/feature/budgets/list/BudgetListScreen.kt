package com.dv.moneym.feature.budgets.list

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.dv.moneym.core.designsystem.MM
import com.dv.moneym.core.designsystem.MoneyMTheme
import com.dv.moneym.core.model.BudgetId
import com.dv.moneym.core.model.CurrencyCode
import com.dv.moneym.core.model.Icon
import com.dv.moneym.core.model.Money
import com.dv.moneym.core.navigation.ModalKey
import com.dv.moneym.core.ui.MmButton
import com.dv.moneym.core.ui.MmButtonVariant
import com.dv.moneym.core.ui.MmCard
import com.dv.moneym.core.ui.MmEmptyState
import com.dv.moneym.core.ui.MmDeleteSheet
import com.dv.moneym.core.ui.ScreenHeader
import com.dv.moneym.core.ui.WalletSelector
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
    val allCategoriesLabel = stringResource(Res.string.budgets_all_categories)
    val unlimitedLabel = stringResource(Res.string.budgets_recurring_badge_unlimited)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bg),
    ) {
        ScreenHeader(
            title = stringResource(Res.string.budgets_title),
            onBack = onBack,
            trailingContent = if (state.accounts.size > 1) {
                {
                    WalletSelector(
                        accounts = state.accounts,
                        selectedAccountId = state.selectedAccountId,
                        onSelect = { onIntent(BudgetListIntent.AccountSelected(it)) },
                    )
                }
            } else null,
        )

        if (state.rows.isEmpty() && !state.isLoading) {
            MmEmptyState(
                message = stringResource(Res.string.budgets_empty),
                modifier = Modifier.weight(1f),
            )
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(
                    horizontal = MM.dimen.padding_2_5x,
                    vertical = MM.dimen.padding_2x,
                ),
            ) {
                item {
                    MmCard(padded = false, shape = MM.dimen.radius_1_5x) {
                        Column {
                            state.rows.forEachIndexed { idx, row ->
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
                                    divider = idx < state.rows.lastIndex,
                                )
                            }
                        }
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .padding(
                    horizontal = MM.dimen.padding_2_5x,
                    vertical = MM.dimen.padding_2x,
                )
                .navigationBarsPadding(),
        ) {
            MmButton(
                text = stringResource(Res.string.budgets_new),
                onClick = onCreate,
                variant = MmButtonVariant.Primary,
                fullWidth = true,
                leadingIcon = Icon.Plus.imageVector,
            )
        }
    }

    if (state.deleteRequestId != null) {
        MmDeleteSheet(
            title = stringResource(
                Res.string.budgets_delete_confirm_title,
                state.deleteRequestName.orEmpty(),
            ),
            body = stringResource(Res.string.budgets_delete_confirm_body),
            cancelText = stringResource(Res.string.budgets_cancel),
            confirmText = stringResource(Res.string.budgets_delete),
            onConfirm = { onIntent(BudgetListIntent.ConfirmDelete) },
            onCancel = { onIntent(BudgetListIntent.DismissDelete) },
        )
    }
}

@Composable
private fun recurringNMonthsString(n: Int): String =
    stringResource(Res.string.budgets_recurring_badge_n_months, n)

@Preview
@Composable
private fun BudgetListContentPreview() {
    MoneyMTheme {
        BudgetListContent(
            state = BudgetListUiState(
                isLoading = false,
                rows = listOf(
                    BudgetRowVm(
                        id = BudgetId(1),
                        name = "Groceries",
                        amount = Money(50000, CurrencyCode("EUR")),
                        scopeLabel = "Food",
                        recurringLabel = "1",
                    ),
                    BudgetRowVm(
                        id = BudgetId(2),
                        name = "Subscription",
                        amount = Money(1500, CurrencyCode("EUR")),
                        scopeLabel = BudgetListViewModel.ALL_CATEGORIES_SENTINEL,
                        recurringLabel = BudgetListViewModel.RECURRING_UNLIMITED_SENTINEL,
                    ),
                    BudgetRowVm(
                        id = BudgetId(3),
                        name = "One-time",
                        amount = Money(100000, CurrencyCode("EUR")),
                        scopeLabel = "Electronics",
                        recurringLabel = null,
                    ),
                ),
            ),
            onBack = {},
            onCreate = {},
            onEdit = {},
            onIntent = {},
        )
    }
}
