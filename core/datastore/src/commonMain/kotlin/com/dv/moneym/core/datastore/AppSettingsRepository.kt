package com.dv.moneym.core.datastore

import com.dv.moneym.core.model.OverviewPeriodMode
import com.dv.moneym.core.model.ThemeMode
import com.dv.moneym.core.model.TransactionFilter
import com.dv.moneym.core.model.TransactionType
import com.dv.moneym.core.model.TxDisplayPrefs
import kotlinx.coroutines.flow.Flow

interface AppSettingsRepository {
    fun observeThemeMode(): Flow<ThemeMode>
    suspend fun setThemeMode(mode: ThemeMode)
    fun observeTxDisplayPrefs(): Flow<TxDisplayPrefs>
    suspend fun setTxDisplayPrefs(prefs: TxDisplayPrefs)
    fun observeLanguage(): Flow<String>
    suspend fun setLanguage(language: String)

    // User last-selected settings
    fun observeLastTransactionFilter(): Flow<TransactionFilter>
    suspend fun setLastTransactionFilter(filter: TransactionFilter)
    fun observeLastOverviewPeriod(): Flow<OverviewPeriodMode>
    suspend fun setLastOverviewPeriod(mode: OverviewPeriodMode)

    // Selected wallet / account (stored as Long, -1 = not set / use default)
    fun observeSelectedAccountId(): Flow<Long>
    suspend fun setSelectedAccountId(id: Long)

    // Default transaction type for new transactions
    fun observeDefaultTransactionType(): Flow<TransactionType>
    suspend fun setDefaultTransactionType(type: TransactionType)

    // Payment mode feature toggle
    fun observePaymentModeEnabled(): Flow<Boolean>
    suspend fun setPaymentModeEnabled(enabled: Boolean)
}
