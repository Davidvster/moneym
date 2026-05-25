package com.dv.moneym.core.common

import android.text.format.DateFormat
import kotlinx.datetime.LocalDate
import kotlinx.datetime.number
import java.util.Calendar
import java.util.Locale

actual fun formatDate(date: LocalDate, style: DateStyle): String {
    val cal = Calendar.getInstance().apply {
        set(date.year, date.month.number - 1, date.day)
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
