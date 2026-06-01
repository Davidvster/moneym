package com.dv.moneym.data.sync

import com.dv.moneym.core.datastore.AppSettings
import com.dv.moneym.core.datastore.PrefKeys
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json

/**
 * Durable store for pending (unconfirmed) remote deletions. Persisted as a JSON blob in
 * [PrefKeys.PENDING_DELETION_BLOB]. Recomputed (replaced) on every pull, so it is self-healing.
 */
class PendingDeletionStore(
    private val appSettings: AppSettings,
) {
    private val json = Json { ignoreUnknownKeys = true }

    val pending: Flow<List<PendingDeletion>> =
        appSettings.observeString(PrefKeys.PENDING_DELETION_BLOB).map { decode(it) }

    val hasPending: Flow<Boolean> = pending.map { it.isNotEmpty() }

    val count: Flow<Int> = pending.map { it.size }

    fun replaceAll(list: List<PendingDeletion>) {
        if (list.isEmpty()) {
            appSettings.remove(PrefKeys.PENDING_DELETION_BLOB)
        } else {
            appSettings.putString(PrefKeys.PENDING_DELETION_BLOB, json.encodeToString(list))
        }
    }

    fun current(): List<PendingDeletion> =
        decode(appSettings.getString(PrefKeys.PENDING_DELETION_BLOB))

    fun clear() = appSettings.remove(PrefKeys.PENDING_DELETION_BLOB)

    private fun decode(raw: String?): List<PendingDeletion> {
        if (raw.isNullOrBlank()) return emptyList()
        return runCatching { json.decodeFromString<List<PendingDeletion>>(raw) }.getOrElse { emptyList() }
    }
}
