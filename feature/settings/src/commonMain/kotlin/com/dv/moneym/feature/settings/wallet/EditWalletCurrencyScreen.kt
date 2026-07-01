package com.dv.moneym.feature.settings.wallet

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.dv.moneym.core.designsystem.MM
import com.dv.moneym.core.model.CommonCurrencies
import com.dv.moneym.core.model.Icon
import com.dv.moneym.core.model.PopularCurrencyCodes
import com.dv.moneym.core.ui.MmButton
import com.dv.moneym.core.ui.MmButtonVariant
import com.dv.moneym.core.ui.MmField
import com.dv.moneym.core.ui.MmSheetHeader
import com.dv.moneym.core.ui.ScreenHeader
import com.dv.moneym.core.ui.SectionLabel
import com.dv.moneym.core.ui.imageVector
import kotlinx.serialization.Serializable
import moneym.feature.settings.generated.resources.Res
import moneym.feature.settings.generated.resources.settings_currency_all
import moneym.feature.settings.generated.resources.settings_currency_popular
import moneym.feature.settings.generated.resources.settings_edit_currency_cancel
import moneym.feature.settings.generated.resources.settings_edit_currency_confirm
import moneym.feature.settings.generated.resources.settings_edit_currency_converting
import moneym.feature.settings.generated.resources.settings_edit_currency_example
import moneym.feature.settings.generated.resources.settings_edit_currency_rate_label
import moneym.feature.settings.generated.resources.settings_edit_currency_title
import moneym.feature.settings.generated.resources.settings_edit_currency_warning
import moneym.feature.settings.generated.resources.settings_search_currency
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Serializable
data class EditWalletCurrencyKey(val accountId: Long, val currentCurrency: String) : NavKey

private val popularCurrenciesForEdit = CommonCurrencies.filter { it.code in PopularCurrencyCodes }

fun EntryProviderScope<NavKey>.editWalletCurrencyEntry(
    onBack: () -> Unit,
    metadata: Map<String, Any> = emptyMap(),
) = entry<EditWalletCurrencyKey>(metadata = metadata) { key ->
    EditWalletCurrencyScreen(
        accountId = key.accountId,
        currentCurrency = key.currentCurrency,
        onBack = onBack,
    )
}

@Composable
private fun EditWalletCurrencyScreen(
    accountId: Long,
    currentCurrency: String,
    onBack: () -> Unit,
    viewModel: EditWalletCurrencyViewModel = koinViewModel(
        parameters = { parametersOf(accountId, currentCurrency) }
    ),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                EditWalletCurrencyEffect.Done -> onBack()
            }
        }
    }
    EditWalletCurrencyContent(
        state = state,
        currentCurrency = currentCurrency,
        onIntent = viewModel::onIntent,
        onBack = onBack,
    )
}

@Composable
private fun EditWalletCurrencyContent(
    state: EditWalletCurrencyUiState,
    currentCurrency: String,
    onIntent: (EditWalletCurrencyIntent) -> Unit,
    onBack: () -> Unit,
) {
    val colors = MM.colors
    val space = MM.dimen

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().background(colors.bg)) {
            ScreenHeader(
                title = stringResource(Res.string.settings_edit_currency_title),
                onBack = onBack,
            )

            MmField(
                value = state.searchQuery,
                onValueChange = { onIntent(EditWalletCurrencyIntent.SearchQueryChanged(it)) },
                placeholder = stringResource(Res.string.settings_search_currency),
                prefix = {
                    Icon(
                        imageVector = Icon.Search.imageVector,
                        contentDescription = null,
                        tint = colors.text3,
                        modifier = Modifier.size(MM.dimen.iconMd),
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = space.padding_2x, vertical = space.padding_1x),
            )

            val filteredAll = remember(state.searchQuery) {
                if (state.searchQuery.isBlank()) CommonCurrencies
                else {
                    val q = state.searchQuery.trim().lowercase()
                    CommonCurrencies.filter { c ->
                        c.code.lowercase().contains(q) || c.name.lowercase().contains(q)
                    }
                }
            }
            val filteredPopular = remember(filteredAll) {
                popularCurrenciesForEdit.filter { p -> filteredAll.any { it.code == p.code } }
            }

            LazyColumn(Modifier.fillMaxSize()) {
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
                            onClick = {
                                onIntent(EditWalletCurrencyIntent.CurrencySelected(currency.code))
                            },
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
                        onClick = {
                            onIntent(EditWalletCurrencyIntent.CurrencySelected(currency.code))
                        },
                    )
                }
            }
        }

        if (state.showConfirmSheet) {
            CurrencyConversionSheet(
                oldCurrency = currentCurrency,
                newCurrency = state.selectedCurrency ?: "",
                conversionRate = state.conversionRate,
                isConverting = state.isConverting,
                onRateChanged = { onIntent(EditWalletCurrencyIntent.RateChanged(it)) },
                onConfirm = { onIntent(EditWalletCurrencyIntent.ConfirmConversion) },
                onCancel = { onIntent(EditWalletCurrencyIntent.CancelConversion) },
            )
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview
@Composable
private fun EditWalletCurrencyContentPreview() {
    com.dv.moneym.core.designsystem.MoneyMTheme {
        EditWalletCurrencyContent(
            state = EditWalletCurrencyUiState(currentCurrency = "EUR"),
            currentCurrency = "EUR",
            onIntent = {},
            onBack = {},
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CurrencyConversionSheet(
    oldCurrency: String,
    newCurrency: String,
    conversionRate: String,
    isConverting: Boolean,
    onRateChanged: (String) -> Unit,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
) {
    val colors = MM.colors
    val type = MM.type
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = { if (!isConverting) onCancel() },
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = MM.dimen.padding_2_5x, topEnd = MM.dimen.padding_2_5x),
        containerColor = colors.bg,
        dragHandle = null,
    ) {
        Box {
            Column(
                modifier = Modifier.padding(
                    horizontal = MM.dimen.padding_2_5x,
                    vertical = MM.dimen.padding_3x,
                ),
                verticalArrangement = Arrangement.spacedBy(MM.dimen.padding_2x),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        modifier = Modifier
                            .size(width = 36.dp, height = MM.dimen.padding_0_5x)
                            .clip(RoundedCornerShape(50))
                            .background(colors.borderStrong),
                    )
                }

                MmSheetHeader(onClose = { if (!isConverting) onCancel() })

                Text(
                    text = stringResource(Res.string.settings_edit_currency_warning, oldCurrency, newCurrency),
                    style = type.body,
                    color = colors.text,
                    textAlign = TextAlign.Center,
                )

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(MM.dimen.padding_0_5x),
                ) {
                    Text(
                        text = stringResource(Res.string.settings_edit_currency_rate_label),
                        style = type.caption,
                        color = colors.text2,
                    )
                    MmField(
                        value = conversionRate,
                        onValueChange = onRateChanged,
                        keyboardType = KeyboardType.Decimal,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    val exampleRate = conversionRate.toDoubleOrNull()
                    if (exampleRate != null && exampleRate > 0) {
                        Text(
                            text = stringResource(
                                Res.string.settings_edit_currency_example,
                                oldCurrency,
                                conversionRate,
                                newCurrency,
                            ),
                            style = type.caption,
                            color = colors.text3,
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(MM.dimen.padding_1_5x),
                ) {
                    MmButton(
                        text = stringResource(Res.string.settings_edit_currency_cancel),
                        onClick = onCancel,
                        enabled = !isConverting,
                        variant = MmButtonVariant.Secondary,
                        modifier = Modifier.weight(1f),
                    )
                    MmButton(
                        text = stringResource(Res.string.settings_edit_currency_confirm),
                        onClick = onConfirm,
                        enabled = !isConverting,
                        modifier = Modifier.weight(1f),
                    )
                }

                Spacer(Modifier.height(MM.dimen.padding_1x))
            }

            if (isConverting) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(MM.dimen.padding_3x),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(MM.dimen.padding_1_5x),
                ) {
                    CircularProgressIndicator(color = colors.accent)
                    Text(
                        text = stringResource(Res.string.settings_edit_currency_converting),
                        style = type.caption,
                        color = colors.text2,
                    )
                }
            }
        }
    }
}
