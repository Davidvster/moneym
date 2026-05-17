package com.dv.moneym.core.designsystem

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.dp

@Immutable
data class MoneyMRadius(
    val radius_0_5x: RoundedCornerShape = RoundedCornerShape(4.dp),
    val radius_1x: RoundedCornerShape = RoundedCornerShape(8.dp),
    val radius_1_5x: RoundedCornerShape = RoundedCornerShape(12.dp),
    val radius_2x: RoundedCornerShape = RoundedCornerShape(16.dp),
    val radius_2_5x: RoundedCornerShape = RoundedCornerShape(20.dp),
    val radius_3x: RoundedCornerShape = RoundedCornerShape(24.dp),
    val pill: RoundedCornerShape = RoundedCornerShape(50),
)

val LocalMoneyMRadius = staticCompositionLocalOf { MoneyMRadius() }
