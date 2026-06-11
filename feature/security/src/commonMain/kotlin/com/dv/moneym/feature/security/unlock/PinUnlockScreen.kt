package com.dv.moneym.feature.security.unlock

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.keyframes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dv.moneym.core.designsystem.MM
import com.dv.moneym.core.designsystem.MoneyMTheme
import com.dv.moneym.feature.security.shared.AppLockup
import com.dv.moneym.feature.security.shared.PinDots
import com.dv.moneym.feature.security.shared.PinKeypad
import kotlinx.coroutines.launch
import com.dv.moneym.feature.security.PinError
import moneym.feature.security.generated.resources.Res
import moneym.feature.security.generated.resources.security_app_name
import moneym.feature.security.generated.resources.security_backoff_retry
import moneym.feature.security.generated.resources.security_biometric_unlock_prompt
import moneym.feature.security.generated.resources.security_pin_enter_header
import moneym.feature.security.generated.resources.security_pin_incorrect
import moneym.feature.security.generated.resources.security_pin_mismatch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import kotlin.math.roundToInt

@Composable
fun PinUnlockScreen(
    onUnlocked: () -> Unit,
    viewModel: PinUnlockViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val biometricPrompt = stringResource(Res.string.security_biometric_unlock_prompt)

    LaunchedEffect(viewModel) {
        viewModel.onIntent(PinUnlockIntent.SetBiometricPrompt(biometricPrompt))
        viewModel.effects.collect { if (it == PinUnlockEffect.Unlocked) onUnlocked() }
    }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.onIntent(PinUnlockIntent.Resume)
    }

    PinUnlockContent(
        state = state,
        onIntent = viewModel::onIntent,
        biometricPrompt = biometricPrompt,
    )
}

@Composable
private fun PinUnlockContent(
    state: PinUnlockUiState,
    onIntent: (PinUnlockIntent) -> Unit,
    biometricPrompt: String,
) {
    val colors = MM.colors
    val type = MM.type

    val shakeOffset = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()

    val errorText = when (state.error) {
        is PinError.IncorrectPin -> stringResource(Res.string.security_pin_incorrect)
        is PinError.PinsMismatch -> stringResource(Res.string.security_pin_mismatch)
        null -> null
    }

    // Trigger shake on error
    val errorSnapshot = state.error
    LaunchedEffect(errorSnapshot) {
        if (errorSnapshot != null) {
            scope.launch {
                shakeOffset.animateTo(
                    targetValue = 0f,
                    animationSpec = keyframes {
                        durationMillis = 300
                        8f at 50
                        -8f at 100
                        8f at 150
                        -8f at 200
                        0f at 300
                    },
                )
            }
        }
    }

    val subtitleText = if (state.backoffRemainingMs > 0) {
        val seconds = (state.backoffRemainingMs / 1000) + 1
        stringResource(Res.string.security_backoff_retry, seconds)
    } else {
        null
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bg)
            .statusBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        // App lockup — shared composable
        AppLockup(colors = colors, type = type)

        Spacer(modifier = Modifier.height(MM.dimen.padding_1_5x))

        Text(
            text = stringResource(Res.string.security_app_name),
            style = type.title2,
            color = colors.text,
        )

        Spacer(modifier = Modifier.height(MM.dimen.padding_0_5x))

        // Prominent "Enter your PIN" title
        Text(
            text = stringResource(Res.string.security_pin_enter_header),
            style = type.body,
            color = colors.text,
        )

        // Backoff or error subtitle
        val displaySubtitle = subtitleText ?: errorText
        if (displaySubtitle != null) {
            Spacer(modifier = Modifier.height(MM.dimen.padding_0_5x))
            Text(
                text = displaySubtitle,
                style = type.caption.copy(color = colors.danger),
            )
        }

        Spacer(modifier = Modifier.height(MM.dimen.padding_6x))

        // 4 dots with shake animation — shared composable
        PinDots(
            filledCount = state.pin.length,
            shakeOffsetPx = shakeOffset.value.roundToInt(),
            colors = colors,
        )

        Spacer(modifier = Modifier.height(MM.dimen.padding_6x))

        PinKeypad(
            onKey = { char -> onIntent(PinUnlockIntent.DigitPressed(char.digitToInt())) },
            onDelete = { onIntent(PinUnlockIntent.DeletePressed) },
            onBiometric = if (state.biometricAvailable) {
                { onIntent(PinUnlockIntent.BiometricRequested(biometricPrompt)) }
            } else {
                null
            },
            biometryType = state.biometryType,
        )
    }
}

@Preview
@Composable
private fun PinUnlockContentPreview() {
    MoneyMTheme {
        PinUnlockContent(
            state = PinUnlockUiState(pin = "12"),
            onIntent = {},
            biometricPrompt = "Unlock MoneyM",
        )
    }
}
