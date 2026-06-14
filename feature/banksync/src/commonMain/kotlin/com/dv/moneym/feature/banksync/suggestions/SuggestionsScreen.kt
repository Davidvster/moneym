package com.dv.moneym.feature.banksync.suggestions

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.dv.moneym.core.designsystem.MM
import com.dv.moneym.core.model.AccountId
import com.dv.moneym.core.model.CategoryId
import com.dv.moneym.core.model.Icon
import com.dv.moneym.core.model.SpendingFilter
import com.dv.moneym.core.model.TransactionType
import com.dv.moneym.core.ui.MmButton
import com.dv.moneym.core.ui.MmButtonSize
import com.dv.moneym.core.ui.MmButtonVariant
import com.dv.moneym.core.ui.MmCard
import com.dv.moneym.core.ui.MmCategoryPickerSheet
import androidx.compose.ui.draw.clip
import com.dv.moneym.core.ui.MmCheckbox
import com.dv.moneym.core.ui.mmClickable
import com.dv.moneym.core.ui.MmDialog
import com.dv.moneym.core.ui.MmEmptyState
import com.dv.moneym.core.ui.MmField
import com.dv.moneym.core.ui.MmLoadingOverlay
import com.dv.moneym.core.ui.MmMoney
import com.dv.moneym.core.ui.MmSegmented
import com.dv.moneym.core.ui.MmSheetHeader
import com.dv.moneym.core.ui.MmSnackbarHost
import com.dv.moneym.core.ui.MmWalletPickerSheet
import com.dv.moneym.core.ui.ScreenHeader
import com.dv.moneym.core.ui.imageVector
import com.dv.moneym.core.utils.observeWithLifecycle
import kotlinx.serialization.Serializable
import moneym.feature.banksync.generated.resources.Res
import moneym.feature.banksync.generated.resources.bank_sync_account_target_label
import moneym.feature.banksync.generated.resources.bank_sync_account_target_none
import moneym.feature.banksync.generated.resources.bank_sync_cancel
import moneym.feature.banksync.generated.resources.suggestions_accept
import moneym.feature.banksync.generated.resources.suggestions_accept_all_confirm_body
import moneym.feature.banksync.generated.resources.suggestions_accept_all_confirm_title
import moneym.feature.banksync.generated.resources.suggestions_accept_one_confirm_body
import moneym.feature.banksync.generated.resources.suggestions_accept_one_confirm_title
import moneym.feature.banksync.generated.resources.suggestions_accept_selected
import moneym.feature.banksync.generated.resources.suggestions_assign_account
import moneym.feature.banksync.generated.resources.suggestions_assign_category
import moneym.feature.banksync.generated.resources.suggestions_category_label
import moneym.feature.banksync.generated.resources.suggestions_duplicate_hint
import moneym.feature.banksync.generated.resources.suggestions_empty_pending
import moneym.feature.banksync.generated.resources.suggestions_empty_rejected
import moneym.feature.banksync.generated.resources.suggestions_filter
import moneym.feature.banksync.generated.resources.suggestions_filter_all
import moneym.feature.banksync.generated.resources.suggestions_filter_apply
import moneym.feature.banksync.generated.resources.suggestions_filter_clear
import moneym.feature.banksync.generated.resources.suggestions_filter_expense
import moneym.feature.banksync.generated.resources.suggestions_filter_income
import moneym.feature.banksync.generated.resources.suggestions_filter_max
import moneym.feature.banksync.generated.resources.suggestions_filter_min
import moneym.feature.banksync.generated.resources.suggestions_filter_note
import moneym.feature.banksync.generated.resources.suggestions_filter_title
import moneym.feature.banksync.generated.resources.suggestions_reject
import moneym.feature.banksync.generated.resources.suggestions_reject_all_confirm_body
import moneym.feature.banksync.generated.resources.suggestions_reject_all_confirm_title
import moneym.feature.banksync.generated.resources.suggestions_reject_selected
import moneym.feature.banksync.generated.resources.suggestions_rejected_snackbar
import moneym.feature.banksync.generated.resources.suggestions_restore
import moneym.feature.banksync.generated.resources.suggestions_select_all
import moneym.feature.banksync.generated.resources.suggestions_tab_pending
import moneym.feature.banksync.generated.resources.suggestions_tab_rejected
import moneym.feature.banksync.generated.resources.suggestions_title
import moneym.feature.banksync.generated.resources.suggestions_undo
import moneym.feature.banksync.generated.resources.suggestions_unselect_all
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Serializable
data object BankSuggestionsKey : NavKey

@Serializable
data object WalletSuggestionsKey : NavKey

fun EntryProviderScope<NavKey>.bankSuggestionsEntry(
    onBack: () -> Unit,
    metadata: Map<String, Any> = emptyMap(),
) = entry<BankSuggestionsKey>(metadata = metadata) {
    SuggestionsScreen(source = SuggestionSourceType.BANK, onBack = onBack)
}

fun EntryProviderScope<NavKey>.walletSuggestionsEntry(
    onBack: () -> Unit,
    metadata: Map<String, Any> = emptyMap(),
) = entry<WalletSuggestionsKey>(metadata = metadata) {
    SuggestionsScreen(source = SuggestionSourceType.WALLET, onBack = onBack)
}

@Composable
fun SuggestionsScreen(
    source: SuggestionSourceType,
    onBack: () -> Unit,
    viewModel: SuggestionsViewModel = koinViewModel(parameters = { parametersOf(source) }),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    viewModel.singleEvents.observeWithLifecycle { event ->
        when (event) {
            is SuggestionsViewModel.SuggestionsSingleUiEvent.RejectedWithUndo -> {
                val result = snackbarHostState.showSnackbar(
                    message = getString(Res.string.suggestions_rejected_snackbar),
                    actionLabel = getString(Res.string.suggestions_undo),
                    duration = SnackbarDuration.Short,
                )
                if (result == SnackbarResult.ActionPerformed) {
                    viewModel.onIntent(SuggestionsIntent.UndoReject(event.id))
                }
            }
        }
    }

    SuggestionsContent(
        state = state,
        snackbarHostState = snackbarHostState,
        onBack = onBack,
        onIntent = viewModel::onIntent,
    )
}

@Composable
private fun SuggestionsContent(
    state: SuggestionsUiState,
    snackbarHostState: SnackbarHostState,
    onBack: () -> Unit,
    onIntent: (SuggestionsIntent) -> Unit,
) {
    val colors = MM.colors
    val space = MM.dimen

    Box(modifier = Modifier.fillMaxSize().background(colors.bg)) {
        Column(modifier = Modifier.fillMaxSize()) {
            ScreenHeader(title = stringResource(Res.string.suggestions_title), onBack = onBack)

            MmSegmented(
                options = listOf(
                    stringResource(Res.string.suggestions_tab_pending),
                    stringResource(Res.string.suggestions_tab_rejected),
                ),
                selectedIndex = if (state.tab == SuggestionsTab.PENDING) 0 else 1,
                onOptionSelected = { index ->
                    onIntent(
                        SuggestionsIntent.SetTab(
                            if (index == 0) SuggestionsTab.PENDING else SuggestionsTab.REJECTED
                        )
                    )
                },
                fillWidth = true,
                modifier = Modifier.padding(
                    horizontal = space.padding_2x,
                    vertical = space.padding_1x
                ),
            )

            if (state.tab == SuggestionsTab.PENDING && state.pending.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = space.padding_2x),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    MmButton(
                        text = if (state.filter.isActive) {
                            stringResource(Res.string.suggestions_filter) + " •"
                        } else {
                            stringResource(Res.string.suggestions_filter)
                        },
                        onClick = { onIntent(SuggestionsIntent.ShowFilterSheet(true)) },
                        variant = MmButtonVariant.Ghost,
                        size = MmButtonSize.Sm,
                    )
                    MmButton(
                        text = if (state.allSelected) {
                            stringResource(Res.string.suggestions_unselect_all)
                        } else {
                            stringResource(Res.string.suggestions_select_all)
                        },
                        onClick = { onIntent(SuggestionsIntent.ToggleSelectAll) },
                        variant = MmButtonVariant.Ghost,
                        size = MmButtonSize.Sm,
                    )
                }
            }

            when {
                state.rows.isEmpty() -> MmEmptyState(
                    message = if (state.tab == SuggestionsTab.PENDING) {
                        stringResource(Res.string.suggestions_empty_pending)
                    } else {
                        stringResource(Res.string.suggestions_empty_rejected)
                    },
                    modifier = Modifier.weight(1f),
                )

                else -> LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(
                        start = space.padding_2x,
                        end = space.padding_2x,
                        top = space.padding_1x,
                        bottom = space.padding_4x,
                    ),
                    verticalArrangement = Arrangement.spacedBy(space.padding_2x),
                ) {
                    items(state.rows, key = { it.id }) { row ->
                        SuggestionCard(row = row, state = state, onIntent = onIntent)
                    }
                }
            }

            if (state.tab == SuggestionsTab.PENDING && state.selectedIds.isNotEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = space.padding_2x, vertical = space.padding_1x)
                        .navigationBarsPadding(),
                    verticalArrangement = Arrangement.spacedBy(space.padding_1x),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(space.padding_1x),
                    ) {
                        MmButton(
                            text = stringResource(Res.string.suggestions_assign_category),
                            onClick = { onIntent(SuggestionsIntent.ShowBatchCategoryPicker(true)) },
                            variant = MmButtonVariant.Outline,
                            modifier = Modifier.weight(1f),
                        )
                        MmButton(
                            text = stringResource(Res.string.suggestions_assign_account),
                            onClick = { onIntent(SuggestionsIntent.ShowBatchAccountPicker(true)) },
                            variant = MmButtonVariant.Outline,
                            modifier = Modifier.weight(1f),
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(space.padding_1x),
                    ) {
                        MmButton(
                            text = stringResource(
                                Res.string.suggestions_accept_selected,
                                state.selectedIds.size
                            ),
                            onClick = { onIntent(SuggestionsIntent.RequestAcceptSelected) },
                            variant = MmButtonVariant.Accent,
                            modifier = Modifier.weight(1f),
                        )
                        MmButton(
                            text = stringResource(
                                Res.string.suggestions_reject_selected,
                                state.selectedIds.size
                            ),
                            onClick = { onIntent(SuggestionsIntent.RequestRejectSelected) },
                            variant = MmButtonVariant.Danger,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }

        MmSnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter).padding(space.padding_2x),
        )

        MmLoadingOverlay(visible = state.isLoading || state.isProcessing)
    }

    val categoryPickerRow = state.pending.firstOrNull { it.id == state.categoryPickerForId }
    if (categoryPickerRow != null) {
        MmCategoryPickerSheet(
            categories = state.categories.filter {
                (it.type == TransactionType.EXPENSE) == categoryPickerRow.isExpense
            },
            selectedId = categoryPickerRow.categoryId?.let { CategoryId(it) },
            onPick = { onIntent(SuggestionsIntent.SetCategory(categoryPickerRow.id, it.value)) },
            onDismiss = { onIntent(SuggestionsIntent.ShowCategoryPicker(null)) },
        )
    }

    val accountPickerRow = state.pending.firstOrNull { it.id == state.accountPickerForId }
    if (accountPickerRow != null) {
        MmWalletPickerSheet(
            accounts = state.accounts,
            selectedAccountId = accountPickerRow.targetAccountId?.let { AccountId(it) },
            onSelect = { onIntent(SuggestionsIntent.SetAccount(accountPickerRow.id, it.value)) },
            onDismiss = { onIntent(SuggestionsIntent.ShowAccountPicker(null)) },
        )
    }

    if (state.showBatchCategoryPicker) {
        MmCategoryPickerSheet(
            categories = state.categories.filter {
                when (state.filter.type) {
                    SpendingFilter.All -> true
                    SpendingFilter.Expenses -> it.type == TransactionType.EXPENSE
                    SpendingFilter.Income -> it.type == TransactionType.INCOME
                }
            },
            selectedId = null,
            onPick = { onIntent(SuggestionsIntent.SetCategoryForSelected(it.value)) },
            onDismiss = { onIntent(SuggestionsIntent.ShowBatchCategoryPicker(false)) },
        )
    }

    if (state.showBatchAccountPicker) {
        MmWalletPickerSheet(
            accounts = state.accounts,
            selectedAccountId = null,
            onSelect = { onIntent(SuggestionsIntent.SetAccountForSelected(it.value)) },
            onDismiss = { onIntent(SuggestionsIntent.ShowBatchAccountPicker(false)) },
        )
    }

    if (state.showFilterSheet) {
        FilterSheet(filter = state.filter, onIntent = onIntent)
    }

    if (state.showAcceptConfirm) {
        MmDialog(
            title = stringResource(Res.string.suggestions_accept_all_confirm_title),
            confirmText = stringResource(Res.string.suggestions_accept),
            onConfirm = { onIntent(SuggestionsIntent.ConfirmAcceptSelected) },
            onDismiss = { onIntent(SuggestionsIntent.DismissConfirm) },
            dismissText = stringResource(Res.string.bank_sync_cancel),
        ) {
            Text(
                text = stringResource(
                    Res.string.suggestions_accept_all_confirm_body,
                    state.selectedIds.size
                ),
                style = MM.type.body,
                color = MM.colors.text2,
            )
        }
    }

    if (state.showRejectConfirm) {
        MmDialog(
            title = stringResource(Res.string.suggestions_reject_all_confirm_title),
            confirmText = stringResource(Res.string.suggestions_reject),
            onConfirm = { onIntent(SuggestionsIntent.ConfirmRejectSelected) },
            onDismiss = { onIntent(SuggestionsIntent.DismissConfirm) },
            dismissText = stringResource(Res.string.bank_sync_cancel),
        ) {
            Text(
                text = stringResource(
                    Res.string.suggestions_reject_all_confirm_body,
                    state.selectedIds.size
                ),
                style = MM.type.body,
                color = MM.colors.text2,
            )
        }
    }

    state.acceptConfirmId?.let {
        MmDialog(
            title = stringResource(Res.string.suggestions_accept_one_confirm_title),
            confirmText = stringResource(Res.string.suggestions_accept),
            onConfirm = { onIntent(SuggestionsIntent.ConfirmAccept) },
            onDismiss = { onIntent(SuggestionsIntent.DismissConfirm) },
            dismissText = stringResource(Res.string.bank_sync_cancel),
        ) {
            Text(
                text = stringResource(Res.string.suggestions_accept_one_confirm_body),
                style = MM.type.body,
                color = MM.colors.text2,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilterSheet(
    filter: SuggestionFilter,
    onIntent: (SuggestionsIntent) -> Unit,
) {
    val colors = MM.colors
    val space = MM.dimen
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = { onIntent(SuggestionsIntent.ShowFilterSheet(false)) },
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = space.padding_2_5x, topEnd = space.padding_2_5x),
        containerColor = colors.bg,
        dragHandle = null,
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(horizontal = space.padding_2_5x, vertical = space.padding_3x),
            verticalArrangement = Arrangement.spacedBy(space.padding_2x),
        ) {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Box(
                    modifier = Modifier
                        .size(width = 36.dp, height = space.padding_0_5x)
                        .clip(RoundedCornerShape(50))
                        .background(colors.borderStrong),
                )
            }

            MmSheetHeader(
                title = stringResource(Res.string.suggestions_filter_title),
                onClose = { onIntent(SuggestionsIntent.ShowFilterSheet(false)) },
            )

            val typeOptions =
                listOf(SpendingFilter.All, SpendingFilter.Expenses, SpendingFilter.Income)
            MmSegmented(
                options = listOf(
                    stringResource(Res.string.suggestions_filter_all),
                    stringResource(Res.string.suggestions_filter_expense),
                    stringResource(Res.string.suggestions_filter_income),
                ),
                selectedIndex = typeOptions.indexOf(filter.type),
                onOptionSelected = { onIntent(SuggestionsIntent.SetFilterType(typeOptions[it])) },
                fillWidth = true,
                modifier = Modifier.fillMaxWidth(),
            )

            MmField(
                value = filter.minText,
                onValueChange = { onIntent(SuggestionsIntent.SetFilterMin(it)) },
                label = stringResource(Res.string.suggestions_filter_min),
                keyboardType = KeyboardType.Decimal,
            )
            MmField(
                value = filter.maxText,
                onValueChange = { onIntent(SuggestionsIntent.SetFilterMax(it)) },
                label = stringResource(Res.string.suggestions_filter_max),
                keyboardType = KeyboardType.Decimal,
            )
            MmField(
                value = filter.note,
                onValueChange = { onIntent(SuggestionsIntent.SetFilterNote(it)) },
                label = stringResource(Res.string.suggestions_filter_note),
                keyboardType = KeyboardType.Text,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(space.padding_1x),
            ) {
                MmButton(
                    text = stringResource(Res.string.suggestions_filter_clear),
                    onClick = { onIntent(SuggestionsIntent.ClearFilter) },
                    variant = MmButtonVariant.Ghost,
                    modifier = Modifier.weight(1f),
                )
                MmButton(
                    text = stringResource(Res.string.suggestions_filter_apply),
                    onClick = { onIntent(SuggestionsIntent.ShowFilterSheet(false)) },
                    variant = MmButtonVariant.Primary,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun SuggestionCard(
    row: SuggestionRow,
    state: SuggestionsUiState,
    onIntent: (SuggestionsIntent) -> Unit,
) {
    val colors = MM.colors
    val space = MM.dimen
    val isPendingTab = state.tab == SuggestionsTab.PENDING

    MmCard(modifier = Modifier.fillMaxWidth(), padded = true) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = if (isPendingTab) {
                Modifier.mmClickable(shape = RoundedCornerShape(MM.dimen.padding_1x)) {
                    onIntent(SuggestionsIntent.ToggleSelect(row.id))
                }
            } else {
                Modifier
            }) {
            if (isPendingTab) {
                MmCheckbox(
                    checked = row.id in state.selectedIds,
                    onCheckedChange = { onIntent(SuggestionsIntent.ToggleSelect(row.id)) },
                )
                Spacer(Modifier.width(MM.dimen.padding_2x))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = row.description ?: row.counterparty ?: row.sourceLabel,
                    style = MM.type.body,
                    color = colors.text,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = listOfNotNull(
                        row.dateIso,
                        row.counterparty,
                        row.sourceLabel.takeIf { it.isNotBlank() })
                        .distinct()
                        .joinToString(" · "),
                    style = MM.type.caption.copy(color = colors.text2),
                )
            }
            MmMoney(
                value = (if (row.amountMinor < 0) -row.amountMinor else row.amountMinor) / 100.0,
                sign = if (row.isExpense) "-" else "+",
                currency = row.currency,
                color = if (row.isExpense) colors.text else colors.accent,
            )
        }

        row.duplicate?.let { duplicate ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = space.padding_1x)
                    .background(colors.bg)
                    .padding(space.padding_1x),
            ) {
                Text(
                    text = stringResource(Res.string.suggestions_duplicate_hint),
                    style = MM.type.micro,
                    color = colors.warning,
                )
                Text(
                    text = listOfNotNull(
                        duplicate.dateIso,
                        duplicate.note,
                        duplicate.categoryName,
                        formatMinor(duplicate.amountMinor, duplicate.currency),
                    ).joinToString(" · "),
                    style = MM.type.caption.copy(color = colors.text2),
                )
            }
        }

        if (isPendingTab) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .mmClickable(shape = RoundedCornerShape(MM.dimen.padding_1x)) { onIntent(SuggestionsIntent.ShowCategoryPicker(row.id)) }
                    .padding(vertical = space.padding_1_5x, horizontal = space.padding_0_5x),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(Res.string.suggestions_category_label) + ": " + (row.categoryName
                        ?: "—"),
                    style = MM.type.caption.copy(color = colors.text2),
                    modifier = Modifier.weight(1f),
                )
                Icon(
                    imageVector = Icon.Edit.imageVector,
                    contentDescription = null,
                    tint = colors.text3,
                    modifier = Modifier.size(MM.dimen.padding_2x),
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .mmClickable(shape = RoundedCornerShape(MM.dimen.padding_1x)) { onIntent(SuggestionsIntent.ShowAccountPicker(row.id)) }
                    .padding(vertical = space.padding_1_5x, horizontal = space.padding_0_5x),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(Res.string.bank_sync_account_target_label) + ": " +
                            (row.targetAccountName
                                ?: stringResource(Res.string.bank_sync_account_target_none)),
                    style = MM.type.caption.copy(
                        color = if (row.targetAccountId == null) colors.danger else colors.text2,
                    ),
                    modifier = Modifier.weight(1f),
                )
                Icon(
                    imageVector = Icon.Edit.imageVector,
                    contentDescription = null,
                    tint = if (row.targetAccountId == null) colors.danger else colors.text3,
                    modifier = Modifier.size(MM.dimen.padding_2x),
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = space.padding_1x),
                horizontalArrangement = Arrangement.spacedBy(space.padding_1x),
            ) {
                MmButton(
                    text = stringResource(Res.string.suggestions_accept),
                    onClick = { onIntent(SuggestionsIntent.RequestAccept(row.id)) },
                    variant = MmButtonVariant.Accent,
                    size = MmButtonSize.Sm,
                    enabled = row.targetAccountId != null && row.categoryId != null,
                )
                MmButton(
                    text = stringResource(Res.string.suggestions_reject),
                    onClick = { onIntent(SuggestionsIntent.Reject(row.id)) },
                    variant = MmButtonVariant.Danger,
                    size = MmButtonSize.Sm,
                )
            }
        } else {
            MmButton(
                text = stringResource(Res.string.suggestions_restore),
                onClick = { onIntent(SuggestionsIntent.RestoreToPending(row.id)) },
                variant = MmButtonVariant.Outline,
                size = MmButtonSize.Sm,
                modifier = Modifier.padding(top = space.padding_1x),
            )
        }
    }
}

private fun formatMinor(amountMinor: Long, currency: String): String {
    val abs = if (amountMinor < 0) -amountMinor else amountMinor
    val major = abs / 100
    val minor = (abs % 100).toString().padStart(2, '0')
    return "$major.$minor $currency"
}
