package com.dv.moneym.feature.settings.ui

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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.dv.moneym.core.designsystem.MM
import com.dv.moneym.core.ui.MmField
import com.dv.moneym.core.ui.MmIcons
import com.dv.moneym.core.ui.MmRow
import com.dv.moneym.core.ui.ScreenHeader
import com.dv.moneym.core.ui.SectionLabel
import com.dv.moneym.feature.settings.presentation.SettingsViewModel
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

data class CurrencyInfo(
    val code: String,
    val name: String,
    val symbol: String,
    val region: String = "",
)

private val popularCurrencyCodes = listOf("USD", "EUR", "GBP", "JPY", "CHF", "CAD", "AUD", "CNY")

private val allCurrencies = listOf(
    CurrencyInfo("USD", "US Dollar", "$", "United States"),
    CurrencyInfo("EUR", "Euro", "€", "Eurozone"),
    CurrencyInfo("GBP", "British Pound", "£", "United Kingdom"),
    CurrencyInfo("JPY", "Japanese Yen", "¥", "Japan"),
    CurrencyInfo("CHF", "Swiss Franc", "CHF", "Switzerland"),
    CurrencyInfo("CAD", "Canadian Dollar", "CA$", "Canada"),
    CurrencyInfo("AUD", "Australian Dollar", "A$", "Australia"),
    CurrencyInfo("CNY", "Chinese Yuan", "¥", "China"),
    CurrencyInfo("HKD", "Hong Kong Dollar", "HK$", "Hong Kong"),
    CurrencyInfo("SGD", "Singapore Dollar", "S$", "Singapore"),
    CurrencyInfo("SEK", "Swedish Krona", "kr", "Sweden"),
    CurrencyInfo("NOK", "Norwegian Krone", "kr", "Norway"),
    CurrencyInfo("DKK", "Danish Krone", "kr", "Denmark"),
    CurrencyInfo("NZD", "New Zealand Dollar", "NZ$", "New Zealand"),
    CurrencyInfo("MXN", "Mexican Peso", "MX$", "Mexico"),
    CurrencyInfo("BRL", "Brazilian Real", "R$", "Brazil"),
    CurrencyInfo("INR", "Indian Rupee", "₹", "India"),
    CurrencyInfo("KRW", "South Korean Won", "₩", "South Korea"),
    CurrencyInfo("ZAR", "South African Rand", "R", "South Africa"),
    CurrencyInfo("TRY", "Turkish Lira", "₺", "Turkey"),
    CurrencyInfo("PLN", "Polish Zloty", "zł", "Poland"),
    CurrencyInfo("CZK", "Czech Koruna", "Kč", "Czech Republic"),
    CurrencyInfo("HUF", "Hungarian Forint", "Ft", "Hungary"),
    CurrencyInfo("RUB", "Russian Ruble", "₽", "Russia"),
    CurrencyInfo("AED", "UAE Dirham", "د.إ", "UAE"),
    CurrencyInfo("SAR", "Saudi Riyal", "﷼", "Saudi Arabia"),
    CurrencyInfo("THB", "Thai Baht", "฿", "Thailand"),
    CurrencyInfo("IDR", "Indonesian Rupiah", "Rp", "Indonesia"),
    CurrencyInfo("MYR", "Malaysian Ringgit", "RM", "Malaysia"),
    CurrencyInfo("PHP", "Philippine Peso", "₱", "Philippines"),
    CurrencyInfo("VND", "Vietnamese Dong", "₫", "Vietnam"),
    CurrencyInfo("UAH", "Ukrainian Hryvnia", "₴", "Ukraine"),
    CurrencyInfo("ILS", "Israeli Shekel", "₪", "Israel"),
    CurrencyInfo("EGP", "Egyptian Pound", "£", "Egypt"),
    CurrencyInfo("NGN", "Nigerian Naira", "₦", "Nigeria"),
    CurrencyInfo("KES", "Kenyan Shilling", "KSh", "Kenya"),
    CurrencyInfo("GHS", "Ghanaian Cedi", "₵", "Ghana"),
)

private val popularCurrencies = allCurrencies.filter { it.code in popularCurrencyCodes }

@Composable
fun CurrencyPickerScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val selectedCurrencyCode = state.defaultCurrency

    var searchQuery by remember { mutableStateOf("") }

    val filteredAll by remember(searchQuery) {
        derivedStateOf {
            if (searchQuery.isBlank()) {
                allCurrencies
            } else {
                val q = searchQuery.trim().lowercase()
                allCurrencies.filter { c ->
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
            viewModel.setDefaultCurrency(code)
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
    val space = MM.space

    Column(Modifier.fillMaxSize().background(colors.bg)) {
        ScreenHeader(stringResource(Res.string.settings_currency_picker_title), onBack = onBack)

        MmField(
            value = searchQuery,
            onValueChange = onSearchQueryChanged,
            placeholder = stringResource(Res.string.settings_search_currency),
            prefix = {
                Icon(
                    imageVector = MmIcons.search,
                    contentDescription = null,
                    tint = colors.text3,
                    modifier = Modifier.size(18.dp),
                )
            },
            modifier = Modifier.fillMaxWidth().padding(horizontal = space.padding_2x, vertical = space.padding_1x),
        )

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            if (filteredPopular.isNotEmpty()) {
                stickyHeader {
                    SectionLabel(
                        text = stringResource(Res.string.settings_currency_popular),
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(colors.bg)
                            .padding(horizontal = 20.dp, vertical = space.padding_0_5x),
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
                        .padding(horizontal = 20.dp, vertical = space.padding_0_5x),
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
    val radius = MM.radius

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
                imageVector = MmIcons.check,
                contentDescription = null,
                tint = colors.accent,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}
