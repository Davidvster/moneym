package com.dv.moneym.data.transactions.internal

import com.dv.moneym.core.model.AccountId
import com.dv.moneym.core.model.CategoryId
import com.dv.moneym.core.model.CurrencyCode
import com.dv.moneym.core.model.Money
import com.dv.moneym.core.model.Transaction
import com.dv.moneym.core.model.TransactionId
import com.dv.moneym.core.model.TransactionType
import kotlin.time.Instant
import kotlinx.datetime.LocalDate
import com.dv.moneym.data.transactions.TransactionEntry as TransactionRow

internal fun TransactionRow.toDomain() = Transaction(
    id = TransactionId(id),
    type = TransactionType.valueOf(type),
    amount = Money(amount_minor, CurrencyCode(currency)),
    occurredOn = LocalDate.parse(occurred_on),
    note = note,
    categoryId = CategoryId(category_id),
    accountId = AccountId(account_id),
    createdAt = Instant.fromEpochMilliseconds(created_at),
    updatedAt = Instant.fromEpochMilliseconds(updated_at),
)

internal fun yearMonthKey(year: Int, month: Int): String =
    "${year}-${month.toString().padStart(2, '0')}"
