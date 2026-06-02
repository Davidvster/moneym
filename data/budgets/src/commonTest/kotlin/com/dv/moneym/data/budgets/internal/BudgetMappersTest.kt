package com.dv.moneym.data.budgets.internal

import com.dv.moneym.core.model.AccountId
import com.dv.moneym.core.model.BudgetId
import com.dv.moneym.core.model.BudgetPeriodType
import com.dv.moneym.core.model.CategoryId
import com.dv.moneym.core.model.CurrencyCode
import com.dv.moneym.core.model.Money
import com.dv.moneym.core.model.YearMonth
import com.dv.moneym.data.budgets.db.BudgetEntity
import kotlin.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class BudgetMappersTest {

    private fun entity(
        id: Long = 1,
        name: String = "Groceries",
        amountMinor: Long = 5000,
        currency: String = "EUR",
        categoryId: Long? = 7,
        accountId: Long = 3,
        periodType: String = "MONTHLY",
        startYearMonth: String = "2026-05",
        recurringMonths: Int? = 12,
        syncId: String? = "sync-1",
        deleted: Boolean = false,
        createdAt: Long = 1000,
        updatedAt: Long = 2000,
    ) = BudgetEntity(
        id = id,
        name = name,
        amountMinor = amountMinor,
        currency = currency,
        categoryId = categoryId,
        accountId = accountId,
        periodType = periodType,
        startYearMonth = startYearMonth,
        recurringMonths = recurringMonths,
        createdAt = createdAt,
        updatedAt = updatedAt,
        syncId = syncId,
        deleted = deleted,
    )

    @Test
    fun toDomainMapsAllFields() {
        val domain = entity().toDomain()

        assertEquals(BudgetId(1), domain.id)
        assertEquals("Groceries", domain.name)
        assertEquals(Money(5000, CurrencyCode("EUR")), domain.amount)
        assertEquals(CategoryId(7), domain.categoryId)
        assertEquals(AccountId(3), domain.accountId)
        assertEquals(BudgetPeriodType.MONTHLY, domain.periodType)
        assertEquals(YearMonth(2026, 5), domain.startYearMonth)
        assertEquals(12, domain.recurringMonths)
        assertEquals(Instant.fromEpochMilliseconds(1000), domain.createdAt)
        assertEquals(Instant.fromEpochMilliseconds(2000), domain.updatedAt)
    }

    @Test
    fun toDomainNullCategoryAndRecurringMonths() {
        val domain = entity(categoryId = null, recurringMonths = null).toDomain()
        assertNull(domain.categoryId)
        assertNull(domain.recurringMonths)
    }

    @Test
    fun toSyncRowMapsAllFields() {
        val row = entity(deleted = true).toSyncRow()

        assertEquals(1, row.id)
        assertEquals("sync-1", row.syncId)
        assertEquals("Groceries", row.name)
        assertEquals(5000, row.amountMinor)
        assertEquals("EUR", row.currency)
        assertEquals(7, row.categoryId)
        assertEquals(3, row.accountId)
        assertEquals("MONTHLY", row.periodType)
        assertEquals("2026-05", row.startYearMonth)
        assertEquals(12, row.recurringMonths)
        assertEquals(true, row.deleted)
        assertEquals(1000, row.createdAt)
        assertEquals(2000, row.updatedAt)
    }

    @Test
    fun toSyncRowNullableFields() {
        val row = entity(categoryId = null, recurringMonths = null, syncId = null).toSyncRow()
        assertNull(row.categoryId)
        assertNull(row.recurringMonths)
        assertNull(row.syncId)
    }

    @Test
    fun parsePeriodTypeKnownValue() {
        assertEquals(BudgetPeriodType.MONTHLY, parsePeriodType("MONTHLY"))
    }

    @Test
    fun parsePeriodTypeFallsBackOnUnknown() {
        assertEquals(BudgetPeriodType.MONTHLY, parsePeriodType("WEEKLY"))
        assertEquals(BudgetPeriodType.MONTHLY, parsePeriodType(""))
    }

    @Test
    fun parseYearMonthValid() {
        assertEquals(YearMonth(2026, 1), parseYearMonth("2026-01"))
        assertEquals(YearMonth(1999, 12), parseYearMonth("1999-12"))
    }

    @Test
    fun parseYearMonthMalformedThrows() {
        assertFailsWith<NumberFormatException> { parseYearMonth("not-a-date") }
        assertFailsWith<IndexOutOfBoundsException> { parseYearMonth("2026") }
    }
}
