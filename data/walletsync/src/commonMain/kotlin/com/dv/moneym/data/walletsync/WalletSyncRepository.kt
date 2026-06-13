package com.dv.moneym.data.walletsync

import com.dv.moneym.core.model.SuggestionSource

interface WalletSyncRepository : SuggestionSource {
    suspend fun filterKnownExternalIds(externalIds: List<String>): Set<String>
    suspend fun insertSuggestionsIfNew(suggestions: List<WalletSuggestion>): Int
    suspend fun clearAll()
}
