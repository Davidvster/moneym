package com.dv.moneym.data.transactions.internal

import com.dv.moneym.core.model.AccountId
import com.dv.moneym.core.model.CategoryId
import com.dv.moneym.core.model.CurrencyCode
import com.dv.moneym.core.model.Money
import com.dv.moneym.core.model.PaymentModeId
import com.dv.moneym.core.model.RecurringTransactionId
import com.dv.moneym.core.model.TransactionId
import com.dv.moneym.core.model.TransactionType
import com.dv.moneym.data.transactions.db.TransactionEntity
import kotlin.time.Instant
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class TransactionMappersTest {

    private fun entity(
        id: Long = 1,
        type: String = "EXPENSE",
        amountMinor: Long = 1500,
        currency: String = "EUR",
        occurredOn: String = "2026-05-10",
        note: String? = "lunch",
        categoryId: Long = 7,
        accountId: Long = 3,
        paymentModeId: Long? = 4,
        recurringId: Long? = 9,
        syncId: String? = "sync-1",
        deleted: Boolean = false,
        createdAt: Long = 1000,
        updatedAt: Long = 2000,
    ) = TransactionEntity(
        id = id,
        type = type,
        amountMinor = amountMinor,
        currency = currency,
        occurredOn = occurredOn,
        note = note,
        categoryId = categoryId,
        accountId = accountId,
        createdAt = createdAt,
        updatedAt = updatedAt,
        paymentModeId = paymentModeId,
        recurringId = recurringId,
        syncId = syncId,
        deleted = deleted,
    )

    @Test
    fun toDomainMapsAllFields() {
        val domain = entity().toDomain()

        assertEquals(TransactionId(1), domain.id)
        assertEquals(TransactionType.EXPENSE, domain.type)
        assertEquals(Money(1500, CurrencyCode("EUR")), domain.amount)
        assertEquals(LocalDate(2026, 5, 10), domain.occurredOn)
        assertEquals("lunch", domain.note)
        assertEquals(CategoryId(7), domain.categoryId)
        assertEquals(AccountId(3), domain.accountId)
        assertEquals(Instant.fromEpochMilliseconds(1000), domain.createdAt)
        assertEquals(Instant.fromEpochMilliseconds(2000), domain.updatedAt)
        assertEquals(PaymentModeId(4), domain.paymentModeId)
        assertEquals(RecurringTransactionId(9), domain.recurringId)
    }

    @Test
    fun toDomainIncomeType() {
        assertEquals(TransactionType.INCOME, entity(type = "INCOME").toDomain().type)
    }

    @Test
    fun toDomainNullOptionalFields() {
        val domain = entity(note = null, paymentModeId = null, recurringId = null).toDomain()
        assertNull(domain.note)
        assertNull(domain.paymentModeId)
        assertNull(domain.recurringId)
    }

    @Test
    fun toSyncRowMapsAllFields() {
        val row = entity(type = "INCOME", deleted = true).toSyncRow()

        assertEquals(1, row.id)
        assertEquals("sync-1", row.syncId)
        assertEquals("INCOME", row.type)
        assertEquals(1500, row.amountMinor)
        assertEquals("EUR", row.currency)
        assertEquals("2026-05-10", row.occurredOn)
        assertEquals("lunch", row.note)
        assertEquals(7, row.categoryId)
        assertEquals(3, row.accountId)
        assertEquals(4, row.paymentModeId)
        assertEquals(9, row.recurringId)
        assertEquals(true, row.deleted)
        assertEquals(1000, row.createdAt)
        assertEquals(2000, row.updatedAt)
    }

    @Test
    fun toSyncRowNullableFields() {
        val row = entity(note = null, paymentModeId = null, recurringId = null, syncId = null).toSyncRow()
        assertNull(row.note)
        assertNull(row.paymentModeId)
        assertNull(row.recurringId)
        assertNull(row.syncId)
    }

    @Test
    fun yearMonthKeyZeroPads() {
        assertEquals("2026-01", yearMonthKey(2026, 1))
        assertEquals("2026-09", yearMonthKey(2026, 9))
        assertEquals("2026-12", yearMonthKey(2026, 12))
    }
}
