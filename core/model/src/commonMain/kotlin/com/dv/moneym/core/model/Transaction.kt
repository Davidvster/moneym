package com.dv.moneym.core.model

import kotlin.time.Instant
import kotlinx.datetime.LocalDate

data class Transaction(
    val id: TransactionId,        // TransactionId(0) = not yet persisted
    val type: TransactionType,
    val amount: Money,
    val occurredOn: LocalDate,
    val note: String?,
    val categoryId: CategoryId,
    val accountId: AccountId,
    val createdAt: Instant,
    val updatedAt: Instant,
    val paymentModeId: PaymentModeId? = null,
    val recurringId: RecurringTransactionId? = null,
)

val UNSAVED_TRANSACTION_ID = TransactionId(0L)
