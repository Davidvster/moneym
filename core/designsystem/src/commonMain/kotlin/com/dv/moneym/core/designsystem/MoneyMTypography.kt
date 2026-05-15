package com.dv.moneym.core.designsystem

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

fun moneyMTypography(): Typography = Typography()

// Tabular-figures style for amount columns — apply via Modifier or directly to Text
val AmountTextStyle = TextStyle(
    fontFeatureSettings = "tnum",
    fontWeight = FontWeight.Medium,
    fontSize = 16.sp,
    letterSpacing = 0.sp,
)
