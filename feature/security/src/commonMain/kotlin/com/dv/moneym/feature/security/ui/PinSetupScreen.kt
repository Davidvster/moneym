package com.dv.moneym.feature.security.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.keyframes
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.dv.moneym.core.designsystem.MM
import com.dv.moneym.core.navigation.ModalKey
import com.dv.moneym.core.security.BiometryType
import com.dv.moneym.core.ui.MmButton
import com.dv.moneym.core.ui.MmButtonSize
import com.dv.moneym.core.ui.MmButtonVariant
import com.dv.moneym.core.ui.MmIcons
import com.dv.moneym.feature.security.presentation.PinSetupEffect
import com.dv.moneym.feature.security.presentation.PinSetupIntent
import com.dv.moneym.feature.security.presentation.PinSetupStep
import com.dv.moneym.feature.security.presentation.PinSetupUiState
import com.dv.moneym.feature.security.presentation.PinSetupViewModel
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import moneym.feature.security.generated.resources.Res
import moneym.feature.security.generated.resources.security_biometric_offer_body
import moneym.feature.security.generated.resources.security_biometric_offer_enable
import moneym.feature.security.generated.resources.security_biometric_offer_skip
import moneym.feature.security.generated.resources.security_biometric_offer_title
import moneym.feature.security.generated.resources.security_cancel
import moneym.feature.security.generated.resources.security_change_pin_title
import moneym.feature.security.generated.resources.security_pin_confirm_header
import moneym.feature.security.generated.resources.security_pin_set_header
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import kotlin.math.roundToInt

/**
 * NavKey for the PIN setup screen.
 * @param isChangePinFlow true if opened from Settings to change an existing PIN
 */
@Serializable
data class PinSetupKey(val isChangePinFlow: Boolean = false) : ModalKey

fun EntryProviderScope<NavKey>.pinSetupEntry(
    onDone: () -> Unit,
    metadata: Map<String, Any> = emptyMap(),
) = entry<PinSetupKey>(metadata = metadata) { key ->
    PinSetupScreen(onDone = onDone, isChangePinFlow = key.isChangePinFlow)
}

@Composable
fun PinSetupScreen(
    onDone: () -> Unit,
    isChangePinFlow: Boolean = false,
    viewModel: PinSetupViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var showBiometricOffer by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.reset()
        viewModel.effects.collect { effect ->
            when (effect) {
                PinSetupEffect.Done -> onDone()
                PinSetupEffect.OfferBiometrics -> showBiometricOffer = true
            }
        }
    }

    if (showBiometricOffer) {
        BiometricOfferDialog(
            biometryType = state.biometryType,
            onAccept = {
                showBiometricOffer = false
                viewModel.onIntent(PinSetupIntent.BiometricOfferAccepted)
            },
            onDecline = {
                showBiometricOffer = false
                viewModel.onIntent(PinSetupIntent.BiometricOfferDeclined)
            },
        )
    }

    PinSetupContent(
        state = state,
        isChangePinFlow = isChangePinFlow,
        onIntent = viewModel::onIntent,
        onDismiss = onDone,
    )
}

@Composable
private fun BiometricOfferDialog(
    biometryType: BiometryType,
    onAccept: () -> Unit,
    onDecline: () -> Unit,
) {
    val colors = MM.colors
    val type = MM.type

    val bodyText = when (biometryType) {
        BiometryType.FaceId -> stringResource(Res.string.security_biometric_offer_body)
        else -> stringResource(Res.string.security_biometric_offer_body)
    }

    AlertDialog(
        onDismissRequest = onDecline,
        containerColor = colors.surface,
        shape = RoundedCornerShape(MM.space.padding_2x),
        title = {
            Text(
                text = stringResource(Res.string.security_biometric_offer_title),
                style = type.title2,
                color = colors.text,
            )
        },
        text = {
            Text(
                text = bodyText,
                style = type.body.copy(color = colors.text2),
            )
        },
        confirmButton = {
            MmButton(
                text = stringResource(Res.string.security_biometric_offer_enable),
                onClick = onAccept,
                variant = MmButtonVariant.Primary,
                size = MmButtonSize.Md,
            )
        },
        dismissButton = {
            MmButton(
                text = stringResource(Res.string.security_biometric_offer_skip),
                onClick = onDecline,
                variant = MmButtonVariant.Ghost,
                size = MmButtonSize.Md,
            )
        },
    )
}

@Composable
private fun PinSetupContent(
    state: PinSetupUiState,
    isChangePinFlow: Boolean,
    onIntent: (PinSetupIntent) -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = MM.colors

    val filledCount = if (state.step == PinSetupStep.ENTER_FIRST) state.firstPin.length else state.secondPin.length

    val shakeOffset = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()

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

    val cancelLabel = stringResource(Res.string.security_cancel)

    // Determine screen title based on flow and step
    val screenTitle = when {
        isChangePinFlow -> stringResource(Res.string.security_change_pin_title)
        state.step == PinSetupStep.ENTER_FIRST -> stringResource(Res.string.security_pin_set_header)
        else -> stringResource(Res.string.security_pin_confirm_header)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bg),
    ) {
        // Close button top-right (respects status bar)
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(top = MM.space.padding_1x, end = MM.space.padding_2x)
                .size(MM.space.padding_5x)
                .pointerInput(Unit) {
                    detectTapGestures(onTap = { onDismiss() })
                },
            contentAlignment = Alignment.Center,
        ) {
            val painter = androidx.compose.ui.graphics.vector.rememberVectorPainter(MmIcons.close)
            androidx.compose.foundation.Image(
                painter = painter,
                contentDescription = cancelLabel,
                modifier = Modifier.size(MM.space.padding_2_5x),
                colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(colors.text),
            )
        }

        PinSetupBody(
            screenTitle = screenTitle,
            errorText = state.error,
            filledCount = filledCount,
            shakeOffsetPx = shakeOffset.value.roundToInt(),
            onIntent = onIntent,
            colors = colors,
        )
    }
}

@Composable
private fun PinSetupBody(
    screenTitle: String,
    errorText: String?,
    filledCount: Int,
    shakeOffsetPx: Int,
    onIntent: (PinSetupIntent) -> Unit,
    colors: com.dv.moneym.core.designsystem.MoneyMColors,
) {
    val type = MM.type

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        AppLockup(colors = colors, type = type)

        Spacer(modifier = Modifier.height(MM.space.padding_1_5x))

        Text(
            text = "MoneyM",
            style = type.title2,
            color = colors.text,
        )

        Spacer(modifier = Modifier.height(4.dp))

        // Prominent screen title
        Text(
            text = screenTitle,
            style = type.title1,
            color = colors.text,
        )

        if (errorText != null) {
            Spacer(modifier = Modifier.height(MM.space.padding_1x))
            Text(
                text = errorText,
                style = type.caption.copy(color = colors.danger),
            )
        }

        Spacer(modifier = Modifier.height(MM.space.padding_6x))

        PinDots(
            filledCount = filledCount,
            shakeOffsetPx = shakeOffsetPx,
            colors = colors,
        )

        Spacer(modifier = Modifier.height(MM.space.padding_6x))

        PinKeypad(
            onKey = { char -> onIntent(PinSetupIntent.DigitPressed(char.digitToInt())) },
            onDelete = { onIntent(PinSetupIntent.DeletePressed) },
            onBiometric = null,
        )
    }
}
