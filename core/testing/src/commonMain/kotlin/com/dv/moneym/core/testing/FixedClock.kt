package com.dv.moneym.core.testing

import com.dv.moneym.core.common.AppClock
import kotlin.time.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

class FixedClock(private var fixedNow: Instant) : AppClock {
    override fun now(): Instant = fixedNow
    override fun today(): LocalDate =
        fixedNow.toLocalDateTime(TimeZone.currentSystemDefault()).date

    fun advanceTo(instant: Instant) { fixedNow = instant }
}
