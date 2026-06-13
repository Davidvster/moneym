package com.dv.moneym.feature.banksync.home

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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.dv.moneym.core.designsystem.MM
import com.dv.moneym.core.model.Icon
import com.dv.moneym.core.ui.MmButton
import com.dv.moneym.core.ui.MmButtonSize
import com.dv.moneym.core.ui.MmButtonVariant
import com.dv.moneym.core.ui.MmCard
import com.dv.moneym.core.ui.MmIconButton
import com.dv.moneym.core.ui.MmLoadingOverlay
import com.dv.moneym.core.ui.MmToggle
import com.dv.moneym.core.ui.ScreenHeader
import com.dv.moneym.core.ui.imageVector
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.Serializable
import moneym.feature.banksync.generated.resources.Res
import moneym.feature.banksync.generated.resources.bank_sync_account_target_label
import moneym.feature.banksync.generated.resources.bank_sync_account_target_none
import moneym.feature.banksync.generated.resources.bank_sync_accounts_header
import moneym.feature.banksync.generated.resources.bank_sync_auto_subtitle
import moneym.feature.banksync.generated.resources.bank_sync_auto_title
import moneym.feature.banksync.generated.resources.bank_sync_connect_bank
import moneym.feature.banksync.generated.resources.bank_sync_disconnect
import moneym.feature.banksync.generated.resources.bank_sync_edit_credentials
import moneym.feature.banksync.generated.resources.bank_sync_error_generic
import moneym.feature.banksync.generated.resources.bank_sync_info
import moneym.feature.banksync.generated.resources.bank_sync_last_sync_never
import moneym.feature.banksync.generated.resources.bank_sync_reconnect_required
import moneym.feature.banksync.generated.resources.bank_sync_review_pending
import moneym.feature.banksync.generated.resources.bank_sync_session_valid_until
import moneym.feature.banksync.generated.resources.bank_sync_setup_cta
import moneym.feature.banksync.generated.resources.bank_sync_setup_header
import moneym.feature.banksync.generated.resources.bank_sync_setup_hint
import moneym.feature.banksync.generated.resources.bank_sync_status_connected
import moneym.feature.banksync.generated.resources.bank_sync_status_not_connected
import moneym.feature.banksync.generated.resources.bank_sync_sync_now
import moneym.feature.banksync.generated.resources.bank_sync_syncing
import moneym.feature.banksync.generated.resources.bank_sync_title
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Serializable
data object BankSyncSettingsKey : NavKey

fun EntryProviderScope<NavKey>.bankSyncSettingsEntry(
    onBack: () -> Unit,
    onOpenSuggestions: () -> Unit,
    onNavigateToCredentials: () -> Unit,
    onNavigateToBankPicker: () -> Unit,
    onNavigateToInfo: () -> Unit = {},
    metadata: Map<String, Any> = emptyMap(),
) = entry<BankSyncSettingsKey>(metadata = metadata) {
    BankSyncHomeScreen(
        onBack = onBack,
        onOpenSuggestions = onOpenSuggestions,
        onNavigateToCredentials = onNavigateToCredentials,
        onNavigateToBankPicker = onNavigateToBankPicker,
        onNavigateToInfo = onNavigateToInfo,
    )
}

@Composable
fun BankSyncHomeScreen(
    onBack: () -> Unit,
    onOpenSuggestions: () -> Unit,
    onNavigateToCredentials: () -> Unit,
    onNavigateToBankPicker: () -> Unit,
    onNavigateToInfo: () -> Unit = {},
    viewModel: BankSyncHomeViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    BankSyncHomeContent(
        state = state,
        onBack = onBack,
        onOpenSuggestions = onOpenSuggestions,
        onNavigateToCredentials = onNavigateToCredentials,
        onNavigateToBankPicker = onNavigateToBankPicker,
        onNavigateToInfo = onNavigateToInfo,
        onIntent = viewModel::onIntent,
    )
}

@Composable
private fun BankSyncHomeContent(
    state: BankSyncHomeUiState,
    onBack: () -> Unit,
    onOpenSuggestions: () -> Unit,
    onNavigateToCredentials: () -> Unit,
    onNavigateToBankPicker: () -> Unit,
    onNavigateToInfo: () -> Unit,
    onIntent: (BankSyncHomeIntent) -> Unit,
) {
    val colors = MM.colors
    val space = MM.dimen

    Box(modifier = Modifier.fillMaxSize().background(colors.bg)) {
        Column(modifier = Modifier.fillMaxSize()) {
            ScreenHeader(
                title = stringResource(Res.string.bank_sync_title),
                onBack = onBack,
                trailingContent = {
                    MmIconButton(
                        icon = Icon.Info.imageVector,
                        onClick = onNavigateToInfo,
                        contentDescription = stringResource(Res.string.bank_sync_info),
                    )
                },
            )

            when {
                state.isLoading -> Unit

                !state.configured -> IntroState(onNavigateToCredentials = onNavigateToCredentials)

                else -> LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(
                        horizontal = space.padding_2x,
                        vertical = space.padding_2x
                    ),
                    verticalArrangement = Arrangement.spacedBy(space.padding_2x),
                ) {
                    item {
                        StatusCard(
                            state = state,
                            onOpenSuggestions = onOpenSuggestions,
                        )
                    }
                    if (!state.connected) {
                        item {
                            NotConnectedActions(
                                onNavigateToBankPicker = onNavigateToBankPicker,
                                onNavigateToCredentials = onNavigateToCredentials,
                            )
                        }
                    } else {
                        if (state.accounts.isNotEmpty()) {
                            item {
                                Text(
                                    text = stringResource(Res.string.bank_sync_accounts_header),
                                    style = MM.type.micro,
                                    color = colors.text2,
                                    modifier = Modifier.padding(top = space.padding_1x),
                                )
                            }
                            items(state.accounts, key = { it.uid }) { account ->
                                AccountCard(account = account, state = state, onIntent = onIntent)
                            }
                        }
                        item {
                            ControlsCard(
                                state = state,
                                onNavigateToBankPicker = onNavigateToBankPicker,
                                onIntent = onIntent,
                            )
                        }
                    }
                }
            }
        }
        MmLoadingOverlay(visible = state.isLoading || state.isSyncing)
    }
}

@Composable
private fun IntroState(onNavigateToCredentials: () -> Unit) {
    val colors = MM.colors
    val space = MM.dimen
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = space.padding_3x),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(Res.string.bank_sync_setup_header),
            style = MM.type.body,
            color = colors.text,
            textAlign = TextAlign.Center,
        )
        Text(
            text = stringResource(Res.string.bank_sync_setup_hint),
            style = MM.type.caption.copy(color = colors.text2),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = space.padding_1x),
        )
        MmButton(
            text = stringResource(Res.string.bank_sync_setup_cta),
            onClick = onNavigateToCredentials,
            modifier = Modifier.padding(top = space.padding_3x),
        )
    }
}

@Composable
private fun NotConnectedActions(
    onNavigateToBankPicker: () -> Unit,
    onNavigateToCredentials: () -> Unit,
) {
    val space = MM.dimen
    Column(verticalArrangement = Arrangement.spacedBy(space.padding_1x)) {
        MmButton(
            text = stringResource(Res.string.bank_sync_connect_bank),
            onClick = onNavigateToBankPicker,
            fullWidth = true,
        )
        MmButton(
            text = stringResource(Res.string.bank_sync_edit_credentials),
            onClick = onNavigateToCredentials,
            variant = MmButtonVariant.Outline,
            fullWidth = true,
        )
    }
}

@Composable
private fun StatusCard(
    state: BankSyncHomeUiState,
    onOpenSuggestions: () -> Unit,
) {
    val colors = MM.colors
    val space = MM.dimen
    MmCard(modifier = Modifier.fillMaxWidth(), padded = true) {
        Text(
            text = if (state.connected) {
                stringResource(Res.string.bank_sync_status_connected)
            } else {
                stringResource(Res.string.bank_sync_status_not_connected)
            },
            style = MM.type.body,
            color = colors.text,
        )
        state.sessionValidUntilMs?.let { ms ->
            Text(
                text = stringResource(Res.string.bank_sync_session_valid_until, formatDate(ms)),
                style = MM.type.caption.copy(color = colors.text2),
                modifier = Modifier.padding(top = space.padding_0_5x),
            )
        }
        if (state.reconnectRequired) {
            Text(
                text = stringResource(Res.string.bank_sync_reconnect_required),
                style = MM.type.caption.copy(color = colors.danger),
                modifier = Modifier.padding(top = space.padding_0_5x),
            )
        }
        if (state.pendingCount > 0) {
            MmButton(
                text = stringResource(Res.string.bank_sync_review_pending, state.pendingCount),
                onClick = onOpenSuggestions,
                size = MmButtonSize.Sm,
                modifier = Modifier.padding(top = space.padding_1x),
            )
        }
    }
}

@Composable
private fun AccountCard(
    account: BankAccountRow,
    state: BankSyncHomeUiState,
    onIntent: (BankSyncHomeIntent) -> Unit,
) {
    val colors = MM.colors
    val space = MM.dimen
    MmCard(modifier = Modifier.fillMaxWidth(), padded = true) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = account.displayName ?: account.iban ?: account.uid,
                    style = MM.type.body,
                    color = colors.text,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "${account.bankName} · ${account.currency}",
                    style = MM.type.caption.copy(color = colors.text2),
                )
            }
            MmToggle(
                checked = account.enabled,
                onCheckedChange = { onIntent(BankSyncHomeIntent.SetAccountEnabled(account.uid, it)) },
            )
        }
        val targetName = state.localAccounts.firstOrNull { it.id == account.localAccountId }?.name
        Text(
            text = stringResource(Res.string.bank_sync_account_target_label) + ": " +
                (targetName ?: stringResource(Res.string.bank_sync_account_target_none)),
            style = MM.type.caption.copy(color = if (targetName == null) colors.danger else colors.text2),
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onIntent(BankSyncHomeIntent.ShowAccountPicker(account.uid)) }
                .padding(top = space.padding_1x),
        )
        if (state.accountPickerForUid == account.uid) {
            Column(modifier = Modifier.padding(top = space.padding_0_5x)) {
                state.localAccounts.forEach { option ->
                    Text(
                        text = option.name,
                        style = MM.type.body,
                        color = colors.text,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onIntent(BankSyncHomeIntent.SetLocalAccountMapping(account.uid, option.id))
                            }
                            .padding(vertical = space.padding_0_5x),
                    )
                }
            }
        }
    }
}

@Composable
private fun ControlsCard(
    state: BankSyncHomeUiState,
    onNavigateToBankPicker: () -> Unit,
    onIntent: (BankSyncHomeIntent) -> Unit,
) {
    val colors = MM.colors
    val space = MM.dimen
    MmCard(modifier = Modifier.fillMaxWidth(), padded = true) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(Res.string.bank_sync_auto_title),
                    style = MM.type.body,
                    color = colors.text,
                )
                Text(
                    text = stringResource(Res.string.bank_sync_auto_subtitle),
                    style = MM.type.caption.copy(color = colors.text2),
                )
            }
            MmToggle(
                checked = state.autoSyncEnabled,
                onCheckedChange = { onIntent(BankSyncHomeIntent.ToggleAutoSync) },
            )
        }
        state.syncError?.let { error ->
            Text(
                text = stringResource(Res.string.bank_sync_error_generic, error),
                style = MM.type.caption.copy(color = colors.danger),
                modifier = Modifier.padding(top = space.padding_1x),
            )
        }
        if (state.lastSyncMs == 0L) {
            Text(
                text = stringResource(Res.string.bank_sync_last_sync_never),
                style = MM.type.caption.copy(color = colors.text2),
                modifier = Modifier.padding(top = space.padding_1x),
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = space.padding_1x),
            horizontalArrangement = Arrangement.spacedBy(space.padding_1x),
        ) {
            MmButton(
                text = if (state.isSyncing) {
                    stringResource(Res.string.bank_sync_syncing)
                } else {
                    stringResource(Res.string.bank_sync_sync_now)
                },
                onClick = { onIntent(BankSyncHomeIntent.SyncNow) },
                size = MmButtonSize.Sm,
                enabled = !state.isSyncing && state.connected,
            )
            MmButton(
                text = stringResource(Res.string.bank_sync_disconnect),
                onClick = { onIntent(BankSyncHomeIntent.Disconnect) },
                variant = MmButtonVariant.Outline,
                size = MmButtonSize.Sm,
            )
        }
        MmButton(
            text = stringResource(Res.string.bank_sync_connect_bank),
            onClick = onNavigateToBankPicker,
            variant = MmButtonVariant.Outline,
            size = MmButtonSize.Sm,
            modifier = Modifier.padding(top = space.padding_1x),
        )
    }
}

private fun formatDate(epochMs: Long): String =
    kotlin.time.Instant.fromEpochMilliseconds(epochMs)
        .toLocalDateTime(TimeZone.currentSystemDefault())
        .date
        .toString()
