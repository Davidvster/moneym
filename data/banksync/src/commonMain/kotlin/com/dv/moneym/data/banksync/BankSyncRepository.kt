package com.dv.moneym.data.banksync

import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate

interface BankSyncRepository {
    fun observeAccounts(): Flow<List<BankAccountLink>>
    suspend fun getEnabledAccounts(): List<BankAccountLink>
    suspend fun upsertAccounts(accounts: List<BankAccountLink>)
    suspend fun setLocalAccountMapping(uid: String, localAccountId: Long?)
    suspend fun setAccountEnabled(uid: String, enabled: Boolean)
    suspend fun advanceCursor(uid: String, syncedThrough: LocalDate, syncedAt: Long)

    fun observePendingSuggestions(): Flow<List<BankSuggestion>>
    fun observeRejectedSuggestions(): Flow<List<BankSuggestion>>
    fun observePendingCount(): Flow<Int>
    suspend fun getSuggestion(id: Long): BankSuggestion?
    suspend fun filterKnownExternalIds(externalIds: List<String>): Set<String>
    suspend fun insertSuggestionsIfNew(suggestions: List<BankSuggestion>): Int
    suspend fun accept(id: Long, createdTransactionId: Long, decidedAt: Long)
    suspend fun reject(id: Long, decidedAt: Long)
    suspend fun restoreToPending(id: Long)
    suspend fun clearAll()
}
