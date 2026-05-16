package com.dv.moneym.feature.security.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.keyframes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dv.moneym.core.designsystem.MM
import com.dv.moneym.feature.security.presentation.PinUnlockEffect
import com.dv.moneym.feature.security.presentation.PinUnlockIntent
import com.dv.moneym.feature.security.presentation.PinUnlockUiState
import com.dv.moneym.feature.security.presentation.PinUnlockViewModel
import kotlinx.coroutines.launch
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bg)
            .statusBarsPadding(),
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
            text = if (state.backoffRemainingMs > 0) {
                val seconds = (state.backoffRemainingMs / 1000) + 1
                "Try again in $seconds seconds"
            } else {
                "Enter your PIN"
            },
            style = type.caption.copy(color = colors.text2),
        )

        Spacer(modifier = Modifier.height(48.dp))

        // 4 dots with shake animation
        Row(
            horizontalArrangement = Arrangement.spacedBy(18.dp),
            modifier = Modifier.offset { IntOffset(shakeOffset.value.roundToInt(), 0) },
        ) {
            val filledCount = state.pin.length
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
