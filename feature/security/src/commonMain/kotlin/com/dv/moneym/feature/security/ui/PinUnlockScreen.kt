package com.dv.moneym.feature.security.ui

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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dv.moneym.core.designsystem.MM
import com.dv.moneym.feature.security.presentation.PinUnlockEffect
import com.dv.moneym.feature.security.presentation.PinUnlockIntent
import com.dv.moneym.feature.security.presentation.PinUnlockUiState
import com.dv.moneym.feature.security.presentation.PinUnlockViewModel
import kotlinx.coroutines.launch
import moneym.feature.security.generated.resources.Res
import moneym.feature.security.generated.resources.security_backoff_retry
import moneym.feature.security.generated.resources.security_pin_enter_header
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import kotlin.math.roundToInt

@Composable
fun PinUnlockScreen(
    onUnlocked: () -> Unit,
    viewModel: PinUnlockViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsState()
    LaunchedEffect(viewModel) {
        viewModel.effects.collect { if (it == PinUnlockEffect.Unlocked) onUnlocked() }
    }

    PinUnlockContent(
        state = state,
        onIntent = viewModel::onIntent,
    )
}

@Composable
private fun PinUnlockContent(
    state: PinUnlockUiState,
    onIntent: (PinUnlockIntent) -> Unit,
) {
    val colors = MM.colors
    val type = MM.type

    val shakeOffset = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()

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

        Spacer(modifier = Modifier.height(MM.space.padding_1_5x))

        Text(
            text = "MoneyM",
            style = type.title2,
            color = colors.text,
        )

        Spacer(modifier = Modifier.height(4.dp))

        // Prominent "Enter your PIN" title
        Text(
            text = stringResource(Res.string.security_pin_enter_header),
            style = type.title1,
            color = colors.text,
        )

        // Backoff subtitle (shown only when locked out)
        if (subtitleText != null) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtitleText,
                style = type.caption.copy(color = colors.danger),
            )
        }

        Spacer(modifier = Modifier.height(MM.space.padding_6x))

        // 4 dots with shake animation — shared composable
        PinDots(
            filledCount = state.pin.length,
            shakeOffsetPx = shakeOffset.value.roundToInt(),
            colors = colors,
        )

        Spacer(modifier = Modifier.height(MM.space.padding_6x))

        PinKeypad(
            onKey = { char -> onIntent(PinUnlockIntent.DigitPressed(char.digitToInt())) },
            onDelete = { onIntent(PinUnlockIntent.DeletePressed) },
            onBiometric = if (state.biometricAvailable) {
                { onIntent(PinUnlockIntent.BiometricRequested) }
            } else {
                null
            },
            biometryType = state.biometryType,
        )
    }
}
