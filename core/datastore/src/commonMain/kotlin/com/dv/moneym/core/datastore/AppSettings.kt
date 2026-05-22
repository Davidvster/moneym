package com.dv.moneym.core.datastore

import kotlinx.coroutines.flow.Flow

interface AppSettings {
    fun getString(key: String, defaultValue: String? = null): String?
    fun putString(key: String, value: String)
    fun getBoolean(key: String, defaultValue: Boolean = false): Boolean
    fun putBoolean(key: String, value: Boolean)
    fun getInt(key: String, defaultValue: Int = 0): Int
    fun putInt(key: String, value: Int)
    fun getLong(key: String, defaultValue: Long = 0L): Long
    fun putLong(key: String, value: Long)
    fun remove(key: String)
    fun observeBoolean(key: String, defaultValue: Boolean = false): Flow<Boolean>
    fun observeString(key: String, defaultValue: String? = null): Flow<String?>
    fun observeInt(key: String, defaultValue: Int = 0): Flow<Int>
}

// Keys used across the app — single source of truth
object PrefKeys {
    const val THEME_MODE = "pref.theme_mode"
    const val ONBOARDING_COMPLETED = "pref.onboarding_completed"
    const val LANGUAGE = "pref.language"
    const val TX_INDICATOR_STYLE = "pref.tx_indicator_style"
    const val TX_SHOW_CATEGORY = "pref.tx_show_category"
    const val TX_SHOW_NOTE = "pref.tx_show_note"
    const val TX_DENSITY = "pref.tx_density"
    const val TX_SHOW_DAILY_SUMS = "tx_show_daily_sums"

    // User last-selected settings (persisted across sessions)
    const val TX_LAST_FILTER = "pref.tx_last_filter"
    const val OVERVIEW_LAST_TAB = "pref.overview_last_tab"

    // Selected wallet / account
    const val SELECTED_ACCOUNT_ID = "pref.selected_account_id"

    // Default transaction type for new transactions
    const val DEFAULT_TX_TYPE = "pref.default_tx_type"

    // Payment mode feature toggle
    const val PAYMENT_MODE_ENABLED = "pref.payment_mode_enabled"

    // Auto-backup
    const val AUTO_BACKUP_ENABLED = "pref.auto_backup_enabled"
    const val LAST_BACKUP_TIME_MS = "pref.last_backup_time_ms"
    const val LAST_BACKUP_PATH = "pref.last_backup_path"
    const val AUTO_BACKUP_DIR_URI = "pref.auto_backup_dir_uri"
}
