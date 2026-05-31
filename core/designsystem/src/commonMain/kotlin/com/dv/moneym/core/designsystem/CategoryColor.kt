package com.dv.moneym.core.designsystem

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

// Design palette — same in light and dark mode
val defaultCategoryColors = listOf(
    Color(0xFFF4743B),
    Color(0xFFC97A4F),
    Color(0xFFD88B33),
    Color(0xFFFF9F1C),
    Color(0xFFB89148),
    Color(0xFFF4B400),
    Color(0xFFA8C63A),
    Color(0xFF7A9572),
    Color(0xFF2EA84F),
    Color(0xFF4A8E5C),
    Color(0xFF3F9E70),
    Color(0xFF12B5A5),
    Color(0xFF1CA7C9),
    Color(0xFF4F8694),
    Color(0xFF3A82A5),
    Color(0xFF5A7BA8),
    Color(0xFF2D6CDF),
    Color(0xFF6B5BC4),
    Color(0xFF5A3FC0),
    Color(0xFF8B6FB0),
    Color(0xFF9B51E0),
    Color(0xFFB07089),
    Color(0xFFE84B8A),
    Color(0xFFD14C7A),
    Color(0xFFC2566B),
    Color(0xFFE63946),
    Color(0xFF8A8A8A),
)

private val fallbackColor = Color(0xFF8A8A8A)

fun categoryColor(hex: String): Color {
    val stripped = hex.trimStart('#')
    return try {
        when (stripped.length) {
            6 -> Color(("FF$stripped").toLong(16))
            8 -> Color(stripped.toLong(16))
            else -> fallbackColor
        }
    } catch (_: NumberFormatException) {
        fallbackColor
    }
}

// Black or white, whichever is readable on the given background.
fun onColorFor(background: Color): Color =
    if (background.luminance() > 0.5f) Color.Black else Color.White
