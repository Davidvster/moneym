package com.dv.moneym.data.transactions

import com.dv.moneym.core.model.AccountId
import com.dv.moneym.core.model.CategoryId
import com.dv.moneym.core.model.CurrencyCode
import com.dv.moneym.core.model.RecurringTransactionId
import com.dv.moneym.core.model.Transaction
import com.dv.moneym.core.model.TransactionFilter
import com.dv.moneym.core.model.TransactionId
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate

interface TransactionRepository {
    fun observeAll(): Flow<List<Transaction>>
    fun observeByMonth(year: Int, month: Int): Flow<List<Transaction>>
    fun observeFiltered(filter: TransactionFilter): Flow<List<Transaction>>
    suspend fun getById(id: TransactionId): Transaction?
    suspend fun upsert(transaction: Transaction): TransactionId
    suspend fun delete(id: TransactionId)
    suspend fun deleteByAccountId(id: AccountId)
    suspend fun reassignCategory(from: CategoryId, to: CategoryId)
    suspend fun deleteByCategory(id: CategoryId)
    suspend fun markDeletedBySyncId(syncId: String, now: Long)
    suspend fun reviveBySyncId(syncId: String, now: Long)
    suspend fun deleteAll()
    suspend fun convertCurrencyForAccount(accountId: AccountId, newCurrency: CurrencyCode, rate: Double)
    suspend fun getEarliestTransactionDate(): LocalDate?
    suspend fun getLatestTransactionDate(): LocalDate?
    fun getTransactionDates(): Flow<Set<LocalDate>>
    suspend fun countByRecurringId(id: RecurringTransactionId): Int
    suspend fun exportForSync(): List<TransactionSyncRow>
    suspend fun upsertFromSync(row: TransactionSyncRow): Long
    suspend fun existsByExternalId(externalId: String): Boolean
    suspend fun setExternalId(id: TransactionId, externalId: String)
    suspend fun findByDateAndAmount(date: LocalDate, amountMinor: Long, currency: CurrencyCode): List<Transaction>
}
