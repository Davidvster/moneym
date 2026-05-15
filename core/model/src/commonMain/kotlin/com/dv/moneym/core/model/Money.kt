package com.dv.moneym.core.model

import kotlinx.serialization.Serializable

@Serializable data class CurrencyCode(val value: String)

@Serializable
data class Money(
    val minorUnits: Long,
    val currency: CurrencyCode,
) {
    operator fun plus(other: Money): Money {
        require(currency == other.currency) { "Cannot add different currencies: $currency vs ${other.currency}" }
        return copy(minorUnits = minorUnits + other.minorUnits)
    }

    operator fun unaryMinus(): Money = copy(minorUnits = -minorUnits)

    val isPositive: Boolean get() = minorUnits > 0
    val isNegative: Boolean get() = minorUnits < 0
    val isZero: Boolean get() = minorUnits == 0L

    companion object {
        fun zero(currency: CurrencyCode) = Money(0L, currency)
    }
}
