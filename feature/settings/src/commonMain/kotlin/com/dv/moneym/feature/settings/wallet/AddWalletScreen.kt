package com.dv.moneym.feature.settings.wallet

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.dv.moneym.core.designsystem.MM
import com.dv.moneym.core.model.CurrencyInfo
import com.dv.moneym.core.model.Icon
import com.dv.moneym.core.ui.MmButton
import com.dv.moneym.core.ui.MmButtonSize
import com.dv.moneym.core.ui.MmButtonVariant
import com.dv.moneym.core.ui.MmField
import com.dv.moneym.core.ui.MmRow
import com.dv.moneym.core.ui.ScreenHeader
import com.dv.moneym.core.ui.SectionLabel
import com.dv.moneym.core.ui.imageVector
import kotlinx.serialization.Serializable
import moneym.feature.settings.generated.resources.Res
import moneym.feature.settings.generated.resources.settings_currency_all
import moneym.feature.settings.generated.resources.settings_currency_picker_title
import moneym.feature.settings.generated.resources.settings_currency_popular
import moneym.feature.settings.generated.resources.settings_search_currency
import moneym.feature.settings.generated.resources.settings_wallet_add_confirm
import moneym.feature.settings.generated.resources.settings_wallet_add_screen_title
import moneym.feature.settings.generated.resources.settings_wallet_currency_change
import moneym.feature.settings.generated.resources.settings_wallet_currency_label
import moneym.feature.settings.generated.resources.settings_wallet_currency_none
import moneym.feature.settings.generated.resources.settings_wallet_name_placeholder
import org.jetbrains.compose.resources.stringResource

@Serializable
data object AddWalletKey : NavKey

@Serializable
data object AddWalletCurrencyPickerKey : NavKey

fun EntryProviderScope<NavKey>.addWalletEntry(
    viewModel: AddWalletViewModel,
    onBack: () -> Unit,
    onNavigateToCurrencyPicker: () -> Unit,
    onConfirm: (name: String, currency: String) -> Unit,
    metadata: Map<String, Any> = emptyMap(),
) = entry<AddWalletKey>(metadata = metadata) {
    AddWalletScreen(
        viewModel = viewModel,
        onBack = onBack,
        onNavigateToCurrencyPicker = onNavigateToCurrencyPicker,
        onConfirm = onConfirm,
    )
}

fun EntryProviderScope<NavKey>.addWalletCurrencyPickerEntry(
    viewModel: AddWalletViewModel,
    currentCurrency: () -> String,
    onBack: () -> Unit,
    onCurrencySelected: (String) -> Unit,
    metadata: Map<String, Any> = emptyMap(),
) = entry<AddWalletCurrencyPickerKey>(metadata = metadata) {
    AddWalletCurrencyPickerScreen(
        viewModel = viewModel,
        currentCurrency = currentCurrency(),
        onBack = onBack,
        onCurrencySelected = { code ->
            onCurrencySelected(code)
            onBack()
        },
    )
}

@Composable
private fun AddWalletScreen(
    viewModel: AddWalletViewModel,
    onBack: () -> Unit,
    onNavigateToCurrencyPicker: () -> Unit,
    onConfirm: (name: String, currency: String) -> Unit,
) {
    val name by viewModel.name.collectAsStateWithLifecycle()
    val selectedCurrency by viewModel.selectedCurrency.collectAsStateWithLifecycle()
    AddWalletContent(
        name = name,
        selectedCurrency = selectedCurrency,
        onNameChange = viewModel::setName,
        onBack = onBack,
        onNavigateToCurrencyPicker = onNavigateToCurrencyPicker,
        onConfirm = onConfirm,
    )
}

@Composable
private fun AddWalletContent(
    name: String,
    selectedCurrency: String,
    onNameChange: (String) -> Unit,
    onBack: () -> Unit,
    onNavigateToCurrencyPicker: () -> Unit,
    onConfirm: (name: String, currency: String) -> Unit,
) {
    val colors = MM.colors
    val type = MM.type
    val space = MM.dimen

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bg),
    ) {
        ScreenHeader(
            title = stringResource(Res.string.settings_wallet_add_screen_title),
            onBack = onBack,
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = space.padding_2x, vertical = space.padding_2x),
            verticalArrangement = Arrangement.spacedBy(space.padding_1_5x),
        ) {
            MmField(
                value = name,
                onValueChange = onNameChange,
                placeholder = stringResource(Res.string.settings_wallet_name_placeholder),
                modifier = Modifier.fillMaxWidth(),
            )

            Column(verticalArrangement = Arrangement.spacedBy(space.padding_0_5x)) {
                SectionLabel(text = stringResource(Res.string.settings_wallet_currency_label))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = selectedCurrency.ifBlank {
                            stringResource(Res.string.settings_wallet_currency_none)
                        },
                        style = type.body,
                        color = if (selectedCurrency.isBlank()) colors.text3 else colors.text,
                    )
                    MmButton(
                        text = stringResource(Res.string.settings_wallet_currency_change),
                        onClick = onNavigateToCurrencyPicker,
                        variant = MmButtonVariant.Outline,
                        size = MmButtonSize.Sm,
                    )
                }
            }
        }

        MmButton(
            text = stringResource(Res.string.settings_wallet_add_confirm),
            onClick = {
                if (name.isNotBlank() && selectedCurrency.isNotBlank()) {
                    onConfirm(name.trim(), selectedCurrency.trim())
                }
            },
            variant = MmButtonVariant.Primary,
            size = MmButtonSize.Lg,
            fullWidth = true,
            enabled = name.isNotBlank() && selectedCurrency.isNotBlank(),
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = space.padding_2x,
                    vertical = space.padding_2x,
                ),
        )
    }
}

@androidx.compose.ui.tooling.preview.Preview
@Composable
private fun AddWalletContentPreview() {
    com.dv.moneym.core.designsystem.MoneyMTheme {
        AddWalletContent(
            name = "Travel",
            selectedCurrency = "USD",
            onNameChange = {},
            onBack = {},
            onNavigateToCurrencyPicker = {},
            onConfirm = { _, _ -> },
        )
    }
}

@Composable
internal fun AddWalletCurrencyPickerScreen(
    viewModel: AddWalletViewModel,
    currentCurrency: String,
    onBack: () -> Unit,
    onCurrencySelected: (String) -> Unit,
) {
    val colors = MM.colors
    val space = MM.dimen

    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val filteredAll by viewModel.filteredCurrencies.collectAsStateWithLifecycle()
    val filteredPopular by viewModel.popularFilteredCurrencies.collectAsStateWithLifecycle()

    Column(Modifier.fillMaxSize().background(colors.bg)) {
        ScreenHeader(
            title = stringResource(Res.string.settings_currency_picker_title),
            onBack = onBack,
        )

        MmField(
            value = searchQuery,
            onValueChange = { viewModel.setSearchQuery(it) },
            placeholder = stringResource(Res.string.settings_search_currency),
            prefix = {
                Icon(
                    imageVector = Icon.Search.imageVector,
                    contentDescription = null,
                    tint = colors.text3,
                    modifier = Modifier.size(MM.dimen.icon_1x),
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = space.padding_2x, vertical = space.padding_1x),
        )

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            if (filteredPopular.isNotEmpty()) {
                stickyHeader {
                    SectionLabel(
                        text = stringResource(Res.string.settings_currency_popular),
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(colors.bg)
                            .padding(
                                horizontal = MM.dimen.padding_2_5x,
                                vertical = space.padding_0_5x,
                            ),
                    )
                }
                items(filteredPopular, key = { "popular_${it.code}" }) { currency ->
                    AddWalletCurrencyRow(
                        currency = currency,
                        isSelected = currency.code == currentCurrency,
                        onClick = { onCurrencySelected(currency.code) },
                    )
                }
            }

            stickyHeader {
                SectionLabel(
                    text = stringResource(Res.string.settings_currency_all),
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(colors.bg)
                        .padding(
                            horizontal = MM.dimen.padding_2_5x,
                            vertical = space.padding_0_5x,
                        ),
                )
            }
            items(filteredAll, key = { "all_${it.code}" }) { currency ->
                AddWalletCurrencyRow(
                    currency = currency,
                    isSelected = currency.code == currentCurrency,
                    onClick = { onCurrencySelected(currency.code) },
                )
            }
        }
    }
}

@Composable
internal fun AddWalletCurrencyRow(
    currency: CurrencyInfo,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val colors = MM.colors
    val type = MM.type
    val dimen = MM.dimen

    MmRow(onClick = onClick) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(dimen.radius_1x)
                .background(colors.surface2),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = currency.symbol,
                style = type.captionMono.copy(color = colors.text2),
            )
        }

        Column(Modifier.weight(1f)) {
            Text(
                text = "${currency.code}  ${currency.name}",
                style = type.body,
                color = colors.text,
            )
            if (currency.region.isNotBlank()) {
                Text(
                    text = currency.region,
                    style = type.caption.copy(color = colors.text2),
                )
            }
        }

        if (isSelected) {
            Icon(
                imageVector = Icon.Check.imageVector,
                contentDescription = null,
                tint = colors.accent,
                modifier = Modifier.size(dimen.padding_2x),
            )
        }
    }
}
