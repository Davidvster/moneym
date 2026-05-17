package com.dv.moneym.feature.onboarding.ui

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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.dv.moneym.core.designsystem.MM
import com.dv.moneym.core.ui.MmButton
import com.dv.moneym.core.ui.MmButtonSize
import com.dv.moneym.core.ui.MmButtonVariant
import com.dv.moneym.core.ui.MmCard
import com.dv.moneym.core.ui.MmField
import com.dv.moneym.core.ui.MmIcons
import com.dv.moneym.core.ui.MmRow
import com.dv.moneym.core.ui.MmToggle
import com.dv.moneym.core.ui.SectionLabel
import com.dv.moneym.feature.onboarding.presentation.OnboardingCurrencyEffect
import com.dv.moneym.feature.onboarding.presentation.OnboardingCurrencyIntent
import com.dv.moneym.feature.onboarding.presentation.OnboardingCurrencyViewModel
import com.dv.moneym.feature.onboarding.presentation.OnboardingSecurityEffect
import com.dv.moneym.feature.onboarding.presentation.OnboardingSecurityIntent
import com.dv.moneym.feature.onboarding.presentation.OnboardingSecurityViewModel
import com.dv.moneym.feature.onboarding.presentation.commonCurrencies
import kotlinx.serialization.Serializable
import moneym.feature.onboarding.generated.resources.Res
import moneym.feature.onboarding.generated.resources.onboarding_biometrics_label
import moneym.feature.onboarding.generated.resources.onboarding_biometrics_subtitle
import moneym.feature.onboarding.generated.resources.onboarding_continue
import moneym.feature.onboarding.generated.resources.onboarding_currencies_header
import moneym.feature.onboarding.generated.resources.onboarding_currency_title
import moneym.feature.onboarding.generated.resources.onboarding_done
import moneym.feature.onboarding.generated.resources.onboarding_pin_label
import moneym.feature.onboarding.generated.resources.onboarding_pin_set
import moneym.feature.onboarding.generated.resources.onboarding_search_currency
import moneym.feature.onboarding.generated.resources.onboarding_security_subtitle
import moneym.feature.onboarding.generated.resources.onboarding_security_title
import moneym.feature.onboarding.generated.resources.onboarding_skip
import moneym.feature.onboarding.generated.resources.onboarding_welcome
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Serializable data object OnboardingKey : NavKey
@Serializable data object OnboardingSecurityKey : NavKey
@Serializable data object OnboardingPinSetupKey : NavKey

fun EntryProviderScope<NavKey>.onboardingCurrencyEntry(
    onNavigateToSecurity: () -> Unit,
) = entry<OnboardingKey> {
    OnboardingCurrencyScreen(onNavigateToSecurity = onNavigateToSecurity)
}

fun EntryProviderScope<NavKey>.onboardingSecurityEntry(
    onNavigateToPinSetup: () -> Unit,
    onComplete: () -> Unit,
    viewModel: OnboardingSecurityViewModel,
) = entry<OnboardingSecurityKey> {
    OnboardingSecurityScreen(
        viewModel = viewModel,
        onNavigateToPinSetup = onNavigateToPinSetup,
        onComplete = onComplete,
    )
}

// ─── Currency Screen ─────────────────────────────────────────────────────────

@Composable
fun OnboardingCurrencyScreen(
    onNavigateToSecurity: () -> Unit,
    viewModel: OnboardingCurrencyViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsState()
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

// ─── Security Screen ─────────────────────────────────────────────────────────

@Composable
fun OnboardingSecurityScreen(
    viewModel: OnboardingSecurityViewModel,
    onNavigateToPinSetup: () -> Unit,
    onComplete: () -> Unit,
) {
    val state by viewModel.state.collectAsState()
    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                OnboardingSecurityEffect.NavigateToPinSetup -> onNavigateToPinSetup()
                OnboardingSecurityEffect.Complete -> onComplete()
            }
        }
    }
    SecurityStep(
        pinEnabled = state.pinEnabled,
        biometricAvailable = state.biometricAvailable,
        biometricEnabled = state.biometricEnabled,
        onSetupPin = { viewModel.onIntent(OnboardingSecurityIntent.SetupPinRequested) },
        onFinish = { viewModel.onIntent(OnboardingSecurityIntent.Finish) },
        onBiometricToggle = { viewModel.onIntent(OnboardingSecurityIntent.BiometricToggled(it)) },
    )
}

// ─── Currency Step UI ────────────────────────────────────────────────────────

private data class CurrencyItem(val code: String, val name: String)

@Composable
private fun CurrencyStep(
    selected: String,
    searchQuery: String,
    onSelect: (String) -> Unit,
    onSearchQueryChanged: (String) -> Unit,
    onContinue: () -> Unit,
) {
    val colors = MM.colors
    val type = MM.type

    val allItems = remember { commonCurrencies.map { (code, name) -> CurrencyItem(code, name) } }

    val filteredItems by remember(searchQuery) {
        derivedStateOf {
            if (searchQuery.isBlank()) allItems
            else {
                val q = searchQuery.trim().lowercase()
                allItems.filter { c ->
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
                .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 8.dp),
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
                    imageVector = MmIcons.search,
                    contentDescription = null,
                    tint = colors.text3,
                    modifier = Modifier.size(18.dp),
                )
            },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
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
                            .padding(horizontal = 20.dp, vertical = 4.dp),
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
    val radius = MM.radius

    MmRow(onClick = onClick) {
        // Leading: code box
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(radius.sm)
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
                imageVector = MmIcons.check,
                contentDescription = null,
                tint = colors.accent,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

// ─── Security Step UI ─────────────────────────────────────────────────────────

@Composable
private fun SecurityStep(
    pinEnabled: Boolean,
    biometricAvailable: Boolean,
    biometricEnabled: Boolean,
    onSetupPin: () -> Unit,
    onFinish: () -> Unit,
    onBiometricToggle: (Boolean) -> Unit,
) {
    val colors = MM.colors
    val type = MM.type

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bg),
    ) {
        // Header
        Column(
            modifier = Modifier
                .statusBarsPadding()
                .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 24.dp),
        ) {
            Text(
                text = stringResource(Res.string.onboarding_security_title),
                style = type.title1,
                color = colors.text,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = stringResource(Res.string.onboarding_security_subtitle),
                style = type.body.copy(color = colors.text2),
            )
        }

        // Security card
        MmCard(Modifier.padding(horizontal = 16.dp)) {
            // PIN toggle row
            MmRow(divider = biometricAvailable && pinEnabled) {
                Icon(
                    imageVector = MmIcons.lock,
                    contentDescription = null,
                    tint = colors.text,
                    modifier = Modifier.size(18.dp),
                )
                Column(Modifier.weight(1f)) {
                    Text(
                        text = stringResource(Res.string.onboarding_pin_label),
                        style = type.body,
                        color = colors.text,
                    )
                    if (pinEnabled) {
                        Text(
                            text = stringResource(Res.string.onboarding_pin_set),
                            style = type.caption.copy(color = colors.accent),
                        )
                    }
                }
                MmToggle(
                    checked = pinEnabled,
                    onCheckedChange = { if (it && !pinEnabled) onSetupPin() },
                )
            }

            // Biometrics toggle row — shown only when hardware is available and PIN is set
            if (biometricAvailable && pinEnabled) {
                MmRow(
                    divider = false,
                    modifier = Modifier.alpha(if (pinEnabled) 1f else 0.45f),
                ) {
                    Icon(
                        imageVector = MmIcons.fingerprint,
                        contentDescription = null,
                        tint = colors.text,
                        modifier = Modifier.size(18.dp),
                    )
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = stringResource(Res.string.onboarding_biometrics_label),
                            style = type.body,
                            color = colors.text,
                        )
                        Text(
                            text = stringResource(Res.string.onboarding_biometrics_subtitle),
                            style = type.caption.copy(color = colors.text2),
                        )
                    }
                    MmToggle(
                        checked = biometricEnabled,
                        onCheckedChange = { onBiometricToggle(it) },
                        enabled = pinEnabled,
                    )
                }
            }
        }

        Spacer(Modifier.weight(1f))

        // Bottom buttons
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            MmButton(
                text = stringResource(if (pinEnabled) Res.string.onboarding_done else Res.string.onboarding_skip),
                onClick = onFinish,
                variant = MmButtonVariant.Primary,
                size = MmButtonSize.Lg,
                fullWidth = true,
            )
        }
    }
}
