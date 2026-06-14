package com.dv.moneym.core.common

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

enum class DateStyle { Full, Medium, Short }

expect fun formatDate(date: LocalDate, style: DateStyle): String

/** Localized date + short time (e.g. "Jun 14, 2026, 7:35 PM" / "14.06.2026, 19:35"). */
expect fun formatDateTime(dateTime: LocalDateTime, style: DateStyle): String

fun formatDateTime(epochMs: Long, style: DateStyle = DateStyle.Medium): String =
    formatDateTime(
        Instant.fromEpochMilliseconds(epochMs)
            .toLocalDateTime(TimeZone.currentSystemDefault()),
        style,
    )
