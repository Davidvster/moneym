package com.dv.moneym.feature.transactionedit.usecase

import com.dv.moneym.core.model.Account
import com.dv.moneym.core.model.AccountId
import com.dv.moneym.core.model.CategoryId
import com.dv.moneym.core.model.CurrencyCode
import com.dv.moneym.core.model.EndCondition
import com.dv.moneym.core.model.Money
import com.dv.moneym.core.model.MonthlyDayKind
import com.dv.moneym.core.model.PaymentModeId
import com.dv.moneym.core.model.RecurrenceRule
import com.dv.moneym.core.model.RecurringTransaction
import com.dv.moneym.core.model.Transaction
import com.dv.moneym.core.model.TransactionId
import com.dv.moneym.core.model.TransactionType
import com.dv.moneym.core.model.UNSAVED_RECURRING_ID
import com.dv.moneym.core.model.UNSAVED_TRANSACTION_ID
import com.dv.moneym.core.model.toMinorUnits
import com.dv.moneym.feature.transactionedit.EndKind
import com.dv.moneym.feature.transactionedit.FreqUnit
import kotlin.time.Instant
import kotlinx.datetime.LocalDate

sealed interface ValidationOutcome {
    data class Ok(
        val transaction: Transaction,
        val rule: RecurringTransaction? = null,
    ) : ValidationOutcome

    enum class Reason {
        InvalidAmount,
        MissingCategory,
        MissingDateOrAccount,
        InvalidRecurrence,
    }

    data class Invalid(val reason: Reason) : ValidationOutcome
}

data class RecurrenceInput(
    val isRecurring: Boolean,
    val freqUnit: FreqUnit,
    val freqInterval: Int,
    val weekDay: Int,
    val monthDayKind: MonthlyDayKind,
    val endKind: EndKind,
    val endCount: Int,
    val endDate: LocalDate?,
)

class ValidateAndBuildTransactionUseCase {
    operator fun invoke(
        existingId: TransactionId?,
        type: TransactionType,
        amountText: String,
        date: LocalDate?,
        selectedCategoryId: CategoryId?,
        selectedAccountId: AccountId?,
        note: String,
        availableAccounts: List<Account>,
        showPaymentMode: Boolean,
        selectedPaymentModeId: PaymentModeId?,
        now: Instant,
        recurrence: RecurrenceInput = RecurrenceInput(
            isRecurring = false,
            freqUnit = FreqUnit.MONTHS,
            freqInterval = 1,
            weekDay = 1,
            monthDayKind = MonthlyDayKind.OnDay(1),
            endKind = EndKind.UNLIMITED,
            endCount = 1,
            endDate = null,
        ),
    ): ValidationOutcome {
        val minorUnits = amountText.toMinorUnits()
        if (minorUnits == null || minorUnits <= 0) {
            return ValidationOutcome.Invalid(ValidationOutcome.Reason.InvalidAmount)
        }
        if (selectedCategoryId == null) {
            return ValidationOutcome.Invalid(ValidationOutcome.Reason.MissingCategory)
        }
        if (date == null || selectedAccountId == null) {
            return ValidationOutcome.Invalid(ValidationOutcome.Reason.MissingDateOrAccount)
        }
        val account = availableAccounts.firstOrNull { it.id == selectedAccountId }
        val currency = account?.currency ?: CurrencyCode("USD")
        val paymentMode = if (showPaymentMode) selectedPaymentModeId else null
        val txn = Transaction(
            id = existingId ?: UNSAVED_TRANSACTION_ID,
            type = type,
            amount = Money(minorUnits, currency),
            occurredOn = date,
            note = note.trim().ifEmpty { null },
            categoryId = selectedCategoryId,
            accountId = selectedAccountId,
            createdAt = now,
            updatedAt = now,
            paymentModeId = paymentMode,
        )

        if (!recurrence.isRecurring) return ValidationOutcome.Ok(txn)

        val rule = buildRecurrence(
            input = recurrence,
            startDate = date,
        ) ?: return ValidationOutcome.Invalid(ValidationOutcome.Reason.InvalidRecurrence)

        val recurring = RecurringTransaction(
            id = UNSAVED_RECURRING_ID,
            type = type,
            amount = Money(minorUnits, currency),
            note = note.trim().ifEmpty { null },
            categoryId = selectedCategoryId,
            accountId = selectedAccountId,
            paymentModeId = paymentMode,
            startDate = date,
            rule = rule.first,
            endCondition = rule.second,
            lastMaterializedDate = null,
            createdAt = now,
            updatedAt = now,
        )
        return ValidationOutcome.Ok(txn, recurring)
    }

    private fun buildRecurrence(
        input: RecurrenceInput,
        startDate: LocalDate,
    ): Pair<RecurrenceRule, EndCondition>? {
        if (input.freqInterval !in 1..30) return null
        val rule: RecurrenceRule = when (input.freqUnit) {
            FreqUnit.DAYS -> RecurrenceRule.Daily(input.freqInterval)
            FreqUnit.WEEKS -> {
                if (input.weekDay !in 1..7) return null
                RecurrenceRule.Weekly(input.freqInterval, input.weekDay)
            }
            FreqUnit.MONTHS -> {
                val kind = input.monthDayKind
                if (kind is MonthlyDayKind.OnDay && kind.day !in 1..28) return null
                RecurrenceRule.Monthly(input.freqInterval, kind)
            }
        }
        val end: EndCondition = when (input.endKind) {
            EndKind.UNLIMITED -> EndCondition.Unlimited
            EndKind.COUNT -> {
                if (input.endCount < 1) return null
                EndCondition.Count(input.endCount)
            }
            EndKind.UNTIL -> {
                val until = input.endDate ?: return null
                if (until < startDate) return null
                EndCondition.Until(until)
            }
        }
        return rule to end
    }
}
