package com.dv.moneym.feature.security.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dv.moneym.core.designsystem.MoneyMIcons
import com.dv.moneym.core.designsystem.MoneyMTheme
import com.dv.moneym.feature.security.presentation.PinSetupEffect
import com.dv.moneym.feature.security.presentation.PinSetupIntent
import com.dv.moneym.feature.security.presentation.PinSetupStep
import com.dv.moneym.feature.security.presentation.PinSetupViewModel
import moneym.feature.security.generated.resources.Res
import moneym.feature.security.generated.resources.*
import org.jetbrains.compose.resources.stringResource
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.EntryProviderScope
import com.dv.moneym.core.navigation.ModalKey
import kotlinx.serialization.Serializable
import org.koin.compose.viewmodel.koinViewModel

@Serializable data object PinSetupKey : ModalKey

fun EntryProviderScope<NavKey>.pinSetupEntry(onDone: () -> Unit, metadata: Map<String, Any> = emptyMap()) = entry<PinSetupKey>(metadata = metadata) {
    PinSetupScreen(onDone = onDone)
}

@Composable
fun PinSetupScreen(
    onDone: () -> Unit,
    viewModel: PinSetupViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) {
        viewModel.reset()
        viewModel.effects.collect { if (it == PinSetupEffect.Done) onDone() }
    }
    PinSetupContent(
        step = state.step,
        filledCount = if (state.step == PinSetupStep.ENTER_FIRST) state.firstPin.length else state.secondPin.length,
        error = state.error,
        onIntent = viewModel::onIntent,
        onDismiss = onDone,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PinSetupContent(
    step: PinSetupStep,
    filledCount: Int,
    error: String?,
    onIntent: (PinSetupIntent) -> Unit,
    onDismiss: () -> Unit,
) {
    val sp = MoneyMTheme.spacing
    Scaffold(
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(MoneyMIcons.Clear, contentDescription = stringResource(Res.string.security_cancel))
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = sp.xxl),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = stringResource(if (step == PinSetupStep.ENTER_FIRST) Res.string.security_pin_setup_title else Res.string.security_pin_confirm_title),
                style = MaterialTheme.typography.headlineMedium,
            )
            Spacer(Modifier.height(sp.md))
            Text(
                text = stringResource(if (step == PinSetupStep.ENTER_FIRST) Res.string.security_pin_subtitle_first else Res.string.security_pin_subtitle_confirm),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (error != null) {
                Spacer(Modifier.height(sp.sm))
                Text(text = error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
            }
            Spacer(Modifier.height(sp.xxl))
            PinDots(length = 4, filled = filledCount, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(sp.xxl))
            PinKeypad(
                onDigit = { onIntent(PinSetupIntent.DigitPressed(it)) },
                onDelete = { onIntent(PinSetupIntent.DeletePressed) },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
