package com.dv.moneym.core.model

import kotlinx.serialization.Serializable
import kotlin.time.Instant

enum class AccountType { CASH, BANK, CARD, OTHER }

@Serializable
data class Account(
    val id: AccountId,
    val name: String,
    val type: AccountType,
    val currency: CurrencyCode,
    val isDefault: Boolean,
    val archived: Boolean,
    @Serializable(with = InstantSerializer::class) val createdAt: Instant,
    @Serializable(with = InstantSerializer::class) val updatedAt: Instant,
)
