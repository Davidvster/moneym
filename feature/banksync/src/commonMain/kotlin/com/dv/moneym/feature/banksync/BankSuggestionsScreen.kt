package com.dv.moneym.feature.banksync

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.dv.moneym.core.designsystem.MM
import com.dv.moneym.core.ui.MmButton
import com.dv.moneym.core.ui.MmButtonSize
import com.dv.moneym.core.ui.MmButtonVariant
import com.dv.moneym.core.ui.MmCard
import com.dv.moneym.core.ui.MmLoadingSpinner
import com.dv.moneym.core.ui.MmMoney
import com.dv.moneym.core.ui.MmSegmented
import com.dv.moneym.core.ui.ScreenHeader
import kotlinx.serialization.Serializable
import moneym.feature.banksync.generated.resources.Res
import moneym.feature.banksync.generated.resources.suggestions_accept
import moneym.feature.banksync.generated.resources.suggestions_accept_selected
import moneym.feature.banksync.generated.resources.suggestions_category_label
import moneym.feature.banksync.generated.resources.suggestions_clear_selection
import moneym.feature.banksync.generated.resources.suggestions_duplicate_hint
import moneym.feature.banksync.generated.resources.suggestions_empty_pending
import moneym.feature.banksync.generated.resources.suggestions_empty_rejected
import moneym.feature.banksync.generated.resources.suggestions_reject
import moneym.feature.banksync.generated.resources.suggestions_reject_selected
import moneym.feature.banksync.generated.resources.suggestions_restore
import moneym.feature.banksync.generated.resources.suggestions_select_all
import moneym.feature.banksync.generated.resources.suggestions_tab_pending
import moneym.feature.banksync.generated.resources.suggestions_tab_rejected
import moneym.feature.banksync.generated.resources.suggestions_title
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
    BankSuggestionsContent(state = state, onBack = onBack, onIntent = viewModel::onIntent)
}

@Composable
private fun BankSuggestionsContent(
    state: BankSuggestionsUiState,
    onBack: () -> Unit,
    onIntent: (BankSuggestionsIntent) -> Unit,
) {
    val colors = MM.colors
    val space = MM.dimen

    Column(modifier = Modifier.fillMaxSize().background(colors.bg)) {
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
            SelectionBar(state = state, onIntent = onIntent)
        }

        when {
            state.isLoading -> Box(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) { MmLoadingSpinner() }

            state.rows.isEmpty() -> Box(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = if (state.tab == SuggestionsTab.PENDING) {
                        stringResource(Res.string.suggestions_empty_pending)
                    } else {
                        stringResource(Res.string.suggestions_empty_rejected)
                    },
                    style = MM.type.body,
                    color = colors.text2,
                )
            }

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
    }
}

@Composable
private fun SelectionBar(
    state: BankSuggestionsUiState,
    onIntent: (BankSuggestionsIntent) -> Unit,
) {
    val space = MM.dimen
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = space.padding_2x),
        horizontalArrangement = Arrangement.spacedBy(space.padding_1x),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (state.selectedIds.isEmpty()) {
            MmButton(
                text = stringResource(Res.string.suggestions_select_all),
                onClick = { onIntent(BankSuggestionsIntent.SelectAll) },
                variant = MmButtonVariant.Outline,
                size = MmButtonSize.Sm,
            )
        } else {
            MmButton(
                text = stringResource(Res.string.suggestions_accept_selected, state.selectedIds.size),
                onClick = { onIntent(BankSuggestionsIntent.AcceptSelected) },
                size = MmButtonSize.Sm,
            )
            MmButton(
                text = stringResource(Res.string.suggestions_reject_selected, state.selectedIds.size),
                onClick = { onIntent(BankSuggestionsIntent.RejectSelected) },
                variant = MmButtonVariant.Outline,
                size = MmButtonSize.Sm,
            )
            MmButton(
                text = stringResource(Res.string.suggestions_clear_selection),
                onClick = { onIntent(BankSuggestionsIntent.ClearSelection) },
                variant = MmButtonVariant.Outline,
                size = MmButtonSize.Sm,
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
                Checkbox(
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
                    color = colors.danger,
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
            if (state.categoryPickerForId == row.id) {
                Column(modifier = Modifier.padding(top = space.padding_0_5x)) {
                    state.categories.filter { it.isExpense == row.isExpense }.forEach { option ->
                        Text(
                            text = option.name,
                            style = MM.type.body,
                            color = colors.text,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onIntent(BankSuggestionsIntent.SetCategory(row.id, option.id)) }
                                .padding(vertical = space.padding_0_5x),
                        )
                    }
                }
            }
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
                    size = MmButtonSize.Sm,
                    enabled = row.targetAccountId != null && row.categoryId != null,
                )
                MmButton(
                    text = stringResource(Res.string.suggestions_reject),
                    onClick = { onIntent(BankSuggestionsIntent.Reject(row.id)) },
                    variant = MmButtonVariant.Outline,
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
