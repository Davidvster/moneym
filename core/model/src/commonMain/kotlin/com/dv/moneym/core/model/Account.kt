package com.dv.moneym.core.model

import kotlin.time.Instant

enum class AccountType { CASH, BANK, CARD, OTHER }

data class Account(
    val id: AccountId,
    val name: String,
    val type: AccountType,
    val currency: CurrencyCode,
    val isDefault: Boolean,
    val archived: Boolean,
    val createdAt: Instant,
    val updatedAt: Instant,
)
