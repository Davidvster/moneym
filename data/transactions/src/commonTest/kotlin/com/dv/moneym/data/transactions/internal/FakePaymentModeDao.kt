package com.dv.moneym.data.transactions.internal

import com.dv.moneym.data.transactions.db.PaymentModeDao
import com.dv.moneym.data.transactions.db.PaymentModeEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

internal class FakePaymentModeDao : PaymentModeDao {

    val rows = MutableStateFlow<List<PaymentModeEntity>>(emptyList())
    private var nextId = 1L

    private fun mutate(block: (List<PaymentModeEntity>) -> List<PaymentModeEntity>) {
        rows.value = block(rows.value)
    }

    override fun selectAll(): Flow<List<PaymentModeEntity>> =
        rows.map { list -> list.filter { !it.deleted }.sortedBy { it.id } }

    override suspend fun selectById(id: Long): PaymentModeEntity? =
        rows.value.firstOrNull { it.id == id }

    override suspend fun selectBySyncId(syncId: String): PaymentModeEntity? =
        rows.value.firstOrNull { it.syncId == syncId }

    override suspend fun insert(entity: PaymentModeEntity): Long {
        val id = nextId++
        mutate { it + entity.copy(id = id) }
        return id
    }

    override suspend fun updateName(id: Long, name: String, updatedAt: Long) {
        mutate { list -> list.map { if (it.id == id) it.copy(name = name, updatedAt = updatedAt) else it } }
    }

    override suspend fun update(entity: PaymentModeEntity) {
        mutate { list -> list.map { if (it.id == entity.id) entity else it } }
    }

    override suspend fun deleteById(id: Long) {
        mutate { list -> list.filterNot { it.id == id } }
    }

    override suspend fun softDeleteById(id: Long, now: Long) {
        mutate { list -> list.map { if (it.id == id) it.copy(deleted = true, updatedAt = now) else it } }
    }

    override suspend fun markDeletedBySyncId(syncId: String, now: Long) {
        mutate { list -> list.map { if (it.syncId == syncId) it.copy(deleted = true, updatedAt = now) else it } }
    }

    override suspend fun touchBySyncId(syncId: String, now: Long) {
        mutate { list -> list.map { if (it.syncId == syncId) it.copy(updatedAt = now) else it } }
    }

    override suspend fun countAll(): Long = rows.value.count { !it.deleted }.toLong()

    override suspend fun selectAllForSync(): List<PaymentModeEntity> = rows.value
}
