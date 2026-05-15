package com.dv.moneym.core.datastore

import com.russhwolf.settings.Settings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

// Phase 1: non-reactive observe methods using flowOf.
// Phase 2 (data:settings) upgrades these to ObservableSettings + toFlowSettings()
// for live updates when prefs change.
class DefaultAppSettings(private val settings: Settings) : AppSettings {

    override fun getString(key: String, defaultValue: String?): String? =
        if (settings.hasKey(key)) settings.getString(key, defaultValue ?: "") else defaultValue

    override fun putString(key: String, value: String) = settings.putString(key, value)

    override fun getBoolean(key: String, defaultValue: Boolean): Boolean =
        settings.getBoolean(key, defaultValue)

    override fun putBoolean(key: String, value: Boolean) = settings.putBoolean(key, value)

    override fun getInt(key: String, defaultValue: Int): Int =
        settings.getInt(key, defaultValue)

    override fun putInt(key: String, value: Int) = settings.putInt(key, value)

    override fun getLong(key: String, defaultValue: Long): Long =
        settings.getLong(key, defaultValue)

    override fun putLong(key: String, value: Long) = settings.putLong(key, value)

    override fun remove(key: String) = settings.remove(key)

    override fun observeBoolean(key: String, defaultValue: Boolean): Flow<Boolean> =
        flowOf(getBoolean(key, defaultValue))

    override fun observeString(key: String, defaultValue: String?): Flow<String?> =
        flowOf(getString(key, defaultValue))

    override fun observeInt(key: String, defaultValue: Int): Flow<Int> =
        flowOf(getInt(key, defaultValue))
}
