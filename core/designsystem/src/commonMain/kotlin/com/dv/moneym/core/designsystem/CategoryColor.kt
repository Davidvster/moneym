package com.dv.moneym.core.designsystem

import androidx.compose.ui.graphics.Color

val defaultCategoryColors = listOf(
    Color(0xFF7E9C8C), Color(0xFFC97B57), Color(0xFF5F6F8A), Color(0xFF3B7080),
    Color(0xFFB89A4B), Color(0xFF9B5C7D), Color(0xFF7C5C9B), Color(0xFF6D6D6D),
    Color(0xFF4A7A56), Color(0xFFB0623B), Color(0xFF4D6E92), Color(0xFF8A8A8A),
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
