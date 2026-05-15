package com.dv.moneym.core.testing

import com.dv.moneym.core.datastore.AppSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

class FakeAppSettings : AppSettings {
    private val store = MutableStateFlow<Map<String, Any?>>(emptyMap())

    override fun getString(key: String, defaultValue: String?): String? =
        store.value[key] as? String ?: defaultValue
    override fun putString(key: String, value: String) = store.update { it + (key to value) }
    override fun getBoolean(key: String, defaultValue: Boolean): Boolean =
        store.value[key] as? Boolean ?: defaultValue
    override fun putBoolean(key: String, value: Boolean) = store.update { it + (key to value) }
    override fun getInt(key: String, defaultValue: Int): Int =
        (store.value[key] as? Number)?.toInt() ?: defaultValue
    override fun putInt(key: String, value: Int) = store.update { it + (key to value) }
    override fun getLong(key: String, defaultValue: Long): Long =
        (store.value[key] as? Number)?.toLong() ?: defaultValue
    override fun putLong(key: String, value: Long) = store.update { it + (key to value) }
    override fun remove(key: String) = store.update { it - key }
    override fun observeBoolean(key: String, defaultValue: Boolean): Flow<Boolean> =
        store.map { it[key] as? Boolean ?: defaultValue }
    override fun observeString(key: String, defaultValue: String?): Flow<String?> =
        store.map { it[key] as? String ?: defaultValue }
    override fun observeInt(key: String, defaultValue: Int): Flow<Int> =
        store.map { (it[key] as? Number)?.toInt() ?: defaultValue }
}
