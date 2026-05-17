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
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.dv.moneym.core.designsystem.MM
import com.dv.moneym.core.model.CommonCurrencies
import com.dv.moneym.core.model.CurrencyInfo
import com.dv.moneym.core.model.PopularCurrencyCodes
import com.dv.moneym.core.ui.MmButton
import com.dv.moneym.core.ui.MmButtonSize
import com.dv.moneym.core.ui.MmButtonVariant
import com.dv.moneym.core.ui.MmField
import com.dv.moneym.core.ui.MmIcons
import com.dv.moneym.core.ui.MmRow
import com.dv.moneym.core.ui.ScreenHeader
import com.dv.moneym.core.ui.SectionLabel
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

private val popularCurrencies = CommonCurrencies.filter { it.code in PopularCurrencyCodes }

fun EntryProviderScope<NavKey>.addWalletEntry(
    viewModel: AddWalletViewModel,
    onBack: () -> Unit,
    onNavigateToCurrencyPicker: () -> Unit,
    onConfirm: (name: String, currency: String) -> Unit,
) = entry<AddWalletKey> {
    AddWalletScreen(
        viewModel = viewModel,
        onBack = onBack,
        onNavigateToCurrencyPicker = onNavigateToCurrencyPicker,
        onConfirm = onConfirm,
    )
}

fun EntryProviderScope<NavKey>.addWalletCurrencyPickerEntry(
    currentCurrency: () -> String,
    onBack: () -> Unit,
    onCurrencySelected: (String) -> Unit,
) = entry<AddWalletCurrencyPickerKey> {
    AddWalletCurrencyPickerScreen(
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
                onValueChange = { viewModel.setName(it) },
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

@Composable
private fun AddWalletCurrencyPickerScreen(
    currentCurrency: String,
    onBack: () -> Unit,
    onCurrencySelected: (String) -> Unit,
) {
    val colors = MM.colors
    val space = MM.dimen

    var searchQuery by remember { mutableStateOf("") }

    val filteredAll by remember(searchQuery) {
        derivedStateOf {
            if (searchQuery.isBlank()) {
                CommonCurrencies
            } else {
                val q = searchQuery.trim().lowercase()
                CommonCurrencies.filter { c ->
                    c.code.lowercase().contains(q) || c.name.lowercase().contains(q)
                }
            }
        }
    }

    val filteredPopular by remember(filteredAll) {
        derivedStateOf {
            popularCurrencies.filter { p -> filteredAll.any { it.code == p.code } }
        }
    }

    Column(Modifier.fillMaxSize().background(colors.bg)) {
        ScreenHeader(
            title = stringResource(Res.string.settings_currency_picker_title),
            onBack = onBack,
        )

        MmField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = stringResource(Res.string.settings_search_currency),
            prefix = {
                Icon(
                    imageVector = MmIcons.search,
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
private fun AddWalletCurrencyRow(
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
                imageVector = MmIcons.check,
                contentDescription = null,
                tint = colors.accent,
                modifier = Modifier.size(dimen.padding_2x),
            )
        }
    }
}
