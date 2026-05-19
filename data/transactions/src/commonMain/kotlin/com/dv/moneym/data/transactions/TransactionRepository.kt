package com.dv.moneym.data.transactions

import com.dv.moneym.core.model.AccountId
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
    suspend fun getEarliestTransactionDate(): LocalDate?
    suspend fun getLatestTransactionDate(): LocalDate?
    fun getTransactionDates(): Flow<Set<LocalDate>>
}
