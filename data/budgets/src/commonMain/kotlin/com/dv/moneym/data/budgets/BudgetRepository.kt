package com.dv.moneym.data.budgets

import com.dv.moneym.core.model.AccountId
import com.dv.moneym.core.model.Budget
import com.dv.moneym.core.model.BudgetId
import kotlinx.coroutines.flow.Flow

interface BudgetRepository {
    fun observeAll(): Flow<List<Budget>>
    fun observeByAccount(accountId: AccountId): Flow<List<Budget>>
    suspend fun getById(id: BudgetId): Budget?
    suspend fun insert(budget: Budget): BudgetId
    suspend fun update(budget: Budget)
    suspend fun delete(id: BudgetId)
    suspend fun markDeletedBySyncId(syncId: String, now: Long)
    suspend fun reviveBySyncId(syncId: String, now: Long)
    suspend fun exportForSync(): List<BudgetSyncRow>
    suspend fun upsertFromSync(row: BudgetSyncRow): Long
}
