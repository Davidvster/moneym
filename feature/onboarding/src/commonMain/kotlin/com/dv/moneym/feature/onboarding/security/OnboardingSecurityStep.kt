package com.dv.moneym.feature.onboarding.security

import com.dv.moneym.core.ui.imageVector
import com.dv.moneym.core.model.Icon
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.dv.moneym.core.designsystem.MM
import com.dv.moneym.core.ui.MmButton
import com.dv.moneym.core.ui.MmButtonSize
import com.dv.moneym.core.ui.MmButtonVariant
import com.dv.moneym.core.ui.MmCard
import com.dv.moneym.core.ui.MmRow
import com.dv.moneym.core.ui.MmToggle
import kotlinx.serialization.Serializable
import moneym.feature.onboarding.generated.resources.Res
import moneym.feature.onboarding.generated.resources.onboarding_biometrics_label
import moneym.feature.onboarding.generated.resources.onboarding_biometrics_subtitle
import moneym.feature.onboarding.generated.resources.onboarding_done
import moneym.feature.onboarding.generated.resources.onboarding_pin_label
import moneym.feature.onboarding.generated.resources.onboarding_pin_set
import moneym.feature.onboarding.generated.resources.onboarding_security_subtitle
import moneym.feature.onboarding.generated.resources.onboarding_security_title
import moneym.feature.onboarding.generated.resources.onboarding_skip
import org.jetbrains.compose.resources.stringResource

@Serializable
data object OnboardingSecurityKey : NavKey

fun EntryProviderScope<NavKey>.onboardingSecurityEntry(
    onNavigateToPinSetup: () -> Unit,
    onNavigateToCurrency: () -> Unit,
    viewModel: OnboardingSecurityViewModel,
) = entry<OnboardingSecurityKey> {
    OnboardingSecurityScreen(
        viewModel = viewModel,
        onNavigateToPinSetup = onNavigateToPinSetup,
        onNavigateToCurrency = onNavigateToCurrency,
    )
}

@Composable
private fun OnboardingSecurityScreen(
    viewModel: OnboardingSecurityViewModel,
    onNavigateToPinSetup: () -> Unit,
    onNavigateToCurrency: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                OnboardingSecurityEffect.NavigateToPinSetup -> onNavigateToPinSetup()
                OnboardingSecurityEffect.NavigateToCurrency -> onNavigateToCurrency()
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

@Composable
internal fun SecurityStep(
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
                .padding(
                    start = MM.dimen.padding_2_5x,
                    end = MM.dimen.padding_2_5x,
                    top = MM.dimen.padding_2x,
                    bottom = 24.dp
                ),
        ) {
            Text(
                text = stringResource(Res.string.onboarding_security_title),
                style = type.title1,
                color = colors.text,
            )
            Spacer(Modifier.height(MM.dimen.padding_0_5x))
            Text(
                text = stringResource(Res.string.onboarding_security_subtitle),
                style = type.body.copy(color = colors.text2),
            )
        }

        // Security card
        MmCard(Modifier.padding(horizontal = MM.dimen.padding_2x)) {
            // PIN toggle row
            MmRow(divider = biometricAvailable && pinEnabled) {
                Icon(
                    imageVector = Icon.Lock.imageVector,
                    contentDescription = null,
                    tint = colors.text,
                    modifier = Modifier.size(MM.dimen.icon_1x),
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
                        imageVector = Icon.Fingerprint.imageVector,
                        contentDescription = null,
                        tint = colors.text,
                        modifier = Modifier.size(MM.dimen.icon_1x),
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
            modifier = Modifier.padding(horizontal = MM.dimen.padding_2x, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(MM.dimen.padding_1x),
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
