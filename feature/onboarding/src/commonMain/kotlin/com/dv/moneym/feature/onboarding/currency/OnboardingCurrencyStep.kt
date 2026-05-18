package com.dv.moneym.feature.onboarding.currency

import com.dv.moneym.core.ui.imageVector
import com.dv.moneym.core.model.Icon
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.dv.moneym.core.designsystem.MM
import com.dv.moneym.core.model.CommonCurrencies
import com.dv.moneym.core.ui.MmButton
import com.dv.moneym.core.ui.MmButtonSize
import com.dv.moneym.core.ui.MmButtonVariant
import com.dv.moneym.core.ui.MmField
import com.dv.moneym.core.ui.MmRow
import com.dv.moneym.core.ui.SectionLabel
import kotlinx.serialization.Serializable
import moneym.feature.onboarding.generated.resources.Res
import moneym.feature.onboarding.generated.resources.onboarding_continue
import moneym.feature.onboarding.generated.resources.onboarding_currencies_header
import moneym.feature.onboarding.generated.resources.onboarding_currency_title
import moneym.feature.onboarding.generated.resources.onboarding_search_currency
import moneym.feature.onboarding.generated.resources.onboarding_welcome
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Serializable data object OnboardingKey : NavKey
fun EntryProviderScope<NavKey>.onboardingCurrencyEntry(
    onNavigateToSecurity: () -> Unit,
) = entry<OnboardingKey> {
    OnboardingCurrencyScreen(onNavigateToSecurity = onNavigateToSecurity)
}

@Composable
private fun OnboardingCurrencyScreen(
    onNavigateToSecurity: () -> Unit,
    viewModel: OnboardingCurrencyViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                OnboardingCurrencyEffect.NavigateToSecurity -> onNavigateToSecurity()
            }
        }
    }
    CurrencyStep(
        selected = state.selectedCurrency,
        searchQuery = state.searchQuery,
        onSelect = { viewModel.onIntent(OnboardingCurrencyIntent.CurrencySelected(it)) },
        onSearchQueryChanged = { viewModel.onIntent(OnboardingCurrencyIntent.SearchQueryChanged(it)) },
        onContinue = { viewModel.onIntent(OnboardingCurrencyIntent.Continue) },
    )
}

@Composable
internal fun CurrencyStep(
    selected: String,
    searchQuery: String,
    onSelect: (String) -> Unit,
    onSearchQueryChanged: (String) -> Unit,
    onContinue: () -> Unit,
) {
    val colors = MM.colors
    val type = MM.type

    val filteredItems by remember(searchQuery) {
        derivedStateOf {
            if (searchQuery.isBlank()) CommonCurrencies
            else {
                val q = searchQuery.trim().lowercase()
                CommonCurrencies.filter { c ->
                    c.code.lowercase().contains(q) || c.name.lowercase().contains(q)
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bg),
    ) {
        // Header
        Column(
            modifier = Modifier
                .statusBarsPadding()
                .padding(
                    start = MM.dimen.padding_2_5x,
                    end = MM.dimen.padding_2_5x,
                    top = MM.dimen.padding_2x,
                    bottom = MM.dimen.padding_1x
                ),
        ) {
            Text(
                text = stringResource(Res.string.onboarding_welcome),
                style = type.title1,
                color = colors.text,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = stringResource(Res.string.onboarding_currency_title),
                style = type.body.copy(color = colors.text2),
            )
        }

        // Search field
        MmField(
            value = searchQuery,
            onValueChange = onSearchQueryChanged,
            placeholder = stringResource(Res.string.onboarding_search_currency),
            prefix = {
                Icon(
                    imageVector = Icon.Search.imageVector,
                    contentDescription = null,
                    tint = colors.text3,
                    modifier = Modifier.size(MM.dimen.icon_1x),
                )
            },
            modifier = Modifier.fillMaxWidth()
                .padding(horizontal = MM.dimen.padding_2x, vertical = MM.dimen.padding_1x),
        )

        // Currency list
        LazyColumn(modifier = Modifier.weight(1f)) {
            if (filteredItems.isNotEmpty()) {
                stickyHeader {
                    SectionLabel(
                        text = stringResource(Res.string.onboarding_currencies_header),
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(colors.bg)
                            .padding(horizontal = MM.dimen.padding_2_5x, vertical = 4.dp),
                    )
                }
                items(filteredItems, key = { it.code }) { currency ->
                    OnboardingCurrencyRow(
                        code = currency.code,
                        name = currency.name,
                        isSelected = currency.code == selected,
                        onClick = { onSelect(currency.code) },
                    )
                }
            }
        }

        // Continue button
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.bg)
                .padding(horizontal = 16.dp, vertical = 16.dp),
        ) {
            MmButton(
                text = stringResource(Res.string.onboarding_continue),
                onClick = onContinue,
                variant = MmButtonVariant.Primary,
                size = MmButtonSize.Lg,
                fullWidth = true,
            )
        }
    }
}

@Composable
private fun OnboardingCurrencyRow(
    code: String,
    name: String,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val colors = MM.colors
    val type = MM.type
    val radius = MM.dimen

    MmRow(onClick = onClick) {
        // Leading: code box
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(radius.radius_1x)
                .background(colors.surface2),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = code,
                style = type.captionMono.copy(color = colors.text2),
            )
        }

        // Middle: code + name
        Column(Modifier.weight(1f)) {
            Text(
                text = code,
                style = type.body,
                color = colors.text,
            )
            Text(
                text = name,
                style = type.caption.copy(color = colors.text2),
            )
        }

        // Trailing: check if selected
        if (isSelected) {
            Icon(
                imageVector = Icon.Check.imageVector,
                contentDescription = null,
                tint = colors.accent,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}
