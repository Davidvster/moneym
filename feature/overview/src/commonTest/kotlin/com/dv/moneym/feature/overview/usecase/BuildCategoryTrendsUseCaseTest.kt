package com.dv.moneym.feature.overview.usecase

import com.dv.moneym.core.model.AccountId
import com.dv.moneym.core.model.Category
import com.dv.moneym.core.model.CategoryId
import com.dv.moneym.core.model.CurrencyCode
import com.dv.moneym.core.model.Icon
import com.dv.moneym.core.model.Money
import com.dv.moneym.core.model.Transaction
import com.dv.moneym.core.model.TransactionId
import com.dv.moneym.core.model.TransactionType
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Instant

class BuildCategoryTrendsUseCaseTest {

    private val epoch = Instant.fromEpochMilliseconds(0)
    private val useCase = BuildCategoryTrendsUseCase()

    private fun category(id: Long, name: String) = Category(
        id = CategoryId(id),
        name = name,
        iconKey = Icon.Basket.key,
        colorHex = "#112233",
        isUserCreated = true,
        archived = false,
        createdAt = epoch,
        updatedAt = epoch,
    )

    private fun txn(
        id: Long,
        type: TransactionType,
        amountMinor: Long,
        date: LocalDate,
        categoryId: Long,
    ) = Transaction(
        id = TransactionId(id),
        type = type,
        amount = Money(amountMinor, CurrencyCode("EUR")),
        occurredOn = date,
        note = null,
        categoryId = CategoryId(categoryId),
        accountId = AccountId(1),
        createdAt = epoch,
        updatedAt = epoch,
    )

    @Test
    fun daily_empty_returns_empty() {
        assertTrue(useCase.daily(emptyList(), emptyMap(), days = 31).isEmpty())
    }

    @Test
    fun daily_only_expenses_with_per_day_series() {
        val cat = category(1, "Food")
        val txns = listOf(
            txn(1, TransactionType.EXPENSE, 1000, LocalDate(2026, 5, 1), 1),
            txn(2, TransactionType.EXPENSE, 2000, LocalDate(2026, 5, 3), 1),
            txn(3, TransactionType.INCOME, 9999, LocalDate(2026, 5, 3), 1),
        )
        val result = useCase.daily(txns, mapOf(cat.id to cat), days = 3, elapsedDays = 3)
        assertEquals(1, result.size)
        val t = result.first()
        assertEquals(listOf(10.0, 0.0, 20.0), t.series)
        assertEquals(30.0, t.totalAmount)
        assertEquals(2, t.txCount)
        assertEquals(10.0, t.avgPerDay)
    }

    @Test
    fun daily_sorted_by_total_descending() {
        val a = category(1, "A")
        val b = category(2, "B")
        val txns = listOf(
            txn(1, TransactionType.EXPENSE, 1000, LocalDate(2026, 5, 1), 1),
            txn(2, TransactionType.EXPENSE, 5000, LocalDate(2026, 5, 1), 2),
        )
        val result = useCase.daily(txns, mapOf(a.id to a, b.id to b), days = 1)
        assertEquals("B", result.first().categoryName)
    }

    @Test
    fun monthly_filters_by_year_and_fills_twelve_months() {
        val cat = category(1, "Food")
        val txns = listOf(
            txn(1, TransactionType.EXPENSE, 1000, LocalDate(2026, 1, 5), 1),
            txn(2, TransactionType.EXPENSE, 2000, LocalDate(2026, 3, 5), 1),
            txn(3, TransactionType.EXPENSE, 9999, LocalDate(2025, 3, 5), 1),
        )
        val result = useCase.monthly(txns, mapOf(cat.id to cat), year = 2026, elapsedMonths = 3, elapsedDays = 90)
        assertEquals(1, result.size)
        val t = result.first()
        assertEquals(12, t.series.size)
        assertEquals(10.0, t.series[0])
        assertEquals(20.0, t.series[2])
        assertEquals(30.0, t.totalAmount)
        assertEquals(10.0, t.avgPerMonth)
    }

    @Test
    fun monthly_empty_returns_empty() {
        assertTrue(useCase.monthly(emptyList(), emptyMap(), year = 2026).isEmpty())
    }

    @Test
    fun range_uses_day_buckets_for_short_spans() {
        val cat = category(1, "Food")
        val start = LocalDate(2026, 5, 1)
        val end = LocalDate(2026, 5, 5)
        val txns = listOf(
            txn(1, TransactionType.EXPENSE, 1000, LocalDate(2026, 5, 1), 1),
            txn(2, TransactionType.EXPENSE, 3000, LocalDate(2026, 5, 5), 1),
        )
        val result = useCase.range(txns, mapOf(cat.id to cat), start, end)
        assertEquals(1, result.size)
        assertEquals(5, result.first().series.size)
        assertEquals(listOf(10.0, 0.0, 0.0, 0.0, 30.0), result.first().series)
    }

    @Test
    fun range_uses_month_buckets_for_long_spans() {
        val cat = category(1, "Food")
        val start = LocalDate(2026, 1, 1)
        val end = LocalDate(2026, 3, 31)
        val txns = listOf(
            txn(1, TransactionType.EXPENSE, 1000, LocalDate(2026, 1, 10), 1),
            txn(2, TransactionType.EXPENSE, 2000, LocalDate(2026, 3, 20), 1),
        )
        val result = useCase.range(txns, mapOf(cat.id to cat), start, end)
        assertEquals(3, result.first().series.size)
        assertEquals(listOf(10.0, 0.0, 20.0), result.first().series)
    }

    @Test
    fun range_empty_returns_empty() {
        val start = LocalDate(2026, 5, 1)
        val end = LocalDate(2026, 5, 5)
        assertTrue(useCase.range(emptyList(), emptyMap(), start, end).isEmpty())
    }
}
