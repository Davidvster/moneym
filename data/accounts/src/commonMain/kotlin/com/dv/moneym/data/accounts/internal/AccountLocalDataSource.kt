package com.dv.moneym.data.accounts.internal

import com.dv.moneym.data.accounts.Account
import kotlinx.coroutines.flow.Flow

internal interface AccountLocalDataSource {
    fun observeAll(): Flow<List<Account>>
    fun observeDefault(): Flow<Account?>
    suspend fun getById(id: Long): Account?
    suspend fun count(): Long
    suspend fun insert(
        name: String, type: String, currency: String,
        isDefault: Boolean, createdAt: Long, updatedAt: Long,
    ): Long

    suspend fun update(
        id: Long, name: String, type: String, currency: String,
        isDefault: Boolean, archived: Boolean, updatedAt: Long,
    )

    suspend fun delete(id: Long)
    suspend fun deleteAll()
}
