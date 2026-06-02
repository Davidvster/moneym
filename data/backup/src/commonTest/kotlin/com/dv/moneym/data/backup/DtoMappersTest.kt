package com.dv.moneym.data.backup

import com.dv.moneym.core.model.Account
import com.dv.moneym.core.model.AccountId
import com.dv.moneym.core.model.AccountType
import com.dv.moneym.core.model.Budget
import com.dv.moneym.core.model.BudgetId
import com.dv.moneym.core.model.BudgetPeriodType
import com.dv.moneym.core.model.Category
import com.dv.moneym.core.model.CategoryId
import com.dv.moneym.core.model.CurrencyCode
import com.dv.moneym.core.model.EndCondition
import com.dv.moneym.core.model.Money
import com.dv.moneym.core.model.MonthlyDayKind
import com.dv.moneym.core.model.PaymentModeId
import com.dv.moneym.core.model.RecurrenceRule
import com.dv.moneym.core.model.RecurringTransaction
import com.dv.moneym.core.model.RecurringTransactionId
import com.dv.moneym.core.model.Transaction
import com.dv.moneym.core.model.TransactionId
import com.dv.moneym.core.model.TransactionType
import com.dv.moneym.core.model.UNSAVED_RECURRING_ID
import com.dv.moneym.core.model.UNSAVED_TRANSACTION_ID
import com.dv.moneym.core.model.YearMonth
import kotlin.time.Instant
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class DtoMappersTest {

    private val epoch = Instant.fromEpochMilliseconds(1000)
    private val epoch2 = Instant.fromEpochMilliseconds(2000)
    private val eur = CurrencyCode("EUR")

    // --- Category ---

    private fun category(id: Long = 5) = Category(
        id = CategoryId(id),
        name = "Food",
        iconKey = "fork",
        colorHex = "#FF0000",
        isUserCreated = true,
        archived = false,
        createdAt = epoch,
        updatedAt = epoch2,
        type = TransactionType.EXPENSE,
    )

    @Test
    fun categoryToDtoMapsFields() {
        val dto = category().toDto()
        assertEquals(5, dto.id)
        assertEquals("Food", dto.name)
        assertEquals("fork", dto.iconKey)
        assertEquals("#FF0000", dto.colorHex)
        assertEquals(true, dto.isUserCreated)
        assertEquals(false, dto.archived)
        assertEquals(1000, dto.createdAt)
        assertEquals(2000, dto.updatedAt)
    }

    @Test
    fun categoryDtoToDomainDefaultId() {
        val domain = category(5).toDto().toDomain()
        assertEquals(CategoryId(5), domain.id)
    }

    @Test
    fun categoryDtoToDomainIdOverride() {
        val domain = category(5).toDto().toDomain(CategoryId(99))
        assertEquals(CategoryId(99), domain.id)
    }

    @Test
    fun categoryRoundTrip() {
        val original = category()
        assertEquals(original, original.toDto().toDomain())
    }

    // --- Account ---

    private fun account(id: Long = 3, colorHex: String? = "#00FF00") = Account(
        id = AccountId(id),
        name = "Wallet",
        type = AccountType.BANK,
        currency = eur,
        isDefault = true,
        archived = false,
        createdAt = epoch,
        updatedAt = epoch2,
        colorHex = colorHex,
    )

    @Test
    fun accountToDtoMapsFields() {
        val dto = account().toDto()
        assertEquals(3, dto.id)
        assertEquals("Wallet", dto.name)
        assertEquals("BANK", dto.type)
        assertEquals("EUR", dto.currency)
        assertEquals(true, dto.isDefault)
        assertEquals(false, dto.archived)
        assertEquals("#00FF00", dto.colorHex)
    }

    @Test
    fun accountDtoToDomainDefaultId() {
        assertEquals(AccountId(3), account(3).toDto().toDomain().id)
    }

    @Test
    fun accountDtoToDomainIdOverride() {
        assertEquals(AccountId(77), account(3).toDto().toDomain(AccountId(77)).id)
    }

    @Test
    fun accountRoundTripWithNullColor() {
        val original = account(colorHex = null)
        val result = original.toDto().toDomain()
        assertNull(result.colorHex)
        assertEquals(original, result)
    }

    // --- Transaction ---

    private fun transaction(
        recurringId: RecurringTransactionId? = RecurringTransactionId(9),
        note: String? = "lunch",
    ) = Transaction(
        id = TransactionId(11),
        type = TransactionType.EXPENSE,
        amount = Money(1500, eur),
        occurredOn = LocalDate(2026, 5, 10),
        note = note,
        categoryId = CategoryId(7),
        accountId = AccountId(3),
        createdAt = epoch,
        updatedAt = epoch2,
        recurringId = recurringId,
    )

    @Test
    fun transactionToDtoMapsFields() {
        val dto = transaction().toDto()
        assertEquals(11, dto.id)
        assertEquals("EXPENSE", dto.type)
        assertEquals(1500, dto.amountMinor)
        assertEquals("EUR", dto.currency)
        assertEquals("2026-05-10", dto.occurredOn)
        assertEquals("lunch", dto.note)
        assertEquals(7, dto.categoryId)
        assertEquals(3, dto.accountId)
        assertEquals(9, dto.recurringId)
    }

    @Test
    fun transactionDtoToDomainDefaultOverrides() {
        val domain = transaction().toDto().toDomain()
        assertEquals(UNSAVED_TRANSACTION_ID, domain.id)
        assertEquals(CategoryId(7), domain.categoryId)
        assertEquals(AccountId(3), domain.accountId)
        assertEquals(RecurringTransactionId(9), domain.recurringId)
    }

    @Test
    fun transactionDtoToDomainExplicitOverrides() {
        val domain = transaction().toDto().toDomain(
            idOverride = TransactionId(100),
            catIdOverride = CategoryId(200),
            accIdOverride = AccountId(300),
            recurringIdOverride = RecurringTransactionId(400),
        )
        assertEquals(TransactionId(100), domain.id)
        assertEquals(CategoryId(200), domain.categoryId)
        assertEquals(AccountId(300), domain.accountId)
        assertEquals(RecurringTransactionId(400), domain.recurringId)
    }

    @Test
    fun transactionDtoToDomainNullRecurringAndNote() {
        val domain = transaction(recurringId = null, note = null).toDto().toDomain()
        assertNull(domain.recurringId)
        assertNull(domain.note)
    }

    // --- RecurringTransaction ---

    private fun recurring(
        rule: RecurrenceRule = RecurrenceRule.Daily(2),
        endCondition: EndCondition = EndCondition.Unlimited,
        paymentModeId: PaymentModeId? = PaymentModeId(4),
        note: String? = "rent",
        lastMaterializedDate: LocalDate? = null,
    ) = RecurringTransaction(
        id = RecurringTransactionId(8),
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
        updatedAt = epoch2,
    )

    @Test
    fun recurringToDtoDailyUnlimited() {
        val dto = recurring(RecurrenceRule.Daily(2), EndCondition.Unlimited).toDto()
        assertEquals("DAILY", dto.freqUnit)
        assertEquals(2, dto.freqInterval)
        assertNull(dto.dayOfWeek)
        assertNull(dto.dayOfMonth)
        assertEquals(false, dto.useLastDay)
        assertEquals("UNLIMITED", dto.endKind)
        assertNull(dto.endCount)
        assertNull(dto.endDate)
    }

    @Test
    fun recurringToDtoWeeklyCount() {
        val dto = recurring(RecurrenceRule.Weekly(1, 3), EndCondition.Count(6)).toDto()
        assertEquals("WEEKLY", dto.freqUnit)
        assertEquals(3, dto.dayOfWeek)
        assertEquals("COUNT", dto.endKind)
        assertEquals(6, dto.endCount)
    }

    @Test
    fun recurringToDtoMonthlyOnDayUntil() {
        val until = EndCondition.Until(LocalDate(2027, 1, 1))
        val dto = recurring(RecurrenceRule.Monthly(2, MonthlyDayKind.OnDay(15)), until).toDto()
        assertEquals("MONTHLY", dto.freqUnit)
        assertEquals(2, dto.freqInterval)
        assertEquals(15, dto.dayOfMonth)
        assertEquals(false, dto.useLastDay)
        assertEquals("UNTIL", dto.endKind)
        assertEquals("2027-01-01", dto.endDate)
    }

    @Test
    fun recurringToDtoMonthlyLastDay() {
        val dto = recurring(RecurrenceRule.Monthly(1, MonthlyDayKind.LastDay)).toDto()
        assertEquals("MONTHLY", dto.freqUnit)
        assertNull(dto.dayOfMonth)
        assertEquals(true, dto.useLastDay)
    }

    @Test
    fun recurringDtoToDomainDefaultIdAndOverrides() {
        val domain = recurring().toDto().toDomain()
        assertEquals(UNSAVED_RECURRING_ID, domain.id)
        assertEquals(CategoryId(7), domain.categoryId)
        assertEquals(AccountId(3), domain.accountId)
    }

    @Test
    fun recurringDtoToDomainExplicitOverrides() {
        val domain = recurring().toDto().toDomain(
            idOverride = RecurringTransactionId(50),
            catIdOverride = CategoryId(60),
            accIdOverride = AccountId(70),
        )
        assertEquals(RecurringTransactionId(50), domain.id)
        assertEquals(CategoryId(60), domain.categoryId)
        assertEquals(AccountId(70), domain.accountId)
    }

    @Test
    fun recurringDtoToDomainWeeklyNullDayOfWeekDefaults() {
        val dto = recurring(RecurrenceRule.Weekly(1, 3)).toDto().copy(dayOfWeek = null)
        assertEquals(RecurrenceRule.Weekly(1, 1), dto.toDomain().rule)
    }

    @Test
    fun recurringDtoToDomainMonthlyNullDayOfMonthDefaults() {
        val dto = recurring(RecurrenceRule.Monthly(1, MonthlyDayKind.OnDay(15)))
            .toDto().copy(dayOfMonth = null)
        assertEquals(RecurrenceRule.Monthly(1, MonthlyDayKind.OnDay(1)), dto.toDomain().rule)
    }

    @Test
    fun recurringDtoToDomainUnknownFreqUnitThrows() {
        val dto = recurring().toDto().copy(freqUnit = "YEARLY")
        kotlin.test.assertFailsWith<IllegalStateException> { dto.toDomain() }
    }

    @Test
    fun recurringDtoToDomainCountNullDefaultsToOne() {
        val dto = recurring(endCondition = EndCondition.Count(5)).toDto().copy(endCount = null)
        assertEquals(EndCondition.Count(1), dto.toDomain().endCondition)
    }

    @Test
    fun recurringDtoToDomainUntilNullDateFallsBackToStart() {
        val dto = recurring(endCondition = EndCondition.Until(LocalDate(2027, 1, 1)))
            .toDto().copy(endDate = null)
        assertEquals(EndCondition.Until(LocalDate(2026, 5, 1)), dto.toDomain().endCondition)
    }

    @Test
    fun recurringDtoToDomainUnknownEndKindFallsBackToUnlimited() {
        val dto = recurring().toDto().copy(endKind = "WHATEVER")
        assertEquals(EndCondition.Unlimited, dto.toDomain().endCondition)
    }

    @Test
    fun recurringRoundTripWithOverrides() {
        val original = recurring(
            rule = RecurrenceRule.Monthly(2, MonthlyDayKind.OnDay(20)),
            endCondition = EndCondition.Until(LocalDate(2027, 6, 30)),
            lastMaterializedDate = LocalDate(2026, 7, 1),
        )
        val result = original.toDto().toDomain(
            idOverride = original.id,
            catIdOverride = original.categoryId,
            accIdOverride = original.accountId,
        )
        assertEquals(original, result)
    }

    @Test
    fun recurringRoundTripNullPaymentModeAndNote() {
        val original = recurring(paymentModeId = null, note = null)
        val result = original.toDto().toDomain(
            idOverride = original.id,
            catIdOverride = original.categoryId,
            accIdOverride = original.accountId,
        )
        assertNull(result.paymentModeId)
        assertNull(result.note)
        assertEquals(original, result)
    }

    // --- Budget ---

    private fun budget(
        id: Long = 12,
        categoryId: CategoryId? = CategoryId(7),
        recurringMonths: Int? = 6,
    ) = Budget(
        id = BudgetId(id),
        name = "Groceries",
        amount = Money(8000, eur),
        categoryId = categoryId,
        accountId = AccountId(3),
        periodType = BudgetPeriodType.MONTHLY,
        startYearMonth = YearMonth(2026, 5),
        recurringMonths = recurringMonths,
        createdAt = epoch,
        updatedAt = epoch2,
    )

    @Test
    fun budgetToDtoMapsFields() {
        val dto = budget().toDto()
        assertEquals(12, dto.id)
        assertEquals("Groceries", dto.name)
        assertEquals(8000, dto.amountMinor)
        assertEquals("EUR", dto.currency)
        assertEquals(7, dto.categoryId)
        assertEquals(3, dto.accountId)
        assertEquals("MONTHLY", dto.periodType)
        assertEquals("2026-05", dto.startYearMonth)
        assertEquals(6, dto.recurringMonths)
    }

    @Test
    fun budgetDtoToDomainDefaultId() {
        assertEquals(BudgetId(0), budget().toDto().toDomain().id)
    }

    @Test
    fun budgetDtoToDomainOverrides() {
        val domain = budget().toDto().toDomain(
            idOverride = BudgetId(55),
            accIdOverride = AccountId(66),
        )
        assertEquals(BudgetId(55), domain.id)
        assertEquals(AccountId(66), domain.accountId)
    }

    @Test
    fun budgetDtoToDomainUnknownPeriodTypeFallsBack() {
        val dto = budget().toDto().copy(periodType = "YEARLY")
        assertEquals(BudgetPeriodType.MONTHLY, dto.toDomain().periodType)
    }

    @Test
    fun budgetRoundTripNullCategoryAndRecurringMonths() {
        val original = budget(categoryId = null, recurringMonths = null)
        val result = original.toDto().toDomain(idOverride = original.id)
        assertNull(result.categoryId)
        assertNull(result.recurringMonths)
        assertEquals(original, result)
    }
}
