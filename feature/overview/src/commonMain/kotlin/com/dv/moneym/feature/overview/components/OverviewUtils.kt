package com.dv.moneym.feature.overview.components

import androidx.compose.ui.unit.dp
import com.dv.moneym.core.common.DateStyle
import com.dv.moneym.core.common.formatDate
import com.dv.moneym.core.common.formatNumber
import kotlinx.datetime.LocalDate

internal val CHART_HEIGHT = 120.dp
internal val YAXIS_WIDTH = 44.dp

internal fun formatAmount(value: Double): String = formatNumber(kotlin.math.abs(value), 2)

internal fun formatAxisAmount(value: Double): String {
    return if (value >= 1000) "${(value / 1000).toInt()}k" else value.toInt().toString()
}

internal fun formatShortDate(year: Int, month: Int, day: Int): String {
    return formatDate(LocalDate(year, month, day), DateStyle.Short)
}

internal fun daysInMonthUi(year: Int, month: Int): Int {
    val first = LocalDate(year, month, 1)
    val next = if (month == 12) LocalDate(year + 1, 1, 1)
    else LocalDate(year, month + 1, 1)
    return (next.toEpochDays() - first.toEpochDays()).toInt()
}
