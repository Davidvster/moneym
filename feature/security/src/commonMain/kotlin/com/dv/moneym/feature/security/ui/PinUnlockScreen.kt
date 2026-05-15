package com.dv.moneym.feature.security.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.dv.moneym.core.designsystem.MoneyMTheme
import com.dv.moneym.feature.security.presentation.PinUnlockEffect
import com.dv.moneym.feature.security.presentation.PinUnlockIntent
import com.dv.moneym.feature.security.presentation.PinUnlockViewModel
import moneym.feature.security.generated.resources.Res
import moneym.feature.security.generated.resources.*
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun PinUnlockScreen(
    onUnlocked: () -> Unit,
    viewModel: PinUnlockViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsState()
    LaunchedEffect(viewModel) {
        viewModel.effects.collect { if (it == PinUnlockEffect.Unlocked) onUnlocked() }
    }

    val sp = MoneyMTheme.spacing
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = sp.xxl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(stringResource(Res.string.security_app_name), style = MaterialTheme.typography.headlineLarge)
        Spacer(Modifier.height(sp.sm))
        Text(
            stringResource(Res.string.security_pin_enter_title),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        when {
            state.error != null -> {
                Spacer(Modifier.height(sp.sm))
                Text(state.error!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
            }
            state.backoffRemainingMs > 0 -> {
                Spacer(Modifier.height(sp.sm))
                val seconds = (state.backoffRemainingMs / 1000) + 1
                Text(
                    stringResource(Res.string.security_backoff_retry, seconds),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
        Spacer(Modifier.height(sp.xxl))
        PinDots(length = 4, filled = state.pin.length, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(sp.xxl))
        PinKeypad(
            onDigit = { viewModel.onIntent(PinUnlockIntent.DigitPressed(it)) },
            onDelete = { viewModel.onIntent(PinUnlockIntent.DeletePressed) },
            enabled = state.backoffRemainingMs <= 0 && !state.isVerifying,
            modifier = Modifier.fillMaxWidth(),
        )
        if (state.biometricAvailable) {
            Spacer(Modifier.height(sp.xl))
            TextButton(onClick = { viewModel.onIntent(PinUnlockIntent.BiometricRequested) }) {
                Text(stringResource(Res.string.security_biometric_use))
            }
        }
    }
}
