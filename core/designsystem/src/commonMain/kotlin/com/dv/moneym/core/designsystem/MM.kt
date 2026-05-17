package com.dv.moneym.core.designsystem

import androidx.compose.runtime.Composable

object MM {
    val colors: MoneyMColors
        @Composable get() = LocalMoneyMColors.current

    val type: MoneyMType
        @Composable get() = LocalMoneyMType.current

    val dimen: MoneyMDimensions
        @Composable get() = LocalMoneyMDimensions.current
}
