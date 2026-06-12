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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalUriHandler
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
import com.dv.moneym.core.ui.MmField
import com.dv.moneym.core.ui.MmIconButton
import com.dv.moneym.core.ui.MmLoadingSpinner
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
import moneym.feature.banksync.generated.resources.bank_sync_app_id_label
import moneym.feature.banksync.generated.resources.bank_sync_auto_subtitle
import moneym.feature.banksync.generated.resources.bank_sync_auto_title
import moneym.feature.banksync.generated.resources.bank_sync_awaiting_auth
import moneym.feature.banksync.generated.resources.bank_sync_cancel
import moneym.feature.banksync.generated.resources.bank_sync_connect_bank
import moneym.feature.banksync.generated.resources.bank_sync_country_label
import moneym.feature.banksync.generated.resources.bank_sync_disconnect
import moneym.feature.banksync.generated.resources.bank_sync_error_generic
import moneym.feature.banksync.generated.resources.bank_sync_info
import moneym.feature.banksync.generated.resources.bank_sync_last_sync_never
import moneym.feature.banksync.generated.resources.bank_sync_load_banks
import moneym.feature.banksync.generated.resources.bank_sync_private_key_label
import moneym.feature.banksync.generated.resources.bank_sync_reconnect_required
import moneym.feature.banksync.generated.resources.bank_sync_redirect_invalid
import moneym.feature.banksync.generated.resources.bank_sync_redirect_label
import moneym.feature.banksync.generated.resources.bank_sync_review_pending
import moneym.feature.banksync.generated.resources.bank_sync_save_credentials
import moneym.feature.banksync.generated.resources.bank_sync_search_banks
import moneym.feature.banksync.generated.resources.bank_sync_session_valid_until
import moneym.feature.banksync.generated.resources.bank_sync_setup_header
import moneym.feature.banksync.generated.resources.bank_sync_setup_hint
import moneym.feature.banksync.generated.resources.bank_sync_status_connected
import moneym.feature.banksync.generated.resources.bank_sync_status_not_connected
import moneym.feature.banksync.generated.resources.bank_sync_submit_redirect
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
    onNavigateToInfo: () -> Unit = {},
    metadata: Map<String, Any> = emptyMap(),
) = entry<BankSyncSettingsKey>(metadata = metadata) {
    BankSyncSettingsScreen(
        onBack = onBack,
        onOpenSuggestions = onOpenSuggestions,
        onNavigateToInfo = onNavigateToInfo,
    )
}

@Composable
fun BankSyncSettingsScreen(
    onBack: () -> Unit,
    onOpenSuggestions: () -> Unit,
    onNavigateToInfo: () -> Unit = {},
    viewModel: BankSyncSettingsViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val uriHandler = LocalUriHandler.current

    LaunchedEffect(state.authUrlToOpen) {
        state.authUrlToOpen?.let { url ->
            uriHandler.openUri(url)
            viewModel.onIntent(BankSyncSettingsIntent.AuthUrlOpened)
        }
    }

    BankSyncSettingsContent(
        state = state,
        onBack = onBack,
        onOpenSuggestions = onOpenSuggestions,
        onNavigateToInfo = onNavigateToInfo,
        onIntent = viewModel::onIntent,
    )
}

@Composable
private fun BankSyncSettingsContent(
    state: BankSyncSettingsUiState,
    onBack: () -> Unit,
    onOpenSuggestions: () -> Unit,
    onNavigateToInfo: () -> Unit,
    onIntent: (BankSyncSettingsIntent) -> Unit,
) {
    val colors = MM.colors
    val space = MM.dimen

    Column(modifier = Modifier.fillMaxSize().background(colors.bg)) {
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

        if (state.isLoading) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                MmLoadingSpinner()
            }
            return@Column
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = space.padding_2x, vertical = space.padding_2x),
            verticalArrangement = Arrangement.spacedBy(space.padding_2x),
        ) {
            if (!state.configured) {
                item { CredentialsCard(state = state, onIntent = onIntent) }
            } else {
                item { StatusCard(state = state, onOpenSuggestions = onOpenSuggestions, onIntent = onIntent) }
                item { ConnectCard(state = state, onIntent = onIntent) }
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
                item { ControlsCard(state = state, onIntent = onIntent) }
            }
        }
    }
}

@Composable
private fun CredentialsCard(
    state: BankSyncSettingsUiState,
    onIntent: (BankSyncSettingsIntent) -> Unit,
) {
    val colors = MM.colors
    val space = MM.dimen
    MmCard(modifier = Modifier.fillMaxWidth(), padded = true) {
        Text(
            text = stringResource(Res.string.bank_sync_setup_header),
            style = MM.type.body,
            color = colors.text,
        )
        Text(
            text = stringResource(Res.string.bank_sync_setup_hint),
            style = MM.type.caption.copy(color = colors.text2),
            modifier = Modifier.padding(top = space.padding_0_5x, bottom = space.padding_1x),
        )
        MmField(
            value = state.appIdDraft,
            onValueChange = { onIntent(BankSyncSettingsIntent.AppIdChanged(it)) },
            label = stringResource(Res.string.bank_sync_app_id_label),
        )
        MmField(
            value = state.pemDraft,
            onValueChange = { onIntent(BankSyncSettingsIntent.PemChanged(it)) },
            label = stringResource(Res.string.bank_sync_private_key_label),
            singleLine = false,
            modifier = Modifier.padding(top = space.padding_1x).heightIn(min = 120.dp),
        )
        state.credentialsError?.let { error ->
            Text(
                text = stringResource(Res.string.bank_sync_error_generic, error),
                style = MM.type.caption.copy(color = colors.danger),
                modifier = Modifier.padding(top = space.padding_1x),
            )
        }
        Row(modifier = Modifier.fillMaxWidth().padding(top = space.padding_1x)) {
            MmButton(
                text = stringResource(Res.string.bank_sync_save_credentials),
                onClick = { onIntent(BankSyncSettingsIntent.SaveCredentials) },
                size = MmButtonSize.Sm,
                enabled = !state.isValidatingCredentials &&
                    state.appIdDraft.isNotBlank() && state.pemDraft.isNotBlank(),
            )
        }
    }
}

@Composable
private fun StatusCard(
    state: BankSyncSettingsUiState,
    onOpenSuggestions: () -> Unit,
    onIntent: (BankSyncSettingsIntent) -> Unit,
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
private fun ConnectCard(
    state: BankSyncSettingsUiState,
    onIntent: (BankSyncSettingsIntent) -> Unit,
) {
    val colors = MM.colors
    val space = MM.dimen
    MmCard(modifier = Modifier.fillMaxWidth(), padded = true) {
        if (state.awaitingAuth) {
            Text(
                text = stringResource(Res.string.bank_sync_awaiting_auth),
                style = MM.type.caption.copy(color = colors.text2),
            )
            MmField(
                value = state.redirectDraft,
                onValueChange = { onIntent(BankSyncSettingsIntent.RedirectChanged(it)) },
                label = stringResource(Res.string.bank_sync_redirect_label),
                modifier = Modifier.padding(top = space.padding_1x),
            )
            if (state.redirectInvalid) {
                Text(
                    text = stringResource(Res.string.bank_sync_redirect_invalid),
                    style = MM.type.caption.copy(color = colors.danger),
                    modifier = Modifier.padding(top = space.padding_0_5x),
                )
            }
            ConnectError(state)
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = space.padding_1x),
                horizontalArrangement = Arrangement.spacedBy(space.padding_1x),
            ) {
                MmButton(
                    text = stringResource(Res.string.bank_sync_submit_redirect),
                    onClick = { onIntent(BankSyncSettingsIntent.SubmitRedirect) },
                    size = MmButtonSize.Sm,
                    enabled = !state.isCompletingConnection && state.redirectDraft.isNotBlank(),
                )
                MmButton(
                    text = stringResource(Res.string.bank_sync_cancel),
                    onClick = { onIntent(BankSyncSettingsIntent.CancelAuth) },
                    variant = MmButtonVariant.Outline,
                    size = MmButtonSize.Sm,
                )
            }
        } else {
            Text(
                text = stringResource(Res.string.bank_sync_connect_bank),
                style = MM.type.body,
                color = colors.text,
            )
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = space.padding_1x),
                horizontalArrangement = Arrangement.spacedBy(space.padding_1x),
                verticalAlignment = Alignment.Bottom,
            ) {
                MmField(
                    value = state.countryDraft,
                    onValueChange = { onIntent(BankSyncSettingsIntent.CountryChanged(it)) },
                    label = stringResource(Res.string.bank_sync_country_label),
                    modifier = Modifier.weight(1f),
                )
                MmButton(
                    text = stringResource(Res.string.bank_sync_load_banks),
                    onClick = { onIntent(BankSyncSettingsIntent.LoadBanks) },
                    size = MmButtonSize.Sm,
                    enabled = !state.isLoadingBanks && state.countryDraft.length == 2,
                )
            }
            ConnectError(state)
            if (state.banks.isNotEmpty()) {
                MmField(
                    value = state.bankSearch,
                    onValueChange = { onIntent(BankSyncSettingsIntent.BankSearchChanged(it)) },
                    label = stringResource(Res.string.bank_sync_search_banks),
                    modifier = Modifier.padding(top = space.padding_1x),
                )
                Column(modifier = Modifier.padding(top = space.padding_1x)) {
                    state.filteredBanks.take(MAX_VISIBLE_BANKS).forEach { bank ->
                        Text(
                            text = bank.name,
                            style = MM.type.body,
                            color = colors.text,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onIntent(BankSyncSettingsIntent.ConnectBank(bank.name, bank.country))
                                }
                                .padding(vertical = space.padding_1x),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ConnectError(state: BankSyncSettingsUiState) {
    val colors = MM.colors
    val space = MM.dimen
    state.connectError?.let { error ->
        Text(
            text = stringResource(Res.string.bank_sync_error_generic, error),
            style = MM.type.caption.copy(color = colors.danger),
            modifier = Modifier.padding(top = space.padding_0_5x),
        )
    }
}

@Composable
private fun AccountCard(
    account: BankAccountRow,
    state: BankSyncSettingsUiState,
    onIntent: (BankSyncSettingsIntent) -> Unit,
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
                onCheckedChange = { onIntent(BankSyncSettingsIntent.SetAccountEnabled(account.uid, it)) },
            )
        }
        val targetName = state.localAccounts.firstOrNull { it.id == account.localAccountId }?.name
        Text(
            text = stringResource(Res.string.bank_sync_account_target_label) + ": " +
                (targetName ?: stringResource(Res.string.bank_sync_account_target_none)),
            style = MM.type.caption.copy(color = if (targetName == null) colors.danger else colors.text2),
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onIntent(BankSyncSettingsIntent.ShowAccountPicker(account.uid)) }
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
                                onIntent(BankSyncSettingsIntent.SetLocalAccountMapping(account.uid, option.id))
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
    state: BankSyncSettingsUiState,
    onIntent: (BankSyncSettingsIntent) -> Unit,
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
                onCheckedChange = { onIntent(BankSyncSettingsIntent.ToggleAutoSync) },
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
                onClick = { onIntent(BankSyncSettingsIntent.SyncNow) },
                size = MmButtonSize.Sm,
                enabled = !state.isSyncing && state.connected,
            )
            MmButton(
                text = stringResource(Res.string.bank_sync_disconnect),
                onClick = { onIntent(BankSyncSettingsIntent.Disconnect) },
                variant = MmButtonVariant.Outline,
                size = MmButtonSize.Sm,
            )
        }
    }
}

private fun formatDate(epochMs: Long): String =
    kotlin.time.Instant.fromEpochMilliseconds(epochMs)
        .toLocalDateTime(TimeZone.currentSystemDefault())
        .date
        .toString()

private const val MAX_VISIBLE_BANKS = 25
