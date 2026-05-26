package com.dv.moneym.feature.settings.overview.currencypicker

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import com.dv.moneym.core.model.Icon
import com.dv.moneym.core.ui.MmField
import com.dv.moneym.core.ui.MmRow
import com.dv.moneym.core.ui.imageVector
import com.dv.moneym.core.ui.ScreenHeader
import com.dv.moneym.core.ui.SectionLabel
import com.dv.moneym.feature.settings.overview.CurrencyPickerKey
import moneym.feature.settings.generated.resources.Res
import moneym.feature.settings.generated.resources.settings_currency_all
import moneym.feature.settings.generated.resources.settings_currency_picker_title
import moneym.feature.settings.generated.resources.settings_currency_popular
import moneym.feature.settings.generated.resources.settings_search_currency
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

fun EntryProviderScope<NavKey>.currencyPickerEntry(
    onBack: () -> Unit,
) = entry<CurrencyPickerKey> {
    CurrencyPickerScreen(onBack = onBack)
}

private val popularCurrencies = CommonCurrencies.filter { it.code in PopularCurrencyCodes }

@Composable
private fun CurrencyPickerScreen(
    onBack: () -> Unit,
    viewModel: CurrencyPickerViewModel = koinViewModel(),
) {
    val selectedCurrencyCode by viewModel.selectedCurrency.collectAsStateWithLifecycle()

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

    CurrencyPickerContent(
        searchQuery = searchQuery,
        onSearchQueryChanged = { searchQuery = it },
        filteredPopular = filteredPopular,
        filteredAll = filteredAll,
        selectedCurrencyCode = selectedCurrencyCode,
        onCurrencySelected = { code ->
            viewModel.onIntent(CurrencyPickerIntent.SetDefaultCurrency(code))
            onBack()
        },
        onBack = onBack,
    )
}

@Composable
private fun CurrencyPickerContent(
    searchQuery: String,
    onSearchQueryChanged: (String) -> Unit,
    filteredPopular: List<CurrencyInfo>,
    filteredAll: List<CurrencyInfo>,
    selectedCurrencyCode: String,
    onCurrencySelected: (String) -> Unit,
    onBack: () -> Unit,
) {
    val colors = MM.colors
    val space = MM.dimen

    Column(Modifier.fillMaxSize().background(colors.bg)) {
        ScreenHeader(stringResource(Res.string.settings_currency_picker_title), onBack = onBack)

        MmField(
            value = searchQuery,
            onValueChange = onSearchQueryChanged,
            placeholder = stringResource(Res.string.settings_search_currency),
            prefix = {
                Icon(
                    imageVector = Icon.Search.imageVector,
                    contentDescription = null,
                    tint = colors.text3,
                    modifier = Modifier.size(MM.dimen.icon_1x),
                )
            },
            modifier = Modifier.fillMaxWidth()
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
                                vertical = space.padding_0_5x
                            ),
                    )
                }
                items(filteredPopular, key = { "popular_${it.code}" }) { currency ->
                    CurrencyRow(
                        currency = currency,
                        isSelected = currency.code == selectedCurrencyCode,
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
                        .padding(horizontal = MM.dimen.padding_2_5x, vertical = space.padding_0_5x),
                )
            }
            items(filteredAll, key = { "all_${it.code}" }) { currency ->
                CurrencyRow(
                    currency = currency,
                    isSelected = currency.code == selectedCurrencyCode,
                    onClick = { onCurrencySelected(currency.code) },
                )
            }
        }
    }
}

@Composable
private fun CurrencyRow(
    currency: CurrencyInfo,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val colors = MM.colors
    val type = MM.type
    val radius = MM.dimen

    MmRow(onClick = onClick) {
        // Leading: symbol box
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(radius.radius_1x)
                .background(colors.surface2),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = currency.symbol,
                style = type.captionMono.copy(color = colors.text2),
            )
        }

        // Middle: code + name + region
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

        // Trailing: check if selected
        if (isSelected) {
            Icon(
                imageVector = Icon.Check.imageVector,
                contentDescription = null,
                tint = colors.accent,
                modifier = Modifier.size(MM.dimen.padding_2x),
            )
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview
@Composable
private fun CurrencyPickerContentPreview() {
    com.dv.moneym.core.designsystem.MoneyMTheme {
        CurrencyPickerContent(
            searchQuery = "",
            onSearchQueryChanged = {},
            filteredPopular = listOf(
                com.dv.moneym.core.model.CurrencyInfo("EUR", "Euro", "€"),
                com.dv.moneym.core.model.CurrencyInfo("USD", "US Dollar", "$"),
                com.dv.moneym.core.model.CurrencyInfo("GBP", "British Pound", "£"),
            ),
            filteredAll = listOf(
                com.dv.moneym.core.model.CurrencyInfo("JPY", "Japanese Yen", "¥"),
                com.dv.moneym.core.model.CurrencyInfo("CHF", "Swiss Franc", "CHF"),
            ),
            selectedCurrencyCode = "EUR",
            onCurrencySelected = {},
            onBack = {},
        )
    }
}
