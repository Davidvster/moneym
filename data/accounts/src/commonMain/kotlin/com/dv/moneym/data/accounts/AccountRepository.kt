package com.dv.moneym.data.accounts

import com.dv.moneym.core.model.Account
import com.dv.moneym.core.model.AccountId
import kotlinx.coroutines.flow.Flow

interface AccountRepository {
    fun observeAll(): Flow<List<Account>>
    fun observeDefault(): Flow<Account?>
    suspend fun getById(id: AccountId): Account?
    suspend fun count(): Long
    suspend fun insert(account: Account): AccountId
    suspend fun update(account: Account)
    suspend fun delete(id: AccountId)
    suspend fun deleteAll()
    suspend fun exportForSync(): List<AccountSyncRow>
}
