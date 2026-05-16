package com.dv.moneym.core.testing

import com.dv.moneym.core.datastore.AppSettingsRepository
import com.dv.moneym.core.model.ThemeMode
import com.dv.moneym.core.model.TxDisplayPrefs
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class FakeAppSettingsRepository : AppSettingsRepository {

    private val _themeMode = MutableStateFlow(ThemeMode.Auto)
    private val _txDisplayPrefs = MutableStateFlow(TxDisplayPrefs())
    private val _defaultCurrency = MutableStateFlow("EUR")
    private val _language = MutableStateFlow("")

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
}
