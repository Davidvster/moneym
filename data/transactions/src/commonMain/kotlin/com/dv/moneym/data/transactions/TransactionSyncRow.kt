package com.dv.moneym.data.transactions

data class TransactionSyncRow(
    val id: Long,
    val syncId: String?,
    val type: String,
    val amountMinor: Long,
    val currency: String,
    val occurredOn: String,
    val note: String?,
    val categoryId: Long,
    val accountId: Long,
    val paymentModeId: Long?,
    val recurringId: Long?,
    val deleted: Boolean,
    val createdAt: Long,
    val updatedAt: Long,
    val externalId: String? = null,
)
