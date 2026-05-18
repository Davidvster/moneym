package com.dv.moneym.core.common

import android.text.format.DateFormat
import kotlinx.datetime.LocalDate
import java.util.Calendar
import java.util.Locale

actual fun formatDate(date: LocalDate, style: DateStyle): String {
    val cal = Calendar.getInstance().apply {
        set(date.year, date.monthNumber - 1, date.dayOfMonth)
    }
    val locale = Locale.getDefault()
    return when (style) {
        DateStyle.Full -> {
            val dayName = java.text.SimpleDateFormat("EEEE", locale).format(cal.time)
            val monthDay = java.text.SimpleDateFormat("d MMMM", locale).format(cal.time)
            "$dayName, $monthDay"
        }
        DateStyle.Medium -> {
            java.text.SimpleDateFormat("MMM d, yyyy", locale).format(cal.time)
        }
        DateStyle.Short -> {
            val pattern = DateFormat.getBestDateTimePattern(locale, "ddMMyyyy")
            java.text.SimpleDateFormat(pattern, locale).format(cal.time)
        }
    }
}
