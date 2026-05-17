package com.dv.moneym.core.model

data class CurrencyInfo(
    val code: String,
    val name: String,
    val symbol: String,
    val region: String = "",
)
