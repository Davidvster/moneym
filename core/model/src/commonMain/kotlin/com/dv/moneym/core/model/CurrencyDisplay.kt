package com.dv.moneym.core.model

fun currencyDisplay(code: String, useSymbol: Boolean): String =
    if (useSymbol) CommonCurrencies.firstOrNull { it.code == code }?.symbol ?: code else code
