package com.dv.moneym.data.banksync.internal

import com.dv.moneym.data.banksync.BankAccountLink
import com.dv.moneym.data.banksync.BankSuggestion
import com.dv.moneym.data.banksync.EbDirection
import com.dv.moneym.data.banksync.SuggestionStatus
import com.dv.moneym.data.banksync.db.BankAccountEntity
import com.dv.moneym.data.banksync.db.BankSuggestionEntity
import kotlinx.datetime.LocalDate

internal fun BankAccountEntity.toDomain() = BankAccountLink(
    uid = uid,
    bankName = bankName,
    displayName = displayName,
    iban = iban,
    currency = currency,
    localAccountId = localAccountId,
    enabled = enabled,
    lastSyncedDate = lastSyncedDate?.let { LocalDate.parse(it) },
    lastSyncedAt = lastSyncedAt,
)

internal fun BankAccountLink.toEntity() = BankAccountEntity(
    uid = uid,
    bankName = bankName,
    displayName = displayName,
    iban = iban,
    currency = currency,
    localAccountId = localAccountId,
    enabled = enabled,
    lastSyncedDate = lastSyncedDate?.toString(),
    lastSyncedAt = lastSyncedAt,
)

internal fun BankSuggestionEntity.toDomain() = BankSuggestion(
    id = id,
    externalId = externalId,
    bankAccountUid = bankAccountUid,
    amountMinor = amountMinor,
    currency = currency,
    direction = EbDirection.valueOf(direction),
    bookingDate = LocalDate.parse(bookingDate),
    valueDate = valueDate?.let { LocalDate.parse(it) },
    description = description,
    counterparty = counterparty,
    status = SuggestionStatus.valueOf(status),
    createdTransactionId = createdTransactionId,
    fetchedAt = fetchedAt,
    decidedAt = decidedAt,
)

internal fun BankSuggestion.toEntity() = BankSuggestionEntity(
    id = id,
    externalId = externalId,
    bankAccountUid = bankAccountUid,
    amountMinor = amountMinor,
    currency = currency,
    direction = direction.name,
    bookingDate = bookingDate.toString(),
    valueDate = valueDate?.toString(),
    description = description,
    counterparty = counterparty,
    status = status.name,
    createdTransactionId = createdTransactionId,
    fetchedAt = fetchedAt,
    decidedAt = decidedAt,
)
