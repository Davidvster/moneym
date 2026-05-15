package com.dv.moneym.core.designsystem

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// Light palette
private val Light_Background = Color(0xFFFAFAFA)
private val Light_Surface = Color(0xFFFFFFFF)
private val Light_SurfaceVariant = Color(0xFFF2F2F2)
private val Light_OnBackground = Color(0xFF0A0A0A)
private val Light_OnSurface = Color(0xFF0A0A0A)
private val Light_OnSurfaceVariant = Color(0xFF5C5C5C)
private val Light_Outline = Color(0xFFD6D6D6)
private val Light_OutlineVariant = Color(0xFFEAEAEA)
private val Light_Primary = Color(0xFF0A0A0A)
private val Light_OnPrimary = Color(0xFFFFFFFF)
private val Light_Error = Color(0xFF7A1F1F)

// Dark palette
private val Dark_Background = Color(0xFF0B0B0B)
private val Dark_Surface = Color(0xFF141414)
private val Dark_SurfaceVariant = Color(0xFF1E1E1E)
private val Dark_OnBackground = Color(0xFFF2F2F2)
private val Dark_OnSurface = Color(0xFFF2F2F2)
private val Dark_OnSurfaceVariant = Color(0xFF9C9C9C)
private val Dark_Outline = Color(0xFF2E2E2E)
private val Dark_OutlineVariant = Color(0xFF1E1E1E)
private val Dark_Primary = Color(0xFFF2F2F2)
private val Dark_OnPrimary = Color(0xFF0A0A0A)
private val Dark_Error = Color(0xFFC46A6A)

fun lightMoneyMColorScheme(): ColorScheme = lightColorScheme(
    background = Light_Background,
    surface = Light_Surface,
    surfaceVariant = Light_SurfaceVariant,
    onBackground = Light_OnBackground,
    onSurface = Light_OnSurface,
    onSurfaceVariant = Light_OnSurfaceVariant,
    outline = Light_Outline,
    outlineVariant = Light_OutlineVariant,
    primary = Light_Primary,
    onPrimary = Light_OnPrimary,
    error = Light_Error,
    onError = Color(0xFFFFFFFF),
)

fun darkMoneyMColorScheme(): ColorScheme = darkColorScheme(
    background = Dark_Background,
    surface = Dark_Surface,
    surfaceVariant = Dark_SurfaceVariant,
    onBackground = Dark_OnBackground,
    onSurface = Dark_OnSurface,
    onSurfaceVariant = Dark_OnSurfaceVariant,
    outline = Dark_Outline,
    outlineVariant = Dark_OutlineVariant,
    primary = Dark_Primary,
    onPrimary = Dark_OnPrimary,
    error = Dark_Error,
    onError = Color(0xFF141414),
)
