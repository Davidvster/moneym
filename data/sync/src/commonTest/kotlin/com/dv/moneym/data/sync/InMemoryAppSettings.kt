package com.dv.moneym.data.sync

import com.dv.moneym.core.datastore.AppSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class InMemoryAppSettings : AppSettings {
    private val values = mutableMapOf<String, Any?>()

    override fun getString(key: String, defaultValue: String?): String? =
        values[key] as? String ?: defaultValue

    override fun putString(key: String, value: String) {
        values[key] = value
    }

    override fun getBoolean(key: String, defaultValue: Boolean): Boolean =
        values[key] as? Boolean ?: defaultValue

    override fun putBoolean(key: String, value: Boolean) {
        values[key] = value
    }

    override fun getInt(key: String, defaultValue: Int): Int =
        values[key] as? Int ?: defaultValue

    override fun putInt(key: String, value: Int) {
        values[key] = value
    }

    override fun getLong(key: String, defaultValue: Long): Long =
        values[key] as? Long ?: defaultValue

    override fun putLong(key: String, value: Long) {
        values[key] = value
    }

    override fun remove(key: String) {
        values.remove(key)
    }

    override fun observeBoolean(key: String, defaultValue: Boolean): Flow<Boolean> =
        flowOf(getBoolean(key, defaultValue))

    override fun observeString(key: String, defaultValue: String?): Flow<String?> =
        flowOf(getString(key, defaultValue))

    override fun observeInt(key: String, defaultValue: Int): Flow<Int> =
        flowOf(getInt(key, defaultValue))
}
