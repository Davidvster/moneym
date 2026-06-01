package com.dv.moneym.data.budgets.internal

import com.dv.moneym.core.model.AccountId
import com.dv.moneym.core.model.Budget
import com.dv.moneym.core.model.BudgetId
import com.dv.moneym.core.model.BudgetPeriodType
import com.dv.moneym.core.model.CategoryId
import com.dv.moneym.core.model.CurrencyCode
import com.dv.moneym.core.model.Money
import com.dv.moneym.core.model.YearMonth
import com.dv.moneym.data.budgets.BudgetSyncRow
import com.dv.moneym.data.budgets.db.BudgetEntity
import kotlin.time.Instant

internal fun BudgetEntity.toDomain(): Budget = Budget(
    id = BudgetId(id),
    name = name,
    amount = Money(amountMinor, CurrencyCode(currency)),
    categoryId = categoryId?.let(::CategoryId),
    accountId = AccountId(accountId),
    periodType = parsePeriodType(periodType),
    startYearMonth = parseYearMonth(startYearMonth),
    recurringMonths = recurringMonths,
    createdAt = Instant.fromEpochMilliseconds(createdAt),
    updatedAt = Instant.fromEpochMilliseconds(updatedAt),
)

internal fun BudgetEntity.toSyncRow() = BudgetSyncRow(
    id = id,
    syncId = syncId,
    name = name,
    amountMinor = amountMinor,
    currency = currency,
    categoryId = categoryId,
    accountId = accountId,
    periodType = periodType,
    startYearMonth = startYearMonth,
    recurringMonths = recurringMonths,
    deleted = deleted,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

internal fun parsePeriodType(raw: String): BudgetPeriodType =
    BudgetPeriodType.entries.firstOrNull { it.name == raw } ?: BudgetPeriodType.MONTHLY

internal fun parseYearMonth(raw: String): YearMonth {
    val parts = raw.split('-')
    return YearMonth(parts[0].toInt(), parts[1].toInt())
}
