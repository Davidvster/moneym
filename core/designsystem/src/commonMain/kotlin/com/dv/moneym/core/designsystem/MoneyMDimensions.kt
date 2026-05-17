package com.dv.moneym.core.designsystem

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Immutable
data class MoneyMDimensions(
    val padding_0_25x: Dp = 2.dp,
    val padding_0_5x: Dp = 4.dp,
    val padding_1x: Dp = 8.dp,
    val padding_1_25x: Dp = 10.dp,
    val padding_1_5x: Dp = 12.dp,
    val padding_2x: Dp = 16.dp,
    val padding_2_5x: Dp = 20.dp,
    val padding_3x: Dp = 24.dp,
    val padding_4x: Dp = 32.dp,
    val padding_5x: Dp = 40.dp,
    val padding_6x: Dp = 48.dp,
    val padding_7x: Dp = 56.dp,
    val padding_8x: Dp = 64.dp,
    val padding_9x: Dp = 72.dp,
    val padding_10x: Dp = 80.dp,

    val radius_0_5x: RoundedCornerShape = RoundedCornerShape(4.dp),
    val radius_1x: RoundedCornerShape = RoundedCornerShape(8.dp),
    val radius_1_5x: RoundedCornerShape = RoundedCornerShape(12.dp),
    val radius_2x: RoundedCornerShape = RoundedCornerShape(16.dp),
    val radius_2_5x: RoundedCornerShape = RoundedCornerShape(20.dp),
    val radius_3x: RoundedCornerShape = RoundedCornerShape(24.dp),
    val pill: RoundedCornerShape = RoundedCornerShape(50),

    val icon_1x: Dp = 18.dp,

    val donutWidth: Dp = 18.dp
)

val LocalMoneyMDimensions = staticCompositionLocalOf { MoneyMDimensions() }
