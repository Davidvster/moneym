package com.dv.moneym.feature.banksync.credentials

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.dv.moneym.core.designsystem.MM
import com.dv.moneym.core.ui.MmButton
import com.dv.moneym.core.ui.MmField
import com.dv.moneym.core.ui.MmLoadingOverlay
import com.dv.moneym.core.ui.ScreenHeader
import com.dv.moneym.core.utils.observeWithLifecycle
import kotlinx.serialization.Serializable
import moneym.feature.banksync.generated.resources.Res
import moneym.feature.banksync.generated.resources.bank_sync_app_id_label
import moneym.feature.banksync.generated.resources.bank_sync_error_generic
import moneym.feature.banksync.generated.resources.bank_sync_private_key_label
import moneym.feature.banksync.generated.resources.bank_sync_save_continue
import moneym.feature.banksync.generated.resources.bank_sync_setup_header
import moneym.feature.banksync.generated.resources.bank_sync_setup_hint
import moneym.feature.banksync.generated.resources.bank_sync_title
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Serializable
data object BankSyncCredentialsKey : NavKey

fun EntryProviderScope<NavKey>.bankSyncCredentialsEntry(
    onBack: () -> Unit,
    onContinueToBankPicker: () -> Unit,
    metadata: Map<String, Any> = emptyMap(),
) = entry<BankSyncCredentialsKey>(metadata = metadata) {
    BankSyncCredentialsScreen(
        onBack = onBack,
        onContinueToBankPicker = onContinueToBankPicker,
    )
}

@Composable
fun BankSyncCredentialsScreen(
    onBack: () -> Unit,
    onContinueToBankPicker: () -> Unit,
    viewModel: BankSyncCredentialsViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    viewModel.singleEvents.observeWithLifecycle { event ->
        when (event) {
            is BankSyncCredentialsViewModel.BankSyncCredentialsSingleUiEvent.Saved ->
                if (event.continueToBankPicker) onContinueToBankPicker() else onBack()
        }
    }
    BankSyncCredentialsContent(
        state = state,
        onBack = onBack,
        onIntent = viewModel::onIntent,
    )
}

@Composable
private fun BankSyncCredentialsContent(
    state: BankSyncCredentialsUiState,
    onBack: () -> Unit,
    onIntent: (BankSyncCredentialsIntent) -> Unit,
) {
    val colors = MM.colors
    val space = MM.dimen

    Box(modifier = Modifier.fillMaxSize().background(colors.bg)) {
        Column(modifier = Modifier.fillMaxSize().imePadding()) {
            ScreenHeader(
                title = stringResource(Res.string.bank_sync_title),
                onBack = onBack,
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = space.padding_2x, vertical = space.padding_2x),
            ) {
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
                    onValueChange = { onIntent(BankSyncCredentialsIntent.AppIdChanged(it)) },
                    label = stringResource(Res.string.bank_sync_app_id_label),
                )
                MmField(
                    value = state.pemDraft,
                    onValueChange = { onIntent(BankSyncCredentialsIntent.PemChanged(it)) },
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
            }
            MmButton(
                text = stringResource(Res.string.bank_sync_save_continue),
                onClick = { onIntent(BankSyncCredentialsIntent.SaveCredentials) },
                enabled = state.canSave,
                fullWidth = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = space.padding_2x, vertical = space.padding_2x)
                    .navigationBarsPadding(),
            )
        }
        MmLoadingOverlay(visible = state.isValidatingCredentials)
    }
}
