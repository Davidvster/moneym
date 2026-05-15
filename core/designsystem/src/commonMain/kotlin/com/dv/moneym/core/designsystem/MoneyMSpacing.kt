package com.dv.moneym.core.designsystem

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Immutable
data class MoneyMSpacing(
    // New canonical names (s1..s12, 4dp base)
    val s1: Dp  = 4.dp,
    val s2: Dp  = 8.dp,
    val s3: Dp  = 12.dp,
    val s4: Dp  = 16.dp,
    val s5: Dp  = 20.dp,
    val s6: Dp  = 24.dp,
    val s8: Dp  = 32.dp,
    val s10: Dp = 40.dp,
    val s12: Dp = 48.dp,
) {
    // Legacy aliases — kept for existing screens during migration
    val xxs: Dp get() = 2.dp
    val xs: Dp  get() = s1
    val sm: Dp  get() = s2
    val md: Dp  get() = s3
    val lg: Dp  get() = s4
    val xl: Dp  get() = s6
    val xxl: Dp get() = s8
}

val LocalMoneyMSpacing = staticCompositionLocalOf { MoneyMSpacing() }
