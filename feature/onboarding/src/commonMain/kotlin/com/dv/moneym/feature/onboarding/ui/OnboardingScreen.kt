package com.dv.moneym.feature.onboarding.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.dv.moneym.core.designsystem.MoneyMTheme
import com.dv.moneym.feature.onboarding.presentation.OnboardingEffect
import com.dv.moneym.feature.onboarding.presentation.OnboardingIntent
import com.dv.moneym.feature.onboarding.presentation.OnboardingStep
import com.dv.moneym.feature.onboarding.presentation.OnboardingViewModel
import com.dv.moneym.feature.onboarding.presentation.commonCurrencies
import moneym.feature.onboarding.generated.resources.Res
import moneym.feature.onboarding.generated.resources.*
import org.jetbrains.compose.resources.stringResource
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.EntryProviderScope
import kotlinx.serialization.Serializable
import org.koin.compose.viewmodel.koinViewModel

@Serializable data object OnboardingKey : NavKey
@Serializable data object OnboardingPinSetupKey : NavKey

fun EntryProviderScope<NavKey>.onboardingEntry(
    viewModel: com.dv.moneym.feature.onboarding.presentation.OnboardingViewModel,
    onNavigateToPinSetup: () -> Unit,
    onComplete: () -> Unit,
) = entry<OnboardingKey> {
    OnboardingScreen(
        viewModel = viewModel,
        onNavigateToPinSetup = onNavigateToPinSetup,
        onComplete = onComplete,
    )
}

@Composable
fun OnboardingScreen(
    onNavigateToPinSetup: () -> Unit,
    onComplete: () -> Unit,
    viewModel: OnboardingViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsState()
    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                OnboardingEffect.NavigateToPinSetup -> onNavigateToPinSetup()
                OnboardingEffect.Complete -> onComplete()
            }
        }
    }
    when (state.step) {
        OnboardingStep.CURRENCY -> CurrencyStep(
            selected = state.selectedCurrency,
            onSelect = { viewModel.onIntent(OnboardingIntent.CurrencySelected(it)) },
            onContinue = { viewModel.onIntent(OnboardingIntent.ContinueToSecurity) },
        )
        OnboardingStep.SECURITY -> SecurityStep(
            pinEnabled = state.pinEnabled,
            onSetupPin = { viewModel.onIntent(OnboardingIntent.SetupPinRequested) },
            onSkip = { viewModel.onIntent(OnboardingIntent.SkipSecurity) },
            onFinish = { viewModel.onIntent(OnboardingIntent.Finish) },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CurrencyStep(
    selected: String,
    onSelect: (String) -> Unit,
    onContinue: () -> Unit,
) {
    val sp = MoneyMTheme.spacing
    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(Res.string.onboarding_welcome)) }) },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            Text(
                stringResource(Res.string.onboarding_currency_title),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(horizontal = sp.lg, vertical = sp.sm),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(commonCurrencies, key = { it.first }) { (code, name) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(code) }
                            .padding(horizontal = sp.lg, vertical = sp.sm),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(sp.md),
                    ) {
                        RadioButton(selected = code == selected, onClick = { onSelect(code) })
                        Column {
                            Text(code, style = MaterialTheme.typography.bodyLarge)
                            Text(name, style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    HorizontalDivider(modifier = Modifier.padding(horizontal = sp.lg))
                }
            }
            Button(
                onClick = onContinue,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(sp.lg),
            ) { Text(stringResource(Res.string.onboarding_continue)) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SecurityStep(
    pinEnabled: Boolean,
    onSetupPin: () -> Unit,
    onSkip: () -> Unit,
    onFinish: () -> Unit,
) {
    val sp = MoneyMTheme.spacing
    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(Res.string.onboarding_security_title)) }) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = sp.lg),
            verticalArrangement = Arrangement.spacedBy(sp.md),
        ) {
            Spacer(Modifier.height(sp.xl))
            Text(
                stringResource(Res.string.onboarding_security_subtitle),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(sp.md))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(stringResource(Res.string.onboarding_pin_label), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                Switch(checked = pinEnabled, onCheckedChange = { if (it && !pinEnabled) onSetupPin() })
            }
            if (pinEnabled) {
                Text(
                    stringResource(Res.string.onboarding_pin_set),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Spacer(Modifier.weight(1f))
            Button(onClick = onFinish, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(if (pinEnabled) Res.string.onboarding_done else Res.string.onboarding_skip))
            }
            if (!pinEnabled) {
                TextButton(onClick = onSkip, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(Res.string.onboarding_skip_short))
                }
            }
        }
    }
}
