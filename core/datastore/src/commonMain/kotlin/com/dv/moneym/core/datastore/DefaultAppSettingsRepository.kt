package com.dv.moneym.core.datastore

import com.dv.moneym.core.model.Density
import com.dv.moneym.core.model.IndicatorStyle
import com.dv.moneym.core.model.ThemeMode
import com.dv.moneym.core.model.TxDisplayPrefs
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map

class DefaultAppSettingsRepository(
    private val appSettings: AppSettings,
) : AppSettingsRepository {

    override fun observeThemeMode(): Flow<ThemeMode> =
        appSettings.observeString(PrefKeys.THEME_MODE, ThemeMode.Auto.name)
            .filterNotNull()
            .map { stored ->
                runCatching { enumValueOf<ThemeMode>(stored) }
                    .getOrElse {
                        when (stored.lowercase()) {
                            "light" -> ThemeMode.Light
                            "dark" -> ThemeMode.Dark
                            else -> ThemeMode.Auto
                        }
                    }
            }

    override suspend fun setThemeMode(mode: ThemeMode) {
        appSettings.putString(PrefKeys.THEME_MODE, mode.name)
    }

    override fun observeTxDisplayPrefs(): Flow<TxDisplayPrefs> {
        val indicatorFlow = appSettings
            .observeString(PrefKeys.TX_INDICATOR_STYLE, IndicatorStyle.IconTile.name)
            .map { stored ->
                runCatching { enumValueOf<IndicatorStyle>(stored ?: IndicatorStyle.IconTile.name) }
                    .getOrDefault(IndicatorStyle.IconTile)
            }
        val showCategoryFlow = appSettings
            .observeBoolean(PrefKeys.TX_SHOW_CATEGORY, true)
        val showNoteFlow = appSettings
            .observeBoolean(PrefKeys.TX_SHOW_NOTE, true)
        val densityFlow = appSettings
            .observeString(PrefKeys.TX_DENSITY, Density.Comfortable.name)
            .map { stored ->
                runCatching { enumValueOf<Density>(stored ?: Density.Comfortable.name) }
                    .getOrDefault(Density.Comfortable)
            }

        return combine(indicatorFlow, showCategoryFlow, showNoteFlow, densityFlow) {
                indicator, showCategory, showNote, density ->
            TxDisplayPrefs(
                indicatorStyle = indicator,
                showCategoryName = showCategory,
                showNote = showNote,
                density = density,
            )
        }
    }

    override suspend fun setTxDisplayPrefs(prefs: TxDisplayPrefs) {
        appSettings.putString(PrefKeys.TX_INDICATOR_STYLE, prefs.indicatorStyle.name)
        appSettings.putBoolean(PrefKeys.TX_SHOW_CATEGORY, prefs.showCategoryName)
        appSettings.putBoolean(PrefKeys.TX_SHOW_NOTE, prefs.showNote)
        appSettings.putString(PrefKeys.TX_DENSITY, prefs.density.name)
    }

    override fun observeDefaultCurrency(): Flow<String> =
        appSettings
            .observeString(PrefKeys.DEFAULT_CURRENCY, "USD")
            .map { it ?: "USD" }

    override suspend fun setDefaultCurrency(currency: String) {
        appSettings.putString(PrefKeys.DEFAULT_CURRENCY, currency)
    }

    override fun observeLanguage(): Flow<String> =
        appSettings
            .observeString(PrefKeys.LANGUAGE, "")
            .map { it ?: "" }

    override suspend fun setLanguage(language: String) {
        appSettings.putString(PrefKeys.LANGUAGE, language)
    }

    override fun observeLastTransactionFilter(): Flow<String> =
        appSettings
            .observeString(PrefKeys.TX_LAST_FILTER, "all")
            .map { it ?: "all" }

    override suspend fun setLastTransactionFilter(encoded: String) {
        appSettings.putString(PrefKeys.TX_LAST_FILTER, encoded)
    }

    override fun observeLastOverviewPeriod(): Flow<String> =
        appSettings
            .observeString(PrefKeys.OVERVIEW_LAST_TAB, "month")
            .map { it ?: "month" }

    override suspend fun setLastOverviewPeriod(encoded: String) {
        appSettings.putString(PrefKeys.OVERVIEW_LAST_TAB, encoded)
    }

    override fun observeSelectedAccountId(): Flow<Long> =
        appSettings
            .observeString(PrefKeys.SELECTED_ACCOUNT_ID, "-1")
            .map { it?.toLongOrNull() ?: -1L }

    override suspend fun setSelectedAccountId(id: Long) {
        appSettings.putString(PrefKeys.SELECTED_ACCOUNT_ID, id.toString())
    }
}
