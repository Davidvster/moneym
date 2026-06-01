package com.dv.moneym.data.categories

data class CategorySyncRow(
    val id: Long,
    val syncId: String?,
    val name: String,
    val iconKey: String,
    val colorHex: String,
    val isUserCreated: Boolean,
    val archived: Boolean,
    val categoryType: String,
    val deleted: Boolean,
    val createdAt: Long,
    val updatedAt: Long,
)
