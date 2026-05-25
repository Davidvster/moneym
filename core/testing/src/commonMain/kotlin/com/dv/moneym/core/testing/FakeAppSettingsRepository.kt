package com.dv.moneym.core.testing

import com.dv.moneym.core.datastore.AppSettingsRepository
import com.dv.moneym.core.model.OverviewPeriodMode
import com.dv.moneym.core.model.SpendingFilter
import com.dv.moneym.core.model.ThemeMode
import com.dv.moneym.core.model.TransactionFilter
import com.dv.moneym.core.model.TransactionType
import com.dv.moneym.core.model.TxDisplayPrefs
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class FakeAppSettingsRepository : AppSettingsRepository {

    private val _themeMode = MutableStateFlow(ThemeMode.Auto)
    private val _txDisplayPrefs = MutableStateFlow(TxDisplayPrefs())
    private val _language = MutableStateFlow("")
    private val _lastTransactionFilter = MutableStateFlow<TransactionFilter>(TransactionFilter.None)
    private val _lastOverviewPeriod = MutableStateFlow(OverviewPeriodMode.Month)
    private val _lastOverviewFilter = MutableStateFlow(SpendingFilter.Expenses)
    private val _selectedAccountId = MutableStateFlow(-1L)
    private val _defaultTransactionType = MutableStateFlow(TransactionType.EXPENSE)
    private val _paymentModeEnabled = MutableStateFlow(false)

    override fun observeThemeMode(): Flow<ThemeMode> = _themeMode.asStateFlow()

    override suspend fun setThemeMode(mode: ThemeMode) {
        _themeMode.value = mode
    }

    override fun observeTxDisplayPrefs(): Flow<TxDisplayPrefs> = _txDisplayPrefs.asStateFlow()

    override suspend fun setTxDisplayPrefs(prefs: TxDisplayPrefs) {
        _txDisplayPrefs.value = prefs
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

    override fun observeLastOverviewFilter(): Flow<SpendingFilter> = _lastOverviewFilter.asStateFlow()

    override suspend fun setLastOverviewFilter(filter: SpendingFilter) {
        _lastOverviewFilter.value = filter
    }

    override fun observeSelectedAccountId(): Flow<Long> = _selectedAccountId.asStateFlow()

    override suspend fun setSelectedAccountId(id: Long) {
        _selectedAccountId.value = id
    }

    override fun observeDefaultTransactionType(): Flow<TransactionType> = _defaultTransactionType.asStateFlow()

    override suspend fun setDefaultTransactionType(type: TransactionType) {
        _defaultTransactionType.value = type
    }

    override fun observePaymentModeEnabled(): Flow<Boolean> = _paymentModeEnabled.asStateFlow()

    override suspend fun setPaymentModeEnabled(enabled: Boolean) {
        _paymentModeEnabled.value = enabled
    }
}
