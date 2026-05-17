package com.dv.moneym.feature.overview.ui.components

import androidx.compose.ui.unit.dp

internal val CHART_HEIGHT = 120.dp
internal val YAXIS_WIDTH = 44.dp

internal fun formatAmount(value: Double): String {
    val abs = kotlin.math.abs(value)
    val intPart = abs.toLong()
    val decPart = kotlin.math.round((abs - intPart) * 100).toInt()
    val intFormatted = buildString {
        val s = intPart.toString()
        var count = 0
        for (i in s.indices.reversed()) {
            if (count > 0 && count % 3 == 0) insert(0, ',')
            insert(0, s[i])
            count++
        }
    }
    return "$intFormatted.${decPart.toString().padStart(2, '0')}"
}

internal fun formatAxisAmount(value: Double): String {
    return if (value >= 1000) "${(value / 1000).toInt()}k" else value.toInt().toString()
}

internal fun formatBarAmount(value: Double): String {
    return if (value >= 1000) {
        val k = (value / 100).toInt() / 10.0
        "${k}k"
    } else {
        value.toInt().toString()
    }
}

internal fun formatShortDate(year: Int, month: Int, day: Int): String {
    return "${day.toString().padStart(2, '0')}.${month.toString().padStart(2, '0')}.${year % 100}"
}

internal fun daysInMonthUi(year: Int, month: Int): Int {
    val first = kotlinx.datetime.LocalDate(year, month, 1)
    val next = if (month == 12) kotlinx.datetime.LocalDate(year + 1, 1, 1)
    else kotlinx.datetime.LocalDate(year, month + 1, 1)
    return (next.toEpochDays() - first.toEpochDays()).toInt()
}
