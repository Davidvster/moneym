package com.dv.moneym.core.designsystem

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Immutable
data class MoneyMSpacing(
    val padding_0_25x: Dp = 2.dp,
    val padding_0_5x: Dp  = 4.dp,
    val padding_1x: Dp    = 8.dp,
    val padding_1_25x: Dp = 10.dp,
    val padding_1_5x: Dp  = 12.dp,
    val padding_2x: Dp    = 16.dp,
    val padding_3x: Dp    = 24.dp,
    val padding_4x: Dp    = 32.dp,
    val padding_5x: Dp    = 40.dp,
    val padding_6x: Dp    = 48.dp,
    val padding_7x: Dp    = 56.dp,
    val padding_8x: Dp    = 64.dp,
    val padding_9x: Dp    = 72.dp,
    val padding_10x: Dp   = 80.dp,
)

val LocalMoneyMSpacing = staticCompositionLocalOf { MoneyMSpacing() }
