package com.dv.moneym.data.budgets.internal

import com.dv.moneym.data.budgets.BudgetSyncRow
import com.dv.moneym.data.budgets.db.BudgetEntity
import kotlinx.coroutines.flow.Flow

internal interface BudgetLocalDataSource {
    fun observeAll(): Flow<List<BudgetEntity>>
    fun observeByAccount(accountId: Long): Flow<List<BudgetEntity>>
    suspend fun getById(id: Long): BudgetEntity?
    suspend fun insert(
        name: String,
        amountMinor: Long,
        currency: String,
        categoryId: Long?,
        accountId: Long,
        periodType: String,
        startYearMonth: String,
        recurringMonths: Int?,
        createdAt: Long,
        updatedAt: Long,
    ): Long

    suspend fun update(
        id: Long,
        name: String,
        amountMinor: Long,
        currency: String,
        categoryId: Long?,
        accountId: Long,
        periodType: String,
        startYearMonth: String,
        recurringMonths: Int?,
        updatedAt: Long,
    )

    suspend fun delete(id: Long)
    suspend fun exportForSync(): List<BudgetEntity>
    suspend fun upsertFromSync(row: BudgetSyncRow): Long
}
