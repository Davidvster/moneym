package com.dv.moneym.feature.banksync.bankpicker

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.dv.moneym.core.designsystem.MM
import com.dv.moneym.core.ui.MmButton
import com.dv.moneym.core.ui.MmButtonSize
import com.dv.moneym.core.ui.MmButtonVariant
import com.dv.moneym.core.ui.MmField
import com.dv.moneym.core.ui.MmLoadingOverlay
import com.dv.moneym.core.ui.ScreenHeader
import com.dv.moneym.core.utils.observeWithLifecycle
import kotlinx.serialization.Serializable
import moneym.feature.banksync.generated.resources.Res
import moneym.feature.banksync.generated.resources.bank_sync_awaiting_auth
import moneym.feature.banksync.generated.resources.bank_sync_cancel
import moneym.feature.banksync.generated.resources.bank_sync_change_country
import moneym.feature.banksync.generated.resources.bank_sync_choose_country
import moneym.feature.banksync.generated.resources.bank_sync_connect_bank
import moneym.feature.banksync.generated.resources.bank_sync_error_generic
import moneym.feature.banksync.generated.resources.bank_sync_redirect_invalid
import moneym.feature.banksync.generated.resources.bank_sync_redirect_label
import moneym.feature.banksync.generated.resources.bank_sync_search_banks
import moneym.feature.banksync.generated.resources.bank_sync_search_country
import moneym.feature.banksync.generated.resources.bank_sync_submit_redirect
import moneym.feature.banksync.generated.resources.bank_sync_supported_countries_hint
import moneym.feature.banksync.generated.resources.bank_sync_title
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Serializable
data object BankPickerKey : NavKey

fun EntryProviderScope<NavKey>.bankPickerEntry(
    onBack: () -> Unit,
    onConnected: () -> Unit,
    metadata: Map<String, Any> = emptyMap(),
) = entry<BankPickerKey>(metadata = metadata) {
    BankPickerScreen(
        onBack = onBack,
        onConnected = onConnected,
    )
}

@Composable
fun BankPickerScreen(
    onBack: () -> Unit,
    onConnected: () -> Unit,
    viewModel: BankPickerViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val uriHandler = LocalUriHandler.current
    viewModel.singleEvents.observeWithLifecycle { event ->
        when (event) {
            BankPickerViewModel.BankPickerSingleUiEvent.Connected -> onConnected()
        }
    }
    LaunchedEffect(state.authUrlToOpen) {
        state.authUrlToOpen?.let { url ->
            uriHandler.openUri(url)
            viewModel.onIntent(BankPickerIntent.AuthUrlOpened)
        }
    }
    BankPickerContent(
        state = state,
        onBack = onBack,
        onIntent = viewModel::onIntent,
    )
}

@Composable
private fun BankPickerContent(
    state: BankPickerUiState,
    onBack: () -> Unit,
    onIntent: (BankPickerIntent) -> Unit,
) {
    val colors = MM.colors
    val space = MM.dimen

    Box(modifier = Modifier.fillMaxSize().background(colors.bg)) {
        Column(modifier = Modifier.fillMaxSize().imePadding()) {
            ScreenHeader(
                title = stringResource(Res.string.bank_sync_title),
                onBack = onBack,
            )
            when {
                state.awaitingAuth -> AuthStep(state = state, onIntent = onIntent)
                state.selectedCountry == null -> CountryStep(state = state, onIntent = onIntent)
                else -> BankStep(state = state, onIntent = onIntent)
            }
        }
        MmLoadingOverlay(visible = state.isLoadingBanks || state.isCompletingConnection)
    }
}

@Composable
private fun CountryStep(
    state: BankPickerUiState,
    onIntent: (BankPickerIntent) -> Unit,
) {
    val colors = MM.colors
    val space = MM.dimen
    Column(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.padding(horizontal = space.padding_2x, vertical = space.padding_1x)) {
            Text(
                text = stringResource(Res.string.bank_sync_choose_country),
                style = MM.type.body,
                color = colors.text,
            )
            Text(
                text = stringResource(Res.string.bank_sync_supported_countries_hint),
                style = MM.type.caption.copy(color = colors.text2),
                modifier = Modifier.padding(top = space.padding_0_5x),
            )
            MmField(
                value = state.countrySearch,
                onValueChange = { onIntent(BankPickerIntent.CountrySearchChanged(it)) },
                label = stringResource(Res.string.bank_sync_search_country),
                modifier = Modifier.padding(top = space.padding_1x),
            )
        }
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(state.filteredCountries, key = { it.code }) { country ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onIntent(BankPickerIntent.CountrySelected(country.code)) }
                        .padding(horizontal = space.padding_2x, vertical = space.padding_1_5x),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = country.displayName,
                        style = MM.type.body,
                        color = colors.text,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = country.code,
                        style = MM.type.caption.copy(color = colors.text2),
                    )
                }
            }
        }
    }
}

@Composable
private fun BankStep(
    state: BankPickerUiState,
    onIntent: (BankPickerIntent) -> Unit,
) {
    val colors = MM.colors
    val space = MM.dimen
    Column(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.padding(horizontal = space.padding_2x, vertical = space.padding_1x)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(Res.string.bank_sync_connect_bank),
                    style = MM.type.body,
                    color = colors.text,
                    modifier = Modifier.weight(1f),
                )
                MmButton(
                    text = stringResource(Res.string.bank_sync_change_country),
                    onClick = { onIntent(BankPickerIntent.ChangeCountry) },
                    variant = MmButtonVariant.Outline,
                    size = MmButtonSize.Sm,
                )
            }
            MmField(
                value = state.bankSearch,
                onValueChange = { onIntent(BankPickerIntent.BankSearchChanged(it)) },
                label = stringResource(Res.string.bank_sync_search_banks),
                modifier = Modifier.padding(top = space.padding_1x),
            )
            state.connectError?.let { error ->
                Text(
                    text = stringResource(Res.string.bank_sync_error_generic, error),
                    style = MM.type.caption.copy(color = colors.danger),
                    modifier = Modifier.padding(top = space.padding_0_5x),
                )
            }
        }
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(state.filteredBanks, key = { it.name }) { bank ->
                Text(
                    text = bank.name,
                    style = MM.type.body,
                    color = colors.text,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            onIntent(BankPickerIntent.ConnectBank(bank.name, bank.country))
                        }
                        .padding(horizontal = space.padding_2x, vertical = space.padding_1_5x),
                )
            }
        }
    }
}

@Composable
private fun AuthStep(
    state: BankPickerUiState,
    onIntent: (BankPickerIntent) -> Unit,
) {
    val colors = MM.colors
    val space = MM.dimen
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = space.padding_2x, vertical = space.padding_2x),
    ) {
        Text(
            text = stringResource(Res.string.bank_sync_awaiting_auth),
            style = MM.type.caption.copy(color = colors.text2),
        )
        MmField(
            value = state.redirectDraft,
            onValueChange = { onIntent(BankPickerIntent.RedirectChanged(it)) },
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
        state.connectError?.let { error ->
            Text(
                text = stringResource(Res.string.bank_sync_error_generic, error),
                style = MM.type.caption.copy(color = colors.danger),
                modifier = Modifier.padding(top = space.padding_0_5x),
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = space.padding_1x),
            horizontalArrangement = Arrangement.spacedBy(space.padding_1x),
        ) {
            MmButton(
                text = stringResource(Res.string.bank_sync_submit_redirect),
                onClick = { onIntent(BankPickerIntent.SubmitRedirect) },
                size = MmButtonSize.Sm,
                enabled = !state.isCompletingConnection && state.redirectDraft.isNotBlank(),
            )
            MmButton(
                text = stringResource(Res.string.bank_sync_cancel),
                onClick = { onIntent(BankPickerIntent.CancelAuth) },
                variant = MmButtonVariant.Outline,
                size = MmButtonSize.Sm,
            )
        }
    }
}
