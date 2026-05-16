package com.dv.moneym.core.designsystem

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// ─── Semantic token layer ────────────────────────────────────

@Immutable
data class MoneyMColors(
    val bg: Color,
    val surface: Color,
    val surface2: Color,
    val border: Color,
    val borderStrong: Color,
    val divider: Color,
    val text: Color,
    val text2: Color,
    val text3: Color,
    val accent: Color = Color(0xFF16A34A),
    val danger: Color = Color(0xFFDC2626),
    // Category palette — same in both modes
    val catHealth: Color        = Color(0xFFC2566B),
    val catEntertainment: Color = Color(0xFF8B6FB0),
    val catSalary: Color        = Color(0xFF4A8E5C),
    val catTransport: Color     = Color(0xFF4F8694),
    val catUtilities: Color     = Color(0xFFB89148),
    val catGroceries: Color     = Color(0xFF7A9572),
    val catEatingOut: Color     = Color(0xFFC97A4F),
    val catRent: Color          = Color(0xFF5A7BA8),
    val catShopping: Color      = Color(0xFFB07089),
    val catOther: Color         = Color(0xFF8A8A8A),
)

val MoneyMLight = MoneyMColors(
    bg           = Color(0xFFFFFFFF),
    surface      = Color(0xFFFAFAFA),
    surface2     = Color(0xFFF4F4F4),
    border       = Color(0xFFECECEC),
    borderStrong = Color(0xFFD4D4D4),
    divider      = Color(0xFFF0F0F0),
    text         = Color(0xFF0A0A0A),
    text2        = Color(0xFF6B6B6B),
    text3        = Color(0xFFA3A3A3),
)

val MoneyMDark = MoneyMColors(
    bg           = Color(0xFF0A0A0A),
    surface      = Color(0xFF141414),
    surface2     = Color(0xFF1C1C1C),
    border       = Color(0xFF232323),
    borderStrong = Color(0xFF353535),
    divider      = Color(0xFF1E1E1E),
    text         = Color(0xFFFAFAFA),
    text2        = Color(0xFFA3A3A3),
    text3        = Color(0xFF6B6B6B),
)

val LocalMoneyMColors = staticCompositionLocalOf<MoneyMColors> { MoneyMLight }
