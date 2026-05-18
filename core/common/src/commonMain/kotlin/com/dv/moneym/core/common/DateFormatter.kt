package com.dv.moneym.core.common

import kotlinx.datetime.LocalDate

enum class DateStyle { Full, Medium, Short }

expect fun formatDate(date: LocalDate, style: DateStyle): String
