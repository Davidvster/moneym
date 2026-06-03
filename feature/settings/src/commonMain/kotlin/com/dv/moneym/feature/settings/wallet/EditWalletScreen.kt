package com.dv.moneym.feature.settings.wallet

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.dv.moneym.core.designsystem.MM
import com.dv.moneym.core.designsystem.categoryColor
import com.dv.moneym.core.designsystem.defaultCategoryColors
import com.dv.moneym.core.ui.HsvColorPickerDialog
import com.dv.moneym.core.ui.MmButton
import com.dv.moneym.core.ui.MmButtonSize
import com.dv.moneym.core.ui.MmButtonVariant
import com.dv.moneym.core.ui.MmField
import com.dv.moneym.core.ui.ScreenHeader
import com.dv.moneym.core.ui.SectionLabel
import com.dv.moneym.core.ui.WalletColorDot
import com.dv.moneym.core.ui.colorToHex
import kotlinx.serialization.Serializable
import moneym.feature.settings.generated.resources.Res
import moneym.feature.settings.generated.resources.settings_wallet_color_change
import moneym.feature.settings.generated.resources.settings_wallet_color_label
import moneym.feature.settings.generated.resources.settings_wallet_color_none
import moneym.feature.settings.generated.resources.settings_wallet_currency_change
import moneym.feature.settings.generated.resources.settings_wallet_currency_label
import moneym.feature.settings.generated.resources.settings_wallet_edit_title
import moneym.feature.settings.generated.resources.settings_wallet_name_placeholder
import moneym.feature.settings.generated.resources.settings_wallet_save
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Serializable
data class EditWalletKey(val accountId: Long, val currency: String) : NavKey

fun EntryProviderScope<NavKey>.editWalletEntry(
    onBack: () -> Unit,
    onNavigateToCurrency: (accountId: Long, currentCurrency: String) -> Unit,
    metadata: Map<String, Any> = emptyMap(),
) = entry<EditWalletKey>(metadata = metadata) { key ->
    EditWalletScreen(
        accountId = key.accountId,
        currency = key.currency,
        onBack = onBack,
        onNavigateToCurrency = onNavigateToCurrency,
    )
}

@Composable
private fun EditWalletScreen(
    accountId: Long,
    currency: String,
    onBack: () -> Unit,
    onNavigateToCurrency: (accountId: Long, currentCurrency: String) -> Unit,
    viewModel: EditWalletViewModel = koinViewModel(
        parameters = { parametersOf(accountId) }
    ),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                EditWalletEffect.Done -> onBack()
            }
        }
    }
    EditWalletContent(
        state = state,
        currency = currency,
        onIntent = viewModel::onIntent,
        onBack = onBack,
        onNavigateToCurrency = { onNavigateToCurrency(accountId, currency) },
    )
}

@Composable
private fun EditWalletContent(
    state: EditWalletUiState,
    currency: String,
    onIntent: (EditWalletIntent) -> Unit,
    onBack: () -> Unit,
    onNavigateToCurrency: () -> Unit,
) {
    val colors = MM.colors
    val type = MM.type
    val space = MM.dimen

    var showColorPicker by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bg),
    ) {
        ScreenHeader(
            title = stringResource(Res.string.settings_wallet_edit_title),
            onBack = onBack,
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = space.padding_2x, vertical = space.padding_2x),
            verticalArrangement = Arrangement.spacedBy(space.padding_1_5x),
        ) {
            MmField(
                value = state.name,
                onValueChange = { onIntent(EditWalletIntent.NameChanged(it)) },
                placeholder = stringResource(Res.string.settings_wallet_name_placeholder),
                modifier = Modifier.fillMaxWidth(),
            )

            Column(verticalArrangement = Arrangement.spacedBy(space.padding_0_5x)) {
                SectionLabel(text = stringResource(Res.string.settings_wallet_color_label))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(space.padding_1x),
                    ) {
                        WalletColorDot(colorHex = state.colorHex)
                        Text(
                            text = state.colorHex
                                ?: stringResource(Res.string.settings_wallet_color_none),
                            style = type.body,
                            color = if (state.colorHex == null) colors.text3 else colors.text,
                        )
                    }
                    MmButton(
                        text = stringResource(Res.string.settings_wallet_color_change),
                        onClick = { showColorPicker = true },
                        variant = MmButtonVariant.Outline,
                        size = MmButtonSize.Sm,
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(space.padding_0_5x)) {
                SectionLabel(text = stringResource(Res.string.settings_wallet_currency_label))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(text = currency, style = type.body, color = colors.text)
                    MmButton(
                        text = stringResource(Res.string.settings_wallet_currency_change),
                        onClick = onNavigateToCurrency,
                        variant = MmButtonVariant.Outline,
                        size = MmButtonSize.Sm,
                    )
                }
            }
        }

        if (showColorPicker) {
            HsvColorPickerDialog(
                initialColor = state.colorHex?.let { categoryColor(it) }
                    ?: defaultCategoryColors.first(),
                onDismiss = { showColorPicker = false },
                onColorSelected = { color ->
                    onIntent(EditWalletIntent.ColorChanged(colorToHex(color)))
                    showColorPicker = false
                },
            )
        }

        MmButton(
            text = stringResource(Res.string.settings_wallet_save),
            onClick = { onIntent(EditWalletIntent.Save) },
            variant = MmButtonVariant.Primary,
            size = MmButtonSize.Lg,
            fullWidth = true,
            enabled = state.name.isNotBlank(),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = space.padding_2x, vertical = space.padding_2x)
                .navigationBarsPadding(),
        )
    }
}

@androidx.compose.ui.tooling.preview.Preview
@Composable
private fun EditWalletContentPreview() {
    com.dv.moneym.core.designsystem.MoneyMTheme {
        EditWalletContent(
            state = EditWalletUiState(name = "Travel", colorHex = "#3B82F6", loaded = true),
            currency = "EUR",
            onIntent = {},
            onBack = {},
            onNavigateToCurrency = {},
        )
    }
}
