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
import com.dv.moneym.data.overview.OverviewAiWidget
import com.dv.moneym.data.overview.OverviewBlockId
import com.dv.moneym.data.overview.OverviewLayoutBlock
import com.dv.moneym.data.overview.OverviewLayoutPrefs
import kotlinx.datetime.LocalDate
import kotlin.time.Instant

fun Category.toDto() = CategoryDto(
    id = id.value,
    name = name,
    iconKey = iconKey,
    colorHex = colorHex,
    isUserCreated = isUserCreated,
    archived = archived,
    createdAt = createdAt.toEpochMilliseconds(),
    updatedAt = updatedAt.toEpochMilliseconds(),
)

fun CategoryDto.toDomain(idOverride: CategoryId = CategoryId(id)) = Category(
    id = idOverride,
    name = name,
    iconKey = iconKey,
    colorHex = colorHex,
    isUserCreated = isUserCreated,
    archived = archived,
    createdAt = Instant.fromEpochMilliseconds(createdAt),
    updatedAt = Instant.fromEpochMilliseconds(updatedAt),
)

fun Account.toDto() = AccountDto(
    id = id.value,
    name = name,
    type = type.name,
    currency = currency.value,
    isDefault = isDefault,
    archived = archived,
    createdAt = createdAt.toEpochMilliseconds(),
    updatedAt = updatedAt.toEpochMilliseconds(),
    colorHex = colorHex,
)

fun AccountDto.toDomain(idOverride: AccountId = AccountId(id)) = Account(
    id = idOverride,
    name = name,
    type = AccountType.valueOf(type),
    currency = CurrencyCode(currency),
    isDefault = isDefault,
    archived = archived,
    createdAt = Instant.fromEpochMilliseconds(createdAt),
    updatedAt = Instant.fromEpochMilliseconds(updatedAt),
    colorHex = colorHex,
)

fun Transaction.toDto() = TransactionDto(
    id = id.value,
    type = type.name,
    amountMinor = amount.minorUnits,
    currency = amount.currency.value,
    occurredOn = occurredOn.toString(),
    note = note,
    categoryId = categoryId.value,
    accountId = accountId.value,
    createdAt = createdAt.toEpochMilliseconds(),
    updatedAt = updatedAt.toEpochMilliseconds(),
    recurringId = recurringId?.value,
)

fun TransactionDto.toDomain(
    idOverride: TransactionId = UNSAVED_TRANSACTION_ID,
    catIdOverride: CategoryId = CategoryId(categoryId),
    accIdOverride: AccountId = AccountId(accountId),
    recurringIdOverride: RecurringTransactionId? = recurringId?.let { RecurringTransactionId(it) },
) = Transaction(
    id = idOverride,
    type = TransactionType.valueOf(type),
    amount = Money(amountMinor, CurrencyCode(currency)),
    occurredOn = LocalDate.parse(occurredOn),
    note = note,
    categoryId = catIdOverride,
    accountId = accIdOverride,
    createdAt = Instant.fromEpochMilliseconds(createdAt),
    updatedAt = Instant.fromEpochMilliseconds(updatedAt),
    recurringId = recurringIdOverride,
)

fun RecurringTransaction.toDto(): RecurringTransactionDto {
    val (freqUnit, dow, dom, lastDay) = when (val r = rule) {
        is RecurrenceRule.Daily -> Quad("DAILY", null, null, false)
        is RecurrenceRule.Weekly -> Quad("WEEKLY", r.dayOfWeek, null, false)
        is RecurrenceRule.Monthly -> when (val d = r.dayKind) {
            is MonthlyDayKind.OnDay -> Quad("MONTHLY", null, d.day, false)
            MonthlyDayKind.LastDay -> Quad("MONTHLY", null, null, true)
        }
    }
    val (endKindStr, endCountVal, endDateVal) = when (val e = endCondition) {
        EndCondition.Unlimited -> Triple("UNLIMITED", null, null)
        is EndCondition.Count -> Triple("COUNT", e.occurrences, null)
        is EndCondition.Until -> Triple("UNTIL", null, e.date.toString())
    }
    return RecurringTransactionDto(
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
        freqInterval = rule.interval,
        dayOfWeek = dow,
        dayOfMonth = dom,
        useLastDay = lastDay,
        endKind = endKindStr,
        endCount = endCountVal,
        endDate = endDateVal,
        lastMaterializedDate = lastMaterializedDate?.toString(),
        createdAt = createdAt.toEpochMilliseconds(),
        updatedAt = updatedAt.toEpochMilliseconds(),
    )
}

fun RecurringTransactionDto.toDomain(
    idOverride: RecurringTransactionId = UNSAVED_RECURRING_ID,
    catIdOverride: CategoryId = CategoryId(categoryId),
    accIdOverride: AccountId = AccountId(accountId),
): RecurringTransaction {
    val rule: RecurrenceRule = when (freqUnit) {
        "DAILY" -> RecurrenceRule.Daily(freqInterval)
        "WEEKLY" -> RecurrenceRule.Weekly(freqInterval, dayOfWeek ?: 1)
        "MONTHLY" -> RecurrenceRule.Monthly(
            freqInterval,
            if (useLastDay) MonthlyDayKind.LastDay else MonthlyDayKind.OnDay(dayOfMonth ?: 1),
        )

        else -> error("Unknown freqUnit: $freqUnit")
    }
    val end: EndCondition = when (endKind) {
        "UNLIMITED" -> EndCondition.Unlimited
        "COUNT" -> EndCondition.Count(endCount ?: 1)
        "UNTIL" -> EndCondition.Until(LocalDate.parse(endDate ?: startDate))
        else -> EndCondition.Unlimited
    }
    return RecurringTransaction(
        id = idOverride,
        type = TransactionType.valueOf(type),
        amount = Money(amountMinor, CurrencyCode(currency)),
        note = note,
        categoryId = catIdOverride,
        accountId = accIdOverride,
        paymentModeId = paymentModeId?.let { PaymentModeId(it) },
        startDate = LocalDate.parse(startDate),
        rule = rule,
        endCondition = end,
        lastMaterializedDate = lastMaterializedDate?.let { LocalDate.parse(it) },
        createdAt = Instant.fromEpochMilliseconds(createdAt),
        updatedAt = Instant.fromEpochMilliseconds(updatedAt),
    )
}

private data class Quad<A, B, C, D>(val a: A, val b: B, val c: C, val d: D)

fun Budget.toDto() = BudgetDto(
    id = id.value,
    name = name,
    amountMinor = amount.minorUnits,
    currency = amount.currency.value,
    categoryId = categoryId?.value,
    accountId = accountId.value,
    periodType = periodType.name,
    startYearMonth = startYearMonth.toString(),
    recurringMonths = recurringMonths,
    createdAt = createdAt.toEpochMilliseconds(),
    updatedAt = updatedAt.toEpochMilliseconds(),
)

fun BudgetDto.toDomain(
    idOverride: BudgetId = BudgetId(0),
    accIdOverride: AccountId = AccountId(accountId),
) = Budget(
    id = idOverride,
    name = name,
    amount = Money(amountMinor, CurrencyCode(currency)),
    categoryId = categoryId?.let(::CategoryId),
    accountId = accIdOverride,
    periodType = BudgetPeriodType.entries.firstOrNull { it.name == periodType }
        ?: BudgetPeriodType.MONTHLY,
    startYearMonth = startYearMonth.split('-').let { p -> YearMonth(p[0].toInt(), p[1].toInt()) },
    recurringMonths = recurringMonths,
    createdAt = Instant.fromEpochMilliseconds(createdAt),
    updatedAt = Instant.fromEpochMilliseconds(updatedAt),
)

fun OverviewLayoutPrefs.toDto() = OverviewLayoutPrefsDto(
    blocks = blocks.map { it.toDto() },
)

fun OverviewLayoutPrefsDto.toDomain() = OverviewLayoutPrefs(
    blocks = blocks.map { it.toDomain() },
)

fun OverviewLayoutBlock.toDto() = OverviewLayoutBlockDto(
    blockId = blockId.value,
    sortOrder = sortOrder,
    visible = visible,
)

fun OverviewLayoutBlockDto.toDomain() = OverviewLayoutBlock(
    blockId = OverviewBlockId(blockId),
    sortOrder = sortOrder,
    visible = visible,
)

fun OverviewAiWidget.toDto() = OverviewAiWidgetDto(
    id = id,
    title = title,
    prompt = prompt,
    a2uiJson = a2uiJson,
    enabled = enabled,
    sortOrder = sortOrder,
    createdAt = createdAt.toEpochMilliseconds(),
    updatedAt = updatedAt.toEpochMilliseconds(),
    lastGeneratedAt = lastGeneratedAt?.toEpochMilliseconds(),
    lastGenerationEngineId = lastGenerationEngineId,
)

fun OverviewAiWidgetDto.toDomain(idOverride: Long = id) = OverviewAiWidget(
    id = idOverride,
    title = title,
    prompt = prompt,
    a2uiJson = a2uiJson,
    enabled = enabled,
    sortOrder = sortOrder,
    createdAt = Instant.fromEpochMilliseconds(createdAt),
    updatedAt = Instant.fromEpochMilliseconds(updatedAt),
    lastGeneratedAt = lastGeneratedAt?.let(Instant::fromEpochMilliseconds),
    lastGenerationEngineId = lastGenerationEngineId,
)
