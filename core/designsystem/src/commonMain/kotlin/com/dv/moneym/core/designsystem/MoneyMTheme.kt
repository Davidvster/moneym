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
    val colorScheme = if (darkTheme) darkMoneyMColorScheme() else lightMoneyMColorScheme()
    CompositionLocalProvider(LocalMoneyMSpacing provides MoneyMSpacing()) {
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
}
