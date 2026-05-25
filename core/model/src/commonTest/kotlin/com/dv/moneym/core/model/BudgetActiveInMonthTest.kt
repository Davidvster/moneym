package com.dv.moneym.core.model

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Instant

class BudgetActiveInMonthTest {

    private val ym = { y: Int, m: Int -> YearMonth(y, m) }

    private fun budget(start: YearMonth, recurringMonths: Int?): Budget = Budget(
        id = BudgetId(0),
        name = "Test",
        amount = Money(0L, CurrencyCode("EUR")),
        categoryId = null,
        periodType = BudgetPeriodType.MONTHLY,
        startYearMonth = start,
        recurringMonths = recurringMonths,
        createdAt = Instant.fromEpochMilliseconds(0),
        updatedAt = Instant.fromEpochMilliseconds(0),
    )

    @Test fun single_matches_only_start_month() {
        val b = budget(ym(2026, 5), recurringMonths = null)
        assertTrue(b.isActiveIn(ym(2026, 5)))
        assertFalse(b.isActiveIn(ym(2026, 4)))
        assertFalse(b.isActiveIn(ym(2026, 6)))
        assertFalse(b.isActiveIn(ym(2027, 5)))
    }

    @Test fun unlimited_matches_start_and_all_future_months() {
        val b = budget(ym(2026, 5), recurringMonths = Budget.UNLIMITED)
        assertTrue(b.isActiveIn(ym(2026, 5)))
        assertTrue(b.isActiveIn(ym(2026, 6)))
        assertTrue(b.isActiveIn(ym(2030, 12)))
        assertFalse(b.isActiveIn(ym(2026, 4)))
        assertFalse(b.isActiveIn(ym(2025, 12)))
    }

    @Test fun n_months_covers_inclusive_start_exclusive_end() {
        val b = budget(ym(2026, 5), recurringMonths = 3)
        assertTrue(b.isActiveIn(ym(2026, 5)))
        assertTrue(b.isActiveIn(ym(2026, 6)))
        assertTrue(b.isActiveIn(ym(2026, 7)))
        assertFalse(b.isActiveIn(ym(2026, 8)))
        assertFalse(b.isActiveIn(ym(2026, 4)))
    }

    @Test fun n_months_crosses_year_boundary() {
        val b = budget(ym(2026, 11), recurringMonths = 4)
        assertTrue(b.isActiveIn(ym(2026, 11)))
        assertTrue(b.isActiveIn(ym(2026, 12)))
        assertTrue(b.isActiveIn(ym(2027, 1)))
        assertTrue(b.isActiveIn(ym(2027, 2)))
        assertFalse(b.isActiveIn(ym(2027, 3)))
    }

    @Test fun zero_or_negative_other_than_unlimited_is_never_active() {
        val zero = budget(ym(2026, 5), recurringMonths = 0)
        assertFalse(zero.isActiveIn(ym(2026, 5)))
        assertFalse(zero.isActiveIn(ym(2026, 6)))

        val nonsense = budget(ym(2026, 5), recurringMonths = -2)
        assertFalse(nonsense.isActiveIn(ym(2026, 5)))
        assertFalse(nonsense.isActiveIn(ym(2026, 6)))
    }
}
