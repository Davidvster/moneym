package com.dv.moneym.data.transactions

import com.dv.moneym.core.model.RecurringTransaction
import com.dv.moneym.core.model.RecurringTransactionId
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate

interface RecurringTransactionRepository {
    fun observeAll(): Flow<List<RecurringTransaction>>
    suspend fun getById(id: RecurringTransactionId): RecurringTransaction?
    suspend fun upsert(rule: RecurringTransaction): RecurringTransactionId
    suspend fun updateCursor(id: RecurringTransactionId, lastMaterialized: LocalDate)
    suspend fun delete(id: RecurringTransactionId)
    suspend fun deleteAll()
    suspend fun exportForSync(): List<RecurringSyncRow>
}
