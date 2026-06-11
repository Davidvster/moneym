package com.dv.moneym.data.transactions.internal

import com.dv.moneym.core.model.AccountId
import com.dv.moneym.core.model.CategoryId
import com.dv.moneym.core.model.CurrencyCode
import com.dv.moneym.core.model.Money
import com.dv.moneym.core.model.PaymentModeId
import com.dv.moneym.core.model.RecurringTransactionId
import com.dv.moneym.core.model.Transaction
import com.dv.moneym.core.model.TransactionId
import com.dv.moneym.core.model.TransactionType
import com.dv.moneym.data.transactions.TransactionSyncRow
import com.dv.moneym.data.transactions.db.TransactionEntity
import kotlin.time.Instant
import kotlinx.datetime.LocalDate

internal fun TransactionEntity.toDomain() = Transaction(
    id = TransactionId(id),
    type = TransactionType.valueOf(type),
    amount = Money(amountMinor, CurrencyCode(currency)),
    occurredOn = LocalDate.parse(occurredOn),
    note = note,
    categoryId = CategoryId(categoryId),
    accountId = AccountId(accountId),
    createdAt = Instant.fromEpochMilliseconds(createdAt),
    updatedAt = Instant.fromEpochMilliseconds(updatedAt),
    paymentModeId = paymentModeId?.let { PaymentModeId(it) },
    recurringId = recurringId?.let { RecurringTransactionId(it) },
)

internal fun TransactionEntity.toSyncRow() = TransactionSyncRow(
    id = id,
    syncId = syncId,
    type = type,
    amountMinor = amountMinor,
    currency = currency,
    occurredOn = occurredOn,
    note = note,
    categoryId = categoryId,
    accountId = accountId,
    paymentModeId = paymentModeId,
    recurringId = recurringId,
    deleted = deleted,
    createdAt = createdAt,
    updatedAt = updatedAt,
    externalId = externalId,
)

internal fun yearMonthKey(year: Int, month: Int): String =
    "${year}-${month.toString().padStart(2, '0')}"
