package com.dv.moneym.feature.overview.usecase

import com.dv.moneym.core.model.Transaction
import com.dv.moneym.feature.overview.OverviewPeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.number

internal data class PeriodRange(
    val startDate: LocalDate?,
    val endDate: LocalDate?,
    val daysInRange: Int,
    val monthsInRange: Int,
    val isCurrentMonth: Boolean = false,
    val isCurrentYear: Boolean = false,
)

class ResolvePeriodRangeUseCase {
    internal operator fun invoke(period: OverviewPeriod, today: LocalDate): PeriodRange =
        when (period) {
            is OverviewPeriod.Month -> {
                val month = period.yearMonth
                val days = daysInMonth(month.year, month.monthNumber)
                val isCurrentMonth =
                    month.year == today.year && month.monthNumber == today.month.number
                val elapsed = if (isCurrentMonth) today.day else days
                PeriodRange(
                    startDate = LocalDate(month.year, month.monthNumber, 1),
                    endDate = LocalDate(month.year, month.monthNumber, days),
                    daysInRange = elapsed.coerceAtLeast(1),
                    monthsInRange = 1,
                    isCurrentMonth = isCurrentMonth,
                )
            }

            is OverviewPeriod.Year -> {
                val isCurrentYear = period.year == today.year
                val elapsedMonths = if (isCurrentYear) today.month.number else 12
                val elapsedDaysInYear = if (isCurrentYear) {
                    val jan1 = LocalDate(period.year, 1, 1)
                    (today.toEpochDays() - jan1.toEpochDays()).toInt() + 1
                } else {
                    val jan1 = LocalDate(period.year, 1, 1)
                    val jan1Next = LocalDate(period.year + 1, 1, 1)
                    (jan1Next.toEpochDays() - jan1.toEpochDays()).toInt()
                }
                PeriodRange(
                    startDate = LocalDate(period.year, 1, 1),
                    endDate = LocalDate(period.year, 12, 31),
                    daysInRange = elapsedDaysInYear.coerceAtLeast(1),
                    monthsInRange = elapsedMonths.coerceAtLeast(1),
                    isCurrentYear = isCurrentYear,
                )
            }

            is OverviewPeriod.DateRange -> {
                val start = LocalDate(period.startYear, period.startMonth, period.startDay)
                val end = LocalDate(period.endYear, period.endMonth, period.endDay)
                val rangeDays =
                    ((end.toEpochDays() - start.toEpochDays()).toInt() + 1).coerceAtLeast(1)
                PeriodRange(
                    startDate = start,
                    endDate = end,
                    daysInRange = rangeDays,
                    monthsInRange = ((rangeDays + 15) / 30).coerceAtLeast(1),
                )
            }
        }

    internal fun filterByPeriod(
        transactions: List<Transaction>,
        period: OverviewPeriod,
    ): List<Transaction> = transactions.filter { it.matchesPeriod(period) }
}

private fun Transaction.matchesPeriod(p: OverviewPeriod): Boolean = when (p) {
    is OverviewPeriod.Month ->
        occurredOn.year == p.yearMonth.year && occurredOn.month.number == p.yearMonth.monthNumber

    is OverviewPeriod.Year -> occurredOn.year == p.year
    is OverviewPeriod.DateRange -> {
        val start = LocalDate(p.startYear, p.startMonth, p.startDay)
        val end = LocalDate(p.endYear, p.endMonth, p.endDay)
        occurredOn in start..end
    }
}

internal fun daysInMonth(year: Int, month: Int): Int {
    val first = LocalDate(year, month, 1)
    val next = if (month == 12) LocalDate(year + 1, 1, 1) else LocalDate(year, month + 1, 1)
    return (next.toEpochDays() - first.toEpochDays()).toInt()
}

internal fun colorHexToLong(hex: String): Long {
    val stripped = hex.trimStart('#')
    return try {
        when (stripped.length) {
            6 -> ("FF$stripped").toLong(16)
            8 -> stripped.toLong(16)
            else -> 0xFF8A8A8AL
        }
    } catch (_: NumberFormatException) {
        0xFF8A8A8AL
    }
}
