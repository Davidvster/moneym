package com.dv.moneym.feature.overview.usecase

import com.dv.moneym.core.model.AccountId
import com.dv.moneym.core.model.CategoryId
import com.dv.moneym.core.model.CurrencyCode
import com.dv.moneym.core.model.Money
import com.dv.moneym.core.model.Transaction
import com.dv.moneym.core.model.TransactionId
import com.dv.moneym.core.model.TransactionType
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

class BuildCumulativeSeriesUseCaseTest {

    private val epoch = Instant.fromEpochMilliseconds(0)
    private val useCase = BuildCumulativeSeriesUseCase()

    private fun txn(
        id: Long,
        type: TransactionType,
        amountMinor: Long,
        date: LocalDate,
    ) = Transaction(
        id = TransactionId(id),
        type = type,
        amount = Money(amountMinor, CurrencyCode("EUR")),
        occurredOn = date,
        note = null,
        categoryId = CategoryId(1),
        accountId = AccountId(1),
        createdAt = epoch,
        updatedAt = epoch,
    )

    @Test
    fun empty_produces_zero_filled_series_for_full_month() {
        val result = useCase(emptyList(), 2026, 5, LocalDate(2026, 7, 1))
        assertEquals(31, result.dailyTotals.size)
        assertEquals(31, result.cumulativeTotals.size)
        assertEquals(List(31) { 0.0 }, result.dailyTotals)
        assertEquals(0.0, result.cumulativeTotals.last())
    }

    @Test
    fun daily_totals_only_count_expenses() {
        val txns = listOf(
            txn(1, TransactionType.EXPENSE, 1000, LocalDate(2026, 5, 1)),
            txn(2, TransactionType.INCOME, 9999, LocalDate(2026, 5, 1)),
            txn(3, TransactionType.EXPENSE, 2000, LocalDate(2026, 5, 2)),
        )
        val result = useCase(txns, 2026, 5, LocalDate(2026, 7, 1))
        assertEquals(10.0, result.dailyTotals[0])
        assertEquals(20.0, result.dailyTotals[1])
    }

    @Test
    fun cumulative_is_running_sum() {
        val txns = listOf(
            txn(1, TransactionType.EXPENSE, 1000, LocalDate(2026, 5, 1)),
            txn(2, TransactionType.EXPENSE, 2000, LocalDate(2026, 5, 2)),
            txn(3, TransactionType.EXPENSE, 3000, LocalDate(2026, 5, 3)),
        )
        val result = useCase(txns, 2026, 5, LocalDate(2026, 7, 1))
        assertEquals(10.0, result.cumulativeTotals[0])
        assertEquals(30.0, result.cumulativeTotals[1])
        assertEquals(60.0, result.cumulativeTotals[2])
        assertEquals(60.0, result.cumulativeTotals.last())
    }

    @Test
    fun today_index_is_last_day_for_past_month() {
        val result = useCase(emptyList(), 2026, 5, LocalDate(2026, 7, 1))
        assertEquals(30, result.todayIndex)
    }

    @Test
    fun today_index_clamped_to_today_for_current_month() {
        val result = useCase(emptyList(), 2026, 5, LocalDate(2026, 5, 12))
        assertEquals(11, result.todayIndex)
    }

    @Test
    fun monthly_totals_returns_twelve_buckets_filtered_by_year_and_expense() {
        val txns = listOf(
            txn(1, TransactionType.EXPENSE, 1000, LocalDate(2026, 1, 5)),
            txn(2, TransactionType.EXPENSE, 2000, LocalDate(2026, 3, 5)),
            txn(3, TransactionType.INCOME, 9999, LocalDate(2026, 3, 6)),
            txn(4, TransactionType.EXPENSE, 9999, LocalDate(2025, 3, 6)),
        )
        val result = useCase.monthlyTotals(txns, 2026)
        assertEquals(12, result.size)
        assertEquals(10.0, result[0])
        assertEquals(20.0, result[2])
        assertEquals(0.0, result[1])
    }
}
