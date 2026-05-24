package com.dv.moneym.feature.transactionedit.usecase

import com.dv.moneym.core.model.Account
import com.dv.moneym.core.model.AccountId
import com.dv.moneym.core.model.CategoryId
import com.dv.moneym.core.model.CurrencyCode
import com.dv.moneym.core.model.Money
import com.dv.moneym.core.model.PaymentModeId
import com.dv.moneym.core.model.Transaction
import com.dv.moneym.core.model.TransactionId
import com.dv.moneym.core.model.TransactionType
import com.dv.moneym.core.model.UNSAVED_TRANSACTION_ID
import com.dv.moneym.core.model.toMinorUnits
import kotlinx.datetime.LocalDate
import kotlin.time.Instant

sealed interface ValidationOutcome {
    data class Ok(val transaction: Transaction) : ValidationOutcome
    enum class Reason { InvalidAmount, MissingCategory, MissingDateOrAccount }
    data class Invalid(val reason: Reason) : ValidationOutcome
}

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
            paymentModeId = if (showPaymentMode) selectedPaymentModeId else null,
        )
        return ValidationOutcome.Ok(txn)
    }
}
