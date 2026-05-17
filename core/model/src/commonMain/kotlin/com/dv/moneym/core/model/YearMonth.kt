package com.dv.moneym.core.model

import kotlinx.serialization.Serializable

@Serializable
data class YearMonth(val year: Int, val monthNumber: Int) {
    fun previous(): YearMonth =
        if (monthNumber == 1) YearMonth(year - 1, 12)
        else YearMonth(year, monthNumber - 1)

    fun next(): YearMonth =
        if (monthNumber == 12) YearMonth(year + 1, 1)
        else YearMonth(year, monthNumber + 1)

    override fun toString(): String =
        "$year-${monthNumber.toString().padStart(2, '0')}"
}
