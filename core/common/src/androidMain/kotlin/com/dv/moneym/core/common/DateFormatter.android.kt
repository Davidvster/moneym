package com.dv.moneym.core.common

import android.text.format.DateFormat
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
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

actual fun formatDateTime(dateTime: LocalDateTime, style: DateStyle): String {
    val cal = Calendar.getInstance().apply {
        set(dateTime.year, dateTime.month.number - 1, dateTime.day, dateTime.hour, dateTime.minute, 0)
    }
    val locale = Locale.getDefault()
    val dateStyle = when (style) {
        DateStyle.Full -> java.text.DateFormat.FULL
        DateStyle.Medium -> java.text.DateFormat.MEDIUM
        DateStyle.Short -> java.text.DateFormat.SHORT
    }
    return java.text.DateFormat
        .getDateTimeInstance(dateStyle, java.text.DateFormat.SHORT, locale)
        .format(cal.time)
}
