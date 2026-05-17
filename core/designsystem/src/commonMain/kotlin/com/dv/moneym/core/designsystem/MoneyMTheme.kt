package com.dv.moneym.core.designsystem

import androidx.compose.foundation.isSystemInDarkTheme
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

    CompositionLocalProvider(
        LocalMoneyMColors provides colors,
        LocalMoneyMType provides moneyMType(geist, geistMono),
        LocalMoneyMDimensions provides MoneyMDimensions(),
        content = content,
    )
}

object MoneyMTheme {
    val dimen: MoneyMDimensions
        @Composable @ReadOnlyComposable get() = LocalMoneyMDimensions.current

    val colors: MoneyMColors
        @Composable @ReadOnlyComposable get() = LocalMoneyMColors.current

    val type: MoneyMType
        @Composable @ReadOnlyComposable get() = LocalMoneyMType.current
}
