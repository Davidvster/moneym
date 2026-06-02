package com.dv.moneym.data.transactions.internal

import com.dv.moneym.core.model.AccountId
import com.dv.moneym.core.model.CategoryId
import com.dv.moneym.core.model.CurrencyCode
import com.dv.moneym.core.model.EndCondition
import com.dv.moneym.core.model.Money
import com.dv.moneym.core.model.MonthlyDayKind
import com.dv.moneym.core.model.PaymentModeId
import com.dv.moneym.core.model.RecurrenceRule
import com.dv.moneym.core.model.RecurringTransaction
import com.dv.moneym.core.model.RecurringTransactionId
import com.dv.moneym.core.model.TransactionType
import com.dv.moneym.data.transactions.db.RecurringTransactionEntity
import kotlin.time.Instant
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class RecurringTransactionMappersTest {

    private val epoch = Instant.fromEpochMilliseconds(0)
    private val eur = CurrencyCode("EUR")

    private fun entity(
        freqUnit: String = "DAILY",
        freqInterval: Int = 1,
        dayOfWeek: Int? = null,
        dayOfMonth: Int? = null,
        useLastDay: Boolean = false,
        endKind: String = "UNLIMITED",
        endCount: Int? = null,
        endDate: String? = null,
        lastMaterializedDate: String? = null,
        note: String? = "rent",
        paymentModeId: Long? = 4,
        type: String = "EXPENSE",
    ) = RecurringTransactionEntity(
        id = 1,
        type = type,
        amountMinor = 5000,
        currency = "EUR",
        note = note,
        categoryId = 7,
        accountId = 3,
        paymentModeId = paymentModeId,
        startDate = "2026-05-01",
        freqUnit = freqUnit,
        freqInterval = freqInterval,
        dayOfWeek = dayOfWeek,
        dayOfMonth = dayOfMonth,
        useLastDay = useLastDay,
        endKind = endKind,
        endCount = endCount,
        endDate = endDate,
        lastMaterializedDate = lastMaterializedDate,
        createdAt = 1000,
        updatedAt = 2000,
    )

    private fun domain(
        rule: RecurrenceRule,
        endCondition: EndCondition,
        note: String? = "rent",
        paymentModeId: PaymentModeId? = PaymentModeId(4),
        lastMaterializedDate: LocalDate? = null,
    ) = RecurringTransaction(
        id = RecurringTransactionId(1),
        type = TransactionType.EXPENSE,
        amount = Money(5000, eur),
        note = note,
        categoryId = CategoryId(7),
        accountId = AccountId(3),
        paymentModeId = paymentModeId,
        startDate = LocalDate(2026, 5, 1),
        rule = rule,
        endCondition = endCondition,
        lastMaterializedDate = lastMaterializedDate,
        createdAt = epoch,
        updatedAt = epoch,
    )

    @Test
    fun toDomainDailyUnlimited() {
        val d = entity(freqUnit = "DAILY", freqInterval = 2).toDomain()
        assertEquals(RecurrenceRule.Daily(2), d.rule)
        assertEquals(EndCondition.Unlimited, d.endCondition)
        assertEquals(TransactionType.EXPENSE, d.type)
        assertEquals(Money(5000, eur), d.amount)
        assertEquals(PaymentModeId(4), d.paymentModeId)
    }

    @Test
    fun toDomainWeeklyUsesDayOfWeek() {
        val d = entity(freqUnit = "WEEKLY", freqInterval = 1, dayOfWeek = 3).toDomain()
        assertEquals(RecurrenceRule.Weekly(1, 3), d.rule)
    }

    @Test
    fun toDomainWeeklyDefaultsDayOfWeekWhenNull() {
        val d = entity(freqUnit = "WEEKLY", dayOfWeek = null).toDomain()
        assertEquals(RecurrenceRule.Weekly(1, 1), d.rule)
    }

    @Test
    fun toDomainMonthlyOnDay() {
        val d = entity(freqUnit = "MONTHLY", dayOfMonth = 15, useLastDay = false).toDomain()
        assertEquals(RecurrenceRule.Monthly(1, MonthlyDayKind.OnDay(15)), d.rule)
    }

    @Test
    fun toDomainMonthlyOnDayDefaultsWhenNull() {
        val d = entity(freqUnit = "MONTHLY", dayOfMonth = null, useLastDay = false).toDomain()
        assertEquals(RecurrenceRule.Monthly(1, MonthlyDayKind.OnDay(1)), d.rule)
    }

    @Test
    fun toDomainMonthlyLastDay() {
        val d = entity(freqUnit = "MONTHLY", useLastDay = true).toDomain()
        assertEquals(RecurrenceRule.Monthly(1, MonthlyDayKind.LastDay), d.rule)
    }

    @Test
    fun toDomainEndCount() {
        val d = entity(endKind = "COUNT", endCount = 6).toDomain()
        assertEquals(EndCondition.Count(6), d.endCondition)
    }

    @Test
    fun toDomainEndUntil() {
        val d = entity(endKind = "UNTIL", endDate = "2026-12-31").toDomain()
        assertEquals(EndCondition.Until(LocalDate(2026, 12, 31)), d.endCondition)
    }

    @Test
    fun toDomainNullNoteAndPaymentModeAndLastMaterialized() {
        val d = entity(note = null, paymentModeId = null, lastMaterializedDate = null).toDomain()
        assertNull(d.note)
        assertNull(d.paymentModeId)
        assertNull(d.lastMaterializedDate)
    }

    @Test
    fun toDomainParsesLastMaterializedDate() {
        val d = entity(lastMaterializedDate = "2026-06-01").toDomain()
        assertEquals(LocalDate(2026, 6, 1), d.lastMaterializedDate)
    }

    @Test
    fun toEntityDailyUnlimited() {
        val e = domain(RecurrenceRule.Daily(2), EndCondition.Unlimited).toEntity()
        assertEquals("DAILY", e.freqUnit)
        assertEquals(2, e.freqInterval)
        assertNull(e.dayOfWeek)
        assertNull(e.dayOfMonth)
        assertEquals(false, e.useLastDay)
        assertEquals("UNLIMITED", e.endKind)
        assertNull(e.endCount)
        assertNull(e.endDate)
    }

    @Test
    fun toEntityWeeklyCount() {
        val e = domain(RecurrenceRule.Weekly(1, 5), EndCondition.Count(4)).toEntity()
        assertEquals("WEEKLY", e.freqUnit)
        assertEquals(5, e.dayOfWeek)
        assertEquals("COUNT", e.endKind)
        assertEquals(4, e.endCount)
    }

    @Test
    fun toEntityMonthlyOnDayUntil() {
        val until = EndCondition.Until(LocalDate(2027, 1, 1))
        val e = domain(RecurrenceRule.Monthly(2, MonthlyDayKind.OnDay(12)), until).toEntity()
        assertEquals("MONTHLY", e.freqUnit)
        assertEquals(2, e.freqInterval)
        assertEquals(12, e.dayOfMonth)
        assertEquals(false, e.useLastDay)
        assertEquals("UNTIL", e.endKind)
        assertEquals("2027-01-01", e.endDate)
    }

    @Test
    fun toEntityMonthlyLastDay() {
        val e = domain(RecurrenceRule.Monthly(1, MonthlyDayKind.LastDay), EndCondition.Unlimited).toEntity()
        assertEquals("MONTHLY", e.freqUnit)
        assertNull(e.dayOfMonth)
        assertEquals(true, e.useLastDay)
    }

    @Test
    fun roundTripDailyUnlimited() {
        assertRoundTrip(domain(RecurrenceRule.Daily(3), EndCondition.Unlimited))
    }

    @Test
    fun roundTripWeeklyCount() {
        assertRoundTrip(domain(RecurrenceRule.Weekly(2, 4), EndCondition.Count(10)))
    }

    @Test
    fun roundTripMonthlyOnDayUntil() {
        assertRoundTrip(
            domain(
                rule = RecurrenceRule.Monthly(1, MonthlyDayKind.OnDay(20)),
                endCondition = EndCondition.Until(LocalDate(2027, 6, 30)),
                lastMaterializedDate = LocalDate(2026, 7, 1),
            ),
        )
    }

    @Test
    fun roundTripMonthlyLastDayNullables() {
        assertRoundTrip(
            domain(
                rule = RecurrenceRule.Monthly(1, MonthlyDayKind.LastDay),
                endCondition = EndCondition.Unlimited,
                note = null,
                paymentModeId = null,
            ),
        )
    }

    private fun assertRoundTrip(original: RecurringTransaction) {
        val result = original.toEntity().toDomain()
        assertEquals(original.id, result.id)
        assertEquals(original.type, result.type)
        assertEquals(original.amount, result.amount)
        assertEquals(original.note, result.note)
        assertEquals(original.categoryId, result.categoryId)
        assertEquals(original.accountId, result.accountId)
        assertEquals(original.paymentModeId, result.paymentModeId)
        assertEquals(original.startDate, result.startDate)
        assertEquals(original.rule, result.rule)
        assertEquals(original.endCondition, result.endCondition)
        assertEquals(original.lastMaterializedDate, result.lastMaterializedDate)
        assertEquals(original.createdAt, result.createdAt)
        assertEquals(original.updatedAt, result.updatedAt)
    }

    @Test
    fun toSyncRowMapsAllFields() {
        val row = entity(
            freqUnit = "WEEKLY",
            freqInterval = 2,
            dayOfWeek = 5,
            endKind = "COUNT",
            endCount = 3,
            lastMaterializedDate = "2026-06-01",
        ).toSyncRow()

        assertEquals(1, row.id)
        assertEquals("EXPENSE", row.type)
        assertEquals(5000, row.amountMinor)
        assertEquals("EUR", row.currency)
        assertEquals("rent", row.note)
        assertEquals(7, row.categoryId)
        assertEquals(3, row.accountId)
        assertEquals(4, row.paymentModeId)
        assertEquals("2026-05-01", row.startDate)
        assertEquals("WEEKLY", row.freqUnit)
        assertEquals(2, row.freqInterval)
        assertEquals(5, row.dayOfWeek)
        assertNull(row.dayOfMonth)
        assertEquals(false, row.useLastDay)
        assertEquals("COUNT", row.endKind)
        assertEquals(3, row.endCount)
        assertNull(row.endDate)
        assertEquals("2026-06-01", row.lastMaterializedDate)
        assertEquals(1000, row.createdAt)
        assertEquals(2000, row.updatedAt)
    }
}
