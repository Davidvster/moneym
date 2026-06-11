package com.dv.moneym.data.banksync.internal

private val ZERO_DECIMAL_CURRENCIES = setOf(
    "BIF", "CLP", "DJF", "GNF", "ISK", "JPY", "KMF", "KRW",
    "PYG", "RWF", "UGX", "VND", "VUV", "XAF", "XOF", "XPF",
)

private val THREE_DECIMAL_CURRENCIES = setOf(
    "BHD", "IQD", "JOD", "KWD", "LYD", "OMR", "TND",
)

internal fun currencyExponent(currency: String): Int = when (currency.uppercase()) {
    in ZERO_DECIMAL_CURRENCIES -> 0
    in THREE_DECIMAL_CURRENCIES -> 3
    else -> 2
}

internal fun parseAmountToMinorUnits(amount: String, currency: String): Long {
    val trimmed = amount.trim()
    require(trimmed.isNotEmpty()) { "empty amount" }
    val negative = trimmed.startsWith("-")
    val unsigned = trimmed.removePrefix("-").removePrefix("+")
    val parts = unsigned.split('.')
    require(parts.size <= 2 && parts.all { p -> p.all { it.isDigit() } } && parts[0].isNotEmpty()) {
        "unparseable amount: $amount"
    }
    val exponent = currencyExponent(currency)
    val whole = parts[0].toLong()
    val fractionRaw = parts.getOrElse(1) { "" }
    require(fractionRaw.length <= exponent) {
        "amount $amount has more decimals than $currency allows ($exponent)"
    }
    val fraction = fractionRaw.padEnd(exponent, '0').ifEmpty { "0" }.toLong()
    var scale = 1L
    repeat(exponent) { scale *= 10 }
    val minor = whole * scale + fraction
    return if (negative) -minor else minor
}
