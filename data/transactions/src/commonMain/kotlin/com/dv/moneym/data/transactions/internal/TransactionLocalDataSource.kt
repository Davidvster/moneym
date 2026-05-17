package com.dv.moneym.data.transactions.internal

import com.dv.moneym.data.transactions.TransactionEntry
import kotlinx.coroutines.flow.Flow

internal interface TransactionLocalDataSource {
    fun observeAll(): Flow<List<TransactionEntry>>
    fun observeByMonth(yearMonth: String): Flow<List<TransactionEntry>>
    fun observeByCategory(categoryId: Long): Flow<List<TransactionEntry>>
    fun observeByType(type: String): Flow<List<TransactionEntry>>
    fun observeByCategoryAndType(categoryId: Long, type: String): Flow<List<TransactionEntry>>
    suspend fun getById(id: Long): TransactionEntry?
    suspend fun insert(
        type: String, amountMinor: Long, currency: String, occurredOn: String,
        note: String?, categoryId: Long, accountId: Long, createdAt: Long, updatedAt: Long,
    ): Long
    suspend fun update(
        id: Long, type: String, amountMinor: Long, currency: String,
        occurredOn: String, note: String?, categoryId: Long, accountId: Long, updatedAt: Long,
    )
    suspend fun delete(id: Long)
    suspend fun getEarliestDate(): String?
    suspend fun getLatestDate(): String?
}
