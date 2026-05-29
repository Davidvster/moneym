package com.dv.moneym.core.designsystem

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

// Design palette — same in light and dark mode
val defaultCategoryColors = listOf(
    Color(0xFFC2566B), // Health
    Color(0xFF8B6FB0), // Entertainment
    Color(0xFF4A8E5C), // Salary
    Color(0xFF4F8694), // Transport
    Color(0xFFB89148), // Utilities
    Color(0xFF7A9572), // Groceries
    Color(0xFFC97A4F), // Eating out
    Color(0xFF5A7BA8), // Rent
    Color(0xFFB07089), // Shopping
    Color(0xFF8A8A8A), // Other
    Color(0xFFD14C7A),
    Color(0xFF6B5BC4),
    Color(0xFF3F9E70),
    Color(0xFF3A82A5),
    Color(0xFFD88B33),
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
