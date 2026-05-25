package com.dv.moneym.data.transactions.internal

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

    suspend fun delete(id: Long)
    suspend fun deleteByAccountId(accountId: Long)
    suspend fun deleteAll()
    suspend fun convertCurrencyForAccount(accountId: Long, currency: String, rate: Double, updatedAt: Long)
    suspend fun getEarliestDate(): String?
    suspend fun getLatestDate(): String?
    fun getDistinctTransactionDates(): Flow<List<String>>
}
