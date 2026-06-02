package com.dv.moneym.data.transactions.internal

import com.dv.moneym.data.transactions.db.RecurringTransactionDao
import com.dv.moneym.data.transactions.db.RecurringTransactionEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

internal class FakeRecurringTransactionDao : RecurringTransactionDao {

    val rows = MutableStateFlow<List<RecurringTransactionEntity>>(emptyList())
    private var nextId = 1L

    private fun mutate(block: (List<RecurringTransactionEntity>) -> List<RecurringTransactionEntity>) {
        rows.value = block(rows.value)
    }

    override fun selectAll(): Flow<List<RecurringTransactionEntity>> =
        rows.map { list -> list.filter { !it.deleted }.sortedByDescending { it.createdAt } }

    override suspend fun selectById(id: Long): RecurringTransactionEntity? =
        rows.value.firstOrNull { it.id == id }

    override suspend fun selectBySyncId(syncId: String): RecurringTransactionEntity? =
        rows.value.firstOrNull { it.syncId == syncId }

    override suspend fun selectAllForSync(): List<RecurringTransactionEntity> = rows.value

    override suspend fun insert(entity: RecurringTransactionEntity): Long {
        val id = nextId++
        mutate { it + entity.copy(id = id) }
        return id
    }

    override suspend fun update(entity: RecurringTransactionEntity) {
        mutate { list -> list.map { if (it.id == entity.id) entity else it } }
    }

    override suspend fun updateCursor(id: Long, date: String, updatedAt: Long) {
        mutate { list ->
            list.map { if (it.id == id) it.copy(lastMaterializedDate = date, updatedAt = updatedAt) else it }
        }
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

    override suspend fun deleteAll() {
        rows.value = emptyList()
    }
}
