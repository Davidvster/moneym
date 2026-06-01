package com.dv.moneym.data.accounts.internal

import com.dv.moneym.core.model.AccountId
import com.dv.moneym.core.model.AccountType
import com.dv.moneym.core.model.CurrencyCode
import com.dv.moneym.data.accounts.AccountSyncRow
import com.dv.moneym.data.accounts.db.AccountEntity
import kotlin.time.Instant
import com.dv.moneym.core.model.Account as DomainAccount

internal fun AccountEntity.toDomain() = DomainAccount(
    id = AccountId(id),
    name = name,
    type = AccountType.valueOf(type),
    currency = CurrencyCode(currency),
    isDefault = isDefault,
    archived = archived,
    createdAt = Instant.fromEpochMilliseconds(createdAt),
    updatedAt = Instant.fromEpochMilliseconds(updatedAt),
    colorHex = colorHex,
)

internal fun AccountEntity.toSyncRow() = AccountSyncRow(
    id = id,
    syncId = syncId,
    name = name,
    type = type,
    currency = currency,
    isDefault = isDefault,
    archived = archived,
    colorHex = colorHex,
    deleted = deleted,
    createdAt = createdAt,
    updatedAt = updatedAt,
)
