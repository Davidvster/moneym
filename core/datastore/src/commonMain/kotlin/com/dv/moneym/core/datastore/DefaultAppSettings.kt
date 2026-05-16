package com.dv.moneym.core.datastore

import com.russhwolf.settings.Settings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart

class DefaultAppSettings(private val settings: Settings) : AppSettings {

    // Emits the key name whenever any value is written, so observers can re-read.
    private val changesFlow = MutableSharedFlow<String>(extraBufferCapacity = 64)

    override fun getString(key: String, defaultValue: String?): String? =
        if (settings.hasKey(key)) settings.getString(key, defaultValue ?: "") else defaultValue

    override fun putString(key: String, value: String) {
        settings.putString(key, value)
        changesFlow.tryEmit(key)
    }

    override fun getBoolean(key: String, defaultValue: Boolean): Boolean =
        settings.getBoolean(key, defaultValue)

    override fun putBoolean(key: String, value: Boolean) {
        settings.putBoolean(key, value)
        changesFlow.tryEmit(key)
    }

    override fun getInt(key: String, defaultValue: Int): Int =
        settings.getInt(key, defaultValue)

    override fun putInt(key: String, value: Int) {
        settings.putInt(key, value)
        changesFlow.tryEmit(key)
    }

    override fun getLong(key: String, defaultValue: Long): Long =
        settings.getLong(key, defaultValue)

    override fun putLong(key: String, value: Long) {
        settings.putLong(key, value)
        changesFlow.tryEmit(key)
    }

    override fun remove(key: String) {
        settings.remove(key)
        changesFlow.tryEmit(key)
    }

    override fun observeBoolean(key: String, defaultValue: Boolean): Flow<Boolean> =
        changesFlow
            .filter { it == key }
            .onStart { emit(key) }
            .map { getBoolean(key, defaultValue) }

    override fun observeString(key: String, defaultValue: String?): Flow<String?> =
        changesFlow
            .filter { it == key }
            .onStart { emit(key) }
            .map { getString(key, defaultValue) }

    override fun observeInt(key: String, defaultValue: Int): Flow<Int> =
        changesFlow
            .filter { it == key }
            .onStart { emit(key) }
            .map { getInt(key, defaultValue) }
}
