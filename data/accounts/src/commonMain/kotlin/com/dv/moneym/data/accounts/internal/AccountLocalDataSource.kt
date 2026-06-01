package com.dv.moneym.data.accounts.internal

import com.dv.moneym.data.accounts.AccountSyncRow
import com.dv.moneym.data.accounts.db.AccountEntity
import kotlinx.coroutines.flow.Flow

internal interface AccountLocalDataSource {
    fun observeAll(): Flow<List<AccountEntity>>
    fun observeDefault(): Flow<AccountEntity?>
    suspend fun getById(id: Long): AccountEntity?
    suspend fun count(): Long
    suspend fun insert(
        name: String, type: String, currency: String,
        isDefault: Boolean, createdAt: Long, updatedAt: Long,
        colorHex: String?,
    ): Long

    suspend fun update(
        id: Long, name: String, type: String, currency: String,
        isDefault: Boolean, archived: Boolean, updatedAt: Long,
        colorHex: String?,
    )

    suspend fun softDelete(id: Long, now: Long)
    suspend fun markDeletedBySyncId(syncId: String, now: Long)
    suspend fun reviveBySyncId(syncId: String, now: Long)
    suspend fun deleteAll()
    suspend fun exportForSync(): List<AccountEntity>
    suspend fun upsertFromSync(row: AccountSyncRow): Long
}
