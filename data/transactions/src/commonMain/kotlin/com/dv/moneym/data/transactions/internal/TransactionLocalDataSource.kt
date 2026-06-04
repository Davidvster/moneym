package com.dv.moneym.data.transactions.internal

import com.dv.moneym.data.transactions.TransactionSyncRow
import com.dv.moneym.data.transactions.db.TransactionEntity
import kotlinx.coroutines.flow.Flow

internal interface TransactionLocalDataSource {
    fun observeAll(): Flow<List<TransactionEntity>>
    fun observeByMonth(yearMonth: String): Flow<List<TransactionEntity>>
    fun observeByCategory(categoryId: Long): Flow<List<TransactionEntity>>
    fun observeByType(type: String): Flow<List<TransactionEntity>>
    fun observeByCategoryAndType(categoryId: Long, type: String): Flow<List<TransactionEntity>>
    suspend fun getById(id: Long): TransactionEntity?
    suspend fun insert(
        type: String, amountMinor: Long, currency: String, occurredOn: String,
        note: String?, categoryId: Long, accountId: Long, createdAt: Long, updatedAt: Long,
        paymentModeId: Long? = null, recurringId: Long? = null,
    ): Long

    suspend fun update(
        id: Long, type: String, amountMinor: Long, currency: String,
        occurredOn: String, note: String?, categoryId: Long, accountId: Long, updatedAt: Long,
        paymentModeId: Long? = null, recurringId: Long? = null,
    )

    suspend fun softDelete(id: Long, now: Long)
    suspend fun softDeleteByAccountId(accountId: Long, now: Long)
    suspend fun reassignCategory(from: Long, to: Long, now: Long)
    suspend fun softDeleteByCategory(categoryId: Long, now: Long)
    suspend fun markDeletedBySyncId(syncId: String, now: Long)
    suspend fun reviveBySyncId(syncId: String, now: Long)
    suspend fun deleteAll()
    suspend fun convertCurrencyForAccount(accountId: Long, currency: String, rate: Double, updatedAt: Long)
    suspend fun getEarliestDate(): String?
    suspend fun getLatestDate(): String?
    fun getDistinctTransactionDates(): Flow<List<String>>
    suspend fun countByRecurringId(recurringId: Long): Int
    suspend fun exportForSync(): List<TransactionEntity>
    suspend fun upsertFromSync(row: TransactionSyncRow): Long
}
