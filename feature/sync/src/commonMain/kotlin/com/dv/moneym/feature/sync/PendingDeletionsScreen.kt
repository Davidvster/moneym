package com.dv.moneym.feature.sync

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.dv.moneym.core.designsystem.MM
import com.dv.moneym.core.designsystem.MoneyMTheme
import com.dv.moneym.core.ui.MmButton
import com.dv.moneym.core.ui.MmButtonVariant
import com.dv.moneym.core.ui.MmCard
import com.dv.moneym.core.ui.MmCheckbox
import com.dv.moneym.core.ui.ScreenHeader
import com.dv.moneym.data.sync.SyncEntityType
import kotlinx.serialization.Serializable
import moneym.feature.sync.generated.resources.Res
import moneym.feature.sync.generated.resources.sync_deletions_cancel
import moneym.feature.sync.generated.resources.sync_deletions_confirm
import moneym.feature.sync.generated.resources.sync_deletions_group_budgets
import moneym.feature.sync.generated.resources.sync_deletions_group_categories
import moneym.feature.sync.generated.resources.sync_deletions_group_payment_modes
import moneym.feature.sync.generated.resources.sync_deletions_group_recurring
import moneym.feature.sync.generated.resources.sync_deletions_group_transactions
import moneym.feature.sync.generated.resources.sync_deletions_group_wallets
import moneym.feature.sync.generated.resources.sync_deletions_subtitle
import moneym.feature.sync.generated.resources.sync_deletions_title
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Serializable
data object PendingDeletionsKey : NavKey

fun EntryProviderScope<NavKey>.pendingDeletionsEntry(
    onDone: () -> Unit,
    metadata: Map<String, Any> = emptyMap(),
) = entry<PendingDeletionsKey>(metadata = metadata) {
    PendingDeletionsScreen(onDone = onDone)
}

@Composable
fun PendingDeletionsScreen(
    onDone: () -> Unit,
    viewModel: PendingDeletionsViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                PendingDeletionsEffect.Done -> onDone()
            }
        }
    }

    PendingDeletionsContent(state = state, onIntent = viewModel::onIntent)
}

@Composable
private fun PendingDeletionsContent(
    state: PendingDeletionsUiState,
    onIntent: (PendingDeletionsIntent) -> Unit,
) {
    val colors = MM.colors

    Column(modifier = Modifier.fillMaxSize().background(colors.bg)) {
        ScreenHeader(title = stringResource(Res.string.sync_deletions_title))

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(
                horizontal = MM.dimen.padding_2_5x,
                vertical = MM.dimen.padding_2x,
            ),
            verticalArrangement = Arrangement.spacedBy(MM.dimen.padding_2x),
        ) {
            item {
                Text(
                    text = stringResource(Res.string.sync_deletions_subtitle),
                    style = MM.type.body,
                    color = colors.text2,
                )
            }

            items(state.groups, key = { it.type }) { group ->
                DeletionGroupCard(group = group, onIntent = onIntent)
            }
        }

        Column(
            modifier = Modifier
                .padding(
                    horizontal = MM.dimen.padding_2_5x,
                    vertical = MM.dimen.padding_2x,
                )
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(MM.dimen.padding_1x),
        ) {
            MmButton(
                text = stringResource(Res.string.sync_deletions_confirm),
                onClick = { onIntent(PendingDeletionsIntent.ConfirmSelected) },
                variant = MmButtonVariant.Danger,
                fullWidth = true,
                enabled = !state.isResolving,
            )
            MmButton(
                text = stringResource(Res.string.sync_deletions_cancel),
                onClick = { onIntent(PendingDeletionsIntent.Cancel) },
                variant = MmButtonVariant.Outline,
                fullWidth = true,
                enabled = !state.isResolving,
            )
        }
    }
}

@Composable
private fun DeletionGroupCard(
    group: DeletionGroup,
    onIntent: (PendingDeletionsIntent) -> Unit,
) {
    val colors = MM.colors

    MmCard(modifier = Modifier.fillMaxWidth(), padded = true) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onIntent(PendingDeletionsIntent.ToggleGroup(group.type)) },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MmCheckbox(
                checked = group.allChecked,
                onCheckedChange = { onIntent(PendingDeletionsIntent.ToggleGroup(group.type)) },
            )
            Text(
                text = stringResource(group.type.titleRes()),
                style = MM.type.title3,
                color = colors.text,
            )
        }

        Column(modifier = Modifier.padding(top = MM.dimen.padding_0_5x)) {
            group.items.forEach { item ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onIntent(PendingDeletionsIntent.ToggleItem(item.syncId)) }
                        .padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    MmCheckbox(
                        checked = item.checked,
                        onCheckedChange = { onIntent(PendingDeletionsIntent.ToggleItem(item.syncId)) },
                    )
                    Text(
                        text = item.label,
                        style = MM.type.body,
                        color = colors.text2,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Preview
@Composable
private fun PendingDeletionsContentPreview() {
    MoneyMTheme {
        PendingDeletionsContent(
            state = PendingDeletionsUiState(
                groups = listOf(
                    DeletionGroup(
                        type = SyncEntityType.TRANSACTION,
                        items = listOf(
                            DeletionItem("t1", "Coffee · 3.50 EUR", checked = true),
                            DeletionItem("t2", "Groceries · 42.10 EUR", checked = false),
                        ),
                    ),
                    DeletionGroup(
                        type = SyncEntityType.CATEGORY,
                        items = listOf(
                            DeletionItem("c1", "Subscriptions", checked = true),
                        ),
                    ),
                ),
                selectedCount = 2,
            ),
            onIntent = {},
        )
    }
}

private fun SyncEntityType.titleRes(): StringResource = when (this) {
    SyncEntityType.ACCOUNT -> Res.string.sync_deletions_group_wallets
    SyncEntityType.TRANSACTION -> Res.string.sync_deletions_group_transactions
    SyncEntityType.BUDGET -> Res.string.sync_deletions_group_budgets
    SyncEntityType.RECURRING -> Res.string.sync_deletions_group_recurring
    SyncEntityType.CATEGORY -> Res.string.sync_deletions_group_categories
    SyncEntityType.PAYMENT_MODE -> Res.string.sync_deletions_group_payment_modes
}
