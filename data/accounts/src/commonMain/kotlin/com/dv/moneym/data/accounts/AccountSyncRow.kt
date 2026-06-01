package com.dv.moneym.data.accounts

data class AccountSyncRow(
    val id: Long,
    val syncId: String?,
    val name: String,
    val type: String,
    val currency: String,
    val isDefault: Boolean,
    val archived: Boolean,
    val colorHex: String?,
    val deleted: Boolean,
    val createdAt: Long,
    val updatedAt: Long,
)
