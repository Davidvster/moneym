package com.dv.moneym.data.transactions

data class PaymentModeSyncRow(
    val id: Long,
    val syncId: String?,
    val name: String,
    val deleted: Boolean,
    val createdAt: Long,
    val updatedAt: Long,
)
