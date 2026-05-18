package com.dv.moneym.core.common

import java.text.NumberFormat
import java.util.Locale
import kotlin.math.abs

actual fun formatNumber(value: Double, decimals: Int): String {
    val fmt = NumberFormat.getNumberInstance(Locale.getDefault())
    fmt.minimumFractionDigits = decimals
    fmt.maximumFractionDigits = decimals
    return fmt.format(abs(value))
}
