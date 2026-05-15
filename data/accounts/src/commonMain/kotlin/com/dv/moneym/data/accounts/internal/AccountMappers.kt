package com.dv.moneym.data.accounts.internal

import com.dv.moneym.core.model.Account as DomainAccount
import com.dv.moneym.core.model.AccountId
import com.dv.moneym.core.model.AccountType
import com.dv.moneym.core.model.CurrencyCode
import com.dv.moneym.data.accounts.Account as AccountRow
import kotlin.time.Instant

internal fun AccountRow.toDomain() = DomainAccount(
    id = AccountId(id),
    name = name,
    type = AccountType.valueOf(type),
    currency = CurrencyCode(currency),
    isDefault = is_default != 0L,
    archived = archived != 0L,
    createdAt = Instant.fromEpochMilliseconds(created_at),
    updatedAt = Instant.fromEpochMilliseconds(updated_at),
)
