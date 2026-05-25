package com.dv.moneym.core.common

import kotlinx.datetime.LocalDate
import kotlinx.datetime.number
import platform.Foundation.NSCalendar
import platform.Foundation.NSDateComponents
import platform.Foundation.NSDateFormatter
import platform.Foundation.NSDateFormatterMediumStyle
import platform.Foundation.NSDateFormatterNoStyle
import platform.Foundation.NSDateFormatterShortStyle
import platform.Foundation.NSLocale
import platform.Foundation.currentLocale

actual fun formatDate(date: LocalDate, style: DateStyle): String {
    val components = NSDateComponents().apply {
        year = date.year.toLong()
        month = date.month.number.toLong()
        day = date.day.toLong()
    }
    val nsDate = NSCalendar.currentCalendar.dateFromComponents(components) ?: return date.toString()
    val formatter = NSDateFormatter().apply {
        locale = NSLocale.currentLocale
    }
    return when (style) {
        DateStyle.Full -> {
            val template =
                NSDateFormatter.dateFormatFromTemplate("EEEEdMMMM", 0u, NSLocale.currentLocale)
                    ?: "EEEE, d MMMM"
            formatter.dateFormat = template
            formatter.stringFromDate(nsDate) ?: date.toString()
        }

        DateStyle.Medium -> {
            formatter.dateStyle = NSDateFormatterMediumStyle
            formatter.timeStyle = NSDateFormatterNoStyle
            formatter.stringFromDate(nsDate) ?: date.toString()
        }

        DateStyle.Short -> {
            formatter.dateStyle = NSDateFormatterShortStyle
            formatter.timeStyle = NSDateFormatterNoStyle
            formatter.stringFromDate(nsDate) ?: date.toString()
        }
    }
}
