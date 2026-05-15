package com.dv.moneym.core.designsystem

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable

@Composable
fun MoneyMTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val geist = rememberGeist()
    val geistMono = rememberGeistMono()
    val colors = if (darkTheme) MoneyMDark else MoneyMLight
    val colorScheme = if (darkTheme) darkMoneyMColorScheme() else lightMoneyMColorScheme()

    CompositionLocalProvider(
        LocalMoneyMColors  provides colors,
        LocalMoneyMType    provides moneyMType(geist, geistMono),
        LocalMoneyMSpacing provides MoneyMSpacing(),
        LocalMoneyMRadius  provides MoneyMRadius(),
    ) {
        // M3 wrapper kept for existing screens during migration — removed in Phase 8
        MaterialTheme(
            colorScheme = colorScheme,
            typography = moneyMTypography(),
            content = content,
        )
    }
}

object MoneyMTheme {
    val spacing: MoneyMSpacing
        @Composable @ReadOnlyComposable get() = LocalMoneyMSpacing.current

    val colors: MoneyMColors
        @Composable @ReadOnlyComposable get() = LocalMoneyMColors.current

    val type: MoneyMType
        @Composable @ReadOnlyComposable get() = LocalMoneyMType.current

    val radius: MoneyMRadius
        @Composable @ReadOnlyComposable get() = LocalMoneyMRadius.current
}
