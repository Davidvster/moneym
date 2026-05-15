package com.dv.moneym.core.designsystem

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf

@Immutable
data class MoneyMRadius(
    val xs: RoundedCornerShape   = RoundedCornerShape(6),
    val sm: RoundedCornerShape   = RoundedCornerShape(8),
    val md: RoundedCornerShape   = RoundedCornerShape(12),
    val lg: RoundedCornerShape   = RoundedCornerShape(16),
    val xl: RoundedCornerShape   = RoundedCornerShape(20),
    val pill: RoundedCornerShape = RoundedCornerShape(50),
)

val LocalMoneyMRadius = staticCompositionLocalOf { MoneyMRadius() }
