package com.dv.moneym.data.banksync

import com.dv.moneym.core.model.SuggestionSource
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate

interface BankSyncRepository : SuggestionSource {
    fun observeAccounts(): Flow<List<BankAccountLink>>
    suspend fun getEnabledAccounts(): List<BankAccountLink>
    suspend fun upsertAccounts(accounts: List<BankAccountLink>)
    suspend fun setLocalAccountMapping(uid: String, localAccountId: Long?)
    suspend fun setAccountEnabled(uid: String, enabled: Boolean)
    suspend fun advanceCursor(uid: String, syncedThrough: LocalDate, syncedAt: Long)

    suspend fun filterKnownExternalIds(externalIds: List<String>): Set<String>
    suspend fun insertSuggestionsIfNew(suggestions: List<BankSuggestion>): Int
    suspend fun clearAll()
}
