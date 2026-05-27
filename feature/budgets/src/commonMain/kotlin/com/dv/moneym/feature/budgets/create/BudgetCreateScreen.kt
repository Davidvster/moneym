package com.dv.moneym.feature.budgets.create

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.dv.moneym.core.designsystem.MM
import com.dv.moneym.core.model.BudgetId
import com.dv.moneym.core.model.Icon
import com.dv.moneym.core.model.YearMonth
import com.dv.moneym.core.navigation.ModalKey
import com.dv.moneym.core.ui.CategoryChip
import com.dv.moneym.core.ui.MmAmountInput
import com.dv.moneym.core.ui.monthLabel
import com.dv.moneym.core.ui.MmButton
import com.dv.moneym.core.ui.MmButtonVariant
import com.dv.moneym.core.ui.MmChip
import com.dv.moneym.core.ui.MmField
import com.dv.moneym.core.ui.MmIconButton
import com.dv.moneym.core.ui.ScreenHeader
import com.dv.moneym.core.ui.imageVector
import kotlinx.serialization.Serializable
import moneym.feature.budgets.generated.resources.Res
import moneym.feature.budgets.generated.resources.budgets_account_label
import moneym.feature.budgets.generated.resources.budgets_all_categories
import moneym.feature.budgets.generated.resources.budgets_amount_label
import moneym.feature.budgets.generated.resources.budgets_category_label
import moneym.feature.budgets.generated.resources.budgets_edit_title
import moneym.feature.budgets.generated.resources.budgets_error_amount
import moneym.feature.budgets.generated.resources.budgets_error_count
import moneym.feature.budgets.generated.resources.budgets_error_required
import moneym.feature.budgets.generated.resources.budgets_name_label
import moneym.feature.budgets.generated.resources.budgets_new_title
import moneym.feature.budgets.generated.resources.budgets_next_month
import moneym.feature.budgets.generated.resources.budgets_no_month
import moneym.feature.budgets.generated.resources.budgets_placeholder_name
import moneym.feature.budgets.generated.resources.budgets_prev_month
import moneym.feature.budgets.generated.resources.budgets_recurring
import moneym.feature.budgets.generated.resources.budgets_recurring_count_label
import moneym.feature.budgets.generated.resources.budgets_recurring_single
import moneym.feature.budgets.generated.resources.budgets_recurring_unlimited
import moneym.feature.budgets.generated.resources.budgets_recurring_n_months
import moneym.feature.budgets.generated.resources.budgets_save
import moneym.feature.budgets.generated.resources.budgets_start_month
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Serializable
data class BudgetCreateKey(val id: Long? = null) : ModalKey

fun EntryProviderScope<NavKey>.budgetCreateEntry(
    onBack: () -> Unit,
    metadata: Map<String, Any> = emptyMap(),
) = entry<BudgetCreateKey>(metadata = metadata) { key ->
    val budgetId: BudgetId? = key.id?.let { BudgetId(it) }
    val viewModel: BudgetCreateViewModel = koinViewModel(parameters = { parametersOf(budgetId) })
    BudgetCreateScreen(onBack = onBack, viewModel = viewModel)
}

@Composable
fun BudgetCreateScreen(
    onBack: () -> Unit,
    viewModel: BudgetCreateViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val event by viewModel.singleEvents.collectAsStateWithLifecycle()
    LaunchedEffect(event?.id) {
        when (event) {
            is BudgetCreateViewModel.BudgetCreateSingleUiEvent.NavigateBack -> onBack()
            null -> Unit
        }
    }
    BudgetCreateContent(
        state = state,
        onBack = onBack,
        onIntent = viewModel::onIntent,
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BudgetCreateContent(
    state: BudgetCreateUiState,
    onBack: () -> Unit,
    onIntent: (BudgetCreateIntent) -> Unit,
) {
    val colors = MM.colors
    val type = MM.type
    val title = if (state.isEditMode) stringResource(Res.string.budgets_edit_title)
    else stringResource(Res.string.budgets_new_title)
    val allCategoriesLabel = stringResource(Res.string.budgets_all_categories)
    val amountFocusRequester = remember { FocusRequester() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bg),
    ) {
        ScreenHeader(title = title, onBack = onBack)
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                horizontal = MM.dimen.padding_2_5x,
                vertical = MM.dimen.padding_2x,
            ),
            verticalArrangement = Arrangement.spacedBy(MM.dimen.padding_2x),
        ) {
            item {
                MmField(
                    value = state.name,
                    onValueChange = { onIntent(BudgetCreateIntent.NameChanged(it)) },
                    label = stringResource(Res.string.budgets_name_label),
                    placeholder = stringResource(Res.string.budgets_placeholder_name),
                )
                if (state.nameError) {
                    Text(stringResource(Res.string.budgets_error_required), style = type.caption.copy(color = colors.danger), modifier = Modifier.padding(top = 4.dp))
                }
            }
            item {
                Text(
                    stringResource(Res.string.budgets_amount_label).uppercase(),
                    style = type.micro,
                    color = colors.text2,
                    modifier = Modifier.padding(bottom = 4.dp),
                )
                MmAmountInput(
                    amountText = state.amountText,
                    currencyCode = state.currency,
                    focusRequester = amountFocusRequester,
                    onAmountChanged = { onIntent(BudgetCreateIntent.AmountChanged(it)) },
                )
                if (state.amountError) {
                    Text(stringResource(Res.string.budgets_error_amount), style = type.caption.copy(color = colors.danger), modifier = Modifier.padding(top = 4.dp))
                }
            }
            item {
                Text(
                    stringResource(Res.string.budgets_account_label).uppercase(),
                    style = type.micro,
                    color = colors.text2,
                    modifier = Modifier.padding(bottom = 4.dp),
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(MM.dimen.padding_1x),
                    verticalArrangement = Arrangement.spacedBy(MM.dimen.padding_1x),
                ) {
                    state.availableAccounts.forEach { account ->
                        MmChip(
                            selected = state.selectedAccountId == account.id,
                            onClick = { onIntent(BudgetCreateIntent.AccountSelected(account.id)) },
                        ) {
                            Text(
                                account.name,
                                style = type.caption,
                                color = if (state.selectedAccountId == account.id) colors.bg else colors.text,
                            )
                        }
                    }
                }
            }
            item {
                Text(
                    stringResource(Res.string.budgets_category_label).uppercase(),
                    style = type.micro,
                    color = colors.text2,
                    modifier = Modifier.padding(bottom = 4.dp),
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(MM.dimen.padding_1x),
                    verticalArrangement = Arrangement.spacedBy(MM.dimen.padding_1x),
                ) {
                    MmChip(
                        selected = state.selectedCategoryId == null,
                        onClick = { onIntent(BudgetCreateIntent.CategorySelected(null)) },
                    ) {
                        Text(
                            allCategoriesLabel,
                            style = type.caption,
                            color = if (state.selectedCategoryId == null) colors.bg else colors.text,
                        )
                    }
                    state.availableCategories.filter { !it.archived }.forEach { cat ->
                        CategoryChip(
                            category = cat,
                            isSelected = state.selectedCategoryId == cat.id,
                            onClick = { onIntent(BudgetCreateIntent.CategorySelected(cat.id)) },
                        )
                    }
                }
            }
            item {
                StartMonthRow(
                    ym = state.startYearMonth,
                    onPrev = { state.startYearMonth?.let { onIntent(BudgetCreateIntent.StartMonthChanged(it.previous())) } },
                    onNext = { state.startYearMonth?.let { onIntent(BudgetCreateIntent.StartMonthChanged(it.next())) } },
                )
            }
            item {
                RecurringRow(
                    kind = state.recurringKind,
                    nMonths = state.recurringNMonths,
                    onKindChanged = { onIntent(BudgetCreateIntent.RecurringKindChanged(it)) },
                    onCountChanged = { onIntent(BudgetCreateIntent.RecurringCountChanged(it)) },
                    showCountError = state.recurringCountError,
                )
            }
        }
        Box(
            modifier = Modifier.padding(
                horizontal = MM.dimen.padding_2_5x,
                vertical = MM.dimen.padding_2x,
            ),
        ) {
            MmButton(
                text = stringResource(Res.string.budgets_save),
                onClick = { onIntent(BudgetCreateIntent.Save) },
                variant = MmButtonVariant.Primary,
                fullWidth = true,
                enabled = !state.isSaving,
                leadingIcon = Icon.Check.imageVector,
            )
        }
    }
}

@Composable
private fun StartMonthRow(
    ym: YearMonth?,
    onPrev: () -> Unit,
    onNext: () -> Unit,
) {
    val colors = MM.colors
    val type = MM.type
    Column {
        Text(
            stringResource(Res.string.budgets_start_month).uppercase(),
            style = type.micro,
            color = colors.text2,
            modifier = Modifier.padding(bottom = 4.dp),
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.padding(vertical = MM.dimen.padding_1x),
        ) {
            MmIconButton(icon = Icon.ChevronLeft.imageVector, onClick = onPrev, contentDescription = stringResource(Res.string.budgets_prev_month))
            Text(
                text = ym?.let { monthLabel(it) } ?: stringResource(Res.string.budgets_no_month),
                style = type.body,
                color = colors.text,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
            )
            MmIconButton(icon = Icon.ChevronRight.imageVector, onClick = onNext, contentDescription = stringResource(Res.string.budgets_next_month))
        }
    }
}

@Composable
private fun RecurringRow(
    kind: RecurringKind,
    nMonths: Int,
    onKindChanged: (RecurringKind) -> Unit,
    onCountChanged: (Int) -> Unit,
    showCountError: Boolean,
) {
    val colors = MM.colors
    val type = MM.type
    Column(verticalArrangement = Arrangement.spacedBy(MM.dimen.padding_1x)) {
        Text(
            stringResource(Res.string.budgets_recurring).uppercase(),
            style = type.micro,
            color = colors.text2,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(MM.dimen.padding_1x)) {
            RecurringChip(stringResource(Res.string.budgets_recurring_single), kind == RecurringKind.Single) {
                onKindChanged(RecurringKind.Single)
            }
            RecurringChip(stringResource(Res.string.budgets_recurring_unlimited), kind == RecurringKind.Unlimited) {
                onKindChanged(RecurringKind.Unlimited)
            }
            RecurringChip(stringResource(Res.string.budgets_recurring_n_months), kind == RecurringKind.NMonths) {
                onKindChanged(RecurringKind.NMonths)
            }
        }
        if (kind == RecurringKind.NMonths) {
            MmField(
                value = nMonths.toString(),
                onValueChange = { onCountChanged(it.toIntOrNull() ?: 0) },
                label = stringResource(Res.string.budgets_recurring_count_label),
                keyboardType = KeyboardType.Number,
            )
            if (showCountError) {
                Text(stringResource(Res.string.budgets_error_count), style = type.caption.copy(color = colors.danger))
            }
        }
    }
}

@Composable
private fun RecurringChip(text: String, selected: Boolean, onClick: () -> Unit) {
    val colors = MM.colors
    val type = MM.type
    MmChip(selected = selected, onClick = onClick) {
        Text(text, style = type.caption, color = if (selected) colors.bg else colors.text)
    }
}



@androidx.compose.ui.tooling.preview.Preview
@androidx.compose.runtime.Composable
private fun BudgetCreateContentPreview() {
    com.dv.moneym.core.designsystem.MoneyMTheme {
        BudgetCreateContent(
            state = BudgetCreateUiState(
                name = "Groceries",
                amountText = "300.00",
                currency = "EUR",
                startYearMonth = com.dv.moneym.core.model.YearMonth(2026, 5),
            ),
            onBack = {},
            onIntent = {},
        )
    }
}
