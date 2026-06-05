package com.dv.moneym.data.sync

import com.dv.moneym.core.datastore.AppSettings
import com.dv.moneym.core.datastore.PrefKeys
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json

/**
 * Durable store for an unresolved password/encryption [SyncConflict], persisted as a JSON blob in
 * [PrefKeys.SYNC_CONFLICT_BLOB] so it survives restarts and surfaces in the UI. While set, sync
 * pull/push are paused.
 */
class SyncConflictStore(
    private val appSettings: AppSettings,
) {
    private val json = Json { ignoreUnknownKeys = true }

    val conflict: Flow<SyncConflict?> =
        appSettings.observeString(PrefKeys.SYNC_CONFLICT_BLOB).map { decode(it) }

    fun current(): SyncConflict? = decode(appSettings.getString(PrefKeys.SYNC_CONFLICT_BLOB))

    fun set(conflict: SyncConflict) {
        appSettings.putString(PrefKeys.SYNC_CONFLICT_BLOB, json.encodeToString(conflict))
    }

    fun clear() = appSettings.remove(PrefKeys.SYNC_CONFLICT_BLOB)

    private fun decode(raw: String?): SyncConflict? {
        if (raw.isNullOrBlank()) return null
        return runCatching { json.decodeFromString<SyncConflict>(raw) }.getOrNull()
    }
}
