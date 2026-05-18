package com.dv.moneym.core.testing

import com.dv.moneym.core.datastore.AppSettingsRepository
import com.dv.moneym.core.model.OverviewPeriodMode
import com.dv.moneym.core.model.ThemeMode
import com.dv.moneym.core.model.TransactionFilter
import com.dv.moneym.core.model.TxDisplayPrefs
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class FakeAppSettingsRepository : AppSettingsRepository {

    private val _themeMode = MutableStateFlow(ThemeMode.Auto)
    private val _txDisplayPrefs = MutableStateFlow(TxDisplayPrefs())
    private val _defaultCurrency = MutableStateFlow("EUR")
    private val _language = MutableStateFlow("")
    private val _lastTransactionFilter = MutableStateFlow<TransactionFilter>(TransactionFilter.None)
    private val _lastOverviewPeriod = MutableStateFlow(OverviewPeriodMode.Month)
    private val _selectedAccountId = MutableStateFlow(-1L)

    override fun observeThemeMode(): Flow<ThemeMode> = _themeMode.asStateFlow()

    override suspend fun setThemeMode(mode: ThemeMode) {
        _themeMode.value = mode
    }

    override fun observeTxDisplayPrefs(): Flow<TxDisplayPrefs> = _txDisplayPrefs.asStateFlow()

    override suspend fun setTxDisplayPrefs(prefs: TxDisplayPrefs) {
        _txDisplayPrefs.value = prefs
    }

    override fun observeDefaultCurrency(): Flow<String> = _defaultCurrency.asStateFlow()

    override suspend fun setDefaultCurrency(currency: String) {
        _defaultCurrency.value = currency
    }

    override fun observeLanguage(): Flow<String> = _language.asStateFlow()

    override suspend fun setLanguage(language: String) {
        _language.value = language
    }

    override fun observeLastTransactionFilter(): Flow<TransactionFilter> = _lastTransactionFilter.asStateFlow()

    override suspend fun setLastTransactionFilter(filter: TransactionFilter) {
        _lastTransactionFilter.value = filter
    }

    override fun observeLastOverviewPeriod(): Flow<OverviewPeriodMode> = _lastOverviewPeriod.asStateFlow()

    override suspend fun setLastOverviewPeriod(mode: OverviewPeriodMode) {
        _lastOverviewPeriod.value = mode
    }

    override fun observeSelectedAccountId(): Flow<Long> = _selectedAccountId.asStateFlow()

    override suspend fun setSelectedAccountId(id: Long) {
        _selectedAccountId.value = id
    }
}
