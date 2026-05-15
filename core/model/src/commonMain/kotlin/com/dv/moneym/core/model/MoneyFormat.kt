package com.dv.moneym.core.model

fun Money.format(): String {
    val negative = minorUnits < 0
    val abs = if (negative) -minorUnits else minorUnits
    val major = abs / 100
    val cents = abs % 100
    val sign = if (negative) "-" else ""
    return "$sign${currency.value} $major.${cents.toString().padStart(2, '0')}"
}

fun String.toMinorUnits(): Long? {
    val cleaned = trim().replace(",", ".")
    if (cleaned.isEmpty()) return null
    return try {
        if ('.' in cleaned) {
            val parts = cleaned.split(".")
            val major = parts[0].toLong()
            val cents = parts.getOrElse(1) { "0" }.padEnd(2, '0').take(2).toLong()
            major * 100 + cents
        } else {
            cleaned.toLong() * 100
        }
    } catch (_: NumberFormatException) {
        null
    }
}

fun Long.toAmountText(): String {
    val abs = if (this < 0) -this else this
    val major = abs / 100
    val cents = abs % 100
    return "$major.${cents.toString().padStart(2, '0')}"
}
