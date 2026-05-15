package com.dv.moneym.data.backup

import com.dv.moneym.core.model.Account
import com.dv.moneym.core.model.AccountId
import com.dv.moneym.core.model.AccountType
import com.dv.moneym.core.model.Category
import com.dv.moneym.core.model.CategoryId
import com.dv.moneym.core.model.CurrencyCode
import com.dv.moneym.core.model.Money
import com.dv.moneym.core.model.Transaction
import com.dv.moneym.core.model.TransactionId
import com.dv.moneym.core.model.TransactionType
import com.dv.moneym.core.model.UNSAVED_TRANSACTION_ID
import kotlin.time.Instant
import kotlinx.datetime.LocalDate

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
)

fun TransactionDto.toDomain(
    idOverride: TransactionId = UNSAVED_TRANSACTION_ID,
    catIdOverride: CategoryId = CategoryId(categoryId),
    accIdOverride: AccountId = AccountId(accountId),
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
)
