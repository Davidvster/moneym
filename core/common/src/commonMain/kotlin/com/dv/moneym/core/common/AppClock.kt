package com.dv.moneym.core.common

import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn

interface AppClock {
    fun now(): Instant
    fun today(): LocalDate
}

class DefaultAppClock : AppClock {
    override fun now(): Instant = Clock.System.now()
    override fun today(): LocalDate = Clock.System.todayIn(TimeZone.currentSystemDefault())
}
