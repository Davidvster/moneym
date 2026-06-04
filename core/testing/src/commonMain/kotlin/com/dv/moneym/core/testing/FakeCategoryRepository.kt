package com.dv.moneym.core.testing

import com.dv.moneym.core.model.Category
import com.dv.moneym.core.model.CategoryId
import com.dv.moneym.core.model.TransactionType
import com.dv.moneym.data.categories.CategoryRepository
import com.dv.moneym.data.categories.CategorySyncRow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlin.time.Instant

class FakeCategoryRepository : CategoryRepository {
    private val _categories = MutableStateFlow<List<Category>>(emptyList())
    private var nextId = 1L

    private val syncIds = mutableMapOf<Long, String>()

    private val tombstoned = mutableSetOf<Long>()
    private val updatedAtOverrides = mutableMapOf<Long, Long>()

    val categories: List<Category> get() = _categories.value.filter { it.id.value !in tombstoned }

    fun addAll(categories: List<Category>) = _categories.update { it + categories }

    override fun observeAll(): Flow<List<Category>> =
        _categories.map { list -> list.filter { it.id.value !in tombstoned }.sortedBy { it.sortOrder } }
    override fun observeActive(): Flow<List<Category>> =
        _categories.map { it.filter { c -> !c.archived && c.id.value !in tombstoned }.sortedBy { it.sortOrder } }
    override suspend fun getById(id: CategoryId): Category? = _categories.value.find { it.id == id }
    override suspend fun count(): Long = _categories.value.count { it.id.value !in tombstoned }.toLong()
    override suspend fun insert(category: Category): CategoryId {
        val id = CategoryId(nextId++)
        _categories.update { it + category.copy(id = id) }
        return id
    }
    override suspend fun update(category: Category) {
        _categories.update { list -> list.map { if (it.id == category.id) category else it } }
    }
    override suspend fun reorder(orderedIds: List<CategoryId>) {
        val orderMap = orderedIds.mapIndexed { index, id -> id to index }.toMap()
        _categories.update { list ->
            list.map { cat -> orderMap[cat.id]?.let { cat.copy(sortOrder = it) } ?: cat }
        }
    }
    override suspend fun delete(id: CategoryId) {
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

    override suspend fun deleteAll() {
        _categories.value = emptyList()
        syncIds.clear()
        tombstoned.clear()
        updatedAtOverrides.clear()
    }

    override suspend fun exportForSync(): List<CategorySyncRow> =
        _categories.value.map { c ->
            CategorySyncRow(
                id = c.id.value,
                syncId = syncIdFor(c.id.value),
                name = c.name,
                iconKey = c.iconKey,
                colorHex = c.colorHex,
                isUserCreated = c.isUserCreated,
                archived = c.archived,
                categoryType = c.type.name,
                deleted = c.id.value in tombstoned,
                createdAt = c.createdAt.toEpochMilliseconds(),
                updatedAt = updatedAtOverrides[c.id.value] ?: c.updatedAt.toEpochMilliseconds(),
                sortOrder = c.sortOrder,
            )
        }

    override suspend fun upsertFromSync(row: CategorySyncRow): Long {
        val syncId = requireNotNull(row.syncId)
        val existingId = syncIds.entries.firstOrNull { it.value == syncId }?.key
        return if (existingId == null) {
            val id = nextId++
            _categories.update { it + row.toDomain(id) }
            syncIds[id] = syncId
            id
        } else {
            _categories.update { list -> list.map { if (it.id.value == existingId) row.toDomain(existingId) else it } }
            existingId
        }
    }

    private fun CategorySyncRow.toDomain(id: Long) = Category(
        id = CategoryId(id),
        name = name,
        iconKey = iconKey,
        colorHex = colorHex,
        isUserCreated = isUserCreated,
        archived = archived,
        createdAt = Instant.fromEpochMilliseconds(createdAt),
        updatedAt = Instant.fromEpochMilliseconds(updatedAt),
        type = if (categoryType == "INCOME") TransactionType.INCOME else TransactionType.EXPENSE,
        sortOrder = sortOrder,
    )

    private fun syncIdFor(id: Long): String = syncIds.getOrPut(id) { "sync-cat-$id" }

    private fun idForSyncId(syncId: String): Long? {
        _categories.value.forEach { syncIdFor(it.id.value) }
        return syncIds.entries.firstOrNull { it.value == syncId }?.key
    }
}
