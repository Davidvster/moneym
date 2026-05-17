package com.dv.moneym.feature.onboarding.ui.components

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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import com.dv.moneym.core.designsystem.MM
import com.dv.moneym.core.ui.MmButton
import com.dv.moneym.core.ui.MmButtonSize
import com.dv.moneym.core.ui.MmButtonVariant
import com.dv.moneym.core.ui.MmCard
import com.dv.moneym.core.ui.MmIcons
import com.dv.moneym.core.ui.MmRow
import com.dv.moneym.core.ui.MmToggle
import com.dv.moneym.feature.onboarding.presentation.OnboardingSecurityEffect
import com.dv.moneym.feature.onboarding.presentation.OnboardingSecurityIntent
import com.dv.moneym.feature.onboarding.presentation.OnboardingSecurityViewModel
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

@Composable
internal fun OnboardingSecurityScreen(
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
                .padding(start = MM.space.padding_2_5x, end = MM.space.padding_2_5x, top = MM.space.padding_2x, bottom = 24.dp),
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
        MmCard(Modifier.padding(horizontal = MM.space.padding_2x)) {
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
            modifier = Modifier.padding(horizontal = MM.space.padding_2x, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(MM.space.padding_1x),
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
