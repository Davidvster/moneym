package com.dv.moneym.feature.banksync.suggestions

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import androidx.compose.material3.SnackbarHostState
import com.dv.moneym.core.designsystem.MM
import com.dv.moneym.core.model.CategoryId
import com.dv.moneym.core.model.TransactionType
import com.dv.moneym.core.ui.MmButton
import com.dv.moneym.core.ui.MmButtonSize
import com.dv.moneym.core.ui.MmButtonVariant
import com.dv.moneym.core.ui.MmCard
import com.dv.moneym.core.ui.MmCategoryPickerSheet
import com.dv.moneym.core.ui.MmCheckbox
import com.dv.moneym.core.ui.MmDialog
import com.dv.moneym.core.ui.MmEmptyState
import com.dv.moneym.core.ui.MmLoadingOverlay
import com.dv.moneym.core.ui.MmMoney
import com.dv.moneym.core.ui.MmSegmented
import com.dv.moneym.core.ui.ScreenHeader
import com.dv.moneym.core.utils.observeWithLifecycle
import kotlinx.serialization.Serializable
import org.jetbrains.compose.resources.getString
import moneym.feature.banksync.generated.resources.Res
import moneym.feature.banksync.generated.resources.bank_sync_cancel
import moneym.feature.banksync.generated.resources.suggestions_accept
import moneym.feature.banksync.generated.resources.suggestions_accept_all_confirm_body
import moneym.feature.banksync.generated.resources.suggestions_accept_all_confirm_title
import moneym.feature.banksync.generated.resources.suggestions_accept_selected
import moneym.feature.banksync.generated.resources.suggestions_category_label
import moneym.feature.banksync.generated.resources.suggestions_duplicate_hint
import moneym.feature.banksync.generated.resources.suggestions_empty_pending
import moneym.feature.banksync.generated.resources.suggestions_empty_rejected
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
import moneym.feature.banksync.generated.resources.suggestions_wallet_missing
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Serializable
data object BankSuggestionsKey : NavKey

fun EntryProviderScope<NavKey>.bankSuggestionsEntry(
    onBack: () -> Unit,
    metadata: Map<String, Any> = emptyMap(),
) = entry<BankSuggestionsKey>(metadata = metadata) {
    BankSuggestionsScreen(onBack = onBack)
}

@Composable
fun BankSuggestionsScreen(
    onBack: () -> Unit,
    viewModel: BankSuggestionsViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    viewModel.singleEvents.observeWithLifecycle { event ->
        when (event) {
            is BankSuggestionsViewModel.BankSuggestionsSingleUiEvent.RejectedWithUndo -> {
                val result = snackbarHostState.showSnackbar(
                    message = getString(Res.string.suggestions_rejected_snackbar),
                    actionLabel = getString(Res.string.suggestions_undo),
                    duration = SnackbarDuration.Short,
                )
                if (result == SnackbarResult.ActionPerformed) {
                    viewModel.onIntent(BankSuggestionsIntent.UndoReject(event.id))
                }
            }
        }
    }

    BankSuggestionsContent(
        state = state,
        snackbarHostState = snackbarHostState,
        onBack = onBack,
        onIntent = viewModel::onIntent,
    )
}

@Composable
private fun BankSuggestionsContent(
    state: BankSuggestionsUiState,
    snackbarHostState: SnackbarHostState,
    onBack: () -> Unit,
    onIntent: (BankSuggestionsIntent) -> Unit,
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
                        BankSuggestionsIntent.SetTab(
                            if (index == 0) SuggestionsTab.PENDING else SuggestionsTab.REJECTED
                        )
                    )
                },
                fillWidth = true,
                modifier = Modifier.padding(horizontal = space.padding_2x, vertical = space.padding_1x),
            )

            if (state.tab == SuggestionsTab.PENDING && state.pending.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = space.padding_2x),
                    horizontalArrangement = Arrangement.End,
                ) {
                    MmButton(
                        text = if (state.allSelected) {
                            stringResource(Res.string.suggestions_unselect_all)
                        } else {
                            stringResource(Res.string.suggestions_select_all)
                        },
                        onClick = { onIntent(BankSuggestionsIntent.ToggleSelectAll) },
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
                    contentPadding = PaddingValues(horizontal = space.padding_2x, vertical = space.padding_1x),
                    verticalArrangement = Arrangement.spacedBy(space.padding_2x),
                ) {
                    items(state.rows, key = { it.id }) { row ->
                        SuggestionCard(row = row, state = state, onIntent = onIntent)
                    }
                }
            }

            if (state.tab == SuggestionsTab.PENDING && state.selectedIds.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = space.padding_2x, vertical = space.padding_1x)
                        .navigationBarsPadding(),
                    horizontalArrangement = Arrangement.spacedBy(space.padding_1x),
                ) {
                    MmButton(
                        text = stringResource(Res.string.suggestions_accept_selected, state.selectedIds.size),
                        onClick = { onIntent(BankSuggestionsIntent.RequestAcceptSelected) },
                        variant = MmButtonVariant.Accent,
                        modifier = Modifier.weight(1f),
                    )
                    MmButton(
                        text = stringResource(Res.string.suggestions_reject_selected, state.selectedIds.size),
                        onClick = { onIntent(BankSuggestionsIntent.RequestRejectSelected) },
                        variant = MmButtonVariant.Danger,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter).padding(space.padding_2x),
        )

        MmLoadingOverlay(visible = state.isLoading || state.isProcessing)
    }

    val pickerRow = state.pending.firstOrNull { it.id == state.categoryPickerForId }
    if (pickerRow != null) {
        MmCategoryPickerSheet(
            categories = state.categories.filter {
                (it.type == TransactionType.EXPENSE) == pickerRow.isExpense
            },
            selectedId = pickerRow.categoryId?.let { CategoryId(it) },
            onPick = { onIntent(BankSuggestionsIntent.SetCategory(pickerRow.id, it.value)) },
            onDismiss = { onIntent(BankSuggestionsIntent.ShowCategoryPicker(null)) },
        )
    }

    if (state.showAcceptConfirm) {
        MmDialog(
            title = stringResource(Res.string.suggestions_accept_all_confirm_title),
            confirmText = stringResource(Res.string.suggestions_accept),
            onConfirm = { onIntent(BankSuggestionsIntent.ConfirmAcceptSelected) },
            onDismiss = { onIntent(BankSuggestionsIntent.DismissConfirm) },
            dismissText = stringResource(Res.string.bank_sync_cancel),
        ) {
            Text(
                text = stringResource(Res.string.suggestions_accept_all_confirm_body, state.selectedIds.size),
                style = MM.type.body,
                color = MM.colors.text2,
            )
        }
    }

    if (state.showRejectConfirm) {
        MmDialog(
            title = stringResource(Res.string.suggestions_reject_all_confirm_title),
            confirmText = stringResource(Res.string.suggestions_reject),
            onConfirm = { onIntent(BankSuggestionsIntent.ConfirmRejectSelected) },
            onDismiss = { onIntent(BankSuggestionsIntent.DismissConfirm) },
            dismissText = stringResource(Res.string.bank_sync_cancel),
        ) {
            Text(
                text = stringResource(Res.string.suggestions_reject_all_confirm_body, state.selectedIds.size),
                style = MM.type.body,
                color = MM.colors.text2,
            )
        }
    }
}

@Composable
private fun SuggestionCard(
    row: SuggestionRow,
    state: BankSuggestionsUiState,
    onIntent: (BankSuggestionsIntent) -> Unit,
) {
    val colors = MM.colors
    val space = MM.dimen
    val isPendingTab = state.tab == SuggestionsTab.PENDING

    MmCard(modifier = Modifier.fillMaxWidth(), padded = true) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (isPendingTab) {
                MmCheckbox(
                    checked = row.id in state.selectedIds,
                    onCheckedChange = { onIntent(BankSuggestionsIntent.ToggleSelect(row.id)) },
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = row.description ?: row.counterparty ?: row.bankName,
                    style = MM.type.body,
                    color = colors.text,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = listOfNotNull(row.dateIso, row.counterparty, row.bankName.takeIf { it.isNotBlank() })
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
            Text(
                text = stringResource(Res.string.suggestions_category_label) + ": " + (row.categoryName ?: "—"),
                style = MM.type.caption.copy(color = colors.text2),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onIntent(BankSuggestionsIntent.ShowCategoryPicker(row.id)) }
                    .padding(top = space.padding_1x),
            )
            if (row.targetAccountId == null) {
                Text(
                    text = stringResource(Res.string.suggestions_wallet_missing),
                    style = MM.type.caption.copy(color = colors.danger),
                    modifier = Modifier.padding(top = space.padding_0_5x),
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = space.padding_1x),
                horizontalArrangement = Arrangement.spacedBy(space.padding_1x),
            ) {
                MmButton(
                    text = stringResource(Res.string.suggestions_accept),
                    onClick = { onIntent(BankSuggestionsIntent.Accept(row.id)) },
                    variant = MmButtonVariant.Accent,
                    size = MmButtonSize.Sm,
                    enabled = row.targetAccountId != null && row.categoryId != null,
                )
                MmButton(
                    text = stringResource(Res.string.suggestions_reject),
                    onClick = { onIntent(BankSuggestionsIntent.Reject(row.id)) },
                    variant = MmButtonVariant.Danger,
                    size = MmButtonSize.Sm,
                )
            }
        } else {
            MmButton(
                text = stringResource(Res.string.suggestions_restore),
                onClick = { onIntent(BankSuggestionsIntent.RestoreToPending(row.id)) },
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
