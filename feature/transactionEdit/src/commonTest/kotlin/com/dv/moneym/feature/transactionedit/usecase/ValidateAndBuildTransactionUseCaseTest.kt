package com.dv.moneym.feature.transactionedit.usecase

import com.dv.moneym.core.model.Account
import com.dv.moneym.core.model.AccountId
import com.dv.moneym.core.model.AccountType
import com.dv.moneym.core.model.CategoryId
import com.dv.moneym.core.model.CurrencyCode
import com.dv.moneym.core.model.EndCondition
import com.dv.moneym.core.model.MonthlyDayKind
import com.dv.moneym.core.model.PaymentModeId
import com.dv.moneym.core.model.RecurrenceRule
import com.dv.moneym.core.model.TransactionId
import com.dv.moneym.core.model.TransactionType
import com.dv.moneym.core.model.UNSAVED_TRANSACTION_ID
import com.dv.moneym.feature.transactionedit.EndKind
import com.dv.moneym.feature.transactionedit.FreqUnit
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.time.Instant

class ValidateAndBuildTransactionUseCaseTest {

    private val epoch = Instant.fromEpochMilliseconds(0)
    private val now = Instant.parse("2026-05-15T12:00:00Z")
    private val useCase = ValidateAndBuildTransactionUseCase()

    private val account = Account(
        id = AccountId(1),
        name = "Main",
        type = AccountType.CASH,
        currency = CurrencyCode("EUR"),
        isDefault = true,
        archived = false,
        createdAt = epoch,
        updatedAt = epoch,
    )
    private val date = LocalDate(2026, 5, 10)

    private fun call(
        existingId: TransactionId? = null,
        amountText: String = "10.00",
        date: LocalDate? = this.date,
        categoryId: CategoryId? = CategoryId(1),
        accountId: AccountId? = AccountId(1),
        accounts: List<Account> = listOf(account),
        note: String = "",
        showPaymentMode: Boolean = false,
        paymentModeId: PaymentModeId? = null,
        recurrence: RecurrenceInput? = null,
    ): ValidationOutcome {
        val base = ValidateAndBuildTransactionUseCase()
        return if (recurrence == null) {
            base(
                existingId = existingId,
                type = TransactionType.EXPENSE,
                amountText = amountText,
                date = date,
                selectedCategoryId = categoryId,
                selectedAccountId = accountId,
                note = note,
                availableAccounts = accounts,
                showPaymentMode = showPaymentMode,
                selectedPaymentModeId = paymentModeId,
                now = now,
            )
        } else {
            base(
                existingId = existingId,
                type = TransactionType.EXPENSE,
                amountText = amountText,
                date = date,
                selectedCategoryId = categoryId,
                selectedAccountId = accountId,
                note = note,
                availableAccounts = accounts,
                showPaymentMode = showPaymentMode,
                selectedPaymentModeId = paymentModeId,
                now = now,
                recurrence = recurrence,
            )
        }
    }

    private fun recurrence(
        freqUnit: FreqUnit = FreqUnit.MONTHS,
        freqInterval: Int = 1,
        weekDay: Int = 1,
        monthDayKind: MonthlyDayKind = MonthlyDayKind.OnDay(1),
        endKind: EndKind = EndKind.UNLIMITED,
        endCount: Int = 1,
        endDate: LocalDate? = null,
    ) = RecurrenceInput(
        isRecurring = true,
        freqUnit = freqUnit,
        freqInterval = freqInterval,
        weekDay = weekDay,
        monthDayKind = monthDayKind,
        endKind = endKind,
        endCount = endCount,
        endDate = endDate,
    )

    @Test
    fun valid_non_recurring_build() {
        val out = assertIs<ValidationOutcome.Ok>(call(note = "  lunch  "))
        assertEquals(UNSAVED_TRANSACTION_ID, out.transaction.id)
        assertEquals(1000L, out.transaction.amount.minorUnits)
        assertEquals("EUR", out.transaction.amount.currency.value)
        assertEquals("lunch", out.transaction.note)
        assertNull(out.rule)
    }

    @Test
    fun existing_id_is_preserved() {
        val out = assertIs<ValidationOutcome.Ok>(call(existingId = TransactionId(42)))
        assertEquals(TransactionId(42), out.transaction.id)
    }

    @Test
    fun blank_note_becomes_null() {
        val out = assertIs<ValidationOutcome.Ok>(call(note = "   "))
        assertNull(out.transaction.note)
    }

    @Test
    fun unknown_account_falls_back_to_usd() {
        val out = assertIs<ValidationOutcome.Ok>(call(accounts = emptyList()))
        assertEquals("USD", out.transaction.amount.currency.value)
    }

    @Test
    fun payment_mode_only_kept_when_shown() {
        val shown = assertIs<ValidationOutcome.Ok>(
            call(showPaymentMode = true, paymentModeId = PaymentModeId(7)),
        )
        assertEquals(PaymentModeId(7), shown.transaction.paymentModeId)

        val hidden = assertIs<ValidationOutcome.Ok>(
            call(showPaymentMode = false, paymentModeId = PaymentModeId(7)),
        )
        assertNull(hidden.transaction.paymentModeId)
    }

    @Test
    fun invalid_amount_rejected() {
        assertEquals(ValidationOutcome.Reason.InvalidAmount, reason(call(amountText = "abc")))
        assertEquals(ValidationOutcome.Reason.InvalidAmount, reason(call(amountText = "0")))
        assertEquals(ValidationOutcome.Reason.InvalidAmount, reason(call(amountText = "")))
    }

    @Test
    fun missing_category_rejected() {
        assertEquals(ValidationOutcome.Reason.MissingCategory, reason(call(categoryId = null)))
    }

    @Test
    fun missing_date_or_account_rejected() {
        assertEquals(ValidationOutcome.Reason.MissingDateOrAccount, reason(call(date = null)))
        assertEquals(ValidationOutcome.Reason.MissingDateOrAccount, reason(call(accountId = null)))
    }

    @Test
    fun recurring_daily_builds_rule() {
        val out = assertIs<ValidationOutcome.Ok>(
            call(recurrence = recurrence(freqUnit = FreqUnit.DAYS, freqInterval = 3)),
        )
        val recurring = assertNotNull(out.rule)
        val rule = assertIs<RecurrenceRule.Daily>(recurring.rule)
        assertEquals(3, rule.interval)
        assertEquals(EndCondition.Unlimited, recurring.endCondition)
        assertEquals(date, recurring.startDate)
    }

    @Test
    fun recurring_weekly_validates_day_of_week() {
        val ok = assertIs<ValidationOutcome.Ok>(
            call(recurrence = recurrence(freqUnit = FreqUnit.WEEKS, weekDay = 5)),
        )
        assertIs<RecurrenceRule.Weekly>(ok.rule!!.rule)

        assertEquals(
            ValidationOutcome.Reason.InvalidRecurrence,
            reason(call(recurrence = recurrence(freqUnit = FreqUnit.WEEKS, weekDay = 8))),
        )
    }

    @Test
    fun recurring_monthly_on_day_out_of_range_rejected() {
        assertEquals(
            ValidationOutcome.Reason.InvalidRecurrence,
            reason(call(recurrence = recurrence(monthDayKind = MonthlyDayKind.OnDay(29)))),
        )
        val lastDay = assertIs<ValidationOutcome.Ok>(
            call(recurrence = recurrence(monthDayKind = MonthlyDayKind.LastDay)),
        )
        assertIs<RecurrenceRule.Monthly>(lastDay.rule!!.rule)
    }

    @Test
    fun freq_interval_out_of_range_rejected() {
        assertEquals(
            ValidationOutcome.Reason.InvalidRecurrence,
            reason(call(recurrence = recurrence(freqInterval = 0))),
        )
        assertEquals(
            ValidationOutcome.Reason.InvalidRecurrence,
            reason(call(recurrence = recurrence(freqInterval = 31))),
        )
    }

    @Test
    fun end_count_zero_rejected_positive_builds_count() {
        assertEquals(
            ValidationOutcome.Reason.InvalidRecurrence,
            reason(call(recurrence = recurrence(endKind = EndKind.COUNT, endCount = 0))),
        )
        val ok = assertIs<ValidationOutcome.Ok>(
            call(recurrence = recurrence(endKind = EndKind.COUNT, endCount = 4)),
        )
        assertEquals(EndCondition.Count(4), ok.rule!!.endCondition)
    }

    @Test
    fun end_until_requires_date_not_before_start() {
        assertEquals(
            ValidationOutcome.Reason.InvalidRecurrence,
            reason(call(recurrence = recurrence(endKind = EndKind.UNTIL, endDate = null))),
        )
        assertEquals(
            ValidationOutcome.Reason.InvalidRecurrence,
            reason(call(recurrence = recurrence(endKind = EndKind.UNTIL, endDate = LocalDate(2026, 5, 1)))),
        )
        val ok = assertIs<ValidationOutcome.Ok>(
            call(recurrence = recurrence(endKind = EndKind.UNTIL, endDate = LocalDate(2026, 12, 1))),
        )
        assertEquals(EndCondition.Until(LocalDate(2026, 12, 1)), ok.rule!!.endCondition)
    }

    private fun reason(outcome: ValidationOutcome): ValidationOutcome.Reason =
        assertIs<ValidationOutcome.Invalid>(outcome).reason
}
