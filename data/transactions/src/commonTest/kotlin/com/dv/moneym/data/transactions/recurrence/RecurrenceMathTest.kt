package com.dv.moneym.data.transactions.recurrence

import com.dv.moneym.core.model.EndCondition
import com.dv.moneym.core.model.MonthlyDayKind
import com.dv.moneym.core.model.RecurrenceRule
import com.dv.moneym.core.model.YearMonth
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RecurrenceMathTest {

    @Test
    fun dailyEveryOneDayFirstOccurrenceIsStart() {
        val start = LocalDate(2026, 5, 1)
        assertEquals(start, RecurrenceMath.firstOccurrence(RecurrenceRule.Daily(1), start))
    }

    @Test
    fun dailyEveryThreeDaysSteps() {
        val r = RecurrenceRule.Daily(3)
        val d0 = LocalDate(2026, 5, 1)
        assertEquals(LocalDate(2026, 5, 4), RecurrenceMath.nextOccurrence(r, d0))
        assertEquals(LocalDate(2026, 5, 7), RecurrenceMath.nextOccurrence(r, LocalDate(2026, 5, 4)))
    }

    @Test
    fun weeklyEveryOneWeekOnWednesday() {
        val r = RecurrenceRule.Weekly(1, dayOfWeek = 3) // Wed
        val startMon = LocalDate(2026, 5, 4) // a Monday
        val first = RecurrenceMath.firstOccurrence(r, startMon)
        assertEquals(LocalDate(2026, 5, 6), first) // next Wed
        assertEquals(LocalDate(2026, 5, 13), RecurrenceMath.nextOccurrence(r, first))
    }

    @Test
    fun weeklyEveryTwoWeeksOnMonday() {
        val r = RecurrenceRule.Weekly(2, dayOfWeek = 1)
        val startMon = LocalDate(2026, 5, 4) // Monday
        val first = RecurrenceMath.firstOccurrence(r, startMon)
        assertEquals(startMon, first)
        assertEquals(LocalDate(2026, 5, 18), RecurrenceMath.nextOccurrence(r, first))
    }

    @Test
    fun monthlyOnDay15() {
        val r = RecurrenceRule.Monthly(1, MonthlyDayKind.OnDay(15))
        val start = LocalDate(2026, 5, 10)
        assertEquals(LocalDate(2026, 5, 15), RecurrenceMath.firstOccurrence(r, start))
        assertEquals(LocalDate(2026, 6, 15), RecurrenceMath.nextOccurrence(r, LocalDate(2026, 5, 15)))
    }

    @Test
    fun monthlyLastDayAcrossShortAndLongMonths() {
        val r = RecurrenceRule.Monthly(1, MonthlyDayKind.LastDay)
        val febStart = LocalDate(2026, 2, 1)
        assertEquals(LocalDate(2026, 2, 28), RecurrenceMath.firstOccurrence(r, febStart))
        // Leap year Feb (2024)
        assertEquals(LocalDate(2024, 2, 29), RecurrenceMath.firstOccurrence(r, LocalDate(2024, 2, 1)))
        // April has 30 days
        assertEquals(LocalDate(2026, 4, 30), RecurrenceMath.nextOccurrence(r, LocalDate(2026, 3, 31)))
        // July has 31 days
        assertEquals(LocalDate(2026, 7, 31), RecurrenceMath.nextOccurrence(r, LocalDate(2026, 6, 30)))
    }

    @Test
    fun monthlyStartAfterTargetRollsToNextMonth() {
        val r = RecurrenceRule.Monthly(1, MonthlyDayKind.OnDay(5))
        val start = LocalDate(2026, 5, 20)
        assertEquals(LocalDate(2026, 6, 5), RecurrenceMath.firstOccurrence(r, start))
    }

    @Test
    fun monthlyEveryTwoMonths() {
        val r = RecurrenceRule.Monthly(2, MonthlyDayKind.OnDay(1))
        val start = LocalDate(2026, 1, 1)
        val a = RecurrenceMath.firstOccurrence(r, start)
        assertEquals(LocalDate(2026, 1, 1), a)
        assertEquals(LocalDate(2026, 3, 1), RecurrenceMath.nextOccurrence(r, a))
    }

    @Test
    fun materializeDueWithNullCursorReturnsFirstOccurrence() {
        val r = RecurrenceRule.Monthly(1, MonthlyDayKind.OnDay(15))
        val start = LocalDate(2026, 5, 10)
        val today = LocalDate(2026, 5, 20)
        val out = RecurrenceMath.materializeDue(r, EndCondition.Unlimited, start, cursor = null, today = today)
        assertEquals(listOf(LocalDate(2026, 5, 15)), out)
    }

    @Test
    fun materializeDueCatchUpThreeMissedMonths() {
        val r = RecurrenceRule.Monthly(1, MonthlyDayKind.OnDay(15))
        val start = LocalDate(2026, 2, 15)
        val today = LocalDate(2026, 5, 20)
        val out = RecurrenceMath.materializeDue(
            r, EndCondition.Unlimited, start, cursor = LocalDate(2026, 2, 15), today = today, alreadyMaterializedCount = 1,
        )
        assertEquals(
            listOf(LocalDate(2026, 3, 15), LocalDate(2026, 4, 15), LocalDate(2026, 5, 15)),
            out,
        )
    }

    @Test
    fun materializeDueCountExhausts() {
        val r = RecurrenceRule.Daily(1)
        val start = LocalDate(2026, 5, 1)
        val today = LocalDate(2026, 5, 10)
        val out = RecurrenceMath.materializeDue(
            r, EndCondition.Count(3), start, cursor = null, today = today,
        )
        assertEquals(
            listOf(LocalDate(2026, 5, 1), LocalDate(2026, 5, 2), LocalDate(2026, 5, 3)),
            out,
        )
    }

    @Test
    fun materializeDueRespectsUntilDate() {
        val r = RecurrenceRule.Daily(1)
        val start = LocalDate(2026, 5, 1)
        val today = LocalDate(2026, 5, 10)
        val out = RecurrenceMath.materializeDue(
            r, EndCondition.Until(LocalDate(2026, 5, 3)), start, cursor = null, today = today,
        )
        assertEquals(
            listOf(LocalDate(2026, 5, 1), LocalDate(2026, 5, 2), LocalDate(2026, 5, 3)),
            out,
        )
    }

    @Test
    fun materializeDueCursorAheadOfTodayReturnsEmpty() {
        val r = RecurrenceRule.Daily(1)
        val start = LocalDate(2026, 5, 1)
        val today = LocalDate(2026, 5, 5)
        val out = RecurrenceMath.materializeDue(
            r, EndCondition.Unlimited, start, cursor = LocalDate(2026, 5, 10), today = today,
        )
        assertTrue(out.isEmpty())
    }

    @Test
    fun occurrencesInMonthReturnsAllInWindow() {
        val r = RecurrenceRule.Weekly(1, dayOfWeek = 1) // Mondays
        val start = LocalDate(2026, 5, 1)
        val out = RecurrenceMath.occurrencesInMonth(r, EndCondition.Unlimited, start, YearMonth(2026, 5))
        assertEquals(
            listOf(
                LocalDate(2026, 5, 4),
                LocalDate(2026, 5, 11),
                LocalDate(2026, 5, 18),
                LocalDate(2026, 5, 25),
            ),
            out,
        )
    }

    @Test
    fun occurrencesInMonthBeforeStartIsEmpty() {
        val r = RecurrenceRule.Monthly(1, MonthlyDayKind.OnDay(1))
        val start = LocalDate(2026, 6, 1)
        val out = RecurrenceMath.occurrencesInMonth(r, EndCondition.Unlimited, start, YearMonth(2026, 5))
        assertTrue(out.isEmpty())
    }

    @Test
    fun occurrencesInMonthHonorsCountAcrossMonths() {
        val r = RecurrenceRule.Monthly(1, MonthlyDayKind.OnDay(1))
        val start = LocalDate(2026, 5, 1)
        val out = RecurrenceMath.occurrencesInMonth(
            r, EndCondition.Count(2), start, YearMonth(2026, 7),
        )
        assertTrue(out.isEmpty()) // only 5/1 and 6/1 fit Count(2), 7/1 is past the cap
    }
}
