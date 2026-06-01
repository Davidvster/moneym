package com.dv.moneym.data.sync

import com.dv.moneym.core.model.PaymentMode
import com.dv.moneym.core.model.PaymentModeId
import com.dv.moneym.data.transactions.PaymentModeRepository
import com.dv.moneym.data.transactions.PaymentModeSyncRow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlin.time.Instant

class FakePaymentModeRepository : PaymentModeRepository {
    private val _modes = MutableStateFlow<List<PaymentMode>>(emptyList())
    private var nextId = 1L

    private val syncIds = mutableMapOf<Long, String>()

    private val tombstoned = mutableSetOf<Long>()
    private val updatedAtOverrides = mutableMapOf<Long, Long>()

    fun addAll(modes: List<PaymentMode>) = _modes.update { it + modes }

    override fun observeAll(): Flow<List<PaymentMode>> =
        _modes.map { list -> list.filter { it.id.value !in tombstoned } }

    override suspend fun getById(id: PaymentModeId): PaymentMode? =
        _modes.value.find { it.id == id }

    override suspend fun create(name: String) {
        // not needed for sync export tests
    }

    override suspend fun rename(id: PaymentModeId, name: String) {
        _modes.update { list -> list.map { if (it.id == id) it.copy(name = name) else it } }
    }

    override suspend fun delete(id: PaymentModeId) {
        tombstoned.add(id.value)
    }

    override suspend fun markDeletedBySyncId(syncId: String, now: Long) {
        idForSyncId(syncId)?.let { id ->
            tombstoned.add(id)
            updatedAtOverrides[id] = now
        }
    }

    override suspend fun reviveBySyncId(syncId: String, now: Long) {
        idForSyncId(syncId)?.let { id ->
            tombstoned.remove(id)
            updatedAtOverrides[id] = now
        }
    }

    override suspend fun exportForSync(): List<PaymentModeSyncRow> =
        _modes.value.map { m ->
            PaymentModeSyncRow(
                id = m.id.value,
                syncId = syncIdFor(m.id.value),
                name = m.name,
                deleted = m.id.value in tombstoned,
                createdAt = m.createdAt.toEpochMilliseconds(),
                updatedAt = updatedAtOverrides[m.id.value] ?: m.updatedAt.toEpochMilliseconds(),
            )
        }

    override suspend fun upsertFromSync(row: PaymentModeSyncRow): Long {
        val syncId = requireNotNull(row.syncId)
        val existingId = syncIds.entries.firstOrNull { it.value == syncId }?.key
        return if (existingId == null) {
            val id = nextId++
            _modes.update { it + row.toDomain(id) }
            syncIds[id] = syncId
            id
        } else {
            _modes.update { list -> list.map { if (it.id.value == existingId) row.toDomain(existingId) else it } }
            existingId
        }
    }

    private fun PaymentModeSyncRow.toDomain(id: Long) = PaymentMode(
        id = PaymentModeId(id),
        name = name,
        createdAt = Instant.fromEpochMilliseconds(createdAt),
        updatedAt = Instant.fromEpochMilliseconds(updatedAt),
    )

    private fun syncIdFor(id: Long): String = syncIds.getOrPut(id) { "sync-pm-$id" }

    private fun idForSyncId(syncId: String): Long? {
        _modes.value.forEach { syncIdFor(it.id.value) }
        return syncIds.entries.firstOrNull { it.value == syncId }?.key
    }
}
