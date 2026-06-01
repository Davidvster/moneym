package com.dv.moneym.feature.aianalysis.usecase

import kotlin.math.abs

internal fun formatMinor(minorUnits: Long, currency: String): String {
    val sign = if (minorUnits < 0) "-" else ""
    val absUnits = abs(minorUnits)
    val whole = absUnits / 100
    val cents = (absUnits % 100).toString().padStart(2, '0')
    return "$sign$whole.$cents $currency"
}
