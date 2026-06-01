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
    const val OVERVIEW_LAST_FILTER = "pref.overview_last_filter"

    // Selected wallet / account
    const val SELECTED_ACCOUNT_ID = "pref.selected_account_id"

    // Default transaction type for new transactions
    const val DEFAULT_TX_TYPE = "pref.default_tx_type"

    // User default currency for new accounts
    const val DEFAULT_CURRENCY = "pref.default_currency"

    // Payment mode feature toggle
    const val PAYMENT_MODE_ENABLED = "pref.payment_mode_enabled"

    // Show currency symbol (€) instead of code (EUR) app-wide
    const val USE_CURRENCY_SYMBOL = "pref.use_currency_symbol"

    // Show upcoming (grayed) recurring transaction occurrences in tx list
    const val SHOW_PENDING_RECURRING_TX = "pref.show_pending_recurring_tx"

    // Auto-backup
    const val AUTO_BACKUP_ENABLED = "pref.auto_backup_enabled"
    const val LAST_BACKUP_TIME_MS = "pref.last_backup_time_ms"
    const val LAST_BACKUP_PATH = "pref.last_backup_path"
    const val AUTO_BACKUP_DIR_URI = "pref.auto_backup_dir_uri"
    const val LOCAL_BACKUP_ENCRYPT = "pref.local_backup_encrypt"

    // Remote (Google Drive) backup
    const val AUTO_REMOTE_BACKUP_ENABLED = "pref.auto_remote_backup_enabled"
    const val LAST_REMOTE_BACKUP_TIME_MS = "pref.last_remote_backup_time_ms"
    const val REMOTE_BACKUP_PROVIDER_ID = "pref.remote_backup_provider_id"
    const val REMOTE_BACKUP_ACCOUNT_EMAIL = "pref.remote_backup_account_email"
    const val REMOTE_BACKUP_ENCRYPT = "pref.remote_backup_encrypt"
    const val LAST_LOCAL_MUTATION_MS = "pref.last_local_mutation_ms"

    // Cross-device sync
    const val DEVICE_ID = "pref.device_id"
    const val DEVICE_NAME = "pref.device_name"
    const val LAST_SYNC_PULL_MS = "pref.last_sync_pull_ms"
    const val CROSS_DEVICE_SYNC_ENABLED = "pref.cross_device_sync_enabled"
    const val PENDING_DELETION_BLOB = "pref.pending_deletion_blob"

    // AI analysis grounding mode (SNAPSHOT / TOOLS)
    const val AI_GROUNDING_MODE = "pref.ai_grounding_mode"
}
