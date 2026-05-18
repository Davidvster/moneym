package com.dv.moneym.core.common

import platform.Foundation.NSNumber
import platform.Foundation.NSNumberFormatter
import platform.Foundation.NSNumberFormatterDecimalStyle
import platform.Foundation.NSLocale
import platform.Foundation.currentLocale
import kotlin.math.abs

actual fun formatNumber(value: Double, decimals: Int): String {
    val formatter = NSNumberFormatter().apply {
        numberStyle = NSNumberFormatterDecimalStyle
        locale = NSLocale.currentLocale
        minimumFractionDigits = decimals.toULong()
        maximumFractionDigits = decimals.toULong()
    }
    return formatter.stringFromNumber(NSNumber(double = abs(value))) ?: abs(value).toString()
}
