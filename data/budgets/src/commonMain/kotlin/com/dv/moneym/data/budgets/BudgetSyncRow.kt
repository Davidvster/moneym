package com.dv.moneym.data.budgets

data class BudgetSyncRow(
    val id: Long,
    val syncId: String?,
    val name: String,
    val amountMinor: Long,
    val currency: String,
    val categoryId: Long?,
    val accountId: Long,
    val periodType: String,
    val startYearMonth: String,
    val recurringMonths: Int?,
    val deleted: Boolean,
    val createdAt: Long,
    val updatedAt: Long,
)
