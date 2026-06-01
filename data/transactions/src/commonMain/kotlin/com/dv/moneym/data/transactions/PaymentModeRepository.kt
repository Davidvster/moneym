package com.dv.moneym.data.transactions

import com.dv.moneym.core.model.PaymentMode
import com.dv.moneym.core.model.PaymentModeId
import kotlinx.coroutines.flow.Flow

interface PaymentModeRepository {
    fun observeAll(): Flow<List<PaymentMode>>
    suspend fun getById(id: PaymentModeId): PaymentMode?
    suspend fun create(name: String)
    suspend fun rename(id: PaymentModeId, name: String)
    suspend fun delete(id: PaymentModeId)
    suspend fun exportForSync(): List<PaymentModeSyncRow>
    suspend fun upsertFromSync(row: PaymentModeSyncRow): Long
}
