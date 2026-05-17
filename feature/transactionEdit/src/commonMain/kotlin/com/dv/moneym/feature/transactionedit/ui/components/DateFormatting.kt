package com.dv.moneym.feature.transactionedit.ui.components

import kotlinx.datetime.LocalDate

internal fun LocalDate.toFriendlyString(today: LocalDate): String {
    val dayName = dayOfWeek.name.lowercase().replaceFirstChar { it.uppercase() }.take(3)
    val monthName = month.name.lowercase().replaceFirstChar { it.uppercase() }.take(3)
    val yearSuffix = if (year != today.year) " $year" else ""
    return "$dayName, $monthName $day$yearSuffix"
}
