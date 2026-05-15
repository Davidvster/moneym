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
    const val DEFAULT_CURRENCY = "pref.default_currency"
    const val THEME_MODE = "pref.theme_mode"
    const val PIN_ENABLED = "pref.pin_enabled"
    const val BIOMETRIC_ENABLED = "pref.biometric_enabled"
    const val BACKGROUND_LOCK_SECONDS = "pref.background_lock_seconds"
    const val LAST_EXPORT_AT = "pref.last_export_at"
    const val ONBOARDING_COMPLETED = "pref.onboarding_completed"
}
