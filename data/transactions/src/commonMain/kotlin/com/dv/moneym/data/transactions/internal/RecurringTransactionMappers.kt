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
import com.dv.moneym.data.transactions.RecurringSyncRow
import com.dv.moneym.data.transactions.db.RecurringTransactionEntity
import kotlin.time.Instant
import kotlinx.datetime.LocalDate

internal fun RecurringTransactionEntity.toDomain(): RecurringTransaction {
    val rule: RecurrenceRule = when (freqUnit) {
        "DAILY" -> RecurrenceRule.Daily(freqInterval)
        "WEEKLY" -> RecurrenceRule.Weekly(freqInterval, dayOfWeek ?: 1)
        "MONTHLY" -> RecurrenceRule.Monthly(
            interval = freqInterval,
            dayKind = if (useLastDay) MonthlyDayKind.LastDay
            else MonthlyDayKind.OnDay(dayOfMonth ?: 1),
        )
        else -> error("Unknown freq_unit: $freqUnit")
    }
    val end: EndCondition = when (endKind) {
        "UNLIMITED" -> EndCondition.Unlimited
        "COUNT" -> EndCondition.Count(endCount ?: error("COUNT end without count"))
        "UNTIL" -> EndCondition.Until(LocalDate.parse(endDate ?: error("UNTIL end without date")))
        else -> error("Unknown end_kind: $endKind")
    }
    return RecurringTransaction(
        id = RecurringTransactionId(id),
        type = TransactionType.valueOf(type),
        amount = Money(amountMinor, CurrencyCode(currency)),
        note = note,
        categoryId = CategoryId(categoryId),
        accountId = AccountId(accountId),
        paymentModeId = paymentModeId?.let { PaymentModeId(it) },
        startDate = LocalDate.parse(startDate),
        rule = rule,
        endCondition = end,
        lastMaterializedDate = lastMaterializedDate?.let { LocalDate.parse(it) },
        createdAt = Instant.fromEpochMilliseconds(createdAt),
        updatedAt = Instant.fromEpochMilliseconds(updatedAt),
    )
}

internal fun RecurringTransaction.toEntity(): RecurringTransactionEntity {
    val (freqUnit, interval, dow, dom, lastDay) = when (val r = rule) {
        is RecurrenceRule.Daily -> Quintuple("DAILY", r.interval, null, null, false)
        is RecurrenceRule.Weekly -> Quintuple("WEEKLY", r.interval, r.dayOfWeek, null, false)
        is RecurrenceRule.Monthly -> when (val d = r.dayKind) {
            is MonthlyDayKind.OnDay -> Quintuple("MONTHLY", r.interval, null, d.day, false)
            MonthlyDayKind.LastDay -> Quintuple("MONTHLY", r.interval, null, null, true)
        }
    }
    val (endKind, endCount, endDate) = when (val e = endCondition) {
        EndCondition.Unlimited -> Triple("UNLIMITED", null, null)
        is EndCondition.Count -> Triple("COUNT", e.occurrences, null)
        is EndCondition.Until -> Triple("UNTIL", null, e.date.toString())
    }
    return RecurringTransactionEntity(
        id = id.value,
        type = type.name,
        amountMinor = amount.minorUnits,
        currency = amount.currency.value,
        note = note,
        categoryId = categoryId.value,
        accountId = accountId.value,
        paymentModeId = paymentModeId?.value,
        startDate = startDate.toString(),
        freqUnit = freqUnit,
        freqInterval = interval,
        dayOfWeek = dow,
        dayOfMonth = dom,
        useLastDay = lastDay,
        endKind = endKind,
        endCount = endCount,
        endDate = endDate,
        lastMaterializedDate = lastMaterializedDate?.toString(),
        createdAt = createdAt.toEpochMilliseconds(),
        updatedAt = updatedAt.toEpochMilliseconds(),
    )
}

internal fun RecurringTransactionEntity.toSyncRow() = RecurringSyncRow(
    id = id,
    syncId = syncId,
    type = type,
    amountMinor = amountMinor,
    currency = currency,
    note = note,
    categoryId = categoryId,
    accountId = accountId,
    paymentModeId = paymentModeId,
    startDate = startDate,
    freqUnit = freqUnit,
    freqInterval = freqInterval,
    dayOfWeek = dayOfWeek,
    dayOfMonth = dayOfMonth,
    useLastDay = useLastDay,
    endKind = endKind,
    endCount = endCount,
    endDate = endDate,
    lastMaterializedDate = lastMaterializedDate,
    deleted = deleted,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

private data class Quintuple<A, B, C, D, E>(val a: A, val b: B, val c: C, val d: D, val e: E)
