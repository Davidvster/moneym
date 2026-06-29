package com.dv.moneym.core.datastore

import com.dv.moneym.core.model.Density
import com.dv.moneym.core.model.CategoryId
import com.dv.moneym.core.model.IndicatorStyle
import com.dv.moneym.core.model.OverviewPeriodMode
import com.dv.moneym.core.model.SpendingFilter
import com.dv.moneym.core.model.ThemeMode
import com.dv.moneym.core.model.TransactionFilter
import com.dv.moneym.core.model.TransactionType
import com.dv.moneym.core.model.TxDisplayPrefs
import com.dv.moneym.core.model.selectedCategoryIds
import com.dv.moneym.core.model.selectedType
import com.dv.moneym.core.model.transactionFilterOf
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
        val showDailySumsFlow = appSettings
            .observeBoolean(PrefKeys.TX_SHOW_DAILY_SUMS, true)
        val showSyncSuggestionBannerFlow = appSettings
            .observeBoolean(PrefKeys.TX_SHOW_SYNC_SUGGESTION_BANNER, true)

        val basePrefsFlow = combine(
            indicatorFlow,
            showCategoryFlow,
            showNoteFlow,
            densityFlow,
            showDailySumsFlow
        ) { indicator, showCategory, showNote, density, showDailySums ->
            TxDisplayPrefs(
                indicatorStyle = indicator,
                showCategoryName = showCategory,
                showNote = showNote,
                density = density,
                showDailySums = showDailySums,
            )
        }
        return combine(basePrefsFlow, showSyncSuggestionBannerFlow) { prefs, showSyncSuggestionBanner ->
            prefs.copy(showSyncSuggestionBanner = showSyncSuggestionBanner)
        }
    }

    override suspend fun setTxDisplayPrefs(prefs: TxDisplayPrefs) {
        appSettings.putString(PrefKeys.TX_INDICATOR_STYLE, prefs.indicatorStyle.name)
        appSettings.putBoolean(PrefKeys.TX_SHOW_CATEGORY, prefs.showCategoryName)
        appSettings.putBoolean(PrefKeys.TX_SHOW_NOTE, prefs.showNote)
        appSettings.putString(PrefKeys.TX_DENSITY, prefs.density.name)
        appSettings.putBoolean(PrefKeys.TX_SHOW_DAILY_SUMS, prefs.showDailySums)
        appSettings.putBoolean(PrefKeys.TX_SHOW_SYNC_SUGGESTION_BANNER, prefs.showSyncSuggestionBanner)
    }

    override fun observeLanguage(): Flow<String> =
        appSettings
            .observeString(PrefKeys.LANGUAGE, "")
            .map { it ?: "" }

    override suspend fun setLanguage(language: String) {
        appSettings.putString(PrefKeys.LANGUAGE, language)
    }

    override fun observeLastTransactionFilter(): Flow<TransactionFilter> =
        appSettings
            .observeString(PrefKeys.TX_LAST_FILTER, "all")
            .map { decodeFilter(it ?: "all") }

    override suspend fun setLastTransactionFilter(filter: TransactionFilter) {
        appSettings.putString(PrefKeys.TX_LAST_FILTER, encodeFilter(filter))
    }

    override fun observeLastOverviewPeriod(): Flow<OverviewPeriodMode> =
        appSettings
            .observeString(PrefKeys.OVERVIEW_LAST_TAB, "month")
            .map { decodeOverviewPeriod(it ?: "month") }

    override suspend fun setLastOverviewPeriod(mode: OverviewPeriodMode) {
        appSettings.putString(PrefKeys.OVERVIEW_LAST_TAB, encodeOverviewPeriod(mode))
    }

    override fun observeLastOverviewFilter(): Flow<SpendingFilter> =
        appSettings
            .observeString(PrefKeys.OVERVIEW_LAST_FILTER, "expenses")
            .map { decodeOverviewFilter(it ?: "expenses") }

    override suspend fun setLastOverviewFilter(filter: SpendingFilter) {
        appSettings.putString(PrefKeys.OVERVIEW_LAST_FILTER, encodeOverviewFilter(filter))
    }

    // Private helpers — strings are ONLY here
    private fun encodeFilter(filter: TransactionFilter): String {
        val type = filter.selectedType()
        val categoryIds = filter.selectedCategoryIds()
        if (categoryIds.isEmpty()) {
            return when (type) {
                TransactionType.EXPENSE -> "expense"
                TransactionType.INCOME -> "income"
                null -> "all"
            }
        }
        val typeValue = type?.name?.lowercase().orEmpty()
        val categoryValue = categoryIds.map { it.value }.sorted().joinToString(",")
        return "v2|type=$typeValue|categories=$categoryValue"
    }

    private fun decodeFilter(encoded: String): TransactionFilter = when (encoded) {
        "expense" -> TransactionFilter.ByType(TransactionType.EXPENSE)
        "income" -> TransactionFilter.ByType(TransactionType.INCOME)
        "all" -> TransactionFilter.None
        else -> decodeStructuredFilter(encoded)
    }

    private fun decodeStructuredFilter(encoded: String): TransactionFilter {
        if (!encoded.startsWith("v2|")) return TransactionFilter.None
        val fields = encoded
            .removePrefix("v2|")
            .split("|")
            .mapNotNull { part ->
                val index = part.indexOf("=")
                if (index < 0) null else part.substring(0, index) to part.substring(index + 1)
            }
            .toMap()
        val type = when (fields["type"]) {
            "expense" -> TransactionType.EXPENSE
            "income" -> TransactionType.INCOME
            else -> null
        }
        val categoryIds = fields["categories"]
            ?.split(",")
            ?.mapNotNull { it.toLongOrNull() }
            ?.map { CategoryId(it) }
            ?.toSet()
            ?: emptySet()
        return transactionFilterOf(type, categoryIds)
    }

    private fun encodeOverviewPeriod(mode: OverviewPeriodMode): String = when (mode) {
        OverviewPeriodMode.Month -> "month"
        OverviewPeriodMode.Year -> "year"
        OverviewPeriodMode.DateRange -> "range"
    }

    private fun decodeOverviewPeriod(encoded: String): OverviewPeriodMode = when (encoded) {
        "year" -> OverviewPeriodMode.Year
        "range" -> OverviewPeriodMode.DateRange
        else -> OverviewPeriodMode.Month
    }

    private fun encodeOverviewFilter(filter: SpendingFilter): String = when (filter) {
        SpendingFilter.All -> "all"
        SpendingFilter.Expenses -> "expenses"
        SpendingFilter.Income -> "income"
    }

    private fun decodeOverviewFilter(encoded: String): SpendingFilter = when (encoded) {
        "all" -> SpendingFilter.All
        "income" -> SpendingFilter.Income
        else -> SpendingFilter.Expenses
    }

    override fun observeSelectedAccountId(): Flow<Long> =
        appSettings
            .observeString(PrefKeys.SELECTED_ACCOUNT_ID, "-1")
            .map { it?.toLongOrNull() ?: -1L }

    override suspend fun setSelectedAccountId(id: Long) {
        appSettings.putString(PrefKeys.SELECTED_ACCOUNT_ID, id.toString())
    }

    override fun observeDefaultTransactionType(): Flow<TransactionType> =
        appSettings.observeString(PrefKeys.DEFAULT_TX_TYPE, TransactionType.EXPENSE.name)
            .map { stored ->
                runCatching { enumValueOf<TransactionType>(stored ?: TransactionType.EXPENSE.name) }
                    .getOrDefault(TransactionType.EXPENSE)
            }

    override suspend fun setDefaultTransactionType(type: TransactionType) {
        appSettings.putString(PrefKeys.DEFAULT_TX_TYPE, type.name)
    }

    override fun observePaymentModeEnabled(): Flow<Boolean> =
        appSettings.observeBoolean(PrefKeys.PAYMENT_MODE_ENABLED, false)

    override suspend fun setPaymentModeEnabled(enabled: Boolean) {
        appSettings.putBoolean(PrefKeys.PAYMENT_MODE_ENABLED, enabled)
    }

    override fun observeShowPendingRecurring(): Flow<Boolean> =
        appSettings.observeBoolean(PrefKeys.SHOW_PENDING_RECURRING_TX, true)

    override suspend fun setShowPendingRecurring(enabled: Boolean) {
        appSettings.putBoolean(PrefKeys.SHOW_PENDING_RECURRING_TX, enabled)
    }

    override fun observeUseCurrencySymbol(): Flow<Boolean> =
        appSettings.observeBoolean(PrefKeys.USE_CURRENCY_SYMBOL, false)

    override suspend fun setUseCurrencySymbol(enabled: Boolean) {
        appSettings.putBoolean(PrefKeys.USE_CURRENCY_SYMBOL, enabled)
    }
}
