package com.dv.moneym.feature.security.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.keyframes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.dv.moneym.core.designsystem.MM
import com.dv.moneym.core.navigation.ModalKey
import com.dv.moneym.core.ui.MmIcons
import com.dv.moneym.feature.security.presentation.PinSetupEffect
import com.dv.moneym.feature.security.presentation.PinSetupIntent
import com.dv.moneym.feature.security.presentation.PinSetupStep
import com.dv.moneym.feature.security.presentation.PinSetupUiState
import com.dv.moneym.feature.security.presentation.PinSetupViewModel
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import org.koin.compose.viewmodel.koinViewModel
import kotlin.math.roundToInt

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
        state = state,
        onIntent = viewModel::onIntent,
        onDismiss = onDone,
    )
}

@Composable
private fun PinSetupContent(
    state: PinSetupUiState,
    onIntent: (PinSetupIntent) -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = MM.colors
    val type = MM.type

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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bg),
    ) {
        // Close button top-right
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 16.dp, end = 16.dp)
                .size(40.dp)
                .pointerInput(Unit) {
                    detectTapGestures(onTap = { onDismiss() })
                },
            contentAlignment = Alignment.Center,
        ) {
            val painter = androidx.compose.ui.graphics.vector.rememberVectorPainter(MmIcons.close)
            androidx.compose.foundation.Image(
                painter = painter,
                contentDescription = "Cancel",
                modifier = Modifier.size(20.dp),
                colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(colors.text),
            )
        }

        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            // App lockup
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(colors.text),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "M",
                    style = type.title2.copy(
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.bg,
                    ),
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "MoneyM",
                style = type.title2,
                color = colors.text,
            )

            Text(
                text = if (state.step == PinSetupStep.ENTER_FIRST) {
                    "Create a PIN"
                } else {
                    "Confirm your PIN"
                },
                style = type.caption.copy(color = colors.text2),
            )

            if (state.error != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = state.error,
                    style = type.caption.copy(color = colors.danger),
                )
            }

            Spacer(modifier = Modifier.height(48.dp))

            // 4 dots with shake
            Row(
                horizontalArrangement = Arrangement.spacedBy(18.dp),
                modifier = Modifier.offset { IntOffset(shakeOffset.value.roundToInt(), 0) },
            ) {
                repeat(4) { i ->
                    val filled = i < filledCount
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(
                                if (filled) colors.text else Color.Transparent,
                                CircleShape,
                            )
                            .border(
                                width = 1.5.dp,
                                color = if (filled) colors.text else colors.borderStrong,
                                shape = CircleShape,
                            ),
                    )
                }
            }

            Spacer(modifier = Modifier.height(48.dp))

            PinKeypad(
                onKey = { char -> onIntent(PinSetupIntent.DigitPressed(char.digitToInt())) },
                onDelete = { onIntent(PinSetupIntent.DeletePressed) },
                onBiometric = null,
            )
        }
    }
}
