package com.dv.moneym.feature.overview.usecase

import com.dv.moneym.core.model.YearMonth
import com.dv.moneym.feature.overview.OverviewPeriod
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ResolvePeriodRangeUseCaseTest {

    private val sut = ResolvePeriodRangeUseCase()

    @Test
    fun monthRangeForCurrentMonthCountsElapsedDays() {
        val today = LocalDate(2026, 5, 10)
        val range = sut(OverviewPeriod.Month(YearMonth(2026, 5)), today)
        assertEquals(LocalDate(2026, 5, 1), range.startDate)
        assertEquals(LocalDate(2026, 5, 31), range.endDate)
        assertEquals(10, range.daysInRange)
        assertEquals(1, range.monthsInRange)
        assertTrue(range.isCurrentMonth)
    }

    @Test
    fun monthRangeForPastMonthUsesFullMonth() {
        val today = LocalDate(2026, 5, 10)
        val range = sut(OverviewPeriod.Month(YearMonth(2026, 2)), today)
        assertEquals(28, range.daysInRange)
        assertEquals(false, range.isCurrentMonth)
    }

    @Test
    fun yearRangeForCurrentYearCountsElapsedMonths() {
        val today = LocalDate(2026, 5, 10)
        val range = sut(OverviewPeriod.Year(2026), today)
        assertEquals(LocalDate(2026, 1, 1), range.startDate)
        assertEquals(LocalDate(2026, 12, 31), range.endDate)
        assertEquals(5, range.monthsInRange)
        assertTrue(range.isCurrentYear)
    }

    @Test
    fun yearRangeForPastYearCountsAllTwelveMonths() {
        val today = LocalDate(2026, 5, 10)
        val range = sut(OverviewPeriod.Year(2025), today)
        assertEquals(12, range.monthsInRange)
        assertEquals(365, range.daysInRange)
        assertEquals(false, range.isCurrentYear)
    }

    @Test
    fun dateRangeUsesProvidedBounds() {
        val today = LocalDate(2026, 5, 10)
        val range = sut(
            OverviewPeriod.DateRange(2026, 3, 1, 2026, 3, 10),
            today,
        )
        assertEquals(LocalDate(2026, 3, 1), range.startDate)
        assertEquals(LocalDate(2026, 3, 10), range.endDate)
        assertEquals(10, range.daysInRange)
    }
}
