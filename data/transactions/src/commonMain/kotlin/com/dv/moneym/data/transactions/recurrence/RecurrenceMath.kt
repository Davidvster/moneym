package com.dv.moneym.data.transactions.recurrence

import com.dv.moneym.core.model.EndCondition
import com.dv.moneym.core.model.MonthlyDayKind
import com.dv.moneym.core.model.RecurrenceRule
import com.dv.moneym.core.model.YearMonth
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.minus
import kotlinx.datetime.number
import kotlinx.datetime.plus

object RecurrenceMath {

    fun firstOccurrence(rule: RecurrenceRule, startDate: LocalDate): LocalDate = when (rule) {
        is RecurrenceRule.Daily -> startDate
        is RecurrenceRule.Weekly -> {
            val current = startDate.dayOfWeek.isoDayNumber
            val diff = ((rule.dayOfWeek - current) % 7 + 7) % 7
            startDate.plus(DatePeriod(days = diff))
        }
        is RecurrenceRule.Monthly -> {
            val candidate = resolveMonthDay(startDate.year, startDate.month.number, rule.dayKind)
            if (candidate >= startDate) candidate
            else {
                val (y2, m2) = addMonths(startDate.year, startDate.month.number, rule.interval)
                resolveMonthDay(y2, m2, rule.dayKind)
            }
        }
    }

    fun nextOccurrence(rule: RecurrenceRule, after: LocalDate): LocalDate = when (rule) {
        is RecurrenceRule.Daily -> after.plus(DatePeriod(days = rule.interval))
        is RecurrenceRule.Weekly -> after.plus(DatePeriod(days = 7 * rule.interval))
        is RecurrenceRule.Monthly -> {
            val (y2, m2) = addMonths(after.year, after.month.number, rule.interval)
            resolveMonthDay(y2, m2, rule.dayKind)
        }
    }

    fun materializeDue(
        rule: RecurrenceRule,
        endCondition: EndCondition,
        startDate: LocalDate,
        cursor: LocalDate?,
        today: LocalDate,
        alreadyMaterializedCount: Int = 0,
    ): List<LocalDate> {
        if (cursor != null && cursor > today) return emptyList()
        val out = mutableListOf<LocalDate>()
        var next = if (cursor == null) firstOccurrence(rule, startDate) else nextOccurrence(rule, cursor)
        var count = alreadyMaterializedCount
        while (next <= today && withinEndCondition(endCondition, next, count)) {
            out += next
            count++
            next = nextOccurrence(rule, next)
        }
        return out
    }

    fun occurrencesInMonth(
        rule: RecurrenceRule,
        endCondition: EndCondition,
        startDate: LocalDate,
        ym: YearMonth,
        alreadyMaterializedCount: Int = 0,
    ): List<LocalDate> {
        val monthStart = LocalDate(ym.year, ym.monthNumber, 1)
        val monthEnd = LocalDate(ym.year, ym.monthNumber, lengthOfMonth(ym.year, ym.monthNumber))
        if (monthEnd < startDate) return emptyList()
        val out = mutableListOf<LocalDate>()
        var date = firstOccurrence(rule, startDate)
        var count = alreadyMaterializedCount
        while (date <= monthEnd && withinEndCondition(endCondition, date, count)) {
            if (date >= monthStart) out += date
            count++
            date = nextOccurrence(rule, date)
        }
        return out
    }

    private fun withinEndCondition(end: EndCondition, date: LocalDate, materializedSoFar: Int): Boolean =
        when (end) {
            EndCondition.Unlimited -> true
            is EndCondition.Count -> materializedSoFar < end.occurrences
            is EndCondition.Until -> date <= end.date
        }

    private fun resolveMonthDay(year: Int, month: Int, kind: MonthlyDayKind): LocalDate {
        val last = lengthOfMonth(year, month)
        val day = when (kind) {
            is MonthlyDayKind.OnDay -> minOf(kind.day, last)
            MonthlyDayKind.LastDay -> last
        }
        return LocalDate(year, month, day)
    }

    private fun addMonths(year: Int, month: Int, delta: Int): Pair<Int, Int> {
        val zeroBased = (year * 12 + (month - 1)) + delta
        val y = zeroBased / 12
        val m = zeroBased % 12 + 1
        return y to m
    }

    private fun lengthOfMonth(year: Int, month: Int): Int {
        val firstNext = if (month == 12) LocalDate(year + 1, 1, 1) else LocalDate(year, month + 1, 1)
        return firstNext.minus(DatePeriod(days = 1)).day
    }
}
