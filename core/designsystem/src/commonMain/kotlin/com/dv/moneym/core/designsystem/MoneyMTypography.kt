package com.dv.moneym.core.designsystem

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

@Immutable
data class MoneyMType(
    val display: TextStyle,
    val title1: TextStyle,
    val title2: TextStyle,
    val title3: TextStyle,
    val body: TextStyle,
    val bodyMono: TextStyle,
    val caption: TextStyle,
    val captionMono: TextStyle,
    val micro: TextStyle,
)

fun moneyMType(geist: FontFamily, geistMono: FontFamily) = MoneyMType(
    display = TextStyle(
        fontFamily = geistMono,
        fontWeight = FontWeight.SemiBold,
        fontSize = 56.sp,
        letterSpacing = (-1.6).sp,
        fontFeatureSettings = "tnum",
    ),
    title1 = TextStyle(
        fontFamily = geist,
        fontWeight = FontWeight.SemiBold,
        fontSize = 28.sp,
        letterSpacing = (-0.6).sp,
    ),
    title2 = TextStyle(
        fontFamily = geist,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        letterSpacing = (-0.4).sp,
    ),
    title3 = TextStyle(
        fontFamily = geist,
        fontWeight = FontWeight.SemiBold,
        fontSize = 17.sp,
        letterSpacing = (-0.2).sp,
    ),
    body = TextStyle(
        fontFamily = geist,
        fontWeight = FontWeight.Medium,
        fontSize = 15.sp,
        letterSpacing = (-0.1).sp,
    ),
    bodyMono = TextStyle(
        fontFamily = geistMono,
        fontWeight = FontWeight.Medium,
        fontSize = 15.sp,
        letterSpacing = (-0.2).sp,
        fontFeatureSettings = "tnum",
    ),
    caption = TextStyle(
        fontFamily = geist,
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp,
    ),
    captionMono = TextStyle(
        fontFamily = geistMono,
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp,
        fontFeatureSettings = "tnum",
    ),
    micro = TextStyle(
        fontFamily = geist,
        fontWeight = FontWeight.SemiBold,
        fontSize = 11.sp,
        letterSpacing = 0.08.sp,
    ),
)

val LocalMoneyMType = staticCompositionLocalOf<MoneyMType> {
    error("MoneyMType not provided — wrap tree in MoneyMTheme { }")
}

val AmountTextStyle = TextStyle(
    fontFeatureSettings = "tnum",
    fontWeight = FontWeight.Medium,
    fontSize = 16.sp,
    letterSpacing = 0.sp,
)
