package com.dv.moneym.data.accounts.internal

import com.dv.moneym.core.model.Account as DomainAccount
import com.dv.moneym.core.model.AccountId
import com.dv.moneym.core.model.AccountType
import com.dv.moneym.core.model.CurrencyCode
import com.dv.moneym.data.accounts.db.AccountEntity
import kotlin.time.Instant

internal fun AccountEntity.toDomain() = DomainAccount(
    id = AccountId(id),
    name = name,
    type = AccountType.valueOf(type),
    currency = CurrencyCode(currency),
    isDefault = isDefault,
    archived = archived,
    createdAt = Instant.fromEpochMilliseconds(createdAt),
    updatedAt = Instant.fromEpochMilliseconds(updatedAt),
)
