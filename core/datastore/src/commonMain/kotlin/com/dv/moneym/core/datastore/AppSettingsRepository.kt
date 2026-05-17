package com.dv.moneym.core.datastore

import com.dv.moneym.core.model.ThemeMode
import com.dv.moneym.core.model.TxDisplayPrefs
import kotlinx.coroutines.flow.Flow

interface AppSettingsRepository {
    fun observeThemeMode(): Flow<ThemeMode>
    suspend fun setThemeMode(mode: ThemeMode)
    fun observeTxDisplayPrefs(): Flow<TxDisplayPrefs>
    suspend fun setTxDisplayPrefs(prefs: TxDisplayPrefs)
    fun observeDefaultCurrency(): Flow<String>
    suspend fun setDefaultCurrency(currency: String)
    fun observeLanguage(): Flow<String>
    suspend fun setLanguage(language: String)
    // User last-selected settings
    fun observeLastTransactionFilter(): Flow<String>
    suspend fun setLastTransactionFilter(encoded: String)
    fun observeLastOverviewPeriod(): Flow<String>
    suspend fun setLastOverviewPeriod(encoded: String)
}
